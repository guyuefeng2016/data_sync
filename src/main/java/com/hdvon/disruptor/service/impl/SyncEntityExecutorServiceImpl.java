package com.hdvon.disruptor.service.impl;

import com.hdvon.constant.CachesConstant;
import com.hdvon.disruptor.service.ISyncEntityExecutorService;
import com.hdvon.entity.SyncEntity;
import com.hdvon.enums.CachesEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.LockSupport;

/**
 * @Author:huwenfeng
 * @Description:
 * @Date: 21:32 2019/8/31
 */
@Slf4j
@Service
public class SyncEntityExecutorServiceImpl implements ISyncEntityExecutorService {

    @Autowired
    private CacheManager cacheManager;

    public static volatile boolean canWriteCache = true;

    @Override
    public void execute(SyncEntity syncEntity) {
        writeFileCache(syncEntity);
    }

    /**
     * 本地写文件缓存
     * @param syncEntity
     */
    private void writeFileCache(SyncEntity syncEntity){
        while (!canWriteCache){
            if (log.isDebugEnabled()) log.debug("########  暂停写入缓存，自旋等待1000纳秒  #########");
            LockSupport.parkNanos(1000);
        }
        if (log.isDebugEnabled()) log.debug("########  写入{} 缓存  #########",syncEntity.getSql());
        Cache writeFileCache = cacheManager.getCache(CachesEnum.writeFileCache.name());

        List list = writeFileCache.get(CachesConstant.WRITE_CACHE_KEY, LinkedList.class);

        if (list == null){
            List<SyncEntity> syncEntityList = new LinkedList<>();
            syncEntityList.add(syncEntity);
            writeFileCache.put(CachesConstant.WRITE_CACHE_KEY, syncEntityList);
            return;
        }

        list.add(syncEntity);
        writeFileCache.put(CachesConstant.WRITE_CACHE_KEY, list);
    }

    /**
     * 写入和远程网络传递过来的数据进行比较的缓存
     * @param syncEntity
     */
    public void writeCompareCache(SyncEntity syncEntity){
        if (log.isDebugEnabled()) log.debug("########  写入 比较 缓存  #########");

        Cache compareCache = cacheManager.getCache(CachesEnum.compareCache.name());

        ArrayBlockingQueue blockingQueue = compareCache.get(CachesConstant.COMPARE_CACHE_KEY, ArrayBlockingQueue.class);

        if (blockingQueue == null){
            ArrayBlockingQueue<SyncEntity> syncEntities = new ArrayBlockingQueue<SyncEntity>(5000);
            try {
                syncEntities.offer(syncEntity);

                compareCache.put(CachesConstant.COMPARE_CACHE_KEY, syncEntities);
            } catch (Exception e){
                log.error(e.getMessage());
            }
            return;
        }
        boolean offer = blockingQueue.offer(syncEntity);
        if (!offer){
            //队列已满,出队一百
            int i = 100;
            while (i-- > 0){
                blockingQueue.poll();
            }
        }

        if (!offer){
            blockingQueue.offer(syncEntity);
        }

        compareCache.put(CachesConstant.COMPARE_CACHE_KEY, blockingQueue);
    }


}
