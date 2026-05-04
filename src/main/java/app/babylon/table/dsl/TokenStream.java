/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.dsl;

import java.util.ArrayList;
import java.util.List;

public final class TokenStream
{
    private final List<Token> tokens;
    private int index;

    private TokenStream(List<Token> tokens)
    {
        this.tokens = List.copyOf(tokens);
    }

    public static TokenStream of(CharSequence line)
    {
        return new TokenStream(tokenize(line));
    }

    public Token peek()
    {
        return peek(0);
    }

    public Token peek(int offset)
    {
        int tokenIndex = this.index + Math.max(0, offset);
        return tokenIndex >= this.tokens.size() ? this.tokens.get(this.tokens.size() - 1) : this.tokens.get(tokenIndex);
    }

    public Token next()
    {
        Token token = peek();
        if (!token.is(TokenType.EOF))
        {
            ++this.index;
        }
        return token;
    }

    public boolean isAtEnd()
    {
        return peek().is(TokenType.EOF);
    }

    public boolean match(TokenType type)
    {
        if (peek().is(type))
        {
            next();
            return true;
        }
        return false;
    }

    public boolean matchWord(String word)
    {
        if (peek().isWord(word))
        {
            next();
            return true;
        }
        return false;
    }

    public boolean matchOperator(String operator)
    {
        if (peek().isOperator(operator))
        {
            next();
            return true;
        }
        return false;
    }

    public Token expect(TokenType type)
    {
        Token token = next();
        if (!token.is(type))
        {
            throw error("Expected " + type + " but found " + token.type(), token);
        }
        return token;
    }

    public void expectWord(String word)
    {
        Token token = next();
        if (!token.isWord(word))
        {
            throw error("Expected '" + word + "'", token);
        }
    }

    public String expectValue()
    {
        Token token = next();
        if (!token.isValue())
        {
            throw error("Expected value", token);
        }
        return token.value();
    }

    public String expectLiteral()
    {
        Token token = next();
        if (!token.is(TokenType.LITERAL))
        {
            throw error("Expected literal", token);
        }
        return token.value();
    }

    public String expectOperator()
    {
        Token token = next();
        if (!token.is(TokenType.OPERATOR))
        {
            throw error("Expected operator", token);
        }
        return token.value();
    }

    public void expectEnd()
    {
        if (!isAtEnd())
        {
            throw error("Expected end of statement", peek());
        }
    }

    private TransformDslException error(String message, Token token)
    {
        return new TransformDslException(message, token.position());
    }

    private static List<Token> tokenize(CharSequence line)
    {
        List<Token> tokens = new ArrayList<>();
        int length = line == null ? 0 : line.length();
        int i = 0;
        while (i < length)
        {
            char c = line.charAt(i);
            if (c == '\r')
            {
                tokens.add(new Token(TokenType.CARRIAGE_RETURN, "\r", i));
                ++i;
                continue;
            }
            if (c == '\n')
            {
                tokens.add(new Token(TokenType.LINE_FEED, "\n", i));
                ++i;
                continue;
            }
            if (Character.isWhitespace(c))
            {
                ++i;
                continue;
            }
            if (c == '#')
            {
                break;
            }
            if (c == ',')
            {
                tokens.add(new Token(TokenType.COMMA, ",", i));
                ++i;
                continue;
            }
            if (c == ':')
            {
                tokens.add(new Token(TokenType.COLON, ":", i));
                ++i;
                continue;
            }
            if (c == '{')
            {
                tokens.add(new Token(TokenType.LEFT_CURLY, "{", i));
                ++i;
                continue;
            }
            if (c == '}')
            {
                tokens.add(new Token(TokenType.RIGHT_CURLY, "}", i));
                ++i;
                continue;
            }
            if (c == '[')
            {
                tokens.add(new Token(TokenType.LEFT_SQUARE, "[", i));
                ++i;
                continue;
            }
            if (c == ']')
            {
                tokens.add(new Token(TokenType.RIGHT_SQUARE, "]", i));
                ++i;
                continue;
            }
            if (c == '(')
            {
                tokens.add(new Token(TokenType.LEFT_PAREN, "(", i));
                ++i;
                continue;
            }
            if (c == ')')
            {
                tokens.add(new Token(TokenType.RIGHT_PAREN, ")", i));
                ++i;
                continue;
            }
            if (c == '+' && !isSignedNumberStart(line, i))
            {
                tokens.add(new Token(TokenType.PLUS, "+", i));
                ++i;
                continue;
            }
            if (c == '-' && !isSignedNumberStart(line, i))
            {
                tokens.add(new Token(TokenType.MINUS, "-", i));
                ++i;
                continue;
            }
            if (c == ';')
            {
                tokens.add(new Token(TokenType.SEMICOLON, ";", i));
                ++i;
                continue;
            }
            if (c == '/')
            {
                tokens.add(new Token(TokenType.FORWARD_SLASH, "/", i));
                ++i;
                continue;
            }
            if (c == '\\')
            {
                tokens.add(new Token(TokenType.BACK_SLASH, "\\", i));
                ++i;
                continue;
            }
            if (isOperatorStart(c))
            {
                i = readOperator(line, i, tokens);
                continue;
            }
            if (c == '"' || c == '\'')
            {
                i = readLiteral(line, i, tokens);
                continue;
            }
            i = readWord(line, i, tokens);
        }
        tokens.add(new Token(TokenType.EOF, "", length));
        return tokens;
    }

    private static int readWord(CharSequence line, int start, List<Token> tokens)
    {
        int i = start;
        while (i < line.length())
        {
            char c = line.charAt(i);
            if (Character.isWhitespace(c) || c == ',' || c == ':' || c == '{' || c == '}' || c == '[' || c == ']'
                    || c == '(' || c == ')' || c == ';' || c == '/' || c == '\\' || c == '#')
            {
                break;
            }
            if ((c == '+' || c == '-') && !(i == start && isSignedNumberStart(line, i)))
            {
                break;
            }
            if (isOperatorStart(c))
            {
                break;
            }
            if (c == '"' || c == '\'')
            {
                throw new TransformDslException("Unexpected quote inside word", i);
            }
            ++i;
        }
        tokens.add(new Token(TokenType.WORD, line.subSequence(start, i).toString(), start));
        return i;
    }

    private static boolean isSignedNumberStart(CharSequence line, int start)
    {
        return start + 1 < line.length() && (line.charAt(start) == '+' || line.charAt(start) == '-')
                && Character.isDigit(line.charAt(start + 1));
    }

    private static int readOperator(CharSequence line, int start, List<Token> tokens)
    {
        char c = line.charAt(start);
        if (start + 1 < line.length())
        {
            char next = line.charAt(start + 1);
            if ((c == '=' && next == '=') || (c == '!' && next == '=') || (c == '<' && next == '=')
                    || (c == '>' && next == '=') || (c == '<' && next == '>'))
            {
                tokens.add(new Token(TokenType.OPERATOR, line.subSequence(start, start + 2).toString(), start));
                return start + 2;
            }
        }
        if (c == '=' || c == '<' || c == '>')
        {
            tokens.add(new Token(TokenType.OPERATOR, Character.toString(c), start));
            return start + 1;
        }
        throw new TransformDslException("Unknown operator", start);
    }

    private static boolean isOperatorStart(char c)
    {
        return c == '=' || c == '!' || c == '<' || c == '>';
    }

    private static int readLiteral(CharSequence line, int start, List<Token> tokens)
    {
        char quote = line.charAt(start);
        StringBuilder value = new StringBuilder();
        int i = start + 1;
        while (i < line.length())
        {
            char c = line.charAt(i++);
            if (c == quote)
            {
                tokens.add(new Token(TokenType.LITERAL, value.toString(), start));
                return i;
            }
            if (c == '\\')
            {
                i = readEscape(line, i, value);
            }
            else
            {
                value.append(c);
            }
        }
        throw new TransformDslException("Unterminated literal", start);
    }

    private static int readEscape(CharSequence line, int index, StringBuilder value)
    {
        if (index >= line.length())
        {
            throw new TransformDslException("Unterminated escape sequence", index - 1);
        }
        char escaped = line.charAt(index++);
        switch (escaped)
        {
            case '"' :
                value.append('"');
                break;
            case '\'' :
                value.append('\'');
                break;
            case '\\' :
                value.append('\\');
                break;
            case 'n' :
                value.append('\n');
                break;
            case 'r' :
                value.append('\r');
                break;
            case 't' :
                value.append('\t');
                break;
            default :
                value.append('\\').append(escaped);
                break;
        }
        return index;
    }
}
