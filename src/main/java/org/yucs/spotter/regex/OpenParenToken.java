package org.yucs.spotter.regex;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

class OpenParenToken extends Token {
    final int pos;

    @SuppressWarnings({"FieldCanBeLocal", "unused"}) // might need it later
    final private CloseParenToken matched;

    final private List<Token> alts = new LinkedList<>();

    OpenParenToken(int p, CloseParenToken cp) {
        pos = p;
        matched = cp;
    }

    void addAlt(Token t) {
        alts.add(t);
    }

    @Override
    public boolean match(Matcher m) throws RegexException {
        Iterator<Token> it = alts.iterator();

        m.setParenPosition(pos, m.getTextPosition());

        while (it.hasNext()) {
            Token t = it.next();
            if (t.match(m))
                if (next.match(m))
                    return true;
        }

        return false;
    }
}