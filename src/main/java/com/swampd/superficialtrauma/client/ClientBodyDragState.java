package com.swampd.superficialtrauma.client;

import java.util.HashMap;
import java.util.Map;

public final class ClientBodyDragState {
    private static final Map<Integer, Integer> DRAGGERS_BY_TARGET = new HashMap<>();

    private ClientBodyDragState() {
    }

    public static boolean isBeingDragged(int targetEntityId) {
        return DRAGGERS_BY_TARGET.containsKey(targetEntityId);
    }

    public static void update(int targetEntityId, int draggerEntityId, boolean active) {
        if (active) {
            DRAGGERS_BY_TARGET.put(targetEntityId, draggerEntityId);
        } else {
            DRAGGERS_BY_TARGET.remove(targetEntityId);
        }
    }

    public static void clear() {
        DRAGGERS_BY_TARGET.clear();
    }
}
