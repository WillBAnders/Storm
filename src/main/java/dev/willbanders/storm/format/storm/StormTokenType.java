package dev.willbanders.storm.format.storm;

import dev.willbanders.storm.format.Token;

public enum StormTokenType implements Token.Type {
    COMMENT,
    NEWLINE,
    IDENTIFIER,
    INTEGER,
    DECIMAL,
    CHARACTER,
    STRING,
    OPERATOR,
}
