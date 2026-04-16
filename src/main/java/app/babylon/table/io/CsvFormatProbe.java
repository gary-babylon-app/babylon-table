/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import app.babylon.io.StreamSourceProbe;
import app.babylon.lang.ArgumentCheck;

final class CsvFormatProbe
{
    private static final int BYTES_TO_SNIP = 16384;
    private CsvFormatProbe()
    {
    }

    static CsvFormat detect(BufferedInputStream stream, String resourceName, Charset fallbackCharset,
            char defaultSeparator, char defaultQuote) throws IOException
    {
        BufferedInputStream checkedStream = ArgumentCheck.nonNull(stream);
        StreamSourceProbe sourceProbe = StreamSourceProbe.of(checkedStream, ArgumentCheck.nonNull(resourceName));
        Charset charset = sourceProbe.getCharset(ArgumentCheck.nonNull(fallbackCharset));

        checkedStream.mark(BYTES_TO_SNIP);
        byte[] bytes = checkedStream.readNBytes(BYTES_TO_SNIP);
        checkedStream.reset();

        int bomLength = sourceProbe.bomLengthBytes();
        String sample = new String(bytes, bomLength, Math.max(0, bytes.length - bomLength), charset);
        return detect(sample, charset, defaultSeparator, defaultQuote);
    }

    static CsvFormat detect(String sample, Charset charset, char defaultSeparator, char defaultQuote)
    {
        String checkedSample = sample == null ? "" : sample;
        if (checkedSample.isEmpty())
        {
            return new CsvFormat(charset, defaultSeparator, defaultQuote, 0.0d);
        }

        CharacterCount tally = CharacterCount.of(checkedSample);
        CsvFormat noSeparatorFormat = detectWithoutSeparatorCandidates(tally, charset, defaultSeparator, defaultQuote);
        if (noSeparatorFormat != null)
        {
            return noSeparatorFormat;
        }

        CsvFormat singleSeparatorFormat = detectSingleSeparatorFastPath(tally, charset, defaultQuote);
        if (singleSeparatorFormat != null)
        {
            return singleSeparatorFormat;
        }

        char[] separatorCandidates = separatorCandidates(tally);
        char bestSeparator = defaultSeparator;
        int bestScore = Integer.MIN_VALUE;
        int bestConsistentPairs = 0;
        int bestRowCount = 0;
        for (char separator : separatorCandidates)
        {
            Score score = score(checkedSample, separator, defaultQuote, defaultSeparator, defaultQuote);
            if (score.value > bestScore)
            {
                bestScore = score.value;
                bestSeparator = separator;
                bestConsistentPairs = score.consistentPairs;
                bestRowCount = score.rowCount;
            }
        }

        double confidence = bestRowCount <= 1
                ? 0.0d
                : Math.min(1.0d, (double) bestConsistentPairs / (bestRowCount - 1));
        return new CsvFormat(charset, bestSeparator, defaultQuote, confidence);
    }

    private static CsvFormat detectWithoutSeparatorCandidates(CharacterCount tally, Charset charset,
            char defaultSeparator, char defaultQuote)
    {
        if (tally.getDistinctSeparatorCount() != 0)
        {
            return null;
        }
        return new CsvFormat(charset, defaultSeparator, defaultQuote, 0.0d);
    }

    private static CsvFormat detectSingleSeparatorFastPath(CharacterCount tally, Charset charset, char defaultQuote)
    {
        if (tally.getDistinctSeparatorCount() != 1)
        {
            return null;
        }
        return new CsvFormat(charset, firstSeparatorCandidate(tally), defaultQuote, 0.5d);
    }

    private static char[] separatorCandidates(CharacterCount tally)
    {
        int count = 0;
        if (tally.hasComma())
        {
            ++count;
        }
        if (tally.hasTab())
        {
            ++count;
        }
        if (tally.hasSemiColon())
        {
            ++count;
        }
        if (tally.hasPipe())
        {
            ++count;
        }

        char[] candidates = new char[count];
        int i = 0;
        if (tally.hasComma())
        {
            candidates[i++] = ',';
        }
        if (tally.hasTab())
        {
            candidates[i++] = '\t';
        }
        if (tally.hasSemiColon())
        {
            candidates[i++] = ';';
        }
        if (tally.hasPipe())
        {
            candidates[i++] = '|';
        }
        return candidates;
    }

    private static char firstSeparatorCandidate(CharacterCount tally)
    {
        if (tally.hasComma())
        {
            return ',';
        }
        if (tally.hasTab())
        {
            return '\t';
        }
        if (tally.hasSemiColon())
        {
            return ';';
        }
        if (tally.hasPipe())
        {
            return '|';
        }
        throw new IllegalStateException("Expected at least one separator candidate.");
    }

    private static Score score(String sample, char separator, char quote, char defaultSeparator, char defaultQuote)
    {
        int rawSeparatorCount = countStructuralSeparators(sample, separator, quote);
        int rawQuoteContextCount = countQuoteContexts(sample, separator, quote);
        ParseResult parse = parseRows(sample, separator, quote);

        int consecutivePairs = 0;
        int widestPairWidth = 0;
        int latestMatchedRow = -1;
        int longestRunLength = parse.widthCount > 0 ? 1 : 0;
        int currentRunLength = parse.widthCount > 0 ? 1 : 0;
        if (parse.widthCount > 1)
        {
            for (int i = 1; i < parse.widthCount; ++i)
            {
                if (parse.widths[i] == parse.widths[i - 1])
                {
                    ++consecutivePairs;
                    ++currentRunLength;
                    if (parse.widths[i] > widestPairWidth)
                    {
                        widestPairWidth = parse.widths[i];
                    }
                    latestMatchedRow = i;
                    if (currentRunLength > longestRunLength)
                    {
                        longestRunLength = currentRunLength;
                    }
                }
                else
                {
                    currentRunLength = 1;
                }
            }
        }

        int score = consecutivePairs * 200 + widestPairWidth * 50 + rawSeparatorCount * 10 + rawQuoteContextCount * 20
                - parse.malformedLines * 200;
        if (longestRunLength >= 3)
        {
            score += longestRunLength * 250;
        }
        if (consecutivePairs == 0)
        {
            score -= 500;
        }
        score += latestMatchedRow;
        if (separator == defaultSeparator)
        {
            score += 3;
        }
        if (quote == defaultQuote)
        {
            score += 2;
        }
        return new Score(score, consecutivePairs, parse.widthCount);
    }

    private static int countQuoteContexts(String sample, char separator, char quote)
    {
        int count = 0;
        for (int i = 0; i < sample.length(); ++i)
        {
            char ch = sample.charAt(i);
            if (ch == separator)
            {
                int j = skipInterFieldWhitespace(sample, i + 1, separator);
                if (j < sample.length() && sample.charAt(j) == quote)
                {
                    ++count;
                }
            }
            else if (ch == quote)
            {
                int j = skipInterFieldWhitespace(sample, i + 1, separator);
                if (j < sample.length() && sample.charAt(j) == separator)
                {
                    ++count;
                }
            }
        }
        return count;
    }

    private static int countStructuralSeparators(String sample, char separator, char quote)
    {
        int count = 0;
        boolean inQuotes = false;
        for (int i = 0; i < sample.length(); ++i)
        {
            char ch = sample.charAt(i);
            if (inQuotes)
            {
                if (ch == quote)
                {
                    if (i + 1 < sample.length() && sample.charAt(i + 1) == quote)
                    {
                        ++i;
                    }
                    else
                    {
                        inQuotes = false;
                    }
                }
                continue;
            }
            if (ch == quote)
            {
                inQuotes = true;
                continue;
            }
            if (ch == separator)
            {
                ++count;
            }
        }
        return count;
    }

    private static int skipInterFieldWhitespace(String sample, int start, char separator)
    {
        int i = start;
        while (i < sample.length())
        {
            char ch = sample.charAt(i);
            if (!Character.isWhitespace(ch) || ch == '\r' || ch == '\n' || ch == separator)
            {
                return i;
            }
            ++i;
        }
        return i;
    }

    private static ParseResult parseRows(String sample, char separator, char quote)
    {
        ParseResult result = new ParseResult();
        boolean inQuotes = false;
        boolean lineHasChars = false;
        boolean fieldHasContent = false;
        int fieldCount = 1;

        for (int i = 0; i < sample.length(); ++i)
        {
            char ch = sample.charAt(i);

            if (inQuotes)
            {
                if (ch == quote)
                {
                    if (i + 1 < sample.length() && sample.charAt(i + 1) == quote)
                    {
                        ++i;
                        fieldHasContent = true;
                        lineHasChars = true;
                    }
                    else
                    {
                        inQuotes = false;
                    }
                }
                else
                {
                    fieldHasContent = true;
                    lineHasChars = true;
                }
                continue;
            }

            if (ch == quote)
            {
                if (!fieldHasContent)
                {
                    inQuotes = true;
                    lineHasChars = true;
                    continue;
                }
                fieldHasContent = true;
                lineHasChars = true;
                continue;
            }

            if (ch == separator)
            {
                ++fieldCount;
                lineHasChars = true;
                fieldHasContent = false;
                continue;
            }

            if (ch == '\r' || ch == '\n')
            {
                if (ch == '\r' && i + 1 < sample.length() && sample.charAt(i + 1) == '\n')
                {
                    ++i;
                }
                if (lineHasChars)
                {
                    result.addWidth(fieldCount);
                }
                fieldCount = 1;
                fieldHasContent = false;
                lineHasChars = false;
                if (inQuotes)
                {
                    ++result.malformedLines;
                    inQuotes = false;
                }
                continue;
            }

            if (!Character.isWhitespace(ch) || fieldHasContent)
            {
                fieldHasContent = true;
            }
            lineHasChars = true;
        }

        if (lineHasChars)
        {
            result.addWidth(fieldCount);
        }
        if (inQuotes)
        {
            ++result.malformedLines;
        }
        return result;
    }

    private record Score(int value, int consistentPairs, int rowCount)
    {
    }

    private static final class CharacterCount
    {
        private int commaCount;
        private int tabCount;
        private int semiColonCount;
        private int pipeCount;
        private int doubleQuoteCount;
        private int distinctSeparatorCount;

        private CharacterCount()
        {
            this.commaCount = 0;
            this.tabCount = 0;
            this.semiColonCount = 0;
            this.pipeCount = 0;
            this.doubleQuoteCount = 0;
            this.distinctSeparatorCount = 0;
        }

        static CharacterCount of(String sample)
        {
            CharacterCount count = new CharacterCount();
            for (int i = 0; i < sample.length(); ++i)
            {
                char ch = sample.charAt(i);
                switch (ch)
                {
                    case ',' -> {
                        if (count.commaCount == 0)
                        {
                            ++count.distinctSeparatorCount;
                        }
                        ++count.commaCount;
                    }
                    case '\t' -> {
                        if (count.tabCount == 0)
                        {
                            ++count.distinctSeparatorCount;
                        }
                        ++count.tabCount;
                    }
                    case ';' -> {
                        if (count.semiColonCount == 0)
                        {
                            ++count.distinctSeparatorCount;
                        }
                        ++count.semiColonCount;
                    }
                    case '|' -> {
                        if (count.pipeCount == 0)
                        {
                            ++count.distinctSeparatorCount;
                        }
                        ++count.pipeCount;
                    }
                    case '"' -> {
                        ++count.doubleQuoteCount;
                    }
                    default -> {
                    }
                }
            }
            return count;
        }

        boolean hasComma()
        {
            return this.commaCount > 0;
        }

        int getCommaCount()
        {
            return this.commaCount;
        }

        boolean hasTab()
        {
            return this.tabCount > 0;
        }

        int getTabCount()
        {
            return this.tabCount;
        }

        boolean hasSemiColon()
        {
            return this.semiColonCount > 0;
        }

        int getSemiColonCount()
        {
            return this.semiColonCount;
        }

        boolean hasPipe()
        {
            return this.pipeCount > 0;
        }

        int getPipeCount()
        {
            return this.pipeCount;
        }

        int getDistinctSeparatorCount()
        {
            return this.distinctSeparatorCount;
        }

        boolean hasDoubleQuote()
        {
            return this.doubleQuoteCount > 0;
        }

        int getDoubleQuoteCount()
        {
            return this.doubleQuoteCount;
        }
    }

    private static final class ParseResult
    {
        private int[] widths = new int[16];
        private int widthCount = 0;
        private int malformedLines = 0;

        private void addWidth(int width)
        {
            if (this.widthCount == this.widths.length)
            {
                int[] expanded = new int[this.widths.length * 2];
                System.arraycopy(this.widths, 0, expanded, 0, this.widths.length);
                this.widths = expanded;
            }
            this.widths[this.widthCount++] = width;
        }
    }
}
