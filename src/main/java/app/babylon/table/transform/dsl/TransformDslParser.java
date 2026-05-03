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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.transform.ComparisonCondition;
import app.babylon.table.transform.ConditionExpression;
import app.babylon.table.transform.DateFormat;
import app.babylon.table.transform.LogicalCondition;
import app.babylon.table.transform.Transform;
import app.babylon.table.transform.TransformAbs;
import app.babylon.table.transform.TransformAdd;
import app.babylon.table.transform.TransformAfter;
import app.babylon.table.transform.TransformBefore;
import app.babylon.table.transform.TransformClassify;
import app.babylon.table.transform.TransformClean;
import app.babylon.table.transform.TransformCoalesce;
import app.babylon.table.transform.TransformConcat;
import app.babylon.table.transform.TransformCopy;
import app.babylon.table.transform.TransformCreateConstant;
import app.babylon.table.transform.TransformDecimalBinaryOperator.Operand;
import app.babylon.table.transform.TransformDivide;
import app.babylon.table.transform.TransformExtract;
import app.babylon.table.transform.TransformFlag;
import app.babylon.table.transform.TransformLeft;
import app.babylon.table.transform.TransformMultiply;
import app.babylon.table.transform.TransformNegate;
import app.babylon.table.transform.TransformNormalise;
import app.babylon.table.transform.TransformParseMode;
import app.babylon.table.transform.TransformPrefix;
import app.babylon.table.transform.TransformRight;
import app.babylon.table.transform.TransformRound;
import app.babylon.table.transform.TransformSplit;
import app.babylon.table.transform.TransformStringReplace;
import app.babylon.table.transform.TransformStringReplaceAll;
import app.babylon.table.transform.TransformStrip;
import app.babylon.table.transform.TransformSubstitute;
import app.babylon.table.transform.TransformSubstring;
import app.babylon.table.transform.TransformSubtract;
import app.babylon.table.transform.TransformSuffix;
import app.babylon.table.transform.TransformMetadataConstant;
import app.babylon.table.transform.TransformToDouble;
import app.babylon.table.transform.TransformToInt;
import app.babylon.table.transform.TransformToLocalDate;
import app.babylon.table.transform.TransformToLong;
import app.babylon.table.transform.TransformToLowerCase;
import app.babylon.table.transform.TransformToPrimitive;
import app.babylon.table.transform.TransformToString;
import app.babylon.table.transform.TransformToType;
import app.babylon.table.transform.TransformToUpperCase;

/**
 * Parses a single transformation DSL statement into a {@link Transform}.
 * <p>
 * The first word is always the command. Later keywords introduce named clauses:
 * {@code using} supplies transform-specific configuration such as a date format
 * or concat separator, {@code by} supplies a parse/search strategy such as
 * {@code firstIn}, {@code lastIn}, {@code onlyIn}, or {@code exact}, and
 * {@code into} names the output column. Quoted values are literals; parser
 * rules use literals for free text and patterns where punctuation or spaces
 * should be preserved.
 */
final class TransformDslParser
{
    private final Map<String, TransformCommandParser> parsers;
    private final Map<String, Column.Type> types;
    private final Map<Class<?>, ToIntFunction<Object>> roundScales;

    private TransformDslParser(Map<String, TransformCommandParser> parsers, Map<String, Column.Type> types,
            Map<Class<?>, ToIntFunction<Object>> roundScales)
    {
        this.parsers = Map.copyOf(parsers);
        this.types = Map.copyOf(types);
        this.roundScales = Map.copyOf(roundScales);
    }

    public static TransformDslParser standard()
    {
        Map<String, TransformCommandParser> parsers = new HashMap<>();
        parsers.put("abs", TransformDslParser::parseAbs);
        parsers.put("add", TransformDslParser::parseAdd);
        parsers.put("classify", TransformDslParser::parseClassify);
        parsers.put("clean", TransformDslParser::parseClean);
        parsers.put("coalesce", TransformDslParser::parseCoalesce);
        parsers.put("concat", TransformDslParser::parseConcat);
        parsers.put("constant", TransformDslParser::parseConstant);
        parsers.put("copy", TransformDslParser::parseCopy);
        parsers.put("divide", TransformDslParser::parseDivide);
        parsers.put("extract", TransformDslParser::parseExtract);
        parsers.put("flag", TransformDslParser::parseFlag);
        parsers.put("lowercase", TransformDslParser::parseLowercase);
        parsers.put("multiply", TransformDslParser::parseMultiply);
        parsers.put("negate", TransformDslParser::parseNegate);
        parsers.put("normalise", TransformDslParser::parseNormalise);
        parsers.put("prefix", TransformDslParser::parsePrefix);
        parsers.put("replace", TransformDslParser::parseReplace);
        parsers.put("split", TransformDslParser::parseSplit);
        parsers.put("strip", TransformDslParser::parseStrip);
        parsers.put("substitute", TransformDslParser::parseSubstitute);
        parsers.put("subtract", TransformDslParser::parseSubtract);
        parsers.put("suffix", TransformDslParser::parseSuffix);
        parsers.put("take", TransformDslParser::parseTake);
        parsers.put("uppercase", TransformDslParser::parseUppercase);
        return new TransformDslParser(parsers, standardTypes(), standardRoundScales());
    }

    public TransformDslParser with(String command, TransformCommandParser parser)
    {
        Map<String, TransformCommandParser> copy = new HashMap<>(this.parsers);
        copy.put(normalise(command), ArgumentCheck.nonNull(parser));
        return new TransformDslParser(copy, this.types, this.roundScales);
    }

    public TransformDslParser with(Map<String, TransformCommandParser> parsers)
    {
        Map<String, TransformCommandParser> copy = new HashMap<>(this.parsers);
        for (Map.Entry<String, TransformCommandParser> entry : ArgumentCheck.nonNull(parsers).entrySet())
        {
            copy.put(normalise(entry.getKey()), ArgumentCheck.nonNull(entry.getValue()));
        }
        return new TransformDslParser(copy, this.types, this.roundScales);
    }

    public TransformDslParser withType(String name, Column.Type type)
    {
        Map<String, Column.Type> copy = new HashMap<>(this.types);
        copy.put(normalise(name), ArgumentCheck.nonNull(type));
        return new TransformDslParser(this.parsers, copy, this.roundScales);
    }

    public TransformDslParser withTypes(Map<String, Column.Type> types)
    {
        Map<String, Column.Type> copy = new HashMap<>(this.types);
        for (Map.Entry<String, Column.Type> entry : ArgumentCheck.nonNull(types).entrySet())
        {
            copy.put(normalise(entry.getKey()), ArgumentCheck.nonNull(entry.getValue()));
        }
        return new TransformDslParser(this.parsers, copy, this.roundScales);
    }

    public <T> TransformDslParser withRoundScale(Class<T> type, ToIntFunction<T> roundScale)
    {
        Map<Class<?>, ToIntFunction<Object>> copy = new HashMap<>(this.roundScales);
        copy.put(ArgumentCheck.nonNull(type), TransformRound.roundScale(type, roundScale));
        return new TransformDslParser(this.parsers, this.types, copy);
    }

    public Transform parse(String line)
    {
        return parse(TokenStream.of(line));
    }

    public Transform parse(TokenStream tokens)
    {
        Token token = tokens.next();
        if (!token.is(TokenType.WORD))
        {
            throw new TransformDslException("Expected transform command", token.position());
        }
        String command = normalise(token.value());
        TransformCommandParser parser = this.parsers.get(command);
        Transform transform;
        if (parser == null)
        {
            if ("convert".equals(command))
            {
                transform = parseConvert(tokens);
            }
            else if ("round".equals(command))
            {
                transform = parseRound(tokens);
            }
            else
            {
                throw new TransformDslException("Unknown transform command '" + token.value() + "'", token.position());
            }
        }
        else
        {
            transform = parser.parse(tokens);
        }
        tokens.expectEnd();
        if (transform == null)
        {
            throw new TransformDslException("Could not create transform '" + token.value() + "'", token.position());
        }
        return transform;
    }

    private static Transform parseAbs(TokenStream tokens)
    {
        String source = tokens.expectValue();
        String conditionColumn = null;
        String target = null;
        while (!tokens.isAtEnd())
        {
            if (tokens.matchWord("when"))
            {
                if (conditionColumn != null)
                {
                    throw new TransformDslException("Duplicate when clause", tokens.peek().position());
                }
                conditionColumn = tokens.expectValue();
            }
            else if (tokens.matchWord("into"))
            {
                if (target != null)
                {
                    throw new TransformDslException("Duplicate into clause", tokens.peek().position());
                }
                target = tokens.expectValue();
            }
            else
            {
                throw new TransformDslException("Expected when, into, or end of statement", tokens.peek().position());
            }
        }
        TransformAbs.Builder builder = TransformAbs.builder(ColumnName.of(source))
                .withNewColumnName(columnName(target));
        return conditionColumn == null ? builder.build() : builder.when(ColumnName.of(conditionColumn)).build();
    }

    private static Transform parseAdd(TokenStream tokens)
    {
        Operand left = operand(tokens.expectValue());
        tokens.expectWord("and");
        Operand right = operand(tokens.expectValue());
        tokens.expectWord("into");
        String target = tokens.expectValue();
        return TransformAdd.of(left, right, ColumnName.of(target));
    }

    private static Transform parseClassify(TokenStream tokens)
    {
        String source = tokens.expectValue();
        tokens.expectWord("matching");
        String pattern = tokens.expectLiteral();
        tokens.expectWord("into");
        String target = tokens.expectValue();
        tokens.expectWord("as");
        String found = tokens.expectValue();
        String notFound = null;
        if (tokens.matchWord("else") || tokens.matchWord("default"))
        {
            notFound = tokens.expectValue();
        }
        return TransformClassify.of(ColumnName.of(source), ColumnName.of(target), Pattern.compile(pattern), found,
                notFound);
    }

    private static Transform parseClean(TokenStream tokens)
    {
        String source = tokens.expectValue();
        String cleanCharacters = null;
        if (tokens.matchWord("using"))
        {
            cleanCharacters = tokens.expectValue();
        }
        String target = optionalInto(tokens, source);
        return TransformClean.of(ColumnName.of(source), columnName(target), cleanCharacters);
    }

    private static Transform parseCoalesce(TokenStream tokens)
    {
        List<String> sources = valuesUntil(tokens, "as", "into");
        String mode = null;
        if (tokens.matchWord("as"))
        {
            mode = tokens.expectValue();
        }
        tokens.expectWord("into");
        String target = tokens.expectValue();
        requireMinimumValueCount("coalesce", sources, 1);
        ColumnObject.Mode columnMode = mode == null ? null : ColumnObject.Mode.parse(mode);
        return TransformCoalesce.of(ColumnName.of(target), columnMode, sources);
    }

    private static Transform parseConcat(TokenStream tokens)
    {
        List<String> sources = valuesUntil(tokens, "using", "into");
        String separator = null;
        if (tokens.matchWord("using"))
        {
            separator = tokens.expectValue();
        }
        tokens.expectWord("into");
        String target = tokens.expectValue();
        return TransformConcat.of(ColumnName.of(target), separator, sources);
    }

    private Transform parseConvert(TokenStream tokens)
    {
        String source = tokens.expectValue();
        tokens.expectWord("to");
        String type = tokens.expectValue();
        String format = null;
        TransformParseMode parseMode = null;
        String target = null;
        while (!tokens.isAtEnd())
        {
            if (tokens.matchWord("using"))
            {
                if (format != null)
                {
                    throw new TransformDslException("Duplicate using clause", tokens.peek().position());
                }
                format = tokens.expectValue();
            }
            else if (tokens.matchWord("by"))
            {
                if (parseMode != null)
                {
                    throw new TransformDslException("Duplicate by clause", tokens.peek().position());
                }
                parseMode = TransformParseMode.parse(tokens.expectValue());
            }
            else if (tokens.matchWord("into"))
            {
                if (target != null)
                {
                    throw new TransformDslException("Duplicate into clause", tokens.peek().position());
                }
                target = tokens.expectValue();
            }
            else
            {
                throw new TransformDslException("Expected using, by, into, or end of statement",
                        tokens.peek().position());
            }
        }
        String normalisedType = normalise(type);
        return switch (normalisedType)
        {
            case "decimal" -> format == null
                    ? TransformToType.builder(ColumnTypes.DECIMAL, ColumnName.of(source))
                            .withNewColumnName(columnName(target)).withMode(ColumnObject.Mode.CATEGORICAL)
                            .withParseMode(parseMode).build()
                    : throwUnsupportedUsing(type, tokens);
            case "double" -> format == null
                    ? parseMode == null
                            ? TransformToDouble.of(ColumnName.of(source), columnName(target))
                            : TransformToPrimitive.builder(ColumnTypes.DOUBLE, ColumnName.of(source))
                                    .withNewColumnName(columnName(target)).withParseMode(parseMode).build()
                    : throwUnsupportedUsing(type, tokens);
            case "int",
                    "integer" ->
                format == null
                        ? parseMode == null
                                ? TransformToInt.of(ColumnName.of(source), columnName(target))
                                : TransformToPrimitive.builder(ColumnTypes.INT, ColumnName.of(source))
                                        .withNewColumnName(columnName(target)).withParseMode(parseMode).build()
                        : throwUnsupportedUsing(type, tokens);
            case "long" -> format == null
                    ? parseMode == null
                            ? TransformToLong.of(ColumnName.of(source), columnName(target))
                            : TransformToPrimitive.builder(ColumnTypes.LONG, ColumnName.of(source))
                                    .withNewColumnName(columnName(target)).withParseMode(parseMode).build()
                    : throwUnsupportedUsing(type, tokens);
            case "date", "localdate" ->
                TransformToLocalDate.builder(ColumnName.of(source)).withNewColumnName(columnName(target))
                        .withFormat(DateFormat.parse(format)).withParseMode(parseMode).build();
            case "string" -> format == null
                    ? TransformToString.of(ColumnName.of(source), columnName(target))
                    : throwUnsupportedUsing(type, tokens);
            default -> parseRegisteredType(source, target, type, normalisedType, format, parseMode, tokens);
        };
    }

    private Transform parseRegisteredType(String source, String target, String type, String normalisedType,
            String format, TransformParseMode parseMode, TokenStream tokens)
    {
        if (format != null)
        {
            return throwUnsupportedUsing(type, tokens);
        }
        Column.Type columnType = this.types.get(normalisedType);
        if (columnType == null)
        {
            throw new TransformDslException("Unknown conversion type '" + type + "'", tokens.peek().position());
        }
        if (columnType.isPrimitive())
        {
            return TransformToPrimitive.builder(columnType, ColumnName.of(source)).withNewColumnName(columnName(target))
                    .withParseMode(parseMode).build();
        }
        return TransformToType.builder(columnType, ColumnName.of(source)).withNewColumnName(columnName(target))
                .withMode(ColumnObject.Mode.CATEGORICAL).withParseMode(parseMode).build();
    }

    private static Transform throwUnsupportedParseMode(String type, TokenStream tokens)
    {
        throw new TransformDslException("Parse mode is not supported for conversion type '" + type + "'",
                tokens.peek().position());
    }

    private static Transform throwUnsupportedUsing(String type, TokenStream tokens)
    {
        throw new TransformDslException("Using is not supported for conversion type '" + type + "'",
                tokens.peek().position());
    }

    private static Transform parseCopy(TokenStream tokens)
    {
        String source = tokens.expectValue();
        tokens.expectWord("into");
        String target = tokens.expectValue();
        return TransformCopy.of(ColumnName.of(source), ColumnName.of(target));
    }

    private static Transform parseConstant(TokenStream tokens)
    {
        Token valueToken = tokens.next();
        if (!valueToken.isValue())
        {
            throw new TransformDslException("Expected value", valueToken.position());
        }
        String value = valueToken.value();
        TransformMetadataConstant.Key metadataKey = valueToken.is(TokenType.WORD)
                ? TransformMetadataConstant.Key.parse(value)
                : null;
        Column.Type type = ColumnTypes.STRING;
        String target = null;
        while (!tokens.isAtEnd())
        {
            if (tokens.matchWord("as"))
            {
                if (!ColumnTypes.STRING.equals(type))
                {
                    throw new TransformDslException("Duplicate as clause", tokens.peek().position());
                }
                type = parseColumnType(tokens.expectValue());
            }
            else if (tokens.matchWord("into"))
            {
                if (target != null)
                {
                    throw new TransformDslException("Duplicate into clause", tokens.peek().position());
                }
                target = tokens.expectValue();
            }
            else
            {
                throw new TransformDslException("Expected as, into, or end of statement", tokens.peek().position());
            }
        }
        if (target == null)
        {
            throw new TransformDslException("Expected into clause", tokens.peek().position());
        }
        if (metadataKey != null)
        {
            if (!ColumnTypes.STRING.equals(type))
            {
                throw new TransformDslException("Metadata constants do not support as", tokens.peek().position());
            }
            return TransformMetadataConstant.of(metadataKey, ColumnName.of(target));
        }
        if (!valueToken.is(TokenType.LITERAL))
        {
            throw new TransformDslException("Expected literal", tokens.peek().position());
        }
        Object parsed = parseConstantValue(type, value, tokens);
        return TransformCreateConstant.of(type, ColumnName.of(target), parsed);
    }

    private static Object parseConstantValue(Column.Type type, String value, TokenStream tokens)
    {
        Object parsed = type.getParser().parse(value);
        if (parsed == null && value != null && !value.isEmpty())
        {
            throw new TransformDslException("Could not parse constant value '" + value + "' as " + type,
                    tokens.peek().position());
        }
        return parsed;
    }

    private static Transform parseDivide(TokenStream tokens)
    {
        Operand left = operand(tokens.expectValue());
        tokens.expectWord("by");
        Operand right = operand(tokens.expectValue());
        tokens.expectWord("into");
        String target = tokens.expectValue();
        return TransformDivide.of(left, right, ColumnName.of(target));
    }

    private static Transform parseExtract(TokenStream tokens)
    {
        tokens.expectWord("from");
        String source = tokens.expectValue();
        tokens.expectWord("matching");
        String pattern = tokens.expectLiteral();
        tokens.expectWord("into");
        String target = tokens.expectValue();
        return TransformExtract.of(ColumnName.of(source), ColumnName.of(target), Pattern.compile(pattern));
    }

    private static Transform parseFlag(TokenStream tokens)
    {
        ConditionExpression condition = parseConditionExpression(tokens);
        tokens.expectWord("into");
        return TransformFlag.builder(condition).withNewColumnName(ColumnName.of(tokens.expectValue())).build();
    }

    private static ConditionExpression parseConditionExpression(TokenStream tokens)
    {
        ConditionExpression condition = parseAndCondition(tokens);
        while (tokens.matchWord("or"))
        {
            condition = new LogicalCondition(condition, LogicalCondition.Operator.OR, parseAndCondition(tokens));
        }
        return condition;
    }

    private static ConditionExpression parseAndCondition(TokenStream tokens)
    {
        ConditionExpression condition = parseComparisonCondition(tokens);
        while (tokens.matchWord("and"))
        {
            condition = new LogicalCondition(condition, LogicalCondition.Operator.AND,
                    parseComparisonCondition(tokens));
        }
        return condition;
    }

    private static ConditionExpression parseComparisonCondition(TokenStream tokens)
    {
        String source = tokens.expectValue();
        Column.Operator operator = parseConditionOperator(tokens);
        return new ComparisonCondition(ColumnName.of(source), operator, conditionValues(tokens, operator));
    }

    private static Column.Operator parseConditionOperator(TokenStream tokens)
    {
        if (tokens.peek().is(TokenType.OPERATOR))
        {
            return Column.Operator.parse(tokens.expectOperator());
        }
        if (tokens.matchWord("not"))
        {
            tokens.expectWord("in");
            return Column.Operator.NOT_IN;
        }
        Token token = tokens.next();
        if (!token.is(TokenType.WORD))
        {
            throw new TransformDslException("Expected comparison operator", token.position());
        }
        return Column.Operator.parse(token.value());
    }

    private static String[] conditionValues(TokenStream tokens, Column.Operator operator)
    {
        List<String> values = new ArrayList<>();
        values.add(tokens.expectValue());
        while ((operator == Column.Operator.IN || operator == Column.Operator.NOT_IN) && tokens.match(TokenType.COMMA))
        {
            values.add(tokens.expectValue());
        }
        return values.toArray(String[]::new);
    }

    private static Transform parseLowercase(TokenStream tokens)
    {
        String source = tokens.expectValue();
        String target = optionalInto(tokens, source);
        return TransformToLowerCase.of(ColumnName.of(source), columnName(target));
    }

    private static Transform parseMultiply(TokenStream tokens)
    {
        Operand left = operand(tokens.expectValue());
        tokens.expectWord("by");
        Operand right = operand(tokens.expectValue());
        tokens.expectWord("into");
        String target = tokens.expectValue();
        return TransformMultiply.of(left, right, ColumnName.of(target));
    }

    private static Transform parseNegate(TokenStream tokens)
    {
        String source = tokens.expectValue();
        String conditionColumn = null;
        String target = null;
        while (!tokens.isAtEnd())
        {
            if (tokens.matchWord("when"))
            {
                if (conditionColumn != null)
                {
                    throw new TransformDslException("Duplicate when clause", tokens.peek().position());
                }
                conditionColumn = tokens.expectValue();
            }
            else if (tokens.matchWord("into"))
            {
                if (target != null)
                {
                    throw new TransformDslException("Duplicate into clause", tokens.peek().position());
                }
                target = tokens.expectValue();
            }
            else
            {
                throw new TransformDslException("Expected when, into, or end of statement", tokens.peek().position());
            }
        }
        TransformNegate.Builder builder = TransformNegate.builder(ColumnName.of(source))
                .withNewColumnName(columnName(target));
        return conditionColumn == null ? builder.build() : builder.when(ColumnName.of(conditionColumn)).build();
    }

    private static Transform parseNormalise(TokenStream tokens)
    {
        String source = tokens.expectValue();
        String target = optionalInto(tokens, source);
        return target == null
                ? TransformNormalise.of(ColumnName.of(source))
                : TransformNormalise.of(ColumnName.of(source), ColumnName.of(target));
    }

    private static Transform parsePrefix(TokenStream tokens)
    {
        String source = tokens.expectValue();
        tokens.expectWord("with");
        String prefix = tokens.expectValue();
        String target = optionalInto(tokens, source);
        return TransformPrefix.of(prefix, ColumnName.of(source), columnName(target));
    }

    private static Transform parseReplace(TokenStream tokens)
    {
        boolean all = tokens.matchWord("all");
        String targetText = all ? tokens.expectLiteral() : tokens.expectValue();
        tokens.expectWord("with");
        String replacement = tokens.expectValue();
        tokens.expectWord("in");
        String source = tokens.expectValue();
        String target = optionalInto(tokens, source);
        return all
                ? replaceAll(ColumnName.of(source), columnName(target), targetText, replacement)
                : replace(ColumnName.of(source), columnName(target), targetText, replacement);
    }

    private Transform parseRound(TokenStream tokens)
    {
        String source = tokens.expectValue();
        Integer scale = null;
        String scaleColumn = null;
        if (tokens.matchWord("to"))
        {
            scale = Integer.parseInt(tokens.expectValue());
        }
        else if (tokens.matchWord("using"))
        {
            scaleColumn = tokens.expectValue();
        }
        else
        {
            throw new TransformDslException("Expected to or using", tokens.peek().position());
        }
        RoundingMode roundingMode = null;
        String conditionColumn = null;
        String target = null;
        while (!tokens.isAtEnd())
        {
            if (tokens.matchWord("by"))
            {
                if (roundingMode != null)
                {
                    throw new TransformDslException("Duplicate by clause", tokens.peek().position());
                }
                roundingMode = TransformRound.parseRoundingMode(tokens.expectValue());
            }
            else if (tokens.matchWord("when"))
            {
                if (conditionColumn != null)
                {
                    throw new TransformDslException("Duplicate when clause", tokens.peek().position());
                }
                conditionColumn = tokens.expectValue();
            }
            else if (tokens.matchWord("into"))
            {
                if (target != null)
                {
                    throw new TransformDslException("Duplicate into clause", tokens.peek().position());
                }
                target = tokens.expectValue();
            }
            else
            {
                throw new TransformDslException("Expected by, when, into, or end of statement",
                        tokens.peek().position());
            }
        }
        return TransformRound.builder(ColumnName.of(source)).withScale(scale)
                .withScaleColumnName(columnName(scaleColumn)).withNewColumnName(columnName(target))
                .when(columnName(conditionColumn)).withRoundingMode(roundingMode).withRoundScales(this.roundScales)
                .build();
    }

    private static Transform parseSplit(TokenStream tokens)
    {
        String source = tokens.expectValue();
        tokens.expectWord("on");
        String separator = tokens.expectValue();
        tokens.expectWord("into");
        List<String> targets = commaSeparatedValues(tokens);
        return TransformSplit.of(ColumnName.of(source), separator, targets);
    }

    private static Transform parseStrip(TokenStream tokens)
    {
        String source = tokens.expectValue();
        String target = null;
        String stripCharacters = null;
        while (!tokens.isAtEnd())
        {
            if (tokens.matchWord("using"))
            {
                if (stripCharacters != null)
                {
                    throw new TransformDslException("Duplicate using clause", tokens.peek().position());
                }
                stripCharacters = tokens.expectLiteral();
            }
            else if (tokens.matchWord("into"))
            {
                if (target != null)
                {
                    throw new TransformDslException("Duplicate into clause", tokens.peek().position());
                }
                target = tokens.expectValue();
            }
            else
            {
                throw new TransformDslException("Expected using, into, or end of statement", tokens.peek().position());
            }
        }
        return TransformStrip.of(ColumnName.of(source), columnName(target), stripCharacters);
    }

    private static Transform parseSubstitute(TokenStream tokens)
    {
        String source = tokens.expectValue();
        tokens.expectWord("using");
        Map<String, String> replacements = parseMap(tokens);
        String defaultValue = null;
        if (tokens.matchWord("default") || tokens.matchWord("else"))
        {
            defaultValue = tokens.expectValue();
        }
        tokens.expectWord("into");
        String target = tokens.expectValue();
        return TransformSubstitute.of(ColumnName.of(source), ColumnName.of(target), replacements, defaultValue);
    }

    private static Transform parseSubtract(TokenStream tokens)
    {
        Operand right = operand(tokens.expectValue());
        tokens.expectWord("from");
        Operand left = operand(tokens.expectValue());
        tokens.expectWord("into");
        String target = tokens.expectValue();
        return TransformSubtract.of(left, right, ColumnName.of(target));
    }

    private static Transform parseSuffix(TokenStream tokens)
    {
        String source = tokens.expectValue();
        tokens.expectWord("with");
        String suffix = tokens.expectValue();
        String target = optionalInto(tokens, source);
        return TransformSuffix.of(suffix, ColumnName.of(source), columnName(target));
    }

    private static Transform parseTake(TokenStream tokens)
    {
        if (tokens.matchWord("left"))
        {
            String length = tokens.expectValue();
            tokens.expectWord("from");
            String source = tokens.expectValue();
            tokens.expectWord("into");
            String target = tokens.expectValue();
            return TransformLeft.of(ColumnName.of(source), ColumnName.of(target), Integer.parseInt(length));
        }
        if (tokens.matchWord("right"))
        {
            String length = tokens.expectValue();
            tokens.expectWord("from");
            String source = tokens.expectValue();
            tokens.expectWord("into");
            String target = tokens.expectValue();
            return TransformRight.of(ColumnName.of(source), ColumnName.of(target), Integer.parseInt(length));
        }
        if (tokens.matchWord("substring"))
        {
            String first = tokens.expectValue();
            tokens.expect(TokenType.COMMA);
            String last = tokens.expectValue();
            tokens.expectWord("from");
            String source = tokens.expectValue();
            tokens.expectWord("into");
            String target = tokens.expectValue();
            return TransformSubstring.of(ColumnName.of(source), ColumnName.of(target), Integer.parseInt(first),
                    Integer.parseInt(last));
        }
        if (tokens.matchWord("before"))
        {
            String delimiter = tokens.expectValue();
            tokens.expectWord("from");
            String source = tokens.expectValue();
            tokens.expectWord("into");
            String target = tokens.expectValue();
            return TransformBefore.of(ColumnName.of(source), ColumnName.of(target), delimiter);
        }
        if (tokens.matchWord("after"))
        {
            String delimiter = tokens.expectValue();
            tokens.expectWord("from");
            String source = tokens.expectValue();
            tokens.expectWord("into");
            String target = tokens.expectValue();
            return TransformAfter.of(ColumnName.of(source), ColumnName.of(target), delimiter);
        }
        throw new TransformDslException("Expected take operation", tokens.peek().position());
    }

    private static Transform parseUppercase(TokenStream tokens)
    {
        String source = tokens.expectValue();
        String target = optionalInto(tokens, source);
        return TransformToUpperCase.of(ColumnName.of(source), columnName(target));
    }

    private static List<String> commaSeparatedValues(TokenStream tokens)
    {
        List<String> values = new ArrayList<>();
        values.add(tokens.expectValue());
        while (tokens.match(TokenType.COMMA))
        {
            values.add(tokens.expectValue());
        }
        return values;
    }

    private static Map<String, String> parseMap(TokenStream tokens)
    {
        Map<String, String> values = new LinkedHashMap<>();
        String key = tokens.expectValue();
        tokens.expect(TokenType.COLON);
        values.put(key, tokens.expectValue());
        while (tokens.match(TokenType.COMMA))
        {
            key = tokens.expectValue();
            tokens.expect(TokenType.COLON);
            values.put(key, tokens.expectValue());
        }
        return values;
    }

    private static void requireMinimumValueCount(String command, List<String> values, int count)
    {
        if (values.size() < count)
        {
            throw new TransformDslException(
                    command + " expects at least " + count + " values, but found " + values.size(), 0);
        }
    }

    private static String optionalInto(TokenStream tokens, String fallback)
    {
        if (tokens.matchWord("into"))
        {
            return tokens.expectValue();
        }
        return null;
    }

    private static ColumnName columnName(String name)
    {
        return name == null ? null : ColumnName.of(name);
    }

    private static Transform replace(ColumnName source, ColumnName target, String targetText, String replacement)
    {
        return target == null
                ? TransformStringReplace.of(source, targetText, replacement)
                : TransformStringReplace.of(source, target, targetText, replacement);
    }

    private static Transform replaceAll(ColumnName source, ColumnName target, String targetText, String replacement)
    {
        return target == null
                ? TransformStringReplaceAll.of(source, targetText, replacement)
                : TransformStringReplaceAll.of(source, target, targetText, replacement);
    }

    private static List<String> valuesUntil(TokenStream tokens, String... words)
    {
        List<String> values = new ArrayList<>();
        values.add(tokens.expectValue());
        while (tokens.match(TokenType.COMMA))
        {
            values.add(tokens.expectValue());
        }
        if (!isAnyWord(tokens.peek(), words))
        {
            throw new TransformDslException("Expected '" + String.join("' or '", words) + "'",
                    tokens.peek().position());
        }
        return values;
    }

    private static boolean isAnyWord(Token token, String... words)
    {
        for (String word : words)
        {
            if (token.isWord(word))
            {
                return true;
            }
        }
        return false;
    }

    private static Operand operand(String value)
    {
        try
        {
            return Operand.value(new BigDecimal(value));
        }
        catch (NumberFormatException e)
        {
            return Operand.column(ColumnName.of(value));
        }
    }

    private static String normalise(String command)
    {
        return ArgumentCheck.nonEmpty(command).toLowerCase(Locale.ROOT);
    }

    private static Map<String, Column.Type> standardTypes()
    {
        Map<String, Column.Type> types = new HashMap<>();
        types.put("boolean", ColumnTypes.BOOLEAN);
        types.put("byte", ColumnTypes.BYTE);
        types.put("decimal", ColumnTypes.DECIMAL);
        types.put("bigdecimal", ColumnTypes.DECIMAL);
        types.put("double", ColumnTypes.DOUBLE);
        types.put("int", ColumnTypes.INT);
        types.put("integer", ColumnTypes.INT);
        types.put("instant", ColumnTypes.INSTANT);
        types.put("localdate", ColumnTypes.LOCALDATE);
        types.put("date", ColumnTypes.LOCALDATE);
        types.put("localdatetime", ColumnTypes.LOCAL_DATE_TIME);
        types.put("localtime", ColumnTypes.LOCAL_TIME);
        types.put("long", ColumnTypes.LONG);
        types.put("offsetdatetime", ColumnTypes.OFFSET_DATE_TIME);
        types.put("period", ColumnTypes.PERIOD);
        types.put("string", ColumnTypes.STRING);
        types.put("yearmonth", ColumnTypes.YEAR_MONTH);
        types.put("currency", ColumnTypes.CURRENCY);
        return types;
    }

    private static Map<Class<?>, ToIntFunction<Object>> standardRoundScales()
    {
        return TransformRound.roundScales(Currency.class, Currency::getDefaultFractionDigits);
    }

    private static Column.Type parseColumnType(String type)
    {
        Column.Type columnType = standardTypes().get(normalise(type));
        if (columnType == null)
        {
            throw new TransformDslException("Unknown constant type '" + type + "'", 0);
        }
        return columnType;
    }
}
