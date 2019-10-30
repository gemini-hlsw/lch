package edu.gemini.lch.model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shuttering windows for laser targets.
 * They are currently not persisted but created on the fly.
 */
public class ShutteringWindow extends Window {
    public ShutteringWindow(ZonedDateTime start, ZonedDateTime end) {
        super(start, end);
    }

    public static List<ShutteringWindow> includeBlanketClosures(List<ShutteringWindow> windows, List<BlanketClosure> closures) {
        List<ShutteringWindow> result = new ArrayList<>();
        result.addAll(removeClosures(windows, closures, ShutteringWindow.class));
        result.addAll(closures);
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    public boolean isBlanketClosure() {
        return false;
    }

    // --
    public ShutteringWindow() {}

}
