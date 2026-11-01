package com.example.fileindexer.shell;

import com.example.fileindexer.model.Drive;
import com.example.fileindexer.model.SearchType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.stereotype.Component;

/**
 * Interactive questions asked on the console (drive selection, search type, search text).
 */
@Component
public class Prompter {

    private final Terminal terminal;
    private final LineReader reader;

    public Prompter(Terminal terminal) {
        this.terminal = terminal;
        this.reader = LineReaderBuilder.builder().terminal(terminal).build();
    }

    public void println(String line) {
        terminal.writer().println(line);
        terminal.writer().flush();
    }

    public String ask(String prompt) {
        String colored = new AttributedString(prompt,
                AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN)).toAnsi(terminal);
        return reader.readLine(colored);
    }

    /**
     * Uses {@code preset} (from --drives) when given, otherwise asks until a valid selection is entered.
     */
    public List<Drive> selectDrives(List<Drive> available, String preset) {
        if (available.isEmpty()) {
            throw new IllegalStateException("No readable drives found.");
        }
        if (preset != null && !preset.isBlank()) {
            return parseSelection(preset, available);
        }
        println("Available drives:");
        for (int i = 0; i < available.size(); i++) {
            println("  [%d] %s".formatted(i + 1, available.get(i).label()));
        }
        while (true) {
            String input = ask("Select drives (numbers or names separated by comma, 'all'): ");
            try {
                return parseSelection(input, available);
            } catch (IllegalArgumentException e) {
                println(e.getMessage());
            }
        }
    }

    public SearchType selectSearchType(String preset) {
        if (preset != null && !preset.isBlank()) {
            return parseType(preset);
        }
        println("Search type:");
        println("  [1] regular - words, \"phrases\", wildcards (*, ?), AND / OR / NOT");
        println("  [2] regex   - Lucene regular expression matched against indexed words");
        while (true) {
            String input = ask("Select search type: ");
            try {
                return parseType(input);
            } catch (IllegalArgumentException e) {
                println(e.getMessage());
            }
        }
    }

    public String askText(String preset) {
        if (preset != null && !preset.isBlank()) {
            return preset;
        }
        while (true) {
            String input = ask("Text to search: ");
            if (input != null && !input.isBlank()) {
                return input.trim();
            }
        }
    }

    static SearchType parseType(String input) {
        String v = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        return switch (v) {
            case "1", "regular" -> SearchType.REGULAR;
            case "2", "regex" -> SearchType.REGEX;
            default -> throw new IllegalArgumentException("Enter 1/regular or 2/regex.");
        };
    }

    static List<Drive> parseSelection(String input, List<Drive> available) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Nothing selected.");
        }
        if (input.trim().equalsIgnoreCase("all")) {
            return List.copyOf(available);
        }
        Set<Drive> selected = new LinkedHashSet<>();
        for (String token : input.split("[,;\\s]+")) {
            if (token.isBlank()) {
                continue;
            }
            selected.add(resolve(token, available));
        }
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Nothing selected.");
        }
        return new ArrayList<>(selected);
    }

    private static Drive resolve(String token, List<Drive> available) {
        if (token.chars().allMatch(Character::isDigit)) {
            int index = Integer.parseInt(token);
            if (index < 1 || index > available.size()) {
                throw new IllegalArgumentException("No drive number " + index + ".");
            }
            return available.get(index - 1);
        }
        String t = token.toLowerCase(Locale.ROOT);
        for (Drive d : available) {
            String id = d.id().toLowerCase(Locale.ROOT);
            // "c", "c:" and "c:\" all select C:\
            if (id.equals(t) || id.startsWith(t.endsWith(":") ? t : t + ":")) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown drive '" + token + "'.");
    }
}
