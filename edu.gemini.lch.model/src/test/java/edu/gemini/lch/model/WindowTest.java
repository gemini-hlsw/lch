package edu.gemini.lch.model;

import org.junit.Assert;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class WindowTest {

    final DateTime t = DateTime.now();

    final List<PropagationWindow> p0 = new ArrayList<>();
    final List<PropagationWindow> p1 = new ArrayList<PropagationWindow>() {{
        add(new PropagationWindow(t.plusSeconds(10), t.plusSeconds(20)));
    }};
    final List<PropagationWindow> p2 = new ArrayList<PropagationWindow>(p1) {{
        add(new PropagationWindow(t.plusSeconds(30), t.plusSeconds(40)));
    }};
    final List<PropagationWindow> p3 = new ArrayList<PropagationWindow>(p2) {{
        add(new PropagationWindow(t.plusSeconds(50), t.plusSeconds(60)));
    }};

    final List<BlanketClosure> closuresEmpty = new ArrayList<>();
    final List<BlanketClosure> closuresDisjoint = new ArrayList<BlanketClosure>() {{
        add(new BlanketClosure(t.plusSeconds(2), t.plusSeconds(5)));
        add(new BlanketClosure(t.plusSeconds(22), t.plusSeconds(25)));
    }};
    final List<BlanketClosure> closuresOverlapHead = new ArrayList<BlanketClosure>() {{
        add(new BlanketClosure(t.plusSeconds(8), t.plusSeconds(12)));
    }};
    final List<BlanketClosure> closuresOverlapMiddle = new ArrayList<BlanketClosure>() {{
        add(new BlanketClosure(t.plusSeconds(14), t.plusSeconds(16)));
    }};
    final List<BlanketClosure> closuresOverlapTail = new ArrayList<BlanketClosure>() {{
        add(new BlanketClosure(t.plusSeconds(18), t.plusSeconds(22)));
    }};
    final List<BlanketClosure> closuresAll = new ArrayList<BlanketClosure>() {{
        addAll(closuresDisjoint);
        addAll(closuresOverlapHead);
        addAll(closuresOverlapMiddle);
        addAll(closuresOverlapTail);
    }};


    @Test
    public void createsShutteringWindows() {

        List<ShutteringWindow> s0 = PropagationWindow.getShutteringWindows(new ArrayList<>());
        Assert.assertEquals(0, s0.size());

        List<ShutteringWindow> s1 = PropagationWindow.getShutteringWindows(p1);
        Assert.assertEquals(0, s1.size());

        List<ShutteringWindow> s2 = PropagationWindow.getShutteringWindows(p2);
        Assert.assertEquals(1, s2.size());

        List<ShutteringWindow> s = PropagationWindow.getShutteringWindows(p3);
        Assert.assertEquals(2, s.size());
        Assert.assertEquals(t.plusSeconds(20), s.get(0).getStart());
        Assert.assertEquals(t.plusSeconds(30), s.get(0).getEnd());
        Assert.assertEquals(t.plusSeconds(40), s.get(1).getStart());
        Assert.assertEquals(t.plusSeconds(50), s.get(1).getEnd());
    }

    @Test
    public void removeClosuresHandlesEmptyCollections() {
        List<PropagationWindow> w0 = PropagationWindow.removeClosures(p0, closuresEmpty);
        Assert.assertEquals(0, w0.size());
    }

    @Test
    public void removeClosuresDoesNotRemoveNonOverlapping() {
        List<PropagationWindow> w = PropagationWindow.removeClosures(p1, closuresDisjoint);
        Assert.assertEquals(1, w.size());
        Assert.assertEquals(t.plusSeconds(10), w.get(0).getStart());
        Assert.assertEquals(t.plusSeconds(20), w.get(0).getEnd());
    }

    @Test
    public void removeClosuresRemovesOverlapHeadProperly() {
        List<PropagationWindow> w = PropagationWindow.removeClosures(p1, closuresOverlapHead);
        Assert.assertEquals(1, w.size());
        Assert.assertEquals(t.plusSeconds(12), w.get(0).getStart());
        Assert.assertEquals(t.plusSeconds(20), w.get(0).getEnd());
    }

    @Test
    public void removeClosuresRemovesOverlapMiddleProperly() {
        List<PropagationWindow> w = PropagationWindow.removeClosures(p1, closuresOverlapMiddle);
        Assert.assertEquals(2, w.size());
        Assert.assertEquals(t.plusSeconds(10), w.get(0).getStart());
        Assert.assertEquals(t.plusSeconds(14), w.get(0).getEnd());
        Assert.assertEquals(t.plusSeconds(16), w.get(1).getStart());
        Assert.assertEquals(t.plusSeconds(20), w.get(1).getEnd());
    }

    @Test
    public void removeClosuresRemovesOverlapTailProperly() {
        List<PropagationWindow> w = PropagationWindow.removeClosures(p1, closuresOverlapTail);
        Assert.assertEquals(1, w.size());
        Assert.assertEquals(t.plusSeconds(10), w.get(0).getStart());
        Assert.assertEquals(t.plusSeconds(18), w.get(0).getEnd());
    }

    @Test
    public void doesRemoveClosures() {
        List<PropagationWindow> w = PropagationWindow.removeClosures(p3, closuresAll);
        Assert.assertEquals(4, w.size());
        Assert.assertEquals(t.plusSeconds(12), w.get(0).getStart());
        Assert.assertEquals(t.plusSeconds(14), w.get(0).getEnd());
        Assert.assertEquals(t.plusSeconds(16), w.get(1).getStart());
        Assert.assertEquals(t.plusSeconds(18), w.get(1).getEnd());
        Assert.assertEquals(t.plusSeconds(30), w.get(2).getStart());
        Assert.assertEquals(t.plusSeconds(40), w.get(2).getEnd());
        Assert.assertEquals(t.plusSeconds(50), w.get(3).getStart());
        Assert.assertEquals(t.plusSeconds(60), w.get(3).getEnd());
    }

    @Test
    public void joinsShutteringWindowsAndClosures() {
        List<ShutteringWindow> s = PropagationWindow.getShutteringWindows(p3);
        Assert.assertEquals(2, s.size());
        List<ShutteringWindow> w = ShutteringWindow.includeBlanketClosures(s, closuresAll);
        Assert.assertEquals(7, w.size());
        Assert.assertEquals(t.plusSeconds( 2), w.get(0).getStart());
        Assert.assertEquals(t.plusSeconds( 5), w.get(0).getEnd());
        Assert.assertEquals(t.plusSeconds( 8), w.get(1).getStart());
        Assert.assertEquals(t.plusSeconds(12), w.get(1).getEnd());
        Assert.assertEquals(t.plusSeconds(14), w.get(2).getStart());
        Assert.assertEquals(t.plusSeconds(16), w.get(2).getEnd());
        Assert.assertEquals(t.plusSeconds(18), w.get(3).getStart());
        Assert.assertEquals(t.plusSeconds(22), w.get(3).getEnd());
        Assert.assertEquals(t.plusSeconds(22), w.get(4).getStart());
        Assert.assertEquals(t.plusSeconds(25), w.get(4).getEnd());
        Assert.assertEquals(t.plusSeconds(25), w.get(5).getStart());
        Assert.assertEquals(t.plusSeconds(30), w.get(5).getEnd());
        Assert.assertEquals(t.plusSeconds(40), w.get(6).getStart());
        Assert.assertEquals(t.plusSeconds(50), w.get(6).getEnd());
    }

}
