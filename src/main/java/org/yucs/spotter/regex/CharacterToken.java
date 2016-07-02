package org.yucs.spotter.regex;

import java.util.Stack;

class CharacterToken extends Token {
    private final CharacterClass c;
    private final Quantifier q;

    CharacterToken(CharacterClass c, Quantifier q) {
        this.c = c;
        this.q = q;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (c == null) throw new AssertionError();
        sb.append("CharacterClass: ").append(c.toString()).append(", ");

        if (q != null) {
            sb.append("Quantifier: min = ").append(q.min).append(" max = ").append(q.max);
        } else {
            sb.append("Quantifier: null");
        }

        return sb.toString();
    }

    @Override
    public boolean match(Regex r, String text, int text_pos, Stack<CloseParenToken> closeParen) throws RegexException {
        if (q != null)
            return matchRange(r, text, text_pos, closeParen);

        // 1. if we have no more text (i.e. text_pos >= text.length() short cut and return false
        // 2. see if this character matches, if not short cut and return false
        // 3. if passed above, continue matching rest of regex against rest of text
        return text_pos < text.length() && c.match(text.charAt(text_pos)) && r.match(next, text, text_pos + 1, closeParen);
    }

    /**
     * @param text        the text we are matching
     * @param text_pos    the current position within the text
     * @param closeParen  a stack of closeParens, ignored here, just carried through to recursive calls
     * @return            could we finish matching the regex from here
     * @throws RegexException
     */
    private boolean matchRange(Regex r, String text, int text_pos, Stack<CloseParenToken> closeParen) throws RegexException {
        // match minimum
        for (int i = 0; i < q.min; i++) {
            if (text.length() > text_pos && c.match(text.charAt(text_pos))) {
                text_pos++;
            } else {
                return false;
            }
        }

        if (q.greedy) {
            int consumed = 0;
            // as greedy, match as much as possible
            for(int i=0; i < q.max-q.min || q.max == -1; i++) {
                if (text.length() > text_pos && c.match(text.charAt(text_pos))) {
                    consumed++;
                    text_pos++;
                } else {
                    break;
                }
            }

            // try to match from here (consumed as much as possible) and if can't, back off one by one until minimum match
            for(int i=consumed; i >= 0; i--) {
                if (r.match(next, text, text_pos, closeParen)) {
                    return true;
                }
                text_pos--;
            }

            return false;
        }

        // not greedy
        for(int i=0; i <= q.max-q.min || q.max == -1; i++) {
            // try to match from here (starting with minimum match, adding one by one if can't match)
            if (r.match(next, text, text_pos, closeParen)) { // matched the minimum, see if the rest of text matches the rest of the regex
                return true;
            }
            // couldn't match rest of regex against rest of text
            // so now try to match one more (till maximum/infinity) before we retry
            if (text.length() != text_pos && c.match(text.charAt(text_pos))) {
                text_pos++; // eat one character from text if it matches regex character
            } else {
                return false; // reached end of text without matching the rest of the regex
            }
        }

        return false; // finished the max quantifier without finding regex match with the rest of the text
    }
}
