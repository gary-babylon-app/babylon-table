package app.babylon.table.transform;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToIntFunction;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnBoolean;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.text.Strings;

public class TransformRound extends TransformDecimalUnary
{
    public static final String FUNCTION_NAME = "Round";
    public static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_EVEN;

    private final Integer scale;
    private final ColumnName scaleColumnName;
    private final RoundingMode roundingMode;
    private final Map<Class<?>, ToIntFunction<Object>> roundScales;

    private TransformRound(Builder builder)
    {
        super(FUNCTION_NAME, builder.columnName, builder.newColumnName, builder.conditionColumnName);
        if (builder.scale == null && builder.scaleColumnName == null)
        {
            throw new RuntimeException(FUNCTION_NAME + " requires a scale or scale column.");
        }
        if (builder.scale != null && builder.scaleColumnName != null)
        {
            throw new RuntimeException(FUNCTION_NAME + " requires either a scale or scale column, not both.");
        }
        this.scale = builder.scale == null ? null : ArgumentCheck.nonNegative(builder.scale);
        this.scaleColumnName = builder.scaleColumnName;
        this.roundingMode = builder.roundingMode;
        this.roundScales = Map.copyOf(ArgumentCheck.nonNull(builder.roundScales));
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(ColumnName columnName)
    {
        return builder().withColumnName(columnName);
    }

    public static final class Builder
    {
        private ColumnName columnName;
        private ColumnName newColumnName;
        private Integer scale;
        private ColumnName scaleColumnName;
        private ColumnName conditionColumnName;
        private RoundingMode roundingMode;
        private Map<Class<?>, ToIntFunction<Object>> roundScales = Map.of();

        private Builder()
        {
        }

        public Builder withColumnName(ColumnName columnName)
        {
            this.columnName = columnName;
            return this;
        }

        public Builder withNewColumnName(ColumnName newColumnName)
        {
            this.newColumnName = newColumnName;
            return this;
        }

        public Builder withScale(int scale)
        {
            return withScale(Integer.valueOf(scale));
        }

        public Builder withScale(Integer scale)
        {
            this.scale = scale;
            return this;
        }

        public Builder withScaleColumnName(ColumnName scaleColumnName)
        {
            this.scaleColumnName = scaleColumnName;
            return this;
        }

        public Builder when(ColumnName conditionColumnName)
        {
            this.conditionColumnName = conditionColumnName;
            return this;
        }

        public Builder withRoundingMode(RoundingMode roundingMode)
        {
            this.roundingMode = roundingMode;
            return this;
        }

        public Builder withRoundScales(Map<Class<?>, ToIntFunction<Object>> roundScales)
        {
            this.roundScales = roundScales == null ? Map.of() : Map.copyOf(roundScales);
            return this;
        }

        public <T> Builder withRoundScale(Class<T> type, ToIntFunction<T> roundScale)
        {
            Map<Class<?>, ToIntFunction<Object>> copy = new HashMap<>(this.roundScales);
            copy.put(ArgumentCheck.nonNull(type), roundScale(type, roundScale));
            this.roundScales = Map.copyOf(copy);
            return this;
        }

        public TransformRound build()
        {
            return new TransformRound(this);
        }
    }

    public static TransformRound of(String... params)
    {
        if (Is.empty(params) || params.length < 2)
        {
            return null;
        }
        ColumnName columnName = ColumnName.parse(params[0]);
        int scale = Integer.parseInt(params[1]);
        if (params.length >= 4)
        {
            return builder(columnName).withNewColumnName(ColumnName.parse(params[2])).withScale(scale)
                    .withRoundingMode(parseRoundingMode(params[3])).build();
        }
        if (params.length >= 3)
        {
            return builder(columnName).withNewColumnName(ColumnName.parse(params[2])).withScale(scale).build();
        }
        return builder(columnName).withScale(scale).build();
    }

    public Integer scale()
    {
        return this.scale;
    }

    public ColumnName scaleColumnName()
    {
        return this.scaleColumnName;
    }

    public RoundingMode roundingMode()
    {
        return this.roundingMode;
    }

    public RoundingMode effectiveRoundingMode()
    {
        return this.roundingMode == null ? DEFAULT_ROUNDING_MODE : this.roundingMode;
    }

    public static RoundingMode parseRoundingMode(CharSequence s)
    {
        if (s == null)
        {
            return DEFAULT_ROUNDING_MODE;
        }
        String normalised = Strings.clean(s, ' ', '_', '-').toUpperCase(Locale.ROOT);
        return switch (normalised)
        {
            case "UP" -> RoundingMode.UP;
            case "DOWN" -> RoundingMode.DOWN;
            case "CEILING" -> RoundingMode.CEILING;
            case "FLOOR" -> RoundingMode.FLOOR;
            case "HALFUP" -> RoundingMode.HALF_UP;
            case "HALFDOWN" -> RoundingMode.HALF_DOWN;
            case "HALFEVEN", "BANKERS" -> RoundingMode.HALF_EVEN;
            case "NOLOSS", "UNNECESSARY" -> RoundingMode.UNNECESSARY;
            default -> throw new IllegalArgumentException("Unknown rounding mode: " + s);
        };
    }

    public static String roundingModeName(RoundingMode roundingMode)
    {
        return switch (roundingMode)
        {
            case UP -> "up";
            case DOWN -> "down";
            case CEILING -> "ceiling";
            case FLOOR -> "floor";
            case HALF_UP -> "halfUp";
            case HALF_DOWN -> "halfDown";
            case HALF_EVEN -> "bankers";
            case UNNECESSARY -> "noLoss";
        };
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        Column column = columnsByName.get(columnName());
        if (column != null)
        {
            if (this.scaleColumnName == null)
            {
                super.apply(columnsByName);
                return;
            }
            ColumnBoolean conditionColumn = requireBoolean(columnsByName.get(conditionColumnName()));
            if (conditionColumnName() != null && conditionColumn == null)
            {
                return;
            }
            ColumnObject<BigDecimal> transformed = apply(column, columnsByName.get(this.scaleColumnName),
                    conditionColumn);
            if (transformed != null)
            {
                columnsByName.put(effectiveNewColumnName(), transformed);
            }
        }
    }

    private ColumnObject<BigDecimal> apply(Column column, Column scaleColumn)
    {
        return apply(column, scaleColumn, null);
    }

    private ColumnObject<BigDecimal> apply(Column column, Column scaleColumn, ColumnBoolean conditionColumn)
    {
        if (column == null || scaleColumn == null)
        {
            return null;
        }

        @SuppressWarnings("unchecked")
        ColumnObject<BigDecimal> oldColumn = (ColumnObject<BigDecimal>) column;
        @SuppressWarnings("unchecked")
        ColumnObject<Object> scales = (ColumnObject<Object>) scaleColumn;
        ColumnObject.Builder<BigDecimal> newColumn = ColumnObject.builderDecimal(effectiveNewColumnName());
        for (int i = 0; i < oldColumn.size(); ++i)
        {
            if (oldColumn.isSet(i))
            {
                BigDecimal bd = oldColumn.get(i);
                if (shouldApply(conditionColumn, i))
                {
                    if (scales.isSet(i))
                    {
                        newColumn.add(round(bd, scale(scales.get(i))));
                    }
                    else
                    {
                        newColumn.addNull();
                    }
                }
                else
                {
                    newColumn.add(bd);
                }
            }
            else
            {
                newColumn.addNull();
            }
        }
        return newColumn.build();
    }

    @Override
    protected BigDecimal transform(BigDecimal value, int row)
    {
        if (this.scale == null)
        {
            throw new RuntimeException(FUNCTION_NAME + " requires a scale column.");
        }
        return round(value, this.scale);
    }

    private BigDecimal round(BigDecimal value, int scale)
    {
        return value.setScale(scale, effectiveRoundingMode());
    }

    private int scale(Object value)
    {
        if (value == null)
        {
            throw new RuntimeException(FUNCTION_NAME + " cannot round from a null scale value.");
        }
        ToIntFunction<Object> exact = this.roundScales.get(value.getClass());
        if (exact != null)
        {
            return nonNegativeScale(exact.applyAsInt(value), value);
        }
        for (Map.Entry<Class<?>, ToIntFunction<Object>> entry : this.roundScales.entrySet())
        {
            if (entry.getKey().isInstance(value))
            {
                return nonNegativeScale(entry.getValue().applyAsInt(value), value);
            }
        }
        throw new RuntimeException(FUNCTION_NAME + " has no round scale registered for " + value.getClass().getName());
    }

    private static int nonNegativeScale(int scale, Object value)
    {
        if (scale < 0)
        {
            throw new RuntimeException(FUNCTION_NAME + " scale must be non-negative for " + value);
        }
        return scale;
    }

    public static <T> Map<Class<?>, ToIntFunction<Object>> roundScales(Class<T> type, ToIntFunction<T> roundScale)
    {
        Map<Class<?>, ToIntFunction<Object>> map = new HashMap<>();
        map.put(ArgumentCheck.nonNull(type), roundScale(type, roundScale));
        return map;
    }

    public static <T> ToIntFunction<Object> roundScale(Class<T> type, ToIntFunction<T> roundScale)
    {
        return value -> ArgumentCheck.nonNull(roundScale).applyAsInt(ArgumentCheck.nonNull(type).cast(value));
    }
}
