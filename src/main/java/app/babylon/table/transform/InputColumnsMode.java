package app.babylon.table.transform;

public enum InputColumnsMode
{
    RETAIN, REMOVE;

    public static InputColumnsMode parse(CharSequence s)
    {
        if (s == null)
        {
            return null;
        }
        String x = s.toString().strip();
        if (x.isEmpty())
        {
            return null;
        }
        return InputColumnsMode.valueOf(x.toUpperCase());
    }
}
