package org.yucs.spotter.regex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class Matcher {
    private ArrayList<String> groups;
    private ArrayList<Integer> parenPosition;
    private String text;
    private int text_pos;
    final private int parenCount;

    private final Token t;

    Matcher(Tokenizer tokenizer) throws RegexException {
        t = tokenizer.tokenize();
        parenCount = tokenizer.captureCount;
    }

    /**
     * Returns a boolean that says if the text matched against the Regex
     *
     * @param text the text to match against the regex
     * @return true/false if the text matches against the regex
     * @throws RegexException
     */
    public boolean match(String text) throws RegexException {
        groups        = new ArrayList<>(Arrays.asList(new String[parenCount]));
        parenPosition = new ArrayList<>(Arrays.asList(new Integer[parenCount]));
        this.text     = text;

        for(int i=0; i < text.length() || i == 0; i++) { //need to test empty text string too
            text_pos = i;
            if (match(t)) {
                return true;
            }
        }

        return false;
    }

    /**
     * The main internal matching function
     *
     * @param t           the current regex token we are matching against
     * @return            true/false if we were able to finish matching the regex from here
     * @throws RegexException
     */
    boolean match(Token t) throws RegexException {
        // if we matched every token, we've finished regex, so it passes
        return t == null || t.match(this);

    }

    void recordGroup(int paren, int text_end) {
        groups.set(paren, text.substring(parenPosition.get(paren), text_end));
    }

    int getTextPosition() {
        return text_pos;
    }

    void setTextPosition(int pos) {
        text_pos = pos;
    }

    String getText() { return text; }

    String getGroup(int pos) { return groups.get(pos); }

    void setParenPosition(int paren, int pos) { parenPosition.set(paren, pos); }

    List<String> getGroups() { return groups; }
}
