package space.vampir.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class SortedUniqueList<E extends Comparable<E>> extends ArrayList<E> {

    @Override
    public boolean add(E e) {
        int idx = Collections.binarySearch(this, e);
        if (idx >= 0) {
            return false; // Already exists
        }
        int insertPos = -idx - 1;
        super.add(insertPos, e);
        return true;
    }

    @Override
    public void add(int index, E element) {
        this.add(element);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c) {
            if (this.add(e)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        return this.addAll(c);
    }
}
