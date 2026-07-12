package com.buuz135.transfer_labels.filter;

public class FilterSlot<T> {

    private final int x;
    private final int y;
    private final int filterID;
    private T filter;

    public FilterSlot(int x, int y, int filterID, T filter) {
        this.x = x;
        this.y = y;
        this.filterID = filterID;
        this.filter = filter;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getFilterID() {
        return filterID;
    }

    public T getFilter() {
        return filter;
    }

    public void setFilter(T filter) {
        this.filter = filter;
    }
}
