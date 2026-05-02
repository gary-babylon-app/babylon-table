# Transform DSL Reference

The transform DSL describes one transform per line. The first word is the
command. `into` names the output column, `using` supplies transform-specific
configuration, and `by` supplies a parsing or rounding mode.

Quoted values are literals. Use quotes for text, regex patterns, empty strings,
and values containing punctuation or spaces.

## String cleanup

```text
strip Name
strip Name into CleanName
clean whitespace in Name
clean whitespace in Name into CleanName
```

`strip` trims leading and trailing whitespace. `clean whitespace` normalises
internal whitespace.

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
create constant SourceSystem as 'BrokerA'
create constant SourceRank as Int '1'
create constant Currency as Currency 'USD'
```

If no type is supplied, constants are strings.

## Type conversion

```text
convert AmountText to Double
convert AmountText to Double into Amount
convert AmountText to Double by firstIn into Amount
convert AmountText to Decimal into Amount
convert AmountText to Decimal by onlyIn into Amount
convert QuantityText to Int into Quantity
convert UnitsText to Long into Units
convert AmountText to String into AmountDisplay
convert CurrencyText to Currency into Currency
```

Date conversion accepts a date format with `using`:

```text
convert TradeDateText to Date using YMD into TradeDate
convert TradeDateText to Date into TradeDate using YMD
convert TradeDateText to Date using YMD by lastIn into TradeDate
```

Parse modes are:

```text
exact
firstIn
lastIn
onlyIn
```

Custom conversion types can be registered with `TransformDslParser.withType(...)`.

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
optionally provide a `default` or `else` value.

## Coalesce

```text
coalesce FirstName into DisplayName
coalesce FirstName, PreferredName, LegalName into DisplayName
coalesce FirstAmount, SecondAmount, ThirdAmount as Categorical into ChosenAmount
```

`coalesce` chooses the first set value from the source columns.

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
negate Amount
negate Amount into SignedAmount
negate Quantity when Type is Buy
negate Quantity when Type is Buy into SignedQuantity
normalise Amount
normalise Amount into NormalisedAmount
```

Conditional `negate` parses the value after `is` using the condition column's
declared type before comparing. `normalise` is Decimal-only: it removes
insignificant trailing zeros while preserving numeric value, and clamps negative
scale back to zero.

## Rounding

```text
round Amount to 2
round Amount to 2 by halfUp into RoundedAmount
round Amount to 2 by bankers into RoundedAmount
round Amount using Currency
round Amount using Currency by halfUp into RoundedAmount
```

`round Amount to 2` uses an explicit number of decimal places. `round Amount
using Currency` uses a scale column. The standard parser registers
`java.util.Currency` using `Currency::getDefaultFractionDigits`.

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
TransformDslParser.standard()
        .withRoundScale(MyCurrency.class, MyCurrency::minorUnits);
```

## Extension points

The standard parser can be extended with custom commands and custom conversion
types:

```java
TransformDslParser parser = TransformDslParser.standard()
        .with("enrich", tokens -> ...)
        .withType("Isin", isinType)
        .withRoundScale(MyCurrency.class, MyCurrency::minorUnits);
```

Multiple commands or types can be registered with `with(Map<...>)` and
`withTypes(Map<...>)`.
