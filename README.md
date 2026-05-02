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

## Transform DSL

babylon-table includes a compact, line-oriented transform DSL for describing
repeatable table cleanup and enrichment steps.

```text
strip Name into CleanName
convert AmountText to Decimal into Amount
multiply Rate by 0.01 into DecimalRate
round Amount using Currency by bankers into RoundedAmount
negate Quantity when Type is Buy into SignedQuantity
```

See the [Transform DSL reference](docs/transform-dsl.md) for the complete list
of supported statements and examples.
