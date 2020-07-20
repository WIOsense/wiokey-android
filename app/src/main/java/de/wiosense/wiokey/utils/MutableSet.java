package de.wiosense.wiokey.utils;

import java.util.HashSet;
import java.util.Iterator;

public class MutableSet<T> extends HashSet<T> {
    public T get(int position) {
        T value = null;
        if (position >= 0 && position < size()) {
            Iterator<T> it = this.iterator();
            int i = 0;
            do {
                value = it.next();
                ++i;
            } while (i <= position);
        }
        return value;
    }
}