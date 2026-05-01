package app.babylon.table.transform.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class TransformDslWriterTest
{
    private static final TransformDslWriter WRITER = TransformDslWriter.standard();

    @Test
    void shouldWriteStringCleanup()
    {
        assertFormat("strip Name", "strip Name");
        assertFormat("strip Name into CleanName", "strip Name into CleanName");
        assertFormat("clean whitespace in Name", "clean whitespace in Name");
        assertFormat("clean whitespace in Name into CleanName", "clean whitespace in Name into CleanName");
    }

    @Test
    void shouldWriteStringCase()
    {
        assertFormat("uppercase Symbol", "uppercase Symbol");
        assertFormat("uppercase Symbol into UpperSymbol", "uppercase Symbol into UpperSymbol");
        assertFormat("lowercase Email", "lowercase Email");
        assertFormat("lowercase Email into LowerEmail", "lowercase Email into LowerEmail");
    }

    @Test
    void shouldWriteCopyAndConstants()
    {
        assertFormat("copy Symbol into DisplaySymbol", "copy Symbol into DisplaySymbol");
        assertFormat("create constant SourceSystem as 'BrokerA'", "create constant SourceSystem as 'BrokerA'");
        assertFormat("create constant SourceRank as Int '1'", "create constant SourceRank as Int '1'");
    }

    @Test
    void shouldWriteTypeConversions()
    {
        assertFormat("convert AmountText to Double", "convert AmountText to Double");
        assertFormat("convert AmountText to Double into Amount", "convert AmountText to Double into Amount");
        assertFormat("convert AmountText to Double by firstIn into Amount",
                "convert AmountText to Double by firstIn into Amount");
        assertFormat("convert QuantityText to Int into Quantity", "convert QuantityText to Int into Quantity");
        assertFormat("convert UnitsText to Long into Units", "convert UnitsText to Long into Units");
        assertFormat("convert TradeDateText to Date using YMD into TradeDate",
                "convert TradeDateText to Date using YMD into TradeDate");
        assertFormat("convert TradeDateText to Date into TradeDate using YMD",
                "convert TradeDateText to Date using YMD into TradeDate");
        assertFormat("convert TradeDateText to Date using YMD by lastIn into TradeDate",
                "convert TradeDateText to Date using YMD by lastIn into TradeDate");
        assertFormat("convert AmountText to Decimal into Amount", "convert AmountText to Decimal into Amount");
        assertFormat("convert AmountText to Decimal by onlyIn into Amount",
                "convert AmountText to Decimal by onlyIn into Amount");
        assertFormat("convert AmountText to DecimalAbs into AbsoluteAmount",
                "convert AmountText to DecimalAbs into AbsoluteAmount");
        assertFormat("convert AmountText to String into AmountDisplay",
                "convert AmountText to String into AmountDisplay");
    }

    @Test
    void shouldWriteSubstringParts()
    {
        assertFormat("take left 3 from SortCode into BankCode", "take left 3 from SortCode into BankCode");
        assertFormat("take right 4 from AccountNumber into AccountSuffix",
                "take right 4 from AccountNumber into AccountSuffix");
        assertFormat("take substring 0, 3 from Isin into CountryCode",
                "take substring 0, 3 from Isin into CountryCode");
        assertFormat("take before '-' from TradeReference into TradePrefix",
                "take before '-' from TradeReference into TradePrefix");
        assertFormat("take after '-' from TradeReference into TradeSuffix",
                "take after '-' from TradeReference into TradeSuffix");
    }

    @Test
    void shouldWritePrefixSuffixAndReplace()
    {
        assertFormat("prefix Symbol with 'LSE:'", "prefix Symbol with 'LSE:'");
        assertFormat("prefix Symbol with 'LSE:' into BloombergSymbol",
                "prefix Symbol with 'LSE:' into BloombergSymbol");
        assertFormat("suffix Isin with '.OLD'", "suffix Isin with '.OLD'");
        assertFormat("suffix Isin with '.OLD' into LegacyIsin", "suffix Isin with '.OLD' into LegacyIsin");
        assertFormat("replace ',' with '' in AmountText", "replace ',' with '' in AmountText");
        assertFormat("replace ',' with '' in AmountText into CleanAmountText",
                "replace ',' with '' in AmountText into CleanAmountText");
        assertFormat("replace all '\\s+' with ' ' in Description into CleanDescription",
                "replace all '\\\\s+' with ' ' in Description into CleanDescription");
    }

    @Test
    void shouldWriteSplitAndConcat()
    {
        assertFormat("split Split on '/' into QuantityBefore, QuantityAfter",
                "split Split on '/' into QuantityBefore, QuantityAfter");
        assertFormat("concat AccountType, Country, AccountNumber into AccountKey",
                "concat AccountType, Country, AccountNumber into AccountKey");
        assertFormat("concat AccountType, Country, AccountNumber using '|' into AccountKey",
                "concat AccountType, Country, AccountNumber using '|' into AccountKey");
        assertFormat("concat AccountType, Country using '' into AccountKey",
                "concat AccountType, Country using '' into AccountKey");
    }

    @Test
    void shouldWriteClassifyExtractAndSubstitute()
    {
        assertFormat("classify Description matching 'Dividend|Distribution' into IsIncome as Y",
                "classify Description matching 'Dividend|Distribution' into IsIncome as Y");
        assertFormat("classify Description matching 'Dividend|Distribution' into IsIncome as Y else N",
                "classify Description matching 'Dividend|Distribution' into IsIncome as Y default N");
        assertFormat("extract from Description matching '.*\\(([^)]+)\\)' into Symbol",
                "extract from Description matching '.*\\\\(([^)]+)\\\\)' into Symbol");
        assertFormat("substitute Status using 'A':'Active', 'I':'Inactive' into NormalisedStatus",
                "substitute Status using 'A':'Active', 'I':'Inactive' into NormalisedStatus");
        assertFormat("substitute Status using 'I':'Inactive', 'A':'Active' default 'Other' into NormalisedStatus",
                "substitute Status using 'A':'Active', 'I':'Inactive' default Other into NormalisedStatus");
    }

    @Test
    void shouldWriteCoalesceAndDecimalOperators()
    {
        assertFormat("coalesce FirstName into DisplayName", "coalesce FirstName into DisplayName");
        assertFormat("coalesce FirstName, PreferredName, LegalName into DisplayName",
                "coalesce FirstName, PreferredName, LegalName into DisplayName");
        assertFormat("coalesce FirstAmount, SecondAmount, ThirdAmount as Categorical into ChosenAmount",
                "coalesce FirstAmount, SecondAmount, ThirdAmount as CATEGORICAL into ChosenAmount");
        assertFormat("add DebitAmount and CreditAmount into NetAmount",
                "add DebitAmount and CreditAmount into NetAmount");
        assertFormat("subtract Fees from GrossAmount into NetAmount", "subtract Fees from GrossAmount into NetAmount");
        assertFormat("multiply Quantity by Price into MarketValue", "multiply Quantity by Price into MarketValue");
        assertFormat("divide Amount by Quantity into UnitPrice", "divide Amount by Quantity into UnitPrice");
        assertFormat("abs Amount", "abs Amount");
        assertFormat("abs Amount into AbsoluteAmount", "abs Amount into AbsoluteAmount");
    }

    @Test
    void shouldWriteAll()
    {
        List<String> lines = WRITER.writeAll(List.of(TransformDslParser.standard().parse("strip Name"),
                TransformDslParser.standard().parse("copy Symbol into DisplaySymbol")));

        assertEquals(List.of("strip Name", "copy Symbol into DisplaySymbol"), lines);
    }

    private static void assertFormat(String line, String expected)
    {
        assertEquals(expected, WRITER.format(line));
    }
}
