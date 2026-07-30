package com.dnd.cli.core;

import java.util.Scanner;

/**
 * Default {@link ConsoleIO} implementation backed by real {@code System.out}
 * and a {@link Scanner} over {@code System.in}.
 */
public class SystemConsoleIO implements ConsoleIO {
    private final Scanner scanner;

    public SystemConsoleIO(Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public void print(String text) {
        System.out.print(text);
    }

    @Override
    public void println(String text) {
        System.out.println(text);
    }

    @Override
    public void println() {
        System.out.println();
    }

    @Override
    public String readLine() {
        return scanner.nextLine();
    }
}

