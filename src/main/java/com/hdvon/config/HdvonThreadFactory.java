package com.hdvon.config;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author:huwenfeng
 * @Description: 自定义线程工厂
 * @Date: 18:11 2019/8/31
 */
public class HdvonThreadFactory implements ThreadFactory{

    private static final AtomicLong THREAD_NUM = new AtomicLong(1);

    private static final ThreadGroup THREAD_GROUP = new ThreadGroup("hdvon_group");

    private static volatile boolean daemon;

    private final String threadName;

    private HdvonThreadFactory(final String threadName, final boolean daemon)
    {
        this.threadName = threadName;
        HdvonThreadFactory.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r)
    {
        Thread thread = new Thread(THREAD_GROUP, r, THREAD_GROUP.getName() + "_" + threadName + "_" + THREAD_NUM.getAndIncrement());
        thread.setDaemon(daemon);
        if (thread.getPriority() != Thread.NORM_PRIORITY)
        {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }


    public static ThreadFactory create(final String threadName, final boolean daemon)
    {
        return new HdvonThreadFactory(threadName, daemon);
    }

}
