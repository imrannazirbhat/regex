package org.yucs.spotter.regex;

class LookAheadExpressionToken extends Token implements TestableToken {
    final private NormalExpressionToken t;
    final private boolean positive;

    LookAheadExpressionToken(NormalExpressionToken net, boolean p) {
        t = net;
        positive = p;
    }

    @Override
    boolean match(Matcher m) throws RegexException {
        int pos = m.getTextPosition();

        boolean ret = t.match(m);

        m.setTextPosition(pos);

        if (positive) {
            if (!ret)
                return false;
        } else {
            if (ret)
                return false;
        }

        return next.match(m);
    }
}
