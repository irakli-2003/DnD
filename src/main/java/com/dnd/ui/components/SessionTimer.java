package com.dnd.ui.components;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

/**
 * A small stopwatch / countdown strip that DMs can drop into any window.
 *
 * <p>Both directions matter at the table for different reasons: counting up tracks how long
 * a session or a single combat has run, while counting down enforces a turn time limit or
 * the duration of a spell. One control does both so the DM doesn't have to pick a widget.</p>
 */
public class SessionTimer extends HBox {

    private final Label display = new Label("00:00");
    private final Spinner<Integer> minutesSpinner = new Spinner<>(0, 240, 1);
    private final Button startPause = new Button("Start");
    private final Timeline ticker;

    private int elapsedSeconds;
    private boolean countingDown;
    private boolean running;

    public SessionTimer() {
        super(6);
        setAlignment(Pos.CENTER_LEFT);

        display.getStyleClass().add("timer-display");
        display.setTooltip(new Tooltip("Session timer"));

        minutesSpinner.setPrefWidth(70);
        minutesSpinner.setEditable(true);
        minutesSpinner.setTooltip(new Tooltip("Minutes to count down from"));

        startPause.getStyleClass().add("dnd-button");
        startPause.setOnAction(e -> toggle());

        Button up = themed("Count Up", "Start a stopwatch from zero");
        up.setOnAction(e -> startCountUp());

        Button down = themed("Count Down", "Count down from the chosen number of minutes");
        down.setOnAction(e -> startCountDown(minutesSpinner.getValue() * 60));

        Button reset = themed("Reset", "Stop and clear the timer");
        reset.setOnAction(e -> reset());

        // A one-second tick is accurate enough for a table timer and costs nothing; using a
        // Timeline (rather than a background thread) keeps every update on the FX thread.
        ticker = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        ticker.setCycleCount(Animation.INDEFINITE);

        Label caption = new Label("Timer:");
        caption.getStyleClass().add("body-label");
        getChildren().addAll(caption, display, startPause, up, down, minutesSpinner, reset);
        refreshDisplay();
    }

    private Button themed(String text, String tip) {
        Button b = new Button(text);
        b.getStyleClass().add("dnd-button");
        b.setTooltip(new Tooltip(tip));
        return b;
    }

    public void startCountUp() {
        countingDown = false;
        elapsedSeconds = 0;
        start();
    }

    public void startCountDown(int seconds) {
        countingDown = true;
        elapsedSeconds = Math.max(0, seconds);
        start();
    }

    private void start() {
        running = true;
        startPause.setText("Pause");
        display.getStyleClass().remove("timer-expired");
        ticker.playFromStart();
        refreshDisplay();
    }

    public void toggle() {
        if (running) {
            running = false;
            ticker.pause();
            startPause.setText("Resume");
        } else {
            running = true;
            ticker.play();
            startPause.setText("Pause");
        }
    }

    public void reset() {
        running = false;
        ticker.stop();
        elapsedSeconds = 0;
        startPause.setText("Start");
        display.getStyleClass().remove("timer-expired");
        refreshDisplay();
    }

    /** Stops the underlying animation so a closed window doesn't leave a timer running. */
    public void dispose() {
        ticker.stop();
    }

    private void tick() {
        if (countingDown) {
            elapsedSeconds--;
            if (elapsedSeconds <= 0) {
                elapsedSeconds = 0;
                running = false;
                ticker.stop();
                startPause.setText("Start");
                if (!display.getStyleClass().contains("timer-expired")) {
                    display.getStyleClass().add("timer-expired");
                }
            }
        } else {
            elapsedSeconds++;
        }
        refreshDisplay();
    }

    private void refreshDisplay() {
        display.setText(format(elapsedSeconds));
    }

    /** Formats a whole number of seconds as mm:ss, or h:mm:ss once it passes an hour. */
    public static String format(int totalSeconds) {
        int safe = Math.max(0, totalSeconds);
        int hours = safe / 3600;
        int minutes = (safe % 3600) / 60;
        int seconds = safe % 60;
        if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
        return String.format("%02d:%02d", minutes, seconds);
    }

    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isCountingDown() {
        return countingDown;
    }
}
