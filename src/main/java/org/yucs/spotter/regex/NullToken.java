package org.yucs.spotter.regex;

class NullToken extends Token {
    final static NullToken Instance = new NullToken();

    @Override
    boolean match(Matcher m) throws RegexException {
        return true;
    }

    @Override
    Token reverse() {
        return this;
    }
}
