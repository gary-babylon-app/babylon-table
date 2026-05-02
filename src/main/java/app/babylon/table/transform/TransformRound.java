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
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.text.Strings;

public class TransformRound extends TransformBase
{
    public static final String FUNCTION_NAME = "Round";
    public static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_EVEN;

    private final ColumnName columnName;
    private final ColumnName newColumnName;
    private final Integer scale;
    private final ColumnName scaleColumnName;
    private final RoundingMode roundingMode;
    private final Map<Class<?>, ToIntFunction<Object>> roundScales;

    private TransformRound(ColumnName columnName, ColumnName newColumnName, int scale, RoundingMode roundingMode)
    {
        this(columnName, newColumnName, Integer.valueOf(scale), null, roundingMode, Map.of());
    }

    private TransformRound(ColumnName columnName, ColumnName newColumnName, ColumnName scaleColumnName,
            RoundingMode roundingMode, Map<Class<?>, ToIntFunction<Object>> roundScales)
    {
        this(columnName, newColumnName, null, scaleColumnName, roundingMode, roundScales);
    }

    private TransformRound(ColumnName columnName, ColumnName newColumnName, Integer scale, ColumnName scaleColumnName,
            RoundingMode roundingMode, Map<Class<?>, ToIntFunction<Object>> roundScales)
    {
        super(FUNCTION_NAME);
        this.columnName = ArgumentCheck.nonNull(columnName);
        this.newColumnName = newColumnName;
        if (scale == null && scaleColumnName == null)
        {
            throw new RuntimeException(FUNCTION_NAME + " requires a scale or scale column.");
        }
        this.scale = scale == null ? null : ArgumentCheck.nonNegative(scale);
        this.scaleColumnName = scaleColumnName;
        this.roundingMode = roundingMode;
        this.roundScales = Map.copyOf(ArgumentCheck.nonNull(roundScales));
    }

    public static TransformRound of(ColumnName columnName, int scale)
    {
        return columnName == null ? null : new TransformRound(columnName, null, scale, null);
    }

    public static TransformRound of(ColumnName columnName, int scale, RoundingMode roundingMode)
    {
        return columnName == null ? null : new TransformRound(columnName, null, scale, roundingMode);
    }

    public static TransformRound of(ColumnName columnName, ColumnName newColumnName, int scale)
    {
        return of(columnName, newColumnName, scale, null);
    }

    public static TransformRound of(ColumnName columnName, ColumnName newColumnName, int scale,
            RoundingMode roundingMode)
    {
        return columnName == null || newColumnName == null
                ? null
                : new TransformRound(columnName, newColumnName, scale, roundingMode);
    }

    public static TransformRound using(ColumnName columnName, ColumnName scaleColumnName,
            Map<Class<?>, ToIntFunction<Object>> roundScales)
    {
        return using(columnName, scaleColumnName, null, null, roundScales);
    }

    public static TransformRound using(ColumnName columnName, ColumnName scaleColumnName, ColumnName newColumnName,
            RoundingMode roundingMode, Map<Class<?>, ToIntFunction<Object>> roundScales)
    {
        return columnName == null || scaleColumnName == null
                ? null
                : new TransformRound(columnName, newColumnName, scaleColumnName, roundingMode, roundScales);
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
            return of(columnName, ColumnName.parse(params[2]), scale, parseRoundingMode(params[3]));
        }
        if (params.length >= 3)
        {
            return of(columnName, ColumnName.parse(params[2]), scale);
        }
        return of(columnName, scale);
    }

    public ColumnName columnName()
    {
        return this.columnName;
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
    }

    public ColumnName effectiveNewColumnName()
    {
        return this.newColumnName == null ? this.columnName : this.newColumnName;
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
        String normalised = Strings.strip(s).toString().replace("_", "").replace("-", "").toUpperCase(Locale.ROOT);
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

    public ColumnObject<BigDecimal> apply(Column column)
    {
        if (column == null)
        {
            return null;
        }
        if (this.scale == null)
        {
            throw new RuntimeException(FUNCTION_NAME + " requires a scale column.");
        }

        @SuppressWarnings("unchecked")
        ColumnObject<BigDecimal> oldColumn = (ColumnObject<BigDecimal>) column;
        ColumnObject.Builder<BigDecimal> newColumn = ColumnObject.builderDecimal(effectiveNewColumnName());
        for (int i = 0; i < oldColumn.size(); ++i)
        {
            if (oldColumn.isSet(i))
            {
                BigDecimal bd = oldColumn.get(i);
                newColumn.add(round(bd, this.scale));
            }
            else
            {
                newColumn.addNull();
            }
        }
        return newColumn.build();
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        Column column = columnsByName.get(this.columnName);
        if (column != null)
        {
            ColumnObject<BigDecimal> transformed = this.scaleColumnName == null
                    ? apply(column)
                    : apply(column, columnsByName.get(this.scaleColumnName));
            if (transformed != null)
            {
                columnsByName.put(effectiveNewColumnName(), transformed);
            }
        }
    }

    private ColumnObject<BigDecimal> apply(Column column, Column scaleColumn)
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
            if (oldColumn.isSet(i) && scales.isSet(i))
            {
                BigDecimal bd = oldColumn.get(i);
                newColumn.add(round(bd, scale(scales.get(i))));
            }
            else
            {
                newColumn.addNull();
            }
        }
        return newColumn.build();
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
