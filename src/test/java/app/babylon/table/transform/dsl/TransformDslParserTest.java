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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.transform.Transform;
import java.util.Map;

class TransformDslParserTest
{
    private static final TransformDslParser PARSER = TransformDslParser.standard();

    @Test
    void shouldParseStringCleanupExamples()
    {
        String line = "strip Name";
        assertParses(line);

        line = "strip Name into CleanName";
        assertParses(line);

        line = "clean whitespace in Name";
        assertParses(line);

        line = "clean whitespace in Name into CleanName";
        assertParses(line);
    }

    @Test
    void shouldParseStringCaseExamples()
    {
        String line = "uppercase Symbol";
        assertParses(line);

        line = "uppercase Symbol into UpperSymbol";
        assertParses(line);

        line = "lowercase Email";
        assertParses(line);

        line = "lowercase Email into LowerEmail";
        assertParses(line);
    }

    @Test
    void shouldParseCopyAndConstantExamples()
    {
        String line = "copy Symbol into DisplaySymbol";
        assertParses(line);

        line = "create constant SourceSystem as 'BrokerA'";
        assertParses(line);

        line = "create constant SourceRank as Int '1'";
        assertParses(line);
    }

    @Test
    void shouldCreateTypedConstant()
    {
        ColumnName name = ColumnName.of("Name");
        ColumnName sourceRank = ColumnName.of("SourceRank");
        ColumnObject.Builder<String> names = ColumnObject.builder(name, ColumnTypes.STRING);
        names.add("A");
        names.add("B");
        TableColumnar table = Tables.newTable(TableName.of("t"), names.build());
        String line = "create constant SourceRank as Int '1'";

        TableColumnar transformed = table.apply(PARSER.parse(line));

        assertEquals(1, transformed.getInt(sourceRank).get(0));
        assertEquals(1, transformed.getInt(sourceRank).get(1));
    }

    @Test
    void shouldParseTypeConversionExamples()
    {
        String line = "convert AmountText to Double into Amount";
        assertParses(line);

        line = "convert AmountText to Double by exact into Amount";
        assertParses(line);

        line = "convert Description to Decimal by firstIn into Amount";
        assertParses(line);

        line = "convert Description to Decimal by lastIn into Amount";
        assertParses(line);

        line = "convert Description to Decimal by onlyIn into Amount";
        assertParses(line);

        line = "convert QuantityText to Int into Quantity";
        assertParses(line);

        line = "convert UnitsText to Long into Units";
        assertParses(line);

        line = "convert TradeDateText to Date into TradeDate";
        assertParses(line);

        line = "convert TradeDateText to Date using YMD";
        assertParses(line);

        line = "convert TradeDateText to Date using YMD into TradeDate";
        assertParses(line);

        line = "convert TradeDateText to Date into TradeDate using YMD";
        assertParses(line);

        line = "convert TradeDateText to Date using YMD by exact into TradeDate";
        assertParses(line);

        line = "convert TradeDateText to Date into TradeDate by firstIn using YMD";
        assertParses(line);

        line = "convert AmountText to Decimal into Amount";
        assertParses(line);

        line = "convert AmountText to DecimalAbs into AbsoluteAmount";
        assertParses(line);

        line = "convert AmountText to String into AmountDisplay";
        assertParses(line);
    }

    @Test
    void shouldRejectUsingForNonDateConversion()
    {
        String line = "convert AmountText to Double using YMD into Amount";

        assertThrows(TransformDslException.class, () -> PARSER.parse(line));
    }

    @Test
    void shouldParseSubstringAndPartExamples()
    {
        String line = "take left 3 from SortCode into BankCode";
        assertParses(line);

        line = "take right 4 from AccountNumber into AccountSuffix";
        assertParses(line);

        line = "take substring 0, 3 from Isin into CountryCode";
        assertParses(line);

        line = "take before '-' from TradeReference into TradePrefix";
        assertParses(line);

        line = "take after '-' from TradeReference into TradeSuffix";
        assertParses(line);
    }

    @Test
    void shouldParsePrefixSuffixExamples()
    {
        String line = "prefix Symbol with 'LSE:' into BloombergSymbol";
        assertParses(line);

        line = "suffix Isin with '-OLD' into LegacyIsin";
        assertParses(line);

        line = "replace ',' with '' in AmountText into CleanAmountText";
        assertParses(line);

        line = "replace all '\\s+' with ' ' in Description into CleanDescription";
        assertParses(line);
    }

    @Test
    void shouldParseSplitAndConcatExamples()
    {
        String line = "split Split on / into QuantityBefore, QuantityAfter";
        assertParses(line);

        line = "concat AccountType, Country, AccountNumber using '|' into AccountKey";
        assertParses(line);

        line = "concat AccountType, Country, AccountNumber into AccountKey";
        assertParses(line);

        line = "concat AccountType, Country, AccountNumber using '' into AccountKey";
        assertParses(line);
    }

    @Test
    void shouldParseClassifyAndExtractExamples()
    {
        String line = "classify Description matching 'Dividend|Distribution' into IsIncome as 'Y' else 'N'";
        assertParses(line);

        line = "extract from Description matching '.*\\(([^)]+)\\)' into Symbol";
        assertParses(line);
    }

    @Test
    void shouldParseSubstituteExamples()
    {
        String line = "substitute Status using 'A':'Active', 'I':'Inactive' default 'Other' into NormalisedStatus";
        assertParses(line);

        line = "substitute Status using 'A':'Active', 'I':'Inactive' into NormalisedStatus";
        assertParses(line);
    }

    @Test
    void shouldParseCoalesceExamples()
    {
        String line = "coalesce FirstName, PreferredName, LegalName into DisplayName";
        assertParses(line);

        line = "coalesce FirstName into DisplayName";
        assertParses(line);

        line = "coalesce FirstAmount, SecondAmount, ThirdAmount as Categorical into ChosenAmount";
        assertParses(line);

        line = "coalesce FirstAmount, SecondAmount, ThirdAmount, FourthAmount into ChosenAmount";
        assertParses(line);
    }

    @Test
    void shouldParseDecimalOperatorExamples()
    {
        String line = "add DebitAmount and CreditAmount into NetAmount";
        assertParses(line);

        line = "subtract Fees from GrossAmount into NetAmount";
        assertParses(line);

        line = "multiply Quantity by Price into MarketValue";
        assertParses(line);

        line = "divide Amount by Quantity into UnitPrice";
        assertParses(line);
    }

    @Test
    void shouldParseAbsExamples()
    {
        String line = "abs Amount into AbsoluteAmount";
        assertParses(line);
    }

    @Test
    void shouldRejectTrailingTokens()
    {
        String line = "strip Name please";

        assertThrows(TransformDslException.class, () -> PARSER.parse(line));
    }

    @Test
    void shouldRejectUnknownCommands()
    {
        String line = "enrich AccountId from CustomerMaster into CustomerName";

        assertThrows(TransformDslException.class, () -> PARSER.parse(line));
    }

    @Test
    void shouldAllowCustomCommands()
    {
        TransformDslParser parser = PARSER.with("enrich", tokens -> {
            String source = tokens.expectValue();
            tokens.expectWord("from");
            String lookup = tokens.expectValue();
            tokens.expectWord("into");
            String target = tokens.expectValue();
            return new CustomTransform(source + ":" + lookup + ":" + target);
        });
        String line = "enrich AccountId from CustomerMaster into CustomerName";

        Transform transform = parser.parse(line);

        assertEquals("AccountId:CustomerMaster:CustomerName", transform.getName());
    }

    @Test
    void shouldAllowManyCustomCommands()
    {
        TransformDslParser parser = PARSER.with(Map.of("enrich", tokens -> {
            String source = tokens.expectValue();
            tokens.expectWord("from");
            String lookup = tokens.expectValue();
            tokens.expectWord("into");
            String target = tokens.expectValue();
            return new CustomTransform(source + ":" + lookup + ":" + target);
        }, "tag", tokens -> {
            String target = tokens.expectValue();
            tokens.expectWord("as");
            String value = tokens.expectValue();
            return new CustomTransform(target + "=" + value);
        }));

        Transform enrich = parser.parse("enrich AccountId from CustomerMaster into CustomerName");
        Transform tag = parser.parse("tag Region as EMEA");

        assertEquals("AccountId:CustomerMaster:CustomerName", enrich.getName());
        assertEquals("Region=EMEA", tag.getName());
    }

    private static void assertParses(String line)
    {
        assertNotNull(PARSER.parse(line));
        assertNotNull(PARSER.parse(TokenStream.of(line)));
    }

    private static final class CustomTransform implements Transform
    {
        private final String name;

        private CustomTransform(String name)
        {
            this.name = name;
        }

        @Override
        public String getName()
        {
            return this.name;
        }

        @Override
        public void apply(Map<ColumnName, Column> columnsByName)
        {
        }
    }
}
