package dev.willbanders.storm.format;

public final class ParseException extends RuntimeException {

    private final Diagnostic diagnostic;

    public ParseException(Diagnostic diagnostic) {
        super(diagnostic.getSummary());
        this.diagnostic = diagnostic;
    }

    public Diagnostic getDiagnostic() {
        return diagnostic;
    }

}
