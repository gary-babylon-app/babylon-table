/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.transform.dsl;

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
            if (Character.isWhitespace(c) || c == ',' || c == ':' || c == '#')
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
