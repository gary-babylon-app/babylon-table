/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.grouping;

import app.babylon.lang.ArgumentCheck;

import java.util.Arrays;

/**
 * Represents a composite grouping key made up of one or more ordered key
 * components.
 */
public interface GroupKey extends Comparable<GroupKey>
{
    static GroupKey of(Object... elements)
    {
        ArgumentCheck.nonEmpty(elements);
        return new ElementsGroupKey(elements);
    }

    Object getComponent(int i);

    int size();

    default Object getFirst()
    {
        return getComponent(0);
    }

    default Object getSecond()
    {
        return getComponent(1);
    }

    default Object getLast()
    {
        return getComponent(size() - 1);
    }

    @SuppressWarnings("unchecked")
    @Override
    default int compareTo(GroupKey o)
    {
        int s1 = size();
        int s2 = o.size();
        int size = Math.min(s1, s2);

        for (int i = 0; i < size; ++i)
        {
            Object o1 = getComponent(i);
            Object o2 = o.getComponent(i);
            if (o1 == o2)
            {
                continue;
            }
            if (o1 == null)
            {
                return -1;
            }
            if (o1 instanceof Comparable)
            {
                Comparable<Object> c1 = (Comparable<Object>) o1;
                int c = c1.compareTo(o2);
                if (c != 0)
                {
                    return c;
                }
            }
            else
            {
                throw new RuntimeException("GroupKey element " + o1 + " is not comparable");
            }
        }
        return Integer.compare(s1, s2);
    }

    final class ElementsGroupKey implements GroupKey
    {
        private final Object[] elements;

        private ElementsGroupKey(Object[] elements)
        {
            this.elements = Arrays.copyOf(elements, elements.length);
        }

        @Override
        public int hashCode()
        {
            return Arrays.hashCode(this.elements);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof ElementsGroupKey that)
            {
                return Arrays.equals(this.elements, that.elements);
            }
            return false;
        }

        @Override
        public String toString()
        {
            return Arrays.toString(this.elements);
        }

        @Override
        public Object getComponent(int i)
        {
            return this.elements[i];
        }

        @Override
        public int size()
        {
            return this.elements.length;
        }
    }
}
