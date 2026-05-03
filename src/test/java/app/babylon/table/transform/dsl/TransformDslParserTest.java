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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.RoundingMode;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.transform.Transform;
import app.babylon.table.transform.TransformFlag;
import app.babylon.table.transform.TransformRound;
import app.babylon.table.transform.TransformToType;

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

        line = "strip Name using ' []()-;,' into CleanName";
        assertParses(line);

        line = "strip Name into CleanName using ' []()-;,'";
        assertParses(line);

        line = "clean Name";
        assertParses(line);

        line = "clean Name into CleanName";
        assertParses(line);

        line = "clean AccountNumber using ' -' into AccountKey";
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

        line = "constant metadata.tableName into SourceFileName";
        assertParses(line);

        line = "constant METADATA.DESCRIPTION into SourceDescription";
        assertParses(line);

        line = "constant 'BrokerA' into SourceSystem";
        assertParses(line);

        line = "constant 'USD' into PaymentCurrency";
        assertParses(line);

        line = "constant '1' as Int into SourceRank";
        assertParses(line);

        line = "constant 'true' as Boolean into IsActive";
        assertParses(line);

        line = "constant 'USD' as Currency into PaymentCurrency";
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
        String line = "constant '1' as Int into SourceRank";

        TableColumnar transformed = table.apply(PARSER.parse(line));

        assertEquals(1, transformed.getInt(sourceRank).get(0));
        assertEquals(1, transformed.getInt(sourceRank).get(1));
    }

    @Test
    void shouldRejectConstantTypeWithTo()
    {
        String line = "constant 'USD' to Currency into PaymentCurrency";

        assertThrows(TransformDslException.class, () -> PARSER.parse(line));
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
        assertInstanceOf(TransformToType.class, PARSER.parse(line));

        line = "convert AmountText to String into AmountDisplay";
        assertParses(line);

        line = "convert CurrencyText to Currency into Currency";
        assertParses(line);
    }

    @Test
    void shouldAllowCustomConversionTypes()
    {
        Column.Type isin = Column.Type.of(Isin.class,
                (s, offset, length) -> new Isin(s.subSequence(offset, offset + length).toString()));
        TransformDslParser parser = PARSER.withType("Isin", isin);
        String line = "convert IsinText to Isin into Isin";

        TransformToType<?> transform = assertInstanceOf(TransformToType.class, parser.parse(line));

        assertEquals(isin, transform.type());
    }

    @Test
    void shouldAllowStandardConversionTypesToBeOverwritten()
    {
        Column.Type currency = Column.Type.of(AppCurrency.class,
                (s, offset, length) -> new AppCurrency(s.subSequence(offset, offset + length).toString()));
        TransformDslParser parser = PARSER.withType("Currency", currency);
        String line = "convert CurrencyText to Currency into Currency";

        TransformToType<?> transform = assertInstanceOf(TransformToType.class, parser.parse(line));

        assertEquals(currency, transform.type());
    }

    @Test
    void shouldAllowCustomRoundScaleTypes()
    {
        TransformDslParser parser = PARSER.withRoundScale(AppCurrency.class, AppCurrency::minorUnits);
        String line = "round Amount using Currency by halfUp into RoundedAmount";

        TransformRound transform = assertInstanceOf(TransformRound.class, parser.parse(line));

        assertEquals(ColumnName.of("Amount"), transform.columnName());
        assertEquals(ColumnName.of("Currency"), transform.scaleColumnName());
        assertEquals(ColumnName.of("RoundedAmount"), transform.newColumnName());
        assertEquals(RoundingMode.HALF_UP, transform.roundingMode());
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
    void shouldParseFlagExamples()
    {
        String line = "flag Side=Buy into IsBuy";
        assertInstanceOf(TransformFlag.class, PARSER.parse(line));
        assertParses(line);

        line = "flag Side == Buy into IsBuy";
        assertParses(line);

        line = "flag Side <> Buy into IsNotBuy";
        assertParses(line);

        line = "flag Quantity >= 100 into IsLarge";
        assertParses(line);

        line = "flag Side in Buy, Sell into IsTrade";
        assertParses(line);

        line = "flag Side not in Buy, Sell into IsOther";
        assertParses(line);

        line = "flag Side=Buy and Quantity>=100 into IsLargeBuy";
        assertParses(line);

        line = "flag Side=Buy or Side=Sell into IsTrade";
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

        line = "coalesce FirstAmount, SecondAmount, ThirdAmount into ChosenAmount";
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

        line = "add Amount and 5 into AmountPlusFee";
        assertParses(line);

        line = "subtract 1 from DiscountFactor into DiscountRate";
        assertParses(line);

        line = "multiply Rate by 0.01 into DecimalRate";
        assertParses(line);

        line = "divide Amount by 100 into AmountMajor";
        assertParses(line);
    }

    @Test
    void shouldParseAbsExamples()
    {
        String line = "abs Amount into AbsoluteAmount";
        assertParses(line);

        line = "abs Quantity when ShouldAbs into QuantityAbs";
        assertParses(line);

        line = "abs Quantity into QuantityAbs when ShouldAbs";
        assertParses(line);

        line = "negate Amount into SignedAmount";
        assertParses(line);

        line = "negate QuantityAbs when IsBuy into SignedQuantity";
        assertParses(line);

        line = "negate QuantityAbs into SignedQuantity when IsBuy";
        assertParses(line);

        assertThrows(TransformDslException.class, () -> PARSER.parse("negate Quantity when Type is Buy"));

        line = "normalise Amount";
        assertParses(line);

        line = "normalise Amount into NormalisedAmount";
        assertParses(line);

        line = "round Amount to 2";
        assertParses(line);

        line = "round Amount to 2 by halfUp into RoundedAmount";
        assertParses(line);

        line = "round Amount to 0 when NoCents";
        assertParses(line);

        line = "round Amount to 0 into RoundedAmount when NoCents";
        assertParses(line);

        line = "round Amount to 2 into RoundedAmount by halfUp";
        assertParses(line);

        line = "round Amount using Currency";
        assertParses(line);

        line = "round Amount using Currency by halfUp into RoundedAmount";
        assertParses(line);

        line = "round Amount using Currency when NeedsRound into RoundedAmount";
        assertParses(line);

        line = "round Amount using Currency into RoundedAmount by halfUp";
        assertParses(line);

        line = "round Amount to 2 by bankers into RoundedAmount";
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

    private record Isin(String value)
    {
    }

    private record AppCurrency(String value)
    {
        private int minorUnits()
        {
            return 3;
        }
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
