package com.dnd.cli.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Test double for {@link ConsoleIO} that records everything printed and
 * replays a scripted sequence of input lines. Lets CLI interaction logic be
 * unit tested without capturing real stdout or blocking on real stdin.
 */
public class FakeConsoleIO implements ConsoleIO {
    private final Deque<String> scriptedInput;
    private final List<String> output = new ArrayList<>();

    public FakeConsoleIO(String... scriptedLines) {
        this.scriptedInput = new ArrayDeque<>(List.of(scriptedLines));
    }

    @Override
    public void print(String text) {
        output.add(text);
    }

    @Override
    public void println(String text) {
        output.add(text + "\n");
    }

    @Override
    public void println() {
        output.add("\n");
    }

    @Override
    public String readLine() {
        if (scriptedInput.isEmpty()) {
            throw new NoSuchElementException("No more scripted input lines available.");
        }
        return scriptedInput.poll();
    }

    public List<String> getOutput() {
        return output;
    }

    public String getAllOutput() {
        return String.join("", output);
    }

    /** Appends another scripted input line, for tests that need to react to earlier output before scripting more input. */
    public void queueInput(String line) {
        scriptedInput.add(line);
    }
}

