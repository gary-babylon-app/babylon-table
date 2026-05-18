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

import static app.babylon.table.transform.dsl.TransformDslWords.ABS;
import static app.babylon.table.transform.dsl.TransformDslWords.ADD;
import static app.babylon.table.transform.dsl.TransformDslWords.AFTER;
import static app.babylon.table.transform.dsl.TransformDslWords.ALL;
import static app.babylon.table.transform.dsl.TransformDslWords.AND;
import static app.babylon.table.transform.dsl.TransformDslWords.AS;
import static app.babylon.table.transform.dsl.TransformDslWords.BEFORE;
import static app.babylon.table.transform.dsl.TransformDslWords.BY;
import static app.babylon.table.transform.dsl.TransformDslWords.CLASSIFY;
import static app.babylon.table.transform.dsl.TransformDslWords.CLEAN;
import static app.babylon.table.transform.dsl.TransformDslWords.COALESCE;
import static app.babylon.table.transform.dsl.TransformDslWords.COLUMN_NAME;
import static app.babylon.table.transform.dsl.TransformDslWords.CONCAT;
import static app.babylon.table.transform.dsl.TransformDslWords.CONSTANT;
import static app.babylon.table.transform.dsl.TransformDslWords.CONVERT;
import static app.babylon.table.transform.dsl.TransformDslWords.COPY;
import static app.babylon.table.transform.dsl.TransformDslWords.DEFAULT;
import static app.babylon.table.transform.dsl.TransformDslWords.DIVIDE;
import static app.babylon.table.transform.dsl.TransformDslWords.EXTRACT;
import static app.babylon.table.transform.dsl.TransformDslWords.FLAG;
import static app.babylon.table.transform.dsl.TransformDslWords.FROM;
import static app.babylon.table.transform.dsl.TransformDslWords.IN;
import static app.babylon.table.transform.dsl.TransformDslWords.INTO;
import static app.babylon.table.transform.dsl.TransformDslWords.LEFT;
import static app.babylon.table.transform.dsl.TransformDslWords.LOWERCASE;
import static app.babylon.table.transform.dsl.TransformDslWords.MATCHING;
import static app.babylon.table.transform.dsl.TransformDslWords.MODE_EXACT;
import static app.babylon.table.transform.dsl.TransformDslWords.MODE_FIRST_IN;
import static app.babylon.table.transform.dsl.TransformDslWords.MODE_LAST_IN;
import static app.babylon.table.transform.dsl.TransformDslWords.MODE_ONLY_IN;
import static app.babylon.table.transform.dsl.TransformDslWords.MULTIPLY;
import static app.babylon.table.transform.dsl.TransformDslWords.NEGATE;
import static app.babylon.table.transform.dsl.TransformDslWords.NORMALISE;
import static app.babylon.table.transform.dsl.TransformDslWords.ON;
import static app.babylon.table.transform.dsl.TransformDslWords.OR;
import static app.babylon.table.transform.dsl.TransformDslWords.PREFIX;
import static app.babylon.table.transform.dsl.TransformDslWords.REMOVE;
import static app.babylon.table.transform.dsl.TransformDslWords.REPLACE;
import static app.babylon.table.transform.dsl.TransformDslWords.RETAIN;
import static app.babylon.table.transform.dsl.TransformDslWords.RIGHT;
import static app.babylon.table.transform.dsl.TransformDslWords.ROUND;
import static app.babylon.table.transform.dsl.TransformDslWords.SPLIT;
import static app.babylon.table.transform.dsl.TransformDslWords.STRIP;
import static app.babylon.table.transform.dsl.TransformDslWords.SUBSTITUTE;
import static app.babylon.table.transform.dsl.TransformDslWords.SUBSTRING;
import static app.babylon.table.transform.dsl.TransformDslWords.SUBTRACT;
import static app.babylon.table.transform.dsl.TransformDslWords.SUFFIX;
import static app.babylon.table.transform.dsl.TransformDslWords.TAKE;
import static app.babylon.table.transform.dsl.TransformDslWords.TO;
import static app.babylon.table.transform.dsl.TransformDslWords.TYPE_NAME_BOOLEAN;
import static app.babylon.table.transform.dsl.TransformDslWords.TYPE_NAME_DATE;
import static app.babylon.table.transform.dsl.TransformDslWords.TYPE_NAME_DECIMAL;
import static app.babylon.table.transform.dsl.TransformDslWords.TYPE_NAME_DOUBLE;
import static app.babylon.table.transform.dsl.TransformDslWords.TYPE_NAME_INT;
import static app.babylon.table.transform.dsl.TransformDslWords.TYPE_NAME_LONG;
import static app.babylon.table.transform.dsl.TransformDslWords.TYPE_NAME_STRING;
import static app.babylon.table.transform.dsl.TransformDslWords.UPPERCASE;
import static app.babylon.table.transform.dsl.TransformDslWords.USING;
import static app.babylon.table.transform.dsl.TransformDslWords.WHEN;
import static app.babylon.table.transform.dsl.TransformDslWords.WITH;

import java.util.ArrayList;
import java.util.Arrays;
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
import app.babylon.table.transform.TransformAnyToString;
import app.babylon.table.transform.TransformBefore;
import app.babylon.table.transform.TransformClassify;
import app.babylon.table.transform.TransformClean;
import app.babylon.table.transform.TransformCoalesce;
import app.babylon.table.transform.TransformConcat;
import app.babylon.table.transform.TransformConstant;
import app.babylon.table.transform.TransformCopy;
import app.babylon.table.transform.TransformDecimalBinaryOperator.Operand;
import app.babylon.table.transform.TransformDivide;
import app.babylon.table.transform.TransformExtract;
import app.babylon.table.transform.TransformExtractFromColumnName;
import app.babylon.table.transform.TransformFlag;
import app.babylon.table.transform.TransformLeft;
import app.babylon.table.transform.TransformMetadataConstant;
import app.babylon.table.transform.TransformMultiply;
import app.babylon.table.transform.TransformNegate;
import app.babylon.table.transform.TransformNormalise;
import app.babylon.table.transform.TransformPrefix;
import app.babylon.table.transform.TransformRemove;
import app.babylon.table.transform.TransformRetain;
import app.babylon.table.transform.TransformRight;
import app.babylon.table.transform.TransformRound;
import app.babylon.table.transform.TransformSplit;
import app.babylon.table.transform.TransformStringReplace;
import app.babylon.table.transform.TransformStringReplaceAll;
import app.babylon.table.transform.TransformStringToType;
import app.babylon.table.transform.TransformStrip;
import app.babylon.table.transform.TransformSubstitute;
import app.babylon.table.transform.TransformSubstring;
import app.babylon.table.transform.TransformSubtract;
import app.babylon.table.transform.TransformSuffix;
import app.babylon.table.transform.TransformTakeToType;
import app.babylon.table.transform.TransformToLocalDate;
import app.babylon.table.transform.TransformToLowerCase;
import app.babylon.table.transform.TransformToUpperCase;
import app.babylon.text.Sentence.ParseMode;

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
        writers.put(TransformAdd.class, t -> writeDecimal(ADD, AND, t));
        writers.put(TransformAfter.class, TransformDslWriter::writeAfter);
        writers.put(TransformBefore.class, TransformDslWriter::writeBefore);
        writers.put(TransformClassify.class, TransformDslWriter::writeClassify);
        writers.put(TransformClean.class, TransformDslWriter::writeClean);
        writers.put(TransformCoalesce.class, TransformDslWriter::writeCoalesce);
        writers.put(TransformConcat.class, TransformDslWriter::writeConcat);
        writers.put(TransformCopy.class, TransformDslWriter::writeCopy);
        writers.put(TransformConstant.class, TransformDslWriter::writeConstant);
        writers.put(TransformDivide.class, t -> writeDecimal(DIVIDE, BY, t));
        writers.put(TransformExtract.class, TransformDslWriter::writeExtract);
        writers.put(TransformExtractFromColumnName.class, TransformDslWriter::writeExtractFromColumnName);
        writers.put(TransformFlag.class, TransformDslWriter::writeFlag);
        writers.put(TransformLeft.class, TransformDslWriter::writeLeft);
        writers.put(TransformMultiply.class, t -> writeDecimal(MULTIPLY, BY, t));
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
        writers.put(TransformSubtract.class, t -> writeDecimal(SUBTRACT, FROM, t));
        writers.put(TransformSuffix.class, TransformDslWriter::writeSuffix);
        writers.put(TransformTakeToType.class, TransformDslWriter::writeTakeToType);
        writers.put(TransformTakeToType.Delimited.class, TransformDslWriter::writeTakeToType);
        writers.put(TransformTakeToType.Indexed.class, TransformDslWriter::writeTakeToType);
        writers.put(TransformMetadataConstant.class, TransformDslWriter::writeMetadataConstant);
        writers.put(TransformToLocalDate.class, TransformDslWriter::writeDate);
        writers.put(TransformToLowerCase.class, TransformDslWriter::writeLowercase);
        writers.put(TransformAnyToString.class, t -> writeConvert(TYPE_NAME_STRING, t));
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

    public List<String> writeAll(Iterable<? extends Transform> transforms)
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

    public List<String> writeAll(Transform... transforms)
    {
        return transforms == null
                ? writeAll((Iterable<? extends Transform>) null)
                : writeAll(Arrays.asList(transforms));
    }

    public String format(String line)
    {
        return write(TransformDslParser.standard().parse(line));
    }

    private static String writeAbs(Transform transform)
    {
        TransformAbs abs = (TransformAbs) transform;
        ColumnName source = abs.columnName();
        String line = ABS + " " + column(source);
        if (abs.conditionColumnName() != null)
        {
            line += " " + WHEN + " " + column(abs.conditionColumnName());
        }
        return line + into(source, abs.newColumnName());
    }

    private static String writeClassify(Transform transform)
    {
        TransformClassify classify = (TransformClassify) transform;
        Pattern pattern = classify.pattern();
        String found = classify.newColumnFoundValue();
        String notFound = classify.newColumnNotFoundValue();
        String line = CLASSIFY + " " + column(classify.existingColumnName()) + " " + MATCHING + " "
                + literal(pattern.pattern()) + " " + INTO + " " + column(classify.effectiveNewColumnName()) + " " + AS
                + " " + value(found);
        if (notFound != null)
        {
            line += " " + DEFAULT + " " + value(notFound);
        }
        return line;
    }

    private static String writeClean(Transform transform)
    {
        TransformClean clean = (TransformClean) transform;
        String line = CLEAN + " " + column(clean.existingColumnName());
        if (clean.cleanCharacters() != null)
        {
            line += " " + USING + " " + literal(clean.cleanCharacters());
        }
        return line + into(clean.existingColumnName(), clean.newColumnName());
    }

    private static String writeCoalesce(Transform transform)
    {
        TransformCoalesce coalesce = (TransformCoalesce) transform;
        ColumnName[] columns = coalesce.columnNames();
        ColumnObject.Mode mode = coalesce.mode();
        String line = COALESCE + " " + columns(columns);
        if (mode != null)
        {
            line += " " + AS + " " + mode.name();
        }
        return line + " " + INTO + " " + column(coalesce.newColumnName());
    }

    private static String writeConcat(Transform transform)
    {
        TransformConcat concat = (TransformConcat) transform;
        ColumnName target = concat.concatColumn();
        String separator = concat.separator();
        String line = CONCAT + " " + concatParts(concat.parts());
        if (separator != null)
        {
            line += " " + USING + " " + literal(separator);
        }
        if (!ColumnTypes.STRING.equals(concat.type()))
        {
            line += " " + AS + " " + typeName(concat.type());
        }
        return line + " " + INTO + " " + column(target);
    }

    private static String writeCopy(Transform transform)
    {
        TransformCopy copy = (TransformCopy) transform;
        return COPY + " " + column(copy.columnToCopy()) + " " + INTO + " " + column(copy.newCopyName());
    }

    private static String writeConstant(Transform transform)
    {
        TransformConstant constant = (TransformConstant) transform;
        String line = CONSTANT + " " + literal(String.valueOf(constant.value()));
        if (!ColumnTypes.STRING.equals(constant.type()))
        {
            line += " " + AS + " " + typeName(constant.type());
        }
        return line + " " + INTO + " " + column(constant.newColumnName());
    }

    private static String writeMetadataConstant(Transform transform)
    {
        TransformMetadataConstant metadata = (TransformMetadataConstant) transform;
        return CONSTANT + " " + metadata.key().dslName() + " " + INTO + " " + column(metadata.newColumnName());
    }

    private static String writeRemove(Transform transform)
    {
        TransformRemove remove = (TransformRemove) transform;
        return REMOVE + " " + columns(remove.columnNames());
    }

    private static String writeRetain(Transform transform)
    {
        TransformRetain retain = (TransformRetain) transform;
        return RETAIN + " " + columns(retain.columnNames());
    }

    private static String writeExtract(Transform transform)
    {
        TransformExtract extract = (TransformExtract) transform;
        Pattern pattern = extract.pattern();
        return EXTRACT + " " + FROM + " " + column(extract.existingColumnName()) + " " + MATCHING + " "
                + literal(pattern.pattern()) + " " + INTO + " " + column(extract.effectiveNewColumnName());
    }

    private static String writeExtractFromColumnName(Transform transform)
    {
        TransformExtractFromColumnName extract = (TransformExtractFromColumnName) transform;
        String line = EXTRACT + " " + FROM + " " + COLUMN_NAME + " " + column(extract.sourceColumnName()) + " " + USING
                + " " + literal(extract.pattern().pattern());
        if (!ColumnTypes.STRING.equals(extract.type()))
        {
            line += " " + AS + " " + typeName(extract.type());
        }
        return line + " " + INTO + " " + column(extract.newColumnName());
    }

    private static String writeFlag(Transform transform)
    {
        TransformFlag flag = (TransformFlag) transform;
        return FLAG + " " + condition(flag.condition()) + " " + INTO + " " + column(flag.newColumnName());
    }

    private static String condition(ConditionExpression condition)
    {
        if (condition instanceof ComparisonCondition comparison)
        {
            return comparison(comparison);
        }
        if (condition instanceof LogicalCondition logical)
        {
            return condition(logical.left()) + " " + logicalOperatorName(logical.operator()) + " "
                    + condition(logical.right());
        }
        return condition.toDsl();
    }

    private static String logicalOperatorName(LogicalCondition.Operator operator)
    {
        return switch (operator)
        {
            case AND -> AND;
            case OR -> OR;
        };
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
        return TAKE + " " + LEFT + " " + left.length() + " " + FROM + " " + column(left.existingColumnName()) + " "
                + INTO + " " + column(left.effectiveNewColumnName());
    }

    private static String writeRight(Transform transform)
    {
        TransformRight right = (TransformRight) transform;
        return TAKE + " " + RIGHT + " " + right.length() + " " + FROM + " " + column(right.existingColumnName()) + " "
                + INTO + " " + column(right.effectiveNewColumnName());
    }

    private static String writeSubstring(Transform transform)
    {
        TransformSubstring substring = (TransformSubstring) transform;
        return TAKE + " " + SUBSTRING + " " + substring.first() + ", " + substring.last() + " " + FROM + " "
                + column(substring.existingColumnName()) + " " + INTO + " " + column(substring.newColumnName());
    }

    private static String writeBefore(Transform transform)
    {
        TransformBefore before = (TransformBefore) transform;
        return TAKE + " " + BEFORE + " " + value(before.delimiter()) + " " + FROM + " "
                + column(before.existingColumnName()) + " " + INTO + " " + column(before.effectiveNewColumnName());
    }

    private static String writeAfter(Transform transform)
    {
        TransformAfter after = (TransformAfter) transform;
        return TAKE + " " + AFTER + " " + value(after.delimiter()) + " " + FROM + " "
                + column(after.existingColumnName()) + " " + INTO + " " + column(after.effectiveNewColumnName());
    }

    private static String writeTakeToType(Transform transform)
    {
        TransformTakeToType take = (TransformTakeToType) transform;
        String line = switch (take.operation())
        {
            case LEFT -> TAKE + " " + LEFT + " " + ((TransformTakeToType.Indexed) take).length();
            case RIGHT -> TAKE + " " + RIGHT + " " + ((TransformTakeToType.Indexed) take).length();
            case SUBSTRING -> TAKE + " " + SUBSTRING + " " + ((TransformTakeToType.Indexed) take).first() + ", "
                    + ((TransformTakeToType.Indexed) take).last();
            case BEFORE -> TAKE + " " + BEFORE + " " + value(((TransformTakeToType.Delimited) take).delimiter());
            case AFTER -> TAKE + " " + AFTER + " " + value(((TransformTakeToType.Delimited) take).delimiter());
        };
        line += " " + FROM + " " + column(take.columnName()) + " " + AS + " " + typeName(take.type());
        ParseMode parseMode = take.parseMode();
        if (parseMode != null && parseMode != ParseMode.EXACT)
        {
            line += " " + BY + " " + parseModeName(parseMode);
        }
        return line + " " + INTO + " " + column(take.newColumnName());
    }

    private static String writePrefix(Transform transform)
    {
        TransformPrefix prefix = (TransformPrefix) transform;
        return PREFIX + " " + column(prefix.existingColumnName()) + " " + WITH + " " + value(prefix.prefix())
                + into(prefix.existingColumnName(), prefix.newColumnName());
    }

    private static String writeSuffix(Transform transform)
    {
        TransformSuffix suffix = (TransformSuffix) transform;
        return SUFFIX + " " + column(suffix.existingColumnName()) + " " + WITH + " " + value(suffix.suffix())
                + into(suffix.existingColumnName(), suffix.newColumnName());
    }

    private static String writeReplace(Transform transform)
    {
        TransformStringReplace replace = (TransformStringReplace) transform;
        return REPLACE + " " + value(replace.target()) + " " + WITH + " " + value(replace.replacement()) + " " + IN
                + " " + column(replace.existingColumnName())
                + into(replace.existingColumnName(), replace.newColumnName());
    }

    private static String writeReplaceAll(Transform transform)
    {
        TransformStringReplaceAll replace = (TransformStringReplaceAll) transform;
        return REPLACE + " " + ALL + " " + literal(replace.target()) + " " + WITH + " " + value(replace.replacement())
                + " " + IN + " " + column(replace.existingColumnName())
                + into(replace.existingColumnName(), replace.newColumnName());
    }

    private static String writeSplit(Transform transform)
    {
        TransformSplit split = (TransformSplit) transform;
        String line = SPLIT + " " + column(split.getColumnToSplit()) + " " + ON + " " + literal(split.getSplitOn());
        if (split.getMode() != TransformSplit.Mode.ALL)
        {
            line += " " + BY + " " + split.getMode().name().toLowerCase(java.util.Locale.ROOT);
        }
        return line + " " + INTO + " " + columns(split.getSplitColumnNames());
    }

    private static String writeStrip(Transform transform)
    {
        TransformStrip strip = (TransformStrip) transform;
        String line = STRIP + " " + column(strip.existingColumnName());
        if (strip.stripCharacters() != null)
        {
            line += " " + USING + " " + literal(strip.stripCharacters());
        }
        return line + into(strip.existingColumnName(), strip.newColumnName());
    }

    private static String writeSubstitute(Transform transform)
    {
        TransformSubstitute substitute = (TransformSubstitute) transform;
        Map<String, String> replaces = substitute.replaces();
        String line = SUBSTITUTE + " " + column(substitute.columnName()) + " " + USING + " " + map(replaces);
        String defaultValue = substitute.defaultValueNewColumn();
        if (defaultValue != null)
        {
            line += " " + DEFAULT + " " + value(defaultValue);
        }
        return line + " " + INTO + " " + column(substitute.newColumnName());
    }

    private static String writeUppercase(Transform transform)
    {
        TransformToUpperCase uppercase = (TransformToUpperCase) transform;
        return UPPERCASE + " " + column(uppercase.existingColumnName())
                + into(uppercase.existingColumnName(), uppercase.newColumnName());
    }

    private static String writeLowercase(Transform transform)
    {
        TransformToLowerCase lowercase = (TransformToLowerCase) transform;
        return LOWERCASE + " " + column(lowercase.existingColumnName())
                + into(lowercase.existingColumnName(), lowercase.newColumnName());
    }

    private static String writeNegate(Transform transform)
    {
        TransformNegate negate = (TransformNegate) transform;
        String line = NEGATE + " " + column(negate.columnName());
        if (negate.conditionColumnName() != null)
        {
            line += " " + WHEN + " " + column(negate.conditionColumnName());
        }
        return line + into(negate.columnName(), negate.newColumnName());
    }

    private static String writeNormalise(Transform transform)
    {
        TransformNormalise normalise = (TransformNormalise) transform;
        return NORMALISE + " " + column(normalise.columnName())
                + into(normalise.columnName(), normalise.newColumnName());
    }

    private static String writeRound(Transform transform)
    {
        TransformRound round = (TransformRound) transform;
        String line = ROUND + " " + column(round.columnName());
        if (round.scaleColumnName() == null)
        {
            line += " " + TO + " " + round.scale();
        }
        else
        {
            line += " " + USING + " " + column(round.scaleColumnName());
        }
        if (round.roundingMode() != null)
        {
            line += " " + BY + " " + TransformRound.roundingModeName(round.roundingMode());
        }
        if (round.conditionColumnName() != null)
        {
            line += " " + WHEN + " " + column(round.conditionColumnName());
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
            return command + " " + operand(right) + " " + word + " " + operand(left) + " " + INTO + " "
                    + column(target);
        }
        return command + " " + operand(left) + " " + word + " " + operand(right) + " " + INTO + " " + column(target);
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
        String line = CONVERT + " " + column(sources[0]) + " " + TO + " " + TYPE_NAME_DATE;
        if (format != null && format != DateFormat.Unknown)
        {
            line += " " + USING + " " + format.name();
        }
        if (parseMode != null && parseMode != ParseMode.EXACT)
        {
            line += " " + BY + " " + parseModeName(parseMode);
        }
        return line + into(sources[0], date.newColumnName());
    }

    private static String writeConvert(String type, Transform transform)
    {
        ColumnName source = firstConfiguredSource(transform);
        ColumnName target = firstConfiguredTarget(transform);
        String line = CONVERT + " " + column(source) + " " + TO + " " + type;
        ParseMode parseMode = parseMode(transform);
        if (parseMode != null && parseMode != ParseMode.EXACT)
        {
            line += " " + BY + " " + parseModeName(parseMode);
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

    private static String into(ColumnName source, ColumnName target)
    {
        if (target == null || target.equals(source))
        {
            return "";
        }
        return " " + INTO + " " + column(target);
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

    private static String concatParts(TransformConcat.Part[] parts)
    {
        List<String> values = new ArrayList<>();
        for (TransformConcat.Part part : parts)
        {
            values.add(part.isColumn() ? column(part.columnName()) : literal(part.literalValue()));
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
            return TYPE_NAME_DECIMAL;
        }
        if (ColumnTypes.BOOLEAN.equals(type))
        {
            return TYPE_NAME_BOOLEAN;
        }
        if (ColumnTypes.DOUBLE.equals(type))
        {
            return TYPE_NAME_DOUBLE;
        }
        if (ColumnTypes.INT.equals(type))
        {
            return TYPE_NAME_INT;
        }
        if (ColumnTypes.LONG.equals(type))
        {
            return TYPE_NAME_LONG;
        }
        if (ColumnTypes.LOCALDATE.equals(type))
        {
            return TYPE_NAME_DATE;
        }
        if (ColumnTypes.STRING.equals(type))
        {
            return TYPE_NAME_STRING;
        }
        return type.toString();
    }

    private static String parseModeName(ParseMode parseMode)
    {
        return switch (parseMode)
        {
            case FIRST_IN -> MODE_FIRST_IN;
            case LAST_IN -> MODE_LAST_IN;
            case ONLY_IN -> MODE_ONLY_IN;
            case EXACT -> MODE_EXACT;
        };
    }

}
