package com.dnd.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps windows stacked in the order they were opened: closing a window (or popup dialog)
 * always returns to the window it was opened from, never to some other window that
 * happens to be behind it.
 */
public final class WindowOrder {
    private static final Map<Window, WeakReference<Window>> OPENERS = new WeakHashMap<>();
    private static Window lastFocused;
    private static boolean installed;

    private WindowOrder() {
    }

    /** Starts tracking every window the app shows. Call once on the FX thread. */
    public static void install() {
        if (installed) return;
        installed = true;
        Window.getWindows().forEach(WindowOrder::track);
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                for (Window added : change.getAddedSubList()) {
                    // Only real windows: menus, tooltips and combo popups come and go constantly.
                    if (!(added instanceof Stage)) continue;
                    if (!OPENERS.containsKey(added)) {
                        Window opener = ownerOf(added);
                        OPENERS.put(added, new WeakReference<>(opener != null ? opener : lastFocused));
                    }
                    track(added);
                }
                for (Window removed : change.getRemoved()) {
                    if (removed instanceof Stage) returnToOpener(removed);
                }
            }
        });
    }

    /** The window the user is currently working in - the right owner for anything it opens. */
    public static Window current() {
        for (Window w : Window.getWindows()) {
            if (w instanceof Stage && w.isFocused()) return w;
        }
        if (lastFocused != null && lastFocused.isShowing()) return lastFocused;
        return null;
    }

    /** Makes the dialog belong to the current window, so it stays above it and returns to it. */
    public static void adopt(Dialog<?> dialog) {
        if (dialog.getOwner() != null || dialog.isShowing()) return;
        Window owner = current();
        if (owner != null) dialog.initOwner(owner);
    }

    /** Makes the new stage belong to the current window (must be called before it is shown). */
    public static void adopt(Stage stage) {
        adopt(stage, current());
    }

    public static void adopt(Stage stage, Window owner) {
        if (stage.getOwner() != null || stage.isShowing() || owner == null || owner == stage) return;
        stage.initOwner(owner);
    }

    private static void track(Window window) {
        if (!(window instanceof Stage)) return;
        window.focusedProperty().addListener((o, was, focused) -> {
            if (focused) lastFocused = window;
        });
        if (window.isFocused()) lastFocused = window;
    }

    private static Window openerOf(Window window) {
        WeakReference<Window> ref = OPENERS.get(window);
        return ref == null ? null : ref.get();
    }

    private static Window ownerOf(Window window) {
        return window instanceof Stage stage ? stage.getOwner() : null;
    }

    private static void returnToOpener(Window closed) {
        Window opener = openerOf(closed);
        // If the opener is gone too, fall back along the chain to whatever opened it.
        for (int guard = 0; opener != null && !opener.isShowing() && guard < 32; guard++) {
            opener = openerOf(opener);
        }
        if (lastFocused == closed) lastFocused = opener;
        if (opener == null || !(opener instanceof Stage stage)) return;
        Platform.runLater(() -> {
            if (!stage.isShowing()) return;
            if (stage.isIconified()) stage.setIconified(false);
            stage.toFront();
            stage.requestFocus();
        });
    }
}
