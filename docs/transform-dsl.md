# Quick Transforms Reference

Quick Transforms are written as simple, human-readable Transformation
Statements: one statement per line for cleaning, converting, and deriving data.
Every statement starts with a command, usually a verb, such as `strip`, `clean`,
`convert`, `take`, `split`, or `round`.

## Keywords

- `into` - names the output column.
- `using` - adds a statement-specific parameter.
- `by` - adds a statement-specific mode.

For example, `convert TradeDateText to Date using YMD` uses a date format,
while `convert Description to Currency by firstIn` uses a sentence-search
strategy. Similarly, `round Amount using Currency` uses a scale column, while
`round Amount to 2 by bankers` uses a rounding strategy.

Technically, the statement format is a small domain-specific language (DSL), but
it is designed to read like plain operational instructions.

Quoted values are literals. Use quotes for text, regex patterns, empty strings,
and values containing punctuation or spaces.

## String cleanup

```text
strip Name
strip Name into CleanName
strip Name using ' []()-;,' into CleanName
clean Name
clean Name into CleanName
clean AccountNumber using ' -' into AccountKey
```

`strip` trims leading and trailing whitespace. Add `using` to strip any leading
or trailing character from the supplied literal; for example
`strip Name using ' []()-;,'` changes `" [ABC-123], "` to `"ABC-123"`.

`clean` first strips leading and trailing whitespace, then replaces each
internal run of whitespace with one normal space.

Add `using` when the cleaned value should also drop specific characters
everywhere in the value. The characters to remove are written as one literal, so
`clean AccountNumber using ' -' into AccountKey` removes normal spaces and
hyphens. If the literal contains a normal space, all whitespace is removed after
the whitespace has been cleaned.

For example:

```text
clean Name
"··Alpha··Beta\t\tGamma··" -> "Alpha·Beta·Gamma"

clean AccountNumber using ' -' into AccountKey
"·123-45·6·" -> "123456"
```

In these examples, the dot marks a space for explanation only; it is not part of
the actual value. `\t` marks a tab.

## String case

```text
uppercase Symbol
uppercase Symbol into UpperSymbol
lowercase Email
lowercase Email into LowerEmail
```

## Copy and constants

```text
copy Symbol into DisplaySymbol
constant 'BrokerA' into SourceSystem
constant 'USD' into PaymentCurrency
constant '1' as Int into SourceRank
constant 'true' as Boolean into IsActive
constant 'USD' as Currency into PaymentCurrency
```

If no type is supplied, constants are strings. Use `as Type` when the new
column should have a specific type.

## Metadata

```text
constant metadata.tableName into SourceFileName
constant metadata.description into SourceDescription
```

Metadata constants create a string column from table source metadata. Metadata
names are case-insensitive. This is useful when an ingested file name or table
description carries information such as broker, currency, date, or timestamp.

## Type conversion

```text
convert AmountText to Double
convert AmountText to Double into Amount
convert AmountText to Decimal into Amount
convert QuantityText to Int into Quantity
convert UnitsText to Long into Units
convert AmountText to String into AmountDisplay
convert CurrencyText to Currency into Currency
```

These conversions read a string value and convert it to the requested type. The
normal case is an exact parse: the whole string value should be the value being
converted.

Use `by firstIn`, `by lastIn`, or `by onlyIn` when the value must be extracted
from a longer sentence. A typical case is a description field with several
words, where one word is the useful value:

```text
convert Description to Currency by firstIn into Currency
convert Description to Currency by lastIn into SettlementCurrency
convert Description to Currency by onlyIn into Currency
```

`firstIn` uses the first parseable value found in the sentence. `lastIn` uses
the last parseable value. `onlyIn` uses the value only when exactly one
parseable value is found.

Date conversion accepts a date format with `using`:

```text
convert TradeDateText to Date using YMD into TradeDate
convert TradeDateText to Date into TradeDate using YMD
convert TradeDateText to Date using YMD by lastIn into TradeDate
```

Custom conversion types can be registered with `QuickTransforms.withType(...)`.

## Substrings and parts

```text
take left 3 from SortCode into BankCode
take right 4 from AccountNumber into AccountSuffix
take substring 0, 3 from Isin into CountryCode
take before '-' from TradeReference into TradePrefix
take after '-' from TradeReference into TradeSuffix
```

## Prefix, suffix, and replacement

```text
prefix Symbol with 'LSE:'
prefix Symbol with 'LSE:' into BloombergSymbol
suffix Isin with '.OLD'
suffix Isin with '.OLD' into LegacyIsin
replace ',' with '' in AmountText
replace ',' with '' in AmountText into CleanAmountText
replace all '\s+' with ' ' in Description into CleanDescription
```

`replace all` treats the target text as a regular expression.

## Split and concat

```text
split Split on '/' into QuantityBefore, QuantityAfter
concat AccountType, Country, AccountNumber into AccountKey
concat AccountType, Country, AccountNumber using '|' into AccountKey
concat AccountType, Country using '' into AccountKey
```

`split` keeps the declared output shape. `concat` uses no separator unless a
separator is supplied with `using`.

## Classify, extract, and substitute

```text
classify Description matching 'Dividend|Distribution' into IsIncome as Y
classify Description matching 'Dividend|Distribution' into IsIncome as Y else N
extract from Description matching '.*\(([^)]+)\)' into Symbol
substitute Status using 'A':'Active', 'I':'Inactive' into NormalisedStatus
substitute Status using 'I':'Inactive', 'A':'Active' default 'Other' into NormalisedStatus
```

Patterns are regular expressions. `substitute` maps literal values and can
optionally provide a `default` or `else` value. The default applies only when
the source row is set but its value is not in the map. If the source row is
unset, the output row remains unset.

## Flags

```text
flag Side = Buy into IsBuy
flag Side <> Buy into IsNotBuy
flag Quantity >= 100 into IsLarge
flag Side in Buy, Sell into IsTrade
flag Side not in Buy, Sell into IsOther
flag Side = Buy and Quantity >= 100 into IsLargeBuy
flag Side = Buy or Side = Sell into IsTrade
```

`flag` writes a Boolean column from a row condition. Values are parsed using the
condition column's declared type, so `Quantity >= 100` compares numeric values
when `Quantity` is an Int, Long, Double, or Decimal column. The parser accepts
both `=` and `==` for equality, and both `<>` and `!=` for not-equal. The writer
prefers `=` and `<>`. `and` binds tighter than `or`.

The output column is a primitive Boolean column. It can be used as a typed
condition column by Decimal unary transforms such as `abs` and `negate`.

## Coalesce

```text
coalesce FirstName into DisplayName
coalesce FirstName, PreferredName, LegalName into DisplayName
coalesce FirstAmount, SecondAmount, ThirdAmount into ChosenAmount
```

`coalesce` scans the source columns from left to right for each row and writes
the first set value it finds. If all source values are empty for a row, the
output value is empty. For example, `coalesce FirstName, PreferredName, LegalName
into DisplayName` chooses `FirstName` when present, otherwise `PreferredName`,
otherwise `LegalName`.

## Decimal operators

Binary decimal operators always require `into`. Operands can be decimal columns
or decimal literals.

```text
add DebitAmount and CreditAmount into NetAmount
add Amount and 5 into AmountPlusFee
subtract Fees from GrossAmount into NetAmount
subtract 1 from DiscountFactor into DiscountRate
multiply Quantity by Price into MarketValue
multiply Rate by 0.01 into DecimalRate
divide Amount by Quantity into UnitPrice
divide Amount by 100 into AmountMajor
```

Decimal literals are canonicalised when parsed. For example:

```text
multiply Rate by 0.0100 into DecimalRate
```

pretty-prints as:

```text
multiply Rate by 0.01 into DecimalRate
```

## Decimal unary operators

```text
abs Amount
abs Amount into AbsoluteAmount
abs Quantity when ShouldAbs into QuantityAbs
negate Amount
negate Amount into SignedAmount
negate QuantityAbs when IsBuy into SignedQuantity
normalise Amount
normalise Amount into NormalisedAmount
```

`abs` and `negate` accept an optional primitive Boolean condition column with
`when`. When the condition row is true, the unary operation is applied. When it
is false or unset, the original Decimal value is kept. Source rows that are
unset remain unset. `normalise` is Decimal-only: it removes insignificant
trailing zeros while preserving numeric value, and clamps negative scale back to
zero.

## Rounding

```text
round Amount to 2
round Amount to 0 when NoCents
round Amount to 2 by halfUp into RoundedAmount
round Amount to 2 by bankers into RoundedAmount
round Amount using Currency
round Amount using Currency when NeedsRound into RoundedAmount
round Amount using Currency by halfUp into RoundedAmount
```

`round Amount to 2` uses an explicit number of decimal places. `round Amount
using Currency` uses a scale column. The standard parser registers
`java.util.Currency` using `Currency::getDefaultFractionDigits`.

`round` accepts an optional primitive Boolean condition column with `when`.
Rows are rounded only when the condition is true. When it is false or unset,
the original Decimal value is kept.

Rounding modes include:

```text
up
down
ceiling
floor
halfUp
halfDown
bankers
noLoss
```

`bankers` means `RoundingMode.HALF_EVEN`.
`noLoss` means `RoundingMode.UNNECESSARY`: the transform fails if rounding
would discard significant digits.

`round` sets the decimal scale. For example, rounding `12.1` to 2 places
produces a Decimal value whose plain representation is `12.10`.

Use `normalise` when you want to remove insignificant trailing zeros instead.

Custom currency-like scale types can be registered with:

```java
QuickTransforms.standard()
        .withRoundScale(MyCurrency.class, MyCurrency::minorUnits);
```

## Extension points

Quick Transforms can be extended with custom commands and custom conversion
types:

```java
QuickTransforms quickTransforms = QuickTransforms.standard()
        .with("enrich", tokens -> ...)
        .withType("Isin", isinType)
        .withRoundScale(MyCurrency.class, MyCurrency::minorUnits);
```

Multiple commands or types can be registered with `with(Map<...>)` and
`withTypes(Map<...>)`.
