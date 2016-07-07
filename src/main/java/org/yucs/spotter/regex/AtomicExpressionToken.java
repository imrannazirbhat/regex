package org.yucs.spotter.regex;

import java.util.Iterator;
import java.util.Stack;

// AtomicGroups are alternates where if an alternate matches, even if it fails later, the rest aren't tried

class AtomicExpressionToken extends ExpressionToken {
    @Override
    boolean match(Matcher m) throws RegexException {
        Iterator<Token> it = altIterator();
        int start = m.getTextPosition();

        while (it.hasNext()) {
            Stack<Token> savedStack = m.saveAndResetNextStack();

            Token t = it.next();
            boolean ret = t.match(m);

            if (ret) {
                m.restoreNextStack(savedStack);
                return next.match(m);
            }

            m.restoreNextStack(savedStack);
            m.setTextPosition(start);
        }

        return false;
    }
}
