# babylon-table

[![Build](https://github.com/gary-babylon-app/babylon-table/actions/workflows/build.yml/badge.svg)](https://github.com/gary-babylon-app/babylon-table/actions/workflows/build.yml)
![Coverage](.github/badges/coverage.svg)

babylon-table is a focused Java 21 columnar table library with no third-party runtime
dependencies, offering filtering, grouping, aggregation, data transformations, and
efficient CSV ingestion for both in-memory workflows and streaming-style execution
patterns.


Highlights:

- Java 21 baseline
- No third-party runtime dependencies
- Columnar table operations including filtering, grouping, aggregation, sorting, joins, and transforms
- Table and column transform support for derived columns and reshape-style workflows
- CSV reading support for in-memory and streaming-style workflows

## Quick Transforms

babylon-table includes Quick Transforms for describing repeatable cleanup,
conversion, and enrichment steps. Quick Transforms are written as simple,
human-readable Transformation Statements: one statement per line.

```text
strip Name into CleanName
convert AmountText to Decimal into Amount
multiply Rate by 0.01 into DecimalRate
round Amount using Currency by bankers into RoundedAmount
negate Quantity when Type is Buy into SignedQuantity
```

Technically, the statement format is a small domain-specific language (DSL), but
it is designed to read like plain operational instructions. See the
[Quick Transforms reference](docs/transform-dsl.md) for the complete list of
supported statements and examples.
