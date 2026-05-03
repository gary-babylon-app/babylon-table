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

public record Token(TokenType type, String value, int position)
{
    public boolean is(TokenType type)
    {
        return this.type == type;
    }

    public boolean isWord(String word)
    {
        return this.type == TokenType.WORD && this.value.equalsIgnoreCase(word);
    }

    public boolean isValue()
    {
        return this.type == TokenType.WORD || this.type == TokenType.LITERAL;
    }

    public boolean isOperator(String operator)
    {
        return this.type == TokenType.OPERATOR && this.value.equals(operator);
    }

    public int column()
    {
        return this.position + 1;
    }
}
