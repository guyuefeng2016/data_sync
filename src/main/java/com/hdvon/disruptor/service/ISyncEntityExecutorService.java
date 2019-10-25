package com.hdvon.disruptor.service;

import com.hdvon.entity.SyncEntity;

public interface ISyncEntityExecutorService {
    void execute(SyncEntity syncEntity);
    void writeCompareCache(SyncEntity syncEntity);
}
