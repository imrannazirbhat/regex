package org.yucs.spotter.regex;

import java.util.*;

class Tokenizer {
    private final String regex;

    int captureCount = 0;

    private Token t = null;

    Tokenizer(String r) {
        regex = r;
    }

    Token tokenize() throws RegexException {
        if (t == null)
            t = createNormalExpressionToken(captureCount++, 0, regex.length());

        return t;
    }

    private Token tokenize(int regex_pos, int end, boolean nextToken) throws RegexException {
        Token t = null;

        if (regex_pos >= end) {
            return NullToken.Instance;
        }

        // Non quantifiable regex token, return in their block
        // Quantifiable regex tokens, return at end when quantifier is parsed if present
        switch (regex.charAt(regex_pos)) {
            case '^':
                // start of line anchor token
                t = new AnchorToken('^');
                if (nextToken)
                    t.next = tokenize(regex_pos + 1, end, true);
                return t;
            case '$':
                // end of line anchor token
                t = new AnchorToken('$');
                if (nextToken)
                    t.next = tokenize(regex_pos + 1, end, true);
                return t;
            case '\\':
                if (regex_pos+1 < regex.length() && (regex.charAt(regex_pos+1) == 'b' || regex.charAt(regex_pos+1) == 'B')) {
                    // word boundary anchor token
                    t = new AnchorToken(regex.charAt(regex_pos+1));
                    if (nextToken)
                        t.next = tokenize(regex_pos+2, end, true);
                    return t;
                }

                if (regex_pos+1 < regex.length() && Character.isDigit(regex.charAt(regex_pos+1))) {
                    // Backreference token
                    regex_pos++;
                    int val = Character.digit(regex.charAt(regex_pos), 10);
                    while (Character.isDigit(regex.charAt(regex_pos+1))) {
                        regex_pos++;
                        val *= 10;
                        val += Character.digit(regex.charAt(regex_pos), 10);
                    }

                    t = new BackReferenceToken(val);
                    regex_pos++;
                }
                break;
            case '(':
                int endParen = findMatchingParen(regex_pos);
                if (regex.charAt(regex_pos+1) == '?') {
                    switch (regex.charAt(regex_pos+2)) {
                        case '>':
                            t = createAtomicExpressionToken(regex_pos+3, endParen);
                            break;
                        case '=':
                            t = createLookAheadExpressionToken(regex_pos+3, endParen);
                            break;
                        default:
                            t = createIfThenElseToken(regex_pos + 2, endParen);
                            if (nextToken)
                                t.next = tokenize(endParen + 1, end, true);
                            return t;
//                        default:
//                            throw new RegexException("Unknown grouping type");
                    }
                } else {
                    t = createNormalExpressionToken(captureCount++, regex_pos+1, endParen);
                }
                regex_pos = endParen + 1;
        }

        if (t == null) { //fall through position
            // regular character matching token
            CharacterClassFactory ccf = CharacterClassFactory.getCharacterClass(regex, regex_pos);

            t = new CharacterToken(ccf.c);
            regex_pos = ccf.regex_pos;
        }

        t.next = NullToken.Instance;

        QuantifierFactory qf = QuantifierFactory.parse(regex, regex_pos);
        if (qf != null) {
            t = new QuantifierToken(qf.q, t);
            regex_pos = qf.regex_pos;
        }

        if (nextToken)
            t.next = tokenize(regex_pos, end, true);
        return t;
    }

    private int findMatchingParen(int start) throws RegexException {
        Stack<Integer> parens = new Stack<>();

        // by reading the first character first, can make the switch in loop simpler as don't have to check if index 0
        parens.push(start);

        boolean slashIsEscape = false;
        for(int i = start+1; i < regex.length(); i++) {
            switch (regex.charAt(i)) {
                case '\\':
                    slashIsEscape = !slashIsEscape;
                    break;
                case '(':
                    if (!slashIsEscape || regex.charAt(i-1) != '\\') {
                        parens.push(i);
                    }
                    break;
                case ')':
                    if (!slashIsEscape || regex.charAt(i-1) != '\\') {
                        parens.pop();
                    }
                    if (parens.size() == 0)
                        return i;
                    break;
            }
        }

        throw new RegexException("unbalanced parens");
    }

    private List<Integer> findPipes(int start, int end) throws RegexException {
        LinkedList<Integer> parens = new LinkedList<>();
        LinkedList<Integer> pipes = new LinkedList<>();

        // by reading the first character first, can make the switch in loop simpler as don't have to check if index 0
        boolean slashIsEscape = false;
        switch (regex.charAt(start)) {
            case '\\':
                slashIsEscape = true;
                break;
            case '|':
                pipes.add(start);
                break;
            case '(':
                parens.add(start);
                break;
            case ')':
                throw new RegexException("unbalanced parens");
        }

        // As we are searching for alternates, only find pipes that are not in sub expressions (i.e. surrounded by ()
        for(int i=start+1; i < end; i++) { //last element should be a paren
            switch (regex.charAt(i)) {
                case '\\':
                    slashIsEscape = !slashIsEscape;
                    break;
                case '(':
                    if (!slashIsEscape || regex.charAt(i-1) != '\\') {
                        parens.push(i);
                    }
                    break;
                case ')':
                    if (!slashIsEscape || regex.charAt(i-1) != '\\') {
                        if (parens.size() == 0)
                            throw new RegexException("unbalanced parens");
                        parens.pop();
                    }
                    break;
                case '|':
                    if ((!slashIsEscape || regex.charAt(i-1) != '\\') && parens.size() == 0) {
                        pipes.addLast(i);
                    }
            }
        }

        return pipes;
    }

    private Token createIfThenElseToken(int regex_pos, int endParen) throws RegexException {
        int ifEndParen = regex_pos;
        if (regex.charAt(regex_pos) == '(')
            ifEndParen = findMatchingParen(regex_pos);

        Token ifToken = tokenize(regex_pos, endParen, false); //createLookAheadExpressionToken(regex_pos+3, ifEndParen);
        Token thenToken;
        Token elseToken;

        List<Integer> pipes = findPipes(ifEndParen+1, endParen);
        switch (pipes.size()) {
            case 0:
                thenToken = createNormalExpressionToken(-1, ifEndParen+1, endParen);
                elseToken = NullToken.Instance;
                break;
            case 1:
                thenToken = createNormalExpressionToken(-1, ifEndParen+1, pipes.get(0));
                elseToken = createNormalExpressionToken(-1, pipes.get(0)+1, endParen);
                break;
            default:
                throw new RegexException("Expected only one pipe in if/then/else token parsing");
        }

        return new IfThenElseToken(ifToken, thenToken, elseToken);
    }

    private Token createLookAheadExpressionToken(int regex_pos, int endParen) throws RegexException {
        NormalExpressionToken net = createNormalExpressionToken(-1, regex_pos, endParen);
        return new LookAheadExpressionToken(net);
    }


    private NormalExpressionToken createNormalExpressionToken(int capturePos, int regex_pos, int endParen) throws RegexException {
        NormalExpressionToken t = new NormalExpressionToken(capturePos);
        parseExpression(t, regex_pos, endParen);

        return t;
    }


    private Token createAtomicExpressionToken(int regex_pos, int endParen) throws RegexException {
        AtomicExpressionToken t = new AtomicExpressionToken();
        parseExpression(t, regex_pos, endParen);

        return t;

    }

    private void parseExpression(ExpressionToken t, int regex_pos, int endParen) throws RegexException {
        List<Integer> pipes = findPipes(regex_pos, endParen);

        for (int pipe : pipes) {
            t.addAlt(tokenize(regex_pos, pipe, true));
            regex_pos = pipe + 1;
        }

        t.addAlt(tokenize(regex_pos, endParen, true));
    }
}