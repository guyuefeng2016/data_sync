package com.hdvon.disruptor;

import com.hdvon.event.DisruptorSyncEntityEvent;
import com.lmax.disruptor.EventFactory;

public class SyncEntityEventFactory implements EventFactory<DisruptorSyncEntityEvent>{

    @Override
    public DisruptorSyncEntityEvent newInstance() {
        return new DisruptorSyncEntityEvent();
    }

}
