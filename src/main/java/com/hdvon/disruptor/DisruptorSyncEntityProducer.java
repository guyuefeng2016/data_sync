package com.hdvon.disruptor;

import com.hdvon.config.HdvonThreadFactory;
import com.hdvon.entity.SyncEntity;
import com.hdvon.event.DisruptorSyncEntityEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author:huwenfeng
 * @Description: 
 * @Date: 20:51 2019/8/31
 */
@Slf4j
@Component
public class DisruptorSyncEntityProducer implements DisposableBean{

    @Autowired
    private DisruptorFactory disruptorFactory;
    
    private ExecutorService executor;
    private int threadNum;

    public DisruptorSyncEntityProducer(){
        threadNum = Runtime.getRuntime().availableProcessors();
        
        executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), HdvonThreadFactory
                .create("producer_executor", false), new ThreadPoolExecutor.AbortPolicy());
    }


    public void sendData(SyncEntity syncEntity){
        //为了保证顺序，去掉多线程
//        executor.execute(() -> {
            publishEvent(syncEntity);
//        });
    }

    private void publishEvent(SyncEntity syncEntity){
        long sequence = disruptorFactory.ringBuffer.next();
        try {
            DisruptorSyncEntityEvent disruptorSyncEntityEvent = disruptorFactory.ringBuffer.get(sequence);
            disruptorSyncEntityEvent.setSyncEntity(syncEntity);
        } finally {
            disruptorFactory.ringBuffer.publish(sequence);
        }
    }


    @Override
    public void destroy() throws Exception {
        if (executor != null) executor.shutdown();
    }
}
