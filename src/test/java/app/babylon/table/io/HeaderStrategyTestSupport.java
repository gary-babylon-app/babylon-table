package app.babylon.table.io;

import java.io.IOException;
import java.util.List;

final class HeaderStrategyTestSupport
{
    private HeaderStrategyTestSupport()
    {
    }

    static ByteStringSlices row(String... values)
    {
        ByteStringSlices.Builder row = new ByteStringSlices.Builder();
        for (String value : values)
        {
            if (value != null)
            {
                byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                row.append(bytes, 0, bytes.length);
            }
            row.finishField();
        }
        return row.build();
    }

    static List<ByteStringSlices> rows(ByteStringSlices... rows)
    {
        return List.of(rows);
    }

    static RowStreamMarkable stream(ByteStringSlices... rows)
    {
        return new TestRowStream(List.of(rows));
    }

    private static final class TestRowStream implements RowStreamMarkable
    {
        private final List<ByteStringSlices> rows;
        private int currentIndex;
        private int replayIndex;
        private int markIndex;

        private TestRowStream(List<ByteStringSlices> rows)
        {
            this.rows = rows;
            this.currentIndex = -1;
            this.replayIndex = -1;
            this.markIndex = 0;
        }

        @Override
        public boolean next() throws IOException
        {
            int nextIndex = this.replayIndex >= 0 ? this.replayIndex : this.currentIndex + 1;
            if (nextIndex >= this.rows.size())
            {
                return false;
            }
            this.currentIndex = nextIndex;
            if (this.replayIndex >= 0)
            {
                ++this.replayIndex;
                if (this.replayIndex >= this.rows.size())
                {
                    this.replayIndex = -1;
                }
            }
            return true;
        }

        @Override
        public ByteStringSlices current()
        {
            return this.rows.get(this.currentIndex);
        }

        @Override
        public void mark(int rowIndex)
        {
            this.markIndex = rowIndex + 1;
        }

        @Override
        public void reset()
        {
            this.replayIndex = this.markIndex;
            this.currentIndex = this.markIndex - 1;
        }
    }
}
