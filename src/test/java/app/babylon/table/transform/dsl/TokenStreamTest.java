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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TokenStreamTest
{
    @Test
    void shouldTokenizeStringCleanupExamples()
    {
        String line = "strip Name";
        assertCanTokenize(line);

        line = "strip Name into CleanName";
        assertCanTokenize(line);

        line = "clean Name";
        assertCanTokenize(line);

        line = "clean Name into CleanName";
        assertCanTokenize(line);
    }

    @Test
    void shouldTokenizeStringCaseExamples()
    {
        String line = "uppercase Symbol";
        assertCanTokenize(line);

        line = "uppercase Symbol into UpperSymbol";
        assertCanTokenize(line);

        line = "lowercase Email";
        assertCanTokenize(line);

        line = "lowercase Email into LowerEmail";
        assertCanTokenize(line);
    }

    @Test
    void shouldTokenizeCopyAndConstantExamples()
    {
        String line = "copy Symbol into DisplaySymbol";
        assertCanTokenize(line);

        line = "constant metadata.tableName into SourceFileName";
        assertCanTokenize(line);

        line = "constant metadata.description into SourceDescription";
        assertCanTokenize(line);

        line = "constant 'BrokerA' into SourceSystem";
        assertCanTokenize(line);

        line = "constant 'USD' into PaymentCurrency";
        assertCanTokenize(line);

        line = "constant '1' as Int into SourceRank";
        assertCanTokenize(line);

        line = "constant 'USD' as Currency into PaymentCurrency";
        assertCanTokenize(line);
    }

    @Test
    void shouldTokenizeTypeConversionExamples()
    {
        String line = "convert AmountText to Double into Amount";
        assertCanTokenize(line);

        line = "convert AmountText to Double by exact into Amount";
        assertCanTokenize(line);

        line = "convert Description to Decimal by firstIn into Amount";
        assertCanTokenize(line);

        line = "convert Description to Decimal by lastIn into Amount";
        assertCanTokenize(line);

        line = "convert Description to Decimal by onlyIn into Amount";
        assertCanTokenize(line);

        line = "convert QuantityText to Int into Quantity";
        assertCanTokenize(line);

        line = "convert UnitsText to Long into Units";
        assertCanTokenize(line);

        line = "convert TradeDateText to Date into TradeDate";
        assertCanTokenize(line);

        line = "convert TradeDateText to Date using YMD";
        assertCanTokenize(line);

        line = "convert TradeDateText to Date using YMD into TradeDate";
        assertCanTokenize(line);

        line = "convert TradeDateText to Date into TradeDate using YMD";
        assertCanTokenize(line);

        line = "convert TradeDateText to Date using YMD by exact into TradeDate";
        assertCanTokenize(line);

        line = "convert TradeDateText to Date into TradeDate by firstIn using YMD";
        assertCanTokenize(line);

        line = "convert AmountText to Decimal into Amount";
        assertCanTokenize(line);

        line = "convert AmountText to String into AmountDisplay";
        assertCanTokenize(line);

        line = "convert AmountText to Double into Amount";
        assertCanTokenize(line);

        line = "convert QuantityText to Int into Quantity";
        assertCanTokenize(line);
    }

    @Test
    void shouldTokenizeSubstringAndPartExamples()
    {
        String line = "take left 3 from SortCode into BankCode";
        assertCanTokenize(line);

        line = "take right 4 from AccountNumber into AccountSuffix";
        assertCanTokenize(line);

        line = "take substring 0, 3 from Isin into CountryCode";
        assertCanTokenize(line);

        line = "take before '-' from TradeReference into TradePrefix";
        assertCanTokenize(line);

        line = "take after '-' from TradeReference into TradeSuffix";
        assertCanTokenize(line);
    }

    @Test
    void shouldTokenizePrefixSuffixExamples()
    {
        String line = "prefix Symbol with 'LSE:' into BloombergSymbol";
        assertCanTokenize(line);

        line = "suffix Isin with '-OLD' into LegacyIsin";
        assertCanTokenize(line);

        line = "replace ',' with '' in AmountText into CleanAmountText";
        assertCanTokenize(line);

        line = "replace all '\\s+' with ' ' in Description into CleanDescription";
        assertCanTokenize(line);
    }

    @Test
    void shouldTokenizeSplitAndConcatExamples()
    {
        String line = "split Split on / into QuantityBefore, QuantityAfter";
        assertCanTokenize(line);

        line = "concat AccountType, Country, AccountNumber using '|' into AccountKey";
        assertCanTokenize(line);

        line = "concat AccountType, Country, AccountNumber into AccountKey";
        assertCanTokenize(line);

        line = "concat AccountType, Country, AccountNumber using '' into AccountKey";
        assertCanTokenize(line);
    }

    @Test
    void shouldTokenizeClassifyAndExtractExamples()
    {
        String line = "classify Description matching 'Dividend|Distribution' into IsIncome as 'Y' else 'N'";
        assertCanTokenize(line);

        line = "extract from Description matching '.*\\(([^)]+)\\)' into Symbol";
        assertCanTokenize(line);
    }

    @Test
    void shouldTokenizeSubstituteExamples()
    {
        String line = "substitute Status using 'A':'Active', 'I':'Inactive' default 'Other' into NormalisedStatus";
        assertCanTokenize(line);

        line = "substitute Status using 'A':'Active', 'I':'Inactive' into NormalisedStatus";
        assertCanTokenize(line);
    }

    @Test
    void shouldTokenizeCoalesceExamples()
    {
        String line = "coalesce FirstName, PreferredName, LegalName into DisplayName";
        assertCanTokenize(line);

        line = "coalesce FirstAmount, SecondAmount, ThirdAmount into ChosenAmount";
        assertCanTokenize(line);
    }

    @Test
    void shouldTokenizeDecimalOperatorExamples()
    {
        String line = "add DebitAmount and CreditAmount into NetAmount";
        assertCanTokenize(line);

        line = "subtract Fees from GrossAmount into NetAmount";
        assertCanTokenize(line);

        line = "multiply Quantity by Price into MarketValue";
        assertCanTokenize(line);

        line = "divide Amount by Quantity into UnitPrice";
        assertCanTokenize(line);
    }

    @Test
    void shouldTokenizeAbsExamples()
    {
        String line = "abs Amount into AbsoluteAmount";
        assertCanTokenize(line);

        line = "negate Amount into SignedAmount";
        assertCanTokenize(line);

        line = "negate Quantity when Type is Buy";
        assertCanTokenize(line);

        line = "normalise Amount into NormalisedAmount";
        assertCanTokenize(line);

        line = "round Amount to 2 by halfUp into RoundedAmount";
        assertCanTokenize(line);

        line = "round Amount using Currency by halfUp into RoundedAmount";
        assertCanTokenize(line);
    }

    @Test
    void shouldTokenizeSplitStatement()
    {
        String line = "split Split on / into QuantityBefore, QuantityAfter";

        TokenStream tokens = TokenStream.of(line);

        assertEquals(TokenType.WORD, tokens.peek().type());
        tokens.expectWord("split");
        assertEquals("Split", tokens.expectValue());
        tokens.expectWord("on");
        assertEquals("/", tokens.expectValue());
        tokens.expectWord("into");
        assertEquals("QuantityBefore", tokens.expectValue());
        assertTrue(tokens.match(TokenType.COMMA));
        assertEquals("QuantityAfter", tokens.expectValue());
        assertTrue(tokens.isAtEnd());
    }

    @Test
    void shouldTokenizeSubstitutionMapPairs()
    {
        String line = "substitute Status using 'N/A':'Not Applicable', I:Inactive default Other into Normalised";

        TokenStream tokens = TokenStream.of(line);

        tokens.expectWord("substitute");
        assertEquals("Status", tokens.expectValue());
        tokens.expectWord("using");
        assertEquals("N/A", tokens.expectValue());
        tokens.expect(TokenType.COLON);
        assertEquals("Not Applicable", tokens.expectValue());
        tokens.expect(TokenType.COMMA);
        assertEquals("I", tokens.expectValue());
        tokens.expect(TokenType.COLON);
        assertEquals("Inactive", tokens.expectValue());
    }

    @Test
    void shouldSkipCommentsOutsideStrings()
    {
        String line = "classify Description matching '#dividend' into IsIncome # comment";

        TokenStream tokens = TokenStream.of(line);

        tokens.expectWord("classify");
        assertEquals("Description", tokens.expectValue());
        tokens.expectWord("matching");
        assertEquals("#dividend", tokens.expectValue());
        tokens.expectWord("into");
        assertEquals("IsIncome", tokens.expectValue());
        assertTrue(tokens.isAtEnd());
    }

    @Test
    void shouldTokenizeDoubleQuotedStrings()
    {
        String line = "substitute Status using \"N/A\":Missing default Other";

        TokenStream tokens = TokenStream.of(line);

        tokens.expectWord("substitute");
        assertEquals("Status", tokens.expectValue());
        tokens.expectWord("using");
        assertEquals("N/A", tokens.expectValue());
        tokens.expect(TokenType.COLON);
        assertEquals("Missing", tokens.expectValue());
    }

    @Test
    void shouldPeekMatchAndExpect()
    {
        String line = "convert AmountText to Double into Amount";

        TokenStream tokens = TokenStream.of(line);

        assertEquals("convert", tokens.peek().value());
        assertEquals("AmountText", tokens.peek(1).value());
        assertTrue(tokens.matchWord("convert"));
        assertFalse(tokens.matchWord("split"));
        assertEquals("AmountText", tokens.expectValue());
        tokens.expectWord("to");
        assertEquals("Double", tokens.expectValue());
    }

    @Test
    void shouldExpectLiteral()
    {
        String line = "extract from Description matching '.*\\(([^)]+)\\)' into Symbol";

        TokenStream tokens = TokenStream.of(line);

        tokens.expectWord("extract");
        tokens.expectWord("from");
        assertEquals("Description", tokens.expectValue());
        tokens.expectWord("matching");
        assertEquals(".*\\(([^)]+)\\)", tokens.expectLiteral());
    }

    @Test
    void shouldRejectWordWhenLiteralIsRequired()
    {
        String line = "extract from Description matching Dividend into Symbol";

        TokenStream tokens = TokenStream.of(line);

        tokens.expectWord("extract");
        tokens.expectWord("from");
        tokens.expectValue();
        tokens.expectWord("matching");
        assertThrows(TransformDslException.class, tokens::expectLiteral);
    }

    @Test
    void shouldIgnoreLineEndingsBeforeEndOfFile()
    {
        String line = "strip Name\r\n";

        TokenStream tokens = TokenStream.of(line);

        tokens.expectWord("strip");
        assertEquals("Name", tokens.expectValue());
        assertTrue(tokens.isAtEnd());
        assertEquals(TokenType.EOF, tokens.peek().type());
    }

    @Test
    void shouldReportUnexpectedToken()
    {
        String line = "split Split";

        TokenStream tokens = TokenStream.of(line);

        tokens.expectWord("split");
        tokens.expectValue();
        TransformDslException exception = assertThrows(TransformDslException.class, () -> tokens.expectWord("on"));

        assertEquals(12, exception.column());
    }

    @Test
    void shouldRejectUnclosedSingleQuotedString()
    {
        String line = "classify Description matching 'dividend";

        assertThrows(TransformDslException.class, () -> TokenStream.of(line));
    }

    @Test
    void shouldRejectUnclosedDoubleQuotedString()
    {
        String line = "classify Description matching \"dividend";

        assertThrows(TransformDslException.class, () -> TokenStream.of(line));
    }

    private static void assertCanTokenize(String line)
    {
        TokenStream tokens = TokenStream.of(line);

        assertFalse(tokens.isAtEnd());
        while (!tokens.isAtEnd())
        {
            tokens.next();
        }
        assertEquals(TokenType.EOF, tokens.peek().type());
    }
}
