package edu.gemini.lch.web.app.windows.night;

import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.model.RaDecLaserTarget;

import java.util.*;

/**
 */
public final class RaDecLaserTargetsTable extends TargetsTable {

    public RaDecLaserTargetsTable() {
        init(LaserBean.class);
        setSortContainerPropertyId("lra");
    }

    public void updateNight(LaserNight night) {
        super.updateNight(night, LaserBean.class);
        container.removeAllItems();
        container.addAll(createBeans(night.getRaDecLaserTargets()));
        setPageLength(Math.min(40, container.size()));
        sort();
    }

    private Collection<LaserBean> createBeans(Set<RaDecLaserTarget> targets) {
        List<LaserBean> beans = new ArrayList<>();
        for (RaDecLaserTarget t : targets) {
            beans.add(new LaserBean(t.getId(), t));
        }
        return beans;
    }

}
