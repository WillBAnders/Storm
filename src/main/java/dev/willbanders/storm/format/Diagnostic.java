package dev.willbanders.storm.format;

import com.google.common.collect.ImmutableList;

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class Diagnostic {

    private final String input;
    private final String summary;
    private final String details;
    private final Range range;
    private final ImmutableList<Range> context;

    private Diagnostic(Builder builder) {
        input = builder.input;
        summary = builder.summary;
        details = builder.details;
        range = builder.range;
        context = ImmutableList.copyOf(builder.context);
    }

    public String getInput() {
        return input;
    }

    public String getSummary() {
        return summary;
    }

    public String getDetails() {
        return details;
    }

    public Range getRange() {
        return range;
    }

    public ImmutableList<Range> getContext() {
        return context;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Error: ").append(summary).append("\n");
        builder.append("Line ").append(range.line).append(", ");
        if (range.length == 1) {
            builder.append("Character ").append(range.column).append("\n");
        } else {
            builder.append("Characters ").append(range.column).append("-").append(range.column + range.length - 1).append("\n");
        }
        SortedSet<Range> ranges = new TreeSet<>(Comparator.comparing(Range::getLine));
        ranges.add(range);
        ranges.addAll(context);
        int digits = String.valueOf(ranges.last().line).length();
        for (Diagnostic.Range range : ranges) {
            for (int i = String.valueOf(range.getLine()).length(); i <= digits; i++) {
                builder.append(" ");
            }
            builder.append(range.line).append(" | ");
            int start = range.index;
            while (start > 0 && input.charAt(start - 1) != '\n' && input.charAt(start - 1) != '\r') {
                start--;
            }
            int end = range.index + range.length;
            while (end < input.length() && input.charAt(end) != '\n' && input.charAt(end) != '\r') {
                end++;
            }
            builder.append(input, start, end).append("\n");
            if (range.line == this.range.line) {
                for (int i = 0; i <= digits; i++) {
                    builder.append(" ");
                }
                builder.append(" | ");
                for (int i = 1; i < this.range.column; i++) {
                    builder.append(" ");
                }
                for (int i = 0; i < this.range.length; i++) {
                    builder.append("^");
                }
                builder.append("\n");
            }
        }
        return builder.append(details).toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String input;
        private String summary;
        private String details;
        private Range range;
        private List<Range> context;

        private Builder() {}

        public Builder input(String input) {
            this.input = input;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder details(String details) {
            this.details = details;
            return this;
        }

        public Builder range(Range range) {
            this.range = range;
            return this;
        }

        public Builder context(List<Range> context) {
            this.context = context;
            return this;
        }

        public Diagnostic build() {
            return new Diagnostic(this);
        }

    }

    public static Range range(int index, int line, int column, int length) {
        return new Range(index, line, column, length);
    }

    public static final class Range {

        private final int index;
        private final int line;
        private final int column;
        private final int length;

        private Range(int index, int line, int column, int length) {
            this.index = index;
            this.line = line;
            this.column = column;
            this.length = length;
        }

        public int getIndex() {
            return index;
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }

        public int getLength() {
            return length;
        }

    }

}
