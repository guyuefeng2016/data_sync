package com.hdvon.disruptor;

import com.hdvon.disruptor.service.ISyncEntityExecutorService;
import com.hdvon.event.DisruptorSyncEntityEvent;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;

@Slf4j
public class DisruptorSyncEntityConsumer implements EventHandler<DisruptorSyncEntityEvent>, WorkHandler<DisruptorSyncEntityEvent> {

    private ISyncEntityExecutorService syncEntityExecutorService;

    private Executor executor;

    public DisruptorSyncEntityConsumer(ISyncEntityExecutorService syncEntityExecutorService, Executor executor){
        this.syncEntityExecutorService = syncEntityExecutorService;
        this.executor = executor;
    }

    @Override
    public void onEvent(DisruptorSyncEntityEvent disruptorSyncEntityEvent) throws Exception {
        if (log.isDebugEnabled()) log.debug("############  disruptor开始消费数据  ############");
//        executor.execute(() -> {
            syncEntityExecutorService.execute(disruptorSyncEntityEvent.getSyncEntity());
//        });
    }

    @Override
    public void onEvent(DisruptorSyncEntityEvent event, long sequence, boolean endOfBatch) throws Exception {
        this.onEvent(event);
    }
}
