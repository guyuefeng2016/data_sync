package com.hdvon.disruptor;

import com.hdvon.config.HdvonThreadFactory;
import com.hdvon.disruptor.service.ISyncEntityExecutorService;
import com.hdvon.event.DisruptorSyncEntityEvent;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author:huwenfeng
 * @Description: 
 * @Date: 20:50 2019/8/31
 */
@Component
public class DisruptorFactory implements DisposableBean,ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private ISyncEntityExecutorService syncEntityExecutorService;
    
    private int ringBufferSize = 1024 * 1024;
    public RingBuffer<DisruptorSyncEntityEvent> ringBuffer;
    private Disruptor<DisruptorSyncEntityEvent> disruptor;
    private WaitStrategy waitStrategy;
    private ProducerType producerType;

    private int threadNum;
    private volatile boolean isStart;
    
    public DisruptorFactory(){
        threadNum = Runtime.getRuntime().availableProcessors();

        waitStrategy = new YieldingWaitStrategy();

        producerType = ProducerType.SINGLE;

        //初始化disruptor
        disruptor = new Disruptor<>(new SyncEntityEventFactory(), ringBufferSize, HdvonThreadFactory
                .create("disruptor_producer", false), producerType, waitStrategy);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        initDisruptorSyncEntityConsumer();
    }

    private void initDisruptorSyncEntityConsumer(){
        final Executor executor = new ThreadPoolExecutor(threadNum, threadNum, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), HdvonThreadFactory
                .create("disruptor_consumer", false), new ThreadPoolExecutor.AbortPolicy());
        DisruptorSyncEntityConsumer[] disruptorSyncEntityConsumers = new DisruptorSyncEntityConsumer[threadNum];
        for (int i = 0; i < threadNum; i++) {
            disruptorSyncEntityConsumers[i] = new DisruptorSyncEntityConsumer(syncEntityExecutorService, executor);
        }

//        disruptor.handleEventsWithWorkerPool(disruptorSyncEntityConsumers);
        DisruptorSyncEntityConsumer disruptorSyncEntityConsumer = new DisruptorSyncEntityConsumer(syncEntityExecutorService, executor);
        disruptor.handleEventsWith(disruptorSyncEntityConsumer);

        ringBuffer = disruptor.start();
        this.isStart = true;
    }

    @Override
    public void destroy() throws Exception {
        if (isStart){
            disruptor.shutdown();
        }
    }
}
