package com.kootletland.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ClickTracker {
    private static final List<Long> LEFT = new ArrayList<Long>();
    private static final List<Long> RIGHT = new ArrayList<Long>();

    private ClickTracker() {}

    public static void click(int button) {
        long now = System.currentTimeMillis();
        if (button == 0) LEFT.add(now);
        else if (button == 1) RIGHT.add(now);
    }

    public static int cps(int button) {
        List<Long> list = button == 0 ? LEFT : RIGHT;
        long now = System.currentTimeMillis();
        Iterator<Long> it = list.iterator();
        while (it.hasNext()) {
            if (now - it.next() > 1000L) it.remove();
        }
        return list.size();
    }
}
