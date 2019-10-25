package com.hdvon.config;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hdvon.enums.CachesEnum;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @Author:huwenfeng
 * @Description:
 * @Date: 16:31 2019/8/31
 */
@Configuration
@EnableCaching
public class CaffeineConfig {

    /**
     * 创建基于Caffeine的Cache Manager
     *   initialCapacity=[integer]: 初始的缓存空间大小
     *    maximumSize=[long]: 缓存的最大条数
     *    maximumWeight=[long]: 缓存的最大权重
     *    expireAfterAccess=[duration]: 最后一次写入或访问后经过固定时间过期
     *    expireAfterWrite=[duration]: 最后一次写入后经过固定时间过期
     *    refreshAfterWrite=[duration]: 创建缓存或者最近一次更新缓存后经过固定的时间间隔，刷新缓存
     *    recordStats：开发统计功能
     * @return
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        ArrayList<CaffeineCache> caches = new ArrayList<CaffeineCache>();
        for(CachesEnum c : CachesEnum.values()){
            caches.add(new CaffeineCache(c.name(),
                    Caffeine.newBuilder().recordStats()
                            .expireAfterWrite(c.getTtl(), TimeUnit.HOURS)
                            .refreshAfterWrite(1, TimeUnit.NANOSECONDS)
                            .initialCapacity(c.getInitSize())
                            .maximumSize(c.getMaxSize())
                            .build(cacheLoader()))
            );
        }
        cacheManager.setCaches(caches);
        return cacheManager;
    }

    /**
     * 必须要指定这个Bean，refreshAfterWrite这个配置属性才生效
     * @return
     */
    @Bean
    public CacheLoader<Object, Object> cacheLoader() {
        CacheLoader<Object, Object> cacheLoader = new CacheLoader<Object, Object>() {
            @Override
            public Object load(Object key) throws Exception {
                return null;
            }
            // 重写这个方法将oldValue值返回回去，进而刷新缓存
            @Override
            public Object reload(Object key, Object oldValue) throws Exception {
                return oldValue;
            }
        };

        return cacheLoader;
    }

}
