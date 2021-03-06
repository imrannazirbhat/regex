package org.yucs.spotter.regex;

// Positive and Negative Look behind matching
// Determines if something is matchable (or not) in the backwards direction from this location
// state is reset after the attempted match

import java.util.Stack;

class LookBehindExpressionToken extends Token implements TestableToken {
    final private NormalExpressionToken t;
    final private boolean positive;

    LookBehindExpressionToken(NormalExpressionToken net, boolean p) {
        t = net;
        positive = p;
    }

    @Override
    boolean match(Matcher m) throws RegexException {
        int pos = m.getTextPosition();
        m.setDirection(-1);

        // Empty stack as only matters that its string of tokens match
        Stack<Token> savedState = m.saveAndResetNextStack();

        boolean ret = t.match(m);

        m.restoreNextStack(savedState);

        m.setDirection(1);
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
