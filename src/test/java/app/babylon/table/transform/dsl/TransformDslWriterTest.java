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
        assertFormat("strip Name using ' []()-;,' into CleanName", "strip Name using ' []()-;,' into CleanName");
        assertFormat("strip Name into CleanName using ' []()-;,'", "strip Name using ' []()-;,' into CleanName");
        assertFormat("clean Name", "clean Name");
        assertFormat("clean Name into CleanName", "clean Name into CleanName");
        assertFormat("clean AccountNumber using ' -' into AccountKey",
                "clean AccountNumber using ' -' into AccountKey");
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
        assertFormat("constant metadata.tableName into SourceFileName",
                "constant metadata.tableName into SourceFileName");
        assertFormat("constant METADATA.DESCRIPTION into SourceDescription",
                "constant metadata.description into SourceDescription");
        assertFormat("constant 'BrokerA' into SourceSystem", "constant 'BrokerA' into SourceSystem");
        assertFormat("constant 'USD' into PaymentCurrency", "constant 'USD' into PaymentCurrency");
        assertFormat("constant '1' as Int into SourceRank", "constant '1' as Int into SourceRank");
        assertFormat("constant 'true' as Boolean into IsActive", "constant 'true' as Boolean into IsActive");
        assertFormat("constant 'USD' as Currency into PaymentCurrency",
                "constant 'USD' as Currency into PaymentCurrency");
    }

    @Test
    void shouldWriteTypeConversions()
    {
        assertFormat("convert AmountText to Double", "convert AmountText to Double");
        assertFormat("convert AmountText to Double into Amount", "convert AmountText to Double into Amount");
        assertFormat("convert AmountText to Double by exact into Amount", "convert AmountText to Double into Amount");
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
        assertFormat("split Name on ' ' by first into FirstName, Rest",
                "split Name on ' ' by first into FirstName, Rest");
        assertFormat("split Path on '/' by last into Directory, File",
                "split Path on '/' by last into Directory, File");
        assertFormat("concat AccountType, Country, AccountNumber into AccountKey",
                "concat AccountType, Country, AccountNumber into AccountKey");
        assertFormat("concat AccountType, Country, AccountNumber using '|' into AccountKey",
                "concat AccountType, Country, AccountNumber using '|' into AccountKey");
        assertFormat("concat AccountType, Country using '' into AccountKey",
                "concat AccountType, Country using '' into AccountKey");
        assertFormat("concat AccountType, Country, 'ACCT', AccountNumber using '|' into AccountKey",
                "concat AccountType, Country, 'ACCT', AccountNumber using '|' into AccountKey");
    }

    @Test
    void shouldWriteFinalShaping()
    {
        assertFormat("retain AccountKey, Currency, Amount", "retain AccountKey, Currency, Amount");
        assertFormat("remove RawAmount, Notes", "remove RawAmount, Notes");
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
        assertFormat("extract from columnName AmountUSD using '([A-Z]{3})$' as Currency into Currency",
                "extract from columnName AmountUSD using '([A-Z]{3})$' as Currency into Currency");
        assertFormat("flag Side=Buy into IsBuy", "flag Side = Buy into IsBuy");
        assertFormat("flag Side == Buy into IsBuy", "flag Side = Buy into IsBuy");
        assertFormat("flag Side != Buy into IsNotBuy", "flag Side <> Buy into IsNotBuy");
        assertFormat("flag Quantity>=100 into IsLarge", "flag Quantity >= 100 into IsLarge");
        assertFormat("flag Side in Buy, Sell into IsTrade", "flag Side in Buy, Sell into IsTrade");
        assertFormat("flag Side nin Buy, Sell into IsOther", "flag Side nin Buy, Sell into IsOther");
        assertFormat("flag Side=Buy and Quantity>=100 into IsLargeBuy",
                "flag Side = Buy and Quantity >= 100 into IsLargeBuy");
        assertFormat("flag Side=Buy or Side=Sell into IsTrade", "flag Side = Buy or Side = Sell into IsTrade");
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
        assertFormat("coalesce FirstAmount, SecondAmount, ThirdAmount into ChosenAmount",
                "coalesce FirstAmount, SecondAmount, ThirdAmount into ChosenAmount");
        assertFormat("add DebitAmount and CreditAmount into NetAmount",
                "add DebitAmount and CreditAmount into NetAmount");
        assertFormat("subtract Fees from GrossAmount into NetAmount", "subtract Fees from GrossAmount into NetAmount");
        assertFormat("multiply Quantity by Price into MarketValue", "multiply Quantity by Price into MarketValue");
        assertFormat("divide Amount by Quantity into UnitPrice", "divide Amount by Quantity into UnitPrice");
        assertFormat("add Amount and 5 into AmountPlusFee", "add Amount and 5 into AmountPlusFee");
        assertFormat("subtract 1 from DiscountFactor into DiscountRate",
                "subtract 1 from DiscountFactor into DiscountRate");
        assertFormat("multiply Rate by 0.01 into DecimalRate", "multiply Rate by 0.01 into DecimalRate");
        assertFormat("multiply Rate by 0.0100 into DecimalRate", "multiply Rate by 0.01 into DecimalRate");
        assertFormat("multiply Amount by 1000.000 into ScaledAmount", "multiply Amount by 1000 into ScaledAmount");
        assertFormat("divide Amount by 100 into AmountMajor", "divide Amount by 100 into AmountMajor");
        assertFormat("abs Amount", "abs Amount");
        assertFormat("abs Amount into AbsoluteAmount", "abs Amount into AbsoluteAmount");
        assertFormat("abs Quantity when ShouldAbs into QuantityAbs", "abs Quantity when ShouldAbs into QuantityAbs");
        assertFormat("abs Quantity into QuantityAbs when ShouldAbs", "abs Quantity when ShouldAbs into QuantityAbs");
        assertFormat("negate Amount into SignedAmount", "negate Amount into SignedAmount");
        assertFormat("negate QuantityAbs when IsBuy into SignedQuantity",
                "negate QuantityAbs when IsBuy into SignedQuantity");
        assertFormat("negate QuantityAbs into SignedQuantity when IsBuy",
                "negate QuantityAbs when IsBuy into SignedQuantity");
        assertFormat("normalise Amount", "normalise Amount");
        assertFormat("normalise Amount into NormalisedAmount", "normalise Amount into NormalisedAmount");
        assertFormat("round Amount to 2", "round Amount to 2");
        assertFormat("round Amount to 2 by halfUp into RoundedAmount",
                "round Amount to 2 by halfUp into RoundedAmount");
        assertFormat("round Amount to 0 when NoCents", "round Amount to 0 when NoCents");
        assertFormat("round Amount to 0 into RoundedAmount when NoCents",
                "round Amount to 0 when NoCents into RoundedAmount");
        assertFormat("round Amount to 2 into RoundedAmount by halfUp",
                "round Amount to 2 by halfUp into RoundedAmount");
        assertFormat("round Amount to 2 by bankers into RoundedAmount",
                "round Amount to 2 by bankers into RoundedAmount");
        assertFormat("round Amount to 2 by noLoss into RoundedAmount",
                "round Amount to 2 by noLoss into RoundedAmount");
        assertFormat("round Amount using Currency", "round Amount using Currency");
        assertFormat("round Amount using Currency by halfUp into RoundedAmount",
                "round Amount using Currency by halfUp into RoundedAmount");
        assertFormat("round Amount using Currency when NeedsRound into RoundedAmount",
                "round Amount using Currency when NeedsRound into RoundedAmount");
        assertFormat("round Amount using Currency into RoundedAmount by halfUp",
                "round Amount using Currency by halfUp into RoundedAmount");
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
