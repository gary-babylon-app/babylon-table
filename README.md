# babylon-table

[![Build](https://github.com/gary-babylon-app/babylon-table/actions/workflows/build.yml/badge.svg)](https://github.com/gary-babylon-app/babylon-table/actions/workflows/build.yml)
![Coverage](.github/badges/coverage.svg)

babylon-table is a focused columnar table library with filtering, grouping,
sorting, transformations and aggregations. Columns are immutable
once built. The library has no third-party runtime dependencies.

Highlights:

- Columnar table operations including filtering, grouping, aggregation, sorting, joins, and transforms,
- Quick Transforms DSL for derived columns and reshape-style workflows,
- Immutable columns, with dedicated builders for column construction,
- No third-party runtime dependencies,
- CSV reading support for in-memory and streaming-style workflows,
- Categorical columns are dictionary encoded reducing memory footprint
  and accelerating transformations.

## Quick Transforms

babylon-table includes a domain-specific language (DSL) for describing
repeatable cleanup, conversion, and enrichment steps. These descriptions are
called Quick Transforms and are written as simple, human-readable Transformation
Statements: one statement per line.

```text
strip Name into CleanName
convert AmountText to Decimal into Amount
multiply Rate by 0.01 into DecimalRate
round Amount using Currency by bankers into RoundedAmount
flag Side = Buy into IsBuy
negate QuantityAbs when IsBuy into SignedQuantity
```

The DSL is designed to read like plain operational instructions. See the
[Quick Transforms reference](docs/transform-dsl.md) for the complete list of
supported statements and examples.
