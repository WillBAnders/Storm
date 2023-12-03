package dev.willbanders.storm.format.json;

import dev.willbanders.storm.format.Token;

public enum JsonTokenType implements Token.Type {
    IDENTIFIER,
    INTEGER,
    DECIMAL,
    STRING,
    OPERATOR,
}
