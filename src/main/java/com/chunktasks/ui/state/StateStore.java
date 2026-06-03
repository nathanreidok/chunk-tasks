package com.chunktasks.ui.state;

import lombok.Getter;
import net.runelite.client.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StateStore {
    @Inject
    private EventBus eventBus;

    @Getter
    private boolean dashboardEnabled = false;

    // TODO: maybe add some sort of key to event so subscribers
    //  can more selectively decide how/when to react
    private void postEvent() {
        eventBus.post(new StateChanged());
    }

    public void setDashboardEnabled(boolean enabled) {
        dashboardEnabled = enabled;
        postEvent();
    }
}
