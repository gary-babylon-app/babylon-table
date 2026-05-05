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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.dsl.ComparisonCondition;
import app.babylon.table.dsl.ConditionExpression;
import app.babylon.table.dsl.LogicalCondition;
import app.babylon.table.dsl.TransformDslException;
import app.babylon.table.transform.DateFormat;
import app.babylon.table.transform.Transform;
import app.babylon.table.transform.TransformAbs;
import app.babylon.table.transform.TransformAdd;
import app.babylon.table.transform.TransformAfter;
import app.babylon.table.transform.TransformBefore;
import app.babylon.table.transform.TransformClean;
import app.babylon.table.transform.TransformClassify;
import app.babylon.table.transform.TransformCoalesce;
import app.babylon.table.transform.TransformConcat;
import app.babylon.table.transform.TransformCopy;
import app.babylon.table.transform.TransformConstant;
import app.babylon.table.transform.TransformDecimalBinaryOperator.Operand;
import app.babylon.table.transform.TransformDivide;
import app.babylon.table.transform.TransformExtract;
import app.babylon.table.transform.TransformExtractFromColumnName;
import app.babylon.table.transform.TransformFlag;
import app.babylon.table.transform.TransformLeft;
import app.babylon.table.transform.TransformMultiply;
import app.babylon.table.transform.TransformNegate;
import app.babylon.table.transform.TransformNormalise;
import app.babylon.text.Sentence.ParseMode;
import app.babylon.table.transform.TransformPrefix;
import app.babylon.table.transform.TransformRemove;
import app.babylon.table.transform.TransformRetain;
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
import app.babylon.table.transform.TransformToLocalDate;
import app.babylon.table.transform.TransformToLowerCase;
import app.babylon.table.transform.TransformAnyToString;
import app.babylon.table.transform.TransformStringToType;
import app.babylon.table.transform.TransformToUpperCase;

/**
 * Writes transforms as canonical transformation DSL statements.
 */
public final class TransformDslWriter
{
    private final Map<Class<?>, Function<Transform, String>> writers;

    private TransformDslWriter(Map<Class<?>, Function<Transform, String>> writers)
    {
        this.writers = Map.copyOf(writers);
    }

    public static TransformDslWriter standard()
    {
        Map<Class<?>, Function<Transform, String>> writers = new java.util.HashMap<>();
        writers.put(TransformAbs.class, TransformDslWriter::writeAbs);
        writers.put(TransformAdd.class, t -> writeDecimal("add", "and", t));
        writers.put(TransformAfter.class, TransformDslWriter::writeAfter);
        writers.put(TransformBefore.class, TransformDslWriter::writeBefore);
        writers.put(TransformClassify.class, TransformDslWriter::writeClassify);
        writers.put(TransformClean.class, TransformDslWriter::writeClean);
        writers.put(TransformCoalesce.class, TransformDslWriter::writeCoalesce);
        writers.put(TransformConcat.class, TransformDslWriter::writeConcat);
        writers.put(TransformCopy.class, TransformDslWriter::writeCopy);
        writers.put(TransformConstant.class, TransformDslWriter::writeConstant);
        writers.put(TransformDivide.class, t -> writeDecimal("divide", "by", t));
        writers.put(TransformExtract.class, TransformDslWriter::writeExtract);
        writers.put(TransformExtractFromColumnName.class, TransformDslWriter::writeExtractFromColumnName);
        writers.put(TransformFlag.class, TransformDslWriter::writeFlag);
        writers.put(TransformLeft.class, TransformDslWriter::writeLeft);
        writers.put(TransformMultiply.class, t -> writeDecimal("multiply", "by", t));
        writers.put(TransformNegate.class, TransformDslWriter::writeNegate);
        writers.put(TransformNormalise.class, TransformDslWriter::writeNormalise);
        writers.put(TransformPrefix.class, TransformDslWriter::writePrefix);
        writers.put(TransformRemove.class, TransformDslWriter::writeRemove);
        writers.put(TransformRetain.class, TransformDslWriter::writeRetain);
        writers.put(TransformRight.class, TransformDslWriter::writeRight);
        writers.put(TransformRound.class, TransformDslWriter::writeRound);
        writers.put(TransformSplit.class, TransformDslWriter::writeSplit);
        writers.put(TransformStringReplace.class, TransformDslWriter::writeReplace);
        writers.put(TransformStringReplaceAll.class, TransformDslWriter::writeReplaceAll);
        writers.put(TransformStrip.class, TransformDslWriter::writeStrip);
        writers.put(TransformSubstitute.class, TransformDslWriter::writeSubstitute);
        writers.put(TransformSubstring.class, TransformDslWriter::writeSubstring);
        writers.put(TransformSubtract.class, t -> writeDecimal("subtract", "from", t));
        writers.put(TransformSuffix.class, TransformDslWriter::writeSuffix);
        writers.put(TransformMetadataConstant.class, TransformDslWriter::writeMetadataConstant);
        writers.put(TransformToLocalDate.class, TransformDslWriter::writeDate);
        writers.put(TransformToLowerCase.class, TransformDslWriter::writeLowercase);
        writers.put(TransformAnyToString.class, t -> writeConvert("String", t));
        writers.put(TransformStringToType.class, TransformDslWriter::writeStringToType);
        writers.put(TransformToUpperCase.class, TransformDslWriter::writeUppercase);
        return new TransformDslWriter(writers);
    }

    public TransformDslWriter with(Class<? extends Transform> transformClass, Function<Transform, String> writer)
    {
        Map<Class<?>, Function<Transform, String>> copy = new java.util.HashMap<>(this.writers);
        copy.put(ArgumentCheck.nonNull(transformClass), ArgumentCheck.nonNull(writer));
        return new TransformDslWriter(copy);
    }

    public String write(Transform transform)
    {
        if (transform == null)
        {
            return null;
        }
        Function<Transform, String> writer = this.writers.get(transform.getClass());
        if (writer == null)
        {
            throw new TransformDslException("No DSL writer for " + transform.getClass().getName(), 0);
        }
        return writer.apply(transform);
    }

    public List<String> writeAll(Collection<? extends Transform> transforms)
    {
        List<String> lines = new ArrayList<>();
        if (transforms != null)
        {
            for (Transform transform : transforms)
            {
                lines.add(write(transform));
            }
        }
        return lines;
    }

    public String format(String line)
    {
        return write(TransformDslParser.standard().parse(line));
    }

    private static String writeAbs(Transform transform)
    {
        TransformAbs abs = (TransformAbs) transform;
        ColumnName source = abs.columnName();
        String line = "abs " + column(source);
        if (abs.conditionColumnName() != null)
        {
            line += " when " + column(abs.conditionColumnName());
        }
        return line + into(source, abs.newColumnName());
    }

    private static String writeClassify(Transform transform)
    {
        TransformClassify classify = (TransformClassify) transform;
        Pattern pattern = classify.pattern();
        String found = classify.newColumnFoundValue();
        String notFound = classify.newColumnNotFoundValue();
        String line = "classify " + column(classify.existingColumnName()) + " matching " + literal(pattern.pattern())
                + " into " + column(classify.effectiveNewColumnName()) + " as " + value(found);
        if (notFound != null)
        {
            line += " default " + value(notFound);
        }
        return line;
    }

    private static String writeClean(Transform transform)
    {
        TransformClean clean = (TransformClean) transform;
        String line = "clean " + column(clean.existingColumnName());
        if (clean.cleanCharacters() != null)
        {
            line += " using " + literal(clean.cleanCharacters());
        }
        return line + into(clean.existingColumnName(), clean.newColumnName());
    }

    private static String writeCoalesce(Transform transform)
    {
        TransformCoalesce coalesce = (TransformCoalesce) transform;
        ColumnName[] columns = coalesce.columnNames();
        ColumnObject.Mode mode = coalesce.mode();
        String line = "coalesce " + columns(columns);
        if (mode != null)
        {
            line += " as " + mode.name();
        }
        return line + " into " + column(coalesce.newColumnName());
    }

    private static String writeConcat(Transform transform)
    {
        TransformConcat concat = (TransformConcat) transform;
        ColumnName target = concat.concatColumn();
        String separator = concat.separator();
        String line = "concat " + concatParts(concat.sourceColumns(), concat.literalValues());
        if (separator != null)
        {
            line += " using " + literal(separator);
        }
        return line + " into " + column(target);
    }

    private static String writeCopy(Transform transform)
    {
        TransformCopy copy = (TransformCopy) transform;
        return "copy " + column(copy.columnToCopy()) + " into " + column(copy.newCopyName());
    }

    private static String writeConstant(Transform transform)
    {
        TransformConstant constant = (TransformConstant) transform;
        String line = "constant " + literal(String.valueOf(constant.value()));
        if (!ColumnTypes.STRING.equals(constant.type()))
        {
            line += " as " + typeName(constant.type());
        }
        return line + " into " + column(constant.newColumnName());
    }

    private static String writeMetadataConstant(Transform transform)
    {
        TransformMetadataConstant metadata = (TransformMetadataConstant) transform;
        return "constant " + metadata.key().dslName() + " into " + column(metadata.newColumnName());
    }

    private static String writeRemove(Transform transform)
    {
        TransformRemove remove = (TransformRemove) transform;
        return "remove " + columns(remove.columnNames());
    }

    private static String writeRetain(Transform transform)
    {
        TransformRetain retain = (TransformRetain) transform;
        return "retain " + columns(retain.columnNames());
    }

    private static String writeExtract(Transform transform)
    {
        TransformExtract extract = (TransformExtract) transform;
        Pattern pattern = extract.pattern();
        return "extract from " + column(extract.existingColumnName()) + " matching " + literal(pattern.pattern())
                + " into " + column(extract.effectiveNewColumnName());
    }

    private static String writeExtractFromColumnName(Transform transform)
    {
        TransformExtractFromColumnName extract = (TransformExtractFromColumnName) transform;
        String line = "extract from columnName " + column(extract.sourceColumnName()) + " using "
                + literal(extract.pattern().pattern());
        if (!ColumnTypes.STRING.equals(extract.type()))
        {
            line += " as " + typeName(extract.type());
        }
        return line + " into " + column(extract.newColumnName());
    }

    private static String writeFlag(Transform transform)
    {
        TransformFlag flag = (TransformFlag) transform;
        return "flag " + condition(flag.condition()) + " into " + column(flag.newColumnName());
    }

    private static String condition(ConditionExpression condition)
    {
        if (condition instanceof ComparisonCondition comparison)
        {
            return comparison(comparison);
        }
        if (condition instanceof LogicalCondition logical)
        {
            return condition(logical.left()) + " " + logical.operator().name().toLowerCase() + " "
                    + condition(logical.right());
        }
        return condition.toDsl();
    }

    private static String comparison(ComparisonCondition comparison)
    {
        String line = column(comparison.columnName()) + " " + comparison.operator().text() + " ";
        String[] values = comparison.values();
        List<String> formatted = new ArrayList<>();
        for (String value : values)
        {
            formatted.add(conditionValue(value));
        }
        return line + String.join(", ", formatted);
    }

    private static String writeLeft(Transform transform)
    {
        TransformLeft left = (TransformLeft) transform;
        return "take left " + left.length() + " from " + column(left.existingColumnName()) + " into "
                + column(left.effectiveNewColumnName());
    }

    private static String writeRight(Transform transform)
    {
        TransformRight right = (TransformRight) transform;
        return "take right " + right.length() + " from " + column(right.existingColumnName()) + " into "
                + column(right.effectiveNewColumnName());
    }

    private static String writeSubstring(Transform transform)
    {
        TransformSubstring substring = (TransformSubstring) transform;
        return "take substring " + substring.first() + ", " + substring.last() + " from "
                + column(substring.existingColumnName()) + " into " + column(substring.newColumnName());
    }

    private static String writeBefore(Transform transform)
    {
        TransformBefore before = (TransformBefore) transform;
        return "take before " + value(before.delimiter()) + " from " + column(before.existingColumnName()) + " into "
                + column(before.effectiveNewColumnName());
    }

    private static String writeAfter(Transform transform)
    {
        TransformAfter after = (TransformAfter) transform;
        return "take after " + value(after.delimiter()) + " from " + column(after.existingColumnName()) + " into "
                + column(after.effectiveNewColumnName());
    }

    private static String writePrefix(Transform transform)
    {
        TransformPrefix prefix = (TransformPrefix) transform;
        return "prefix " + column(prefix.existingColumnName()) + " with " + value(prefix.prefix())
                + into(prefix.existingColumnName(), prefix.newColumnName());
    }

    private static String writeSuffix(Transform transform)
    {
        TransformSuffix suffix = (TransformSuffix) transform;
        return "suffix " + column(suffix.existingColumnName()) + " with " + value(suffix.suffix())
                + into(suffix.existingColumnName(), suffix.newColumnName());
    }

    private static String writeReplace(Transform transform)
    {
        TransformStringReplace replace = (TransformStringReplace) transform;
        return "replace " + value(replace.target()) + " with " + value(replace.replacement()) + " in "
                + column(replace.existingColumnName()) + into(replace.existingColumnName(), replace.newColumnName());
    }

    private static String writeReplaceAll(Transform transform)
    {
        TransformStringReplaceAll replace = (TransformStringReplaceAll) transform;
        return "replace all " + literal(replace.target()) + " with " + value(replace.replacement()) + " in "
                + column(replace.existingColumnName()) + into(replace.existingColumnName(), replace.newColumnName());
    }

    private static String writeSplit(Transform transform)
    {
        TransformSplit split = (TransformSplit) transform;
        String line = "split " + column(split.getColumnToSplit()) + " on " + literal(split.getSplitOn());
        if (split.getMode() != TransformSplit.Mode.ALL)
        {
            line += " by " + split.getMode().name().toLowerCase(java.util.Locale.ROOT);
        }
        return line + " into " + columns(split.getSplitColumnNames());
    }

    private static String writeStrip(Transform transform)
    {
        TransformStrip strip = (TransformStrip) transform;
        String line = "strip " + column(strip.existingColumnName());
        if (strip.stripCharacters() != null)
        {
            line += " using " + literal(strip.stripCharacters());
        }
        return line + into(strip.existingColumnName(), strip.newColumnName());
    }

    private static String writeSubstitute(Transform transform)
    {
        TransformSubstitute substitute = (TransformSubstitute) transform;
        Map<String, String> replaces = substitute.replaces();
        String line = "substitute " + column(substitute.columnName()) + " using " + map(replaces);
        String defaultValue = substitute.defaultValueNewColumn();
        if (defaultValue != null)
        {
            line += " default " + value(defaultValue);
        }
        return line + " into " + column(substitute.newColumnName());
    }

    private static String writeUppercase(Transform transform)
    {
        TransformToUpperCase uppercase = (TransformToUpperCase) transform;
        return "uppercase " + column(uppercase.existingColumnName())
                + into(uppercase.existingColumnName(), uppercase.newColumnName());
    }

    private static String writeLowercase(Transform transform)
    {
        TransformToLowerCase lowercase = (TransformToLowerCase) transform;
        return "lowercase " + column(lowercase.existingColumnName())
                + into(lowercase.existingColumnName(), lowercase.newColumnName());
    }

    private static String writeNegate(Transform transform)
    {
        TransformNegate negate = (TransformNegate) transform;
        String line = "negate " + column(negate.columnName());
        if (negate.conditionColumnName() != null)
        {
            line += " when " + column(negate.conditionColumnName());
        }
        return line + into(negate.columnName(), negate.newColumnName());
    }

    private static String writeNormalise(Transform transform)
    {
        TransformNormalise normalise = (TransformNormalise) transform;
        return "normalise " + column(normalise.columnName()) + into(normalise.columnName(), normalise.newColumnName());
    }

    private static String writeRound(Transform transform)
    {
        TransformRound round = (TransformRound) transform;
        String line = "round " + column(round.columnName());
        if (round.scaleColumnName() == null)
        {
            line += " to " + round.scale();
        }
        else
        {
            line += " using " + column(round.scaleColumnName());
        }
        if (round.roundingMode() != null)
        {
            line += " by " + TransformRound.roundingModeName(round.roundingMode());
        }
        if (round.conditionColumnName() != null)
        {
            line += " when " + column(round.conditionColumnName());
        }
        return line + into(round.columnName(), round.newColumnName());
    }

    private static String writeDecimal(String command, String word, Transform transform)
    {
        Operand left = leftOperand(transform);
        Operand right = rightOperand(transform);
        ColumnName target = newColumnName(transform);
        if (transform instanceof TransformSubtract)
        {
            return command + " " + operand(right) + " " + word + " " + operand(left) + " into " + column(target);
        }
        return command + " " + operand(left) + " " + word + " " + operand(right) + " into " + column(target);
    }

    private static String writeStringToType(Transform transform)
    {
        TransformStringToType<?> toType = (TransformStringToType<?>) transform;
        return writeConvert(typeName(toType.type()), toType);
    }

    private static String writeDate(Transform transform)
    {
        TransformToLocalDate date = (TransformToLocalDate) transform;
        ColumnName[] sources = date.columnNames();
        DateFormat format = date.format();
        ParseMode parseMode = date.parseMode();
        String line = "convert " + column(sources[0]) + " to Date";
        if (format != null && format != DateFormat.Unknown)
        {
            line += " using " + format.name();
        }
        if (parseMode != null && parseMode != ParseMode.EXACT)
        {
            line += " by " + parseModeName(parseMode);
        }
        return line + into(sources[0], date.newColumnName());
    }

    private static String writeConvert(String type, Transform transform)
    {
        ColumnName source = firstConfiguredSource(transform);
        ColumnName target = firstConfiguredTarget(transform);
        String line = "convert " + column(source) + " to " + type;
        ParseMode parseMode = parseMode(transform);
        if (parseMode != null && parseMode != ParseMode.EXACT)
        {
            line += " by " + parseModeName(parseMode);
        }
        return line + into(source, target);
    }

    private static ColumnName firstConfiguredSource(Transform transform)
    {
        if (transform instanceof TransformStringToType<?> toType)
        {
            return toType.columnName();
        }
        if (transform instanceof TransformAnyToString toString)
        {
            return toString.columnNames()[0];
        }
        throw new TransformDslException("No configured source for " + transform.getClass().getName(), 0);
    }

    private static ColumnName firstConfiguredTarget(Transform transform)
    {
        if (transform instanceof TransformStringToType<?> toType)
        {
            return toType.newColumnName();
        }
        if (transform instanceof TransformAnyToString toString)
        {
            Map<ColumnName, ColumnName> namesBySource = toString.newColumnNames();
            return namesBySource == null ? null : namesBySource.get(firstConfiguredSource(transform));
        }
        return null;
    }

    private static ParseMode parseMode(Transform transform)
    {
        if (transform instanceof TransformStringToType<?> toType)
        {
            return toType.parseMode();
        }
        return null;
    }

    private static ColumnName leftColumnName(Transform transform)
    {
        if (transform instanceof TransformAdd add)
        {
            return add.leftColumnName();
        }
        if (transform instanceof TransformDivide divide)
        {
            return divide.leftColumnName();
        }
        if (transform instanceof TransformMultiply multiply)
        {
            return multiply.leftColumnName();
        }
        if (transform instanceof TransformSubtract subtract)
        {
            return subtract.leftColumnName();
        }
        throw new TransformDslException("No left column for " + transform.getClass().getName(), 0);
    }

    private static Operand leftOperand(Transform transform)
    {
        if (transform instanceof TransformAdd add)
        {
            return add.left();
        }
        if (transform instanceof TransformDivide divide)
        {
            return divide.left();
        }
        if (transform instanceof TransformMultiply multiply)
        {
            return multiply.left();
        }
        if (transform instanceof TransformSubtract subtract)
        {
            return subtract.left();
        }
        throw new TransformDslException("No left operand for " + transform.getClass().getName(), 0);
    }

    private static ColumnName rightColumnName(Transform transform)
    {
        if (transform instanceof TransformAdd add)
        {
            return add.rightColumnName();
        }
        if (transform instanceof TransformDivide divide)
        {
            return divide.rightColumnName();
        }
        if (transform instanceof TransformMultiply multiply)
        {
            return multiply.rightColumnName();
        }
        if (transform instanceof TransformSubtract subtract)
        {
            return subtract.rightColumnName();
        }
        throw new TransformDslException("No right column for " + transform.getClass().getName(), 0);
    }

    private static Operand rightOperand(Transform transform)
    {
        if (transform instanceof TransformAdd add)
        {
            return add.right();
        }
        if (transform instanceof TransformDivide divide)
        {
            return divide.right();
        }
        if (transform instanceof TransformMultiply multiply)
        {
            return multiply.right();
        }
        if (transform instanceof TransformSubtract subtract)
        {
            return subtract.right();
        }
        throw new TransformDslException("No right operand for " + transform.getClass().getName(), 0);
    }

    private static ColumnName newColumnName(Transform transform)
    {
        if (transform instanceof TransformAdd add)
        {
            return add.newColumnName();
        }
        if (transform instanceof TransformDivide divide)
        {
            return divide.newColumnName();
        }
        if (transform instanceof TransformMultiply multiply)
        {
            return multiply.newColumnName();
        }
        if (transform instanceof TransformSubtract subtract)
        {
            return subtract.newColumnName();
        }
        throw new TransformDslException("No new column for " + transform.getClass().getName(), 0);
    }

    private static ColumnName firstOrNull(ColumnName[] names)
    {
        return names == null || names.length == 0 ? null : names[0];
    }

    private static String into(ColumnName source, ColumnName target)
    {
        if (target == null || target.equals(source))
        {
            return "";
        }
        return " into " + column(target);
    }

    private static String map(Map<String, String> map)
    {
        List<String> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList())
        {
            entries.add(literal(entry.getKey()) + ":" + literal(entry.getValue()));
        }
        return String.join(", ", entries);
    }

    private static String columns(ColumnName[] names)
    {
        List<String> values = new ArrayList<>();
        for (ColumnName name : names)
        {
            values.add(column(name));
        }
        return String.join(", ", values);
    }

    private static String concatParts(ColumnName[] sourceColumns, String[] literalValues)
    {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < sourceColumns.length; ++i)
        {
            values.add(sourceColumns[i] == null ? literal(literalValues[i]) : column(sourceColumns[i]));
        }
        return String.join(", ", values);
    }

    private static String column(ColumnName name)
    {
        return name.getValue();
    }

    private static String operand(Operand operand)
    {
        return operand.isColumn() ? column(operand.columnName()) : operand.value().toPlainString();
    }

    private static String value(String value)
    {
        return simple(value) ? value : literal(value);
    }

    private static String conditionValue(String value)
    {
        return simple(value) || number(value) ? value : literal(value);
    }

    private static boolean simple(String value)
    {
        return value != null && value.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    private static boolean number(String value)
    {
        return value != null && value.matches("-?[0-9]+(\\.[0-9]+)?");
    }

    private static String literal(String value)
    {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static String typeName(Column.Type type)
    {
        if (ColumnTypes.DECIMAL.equals(type))
        {
            return "Decimal";
        }
        if (ColumnTypes.BOOLEAN.equals(type))
        {
            return "Boolean";
        }
        if (ColumnTypes.DOUBLE.equals(type))
        {
            return "Double";
        }
        if (ColumnTypes.INT.equals(type))
        {
            return "Int";
        }
        if (ColumnTypes.LONG.equals(type))
        {
            return "Long";
        }
        if (ColumnTypes.LOCALDATE.equals(type))
        {
            return "Date";
        }
        if (ColumnTypes.STRING.equals(type))
        {
            return "String";
        }
        return type.toString();
    }

    private static String parseModeName(ParseMode parseMode)
    {
        return switch (parseMode)
        {
            case FIRST_IN -> "firstIn";
            case LAST_IN -> "lastIn";
            case ONLY_IN -> "onlyIn";
            case EXACT -> "exact";
        };
    }

}
