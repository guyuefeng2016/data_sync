package com.hdvon.enums;

import com.hdvon.constant.CachesConstant;

/**
 * @Author:huwenfeng
 * @Description:
 * @Date: 16:31 2019/8/31
 */
public enum CachesEnum {
    syncTables(5),
    syncAllTables(5),
    compareCache(2),  //有效期2个小时
    writeFileCache(2);  //有效期2个小时

    CachesEnum() {
    }
    CachesEnum(int ttl) {
        this.ttl = ttl;
    }
    CachesEnum(int ttl, int maxSize) {
        this.ttl = ttl;
        this.maxSize = maxSize;
    }
    CachesEnum(int ttl, int maxSize, int initSize) {
        this.ttl = ttl;
        this.maxSize = maxSize;
        this.initSize = initSize;
    }
    private int maxSize = CachesConstant.DEFAULT_MAXSIZE;    //最大数量
    private int initSize = CachesConstant.INITIAL_CAPACITY;    //初始化数量
    private int ttl = CachesConstant.DEFAULT_TTL;        //过期时间

    public int getMaxSize() {
        return maxSize;
    }
    public int getInitSize() {
        return initSize;
    }
    public int getTtl() {
        return ttl;
    }
}

