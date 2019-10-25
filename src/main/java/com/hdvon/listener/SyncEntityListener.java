package com.hdvon.listener;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.hdvon.constant.CachesConstant;
import com.hdvon.constant.CommonConstant;
import com.hdvon.constant.WorkOrderConstant;
import com.hdvon.disruptor.DisruptorSyncEntityProducer;
import com.hdvon.entity.SqlDataEntity;
import com.hdvon.entity.SyncEntity;
import com.hdvon.entity.SyncWorkOrderEntity;
import com.hdvon.enums.CachesEnum;
import com.hdvon.event.SyncEntityEvent;
import com.hdvon.sql.SqlFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @Author:huwenfeng
 * @Description: 组装最终的对象
 * @Date: 19:31 2019/8/31
 */
@Component
@Slf4j
public class SyncEntityListener implements ApplicationListener<SyncEntityEvent> {

    @Autowired
    private SqlFactory sqlFactory;

    @Autowired
    private DisruptorSyncEntityProducer producer;

    @Autowired
    private CacheManager cacheManager;


    @Override
    public void onApplicationEvent(SyncEntityEvent syncEntityEvent) {
        List<CanalEntry.Column> columns = syncEntityEvent.getColumns();
        CanalEntry.EventType eventType = syncEntityEvent.getEventType();
        CanalEntry.Header header = syncEntityEvent.getHeader();
        SqlDataEntity sqlDataEntity = sqlFactory.buildSqlData(columns, eventType, header.getSchemaName(), header.getTableName());

        SyncEntity syncEntity = buildSyncEntity(sqlDataEntity, eventType, header);

        if (log.isDebugEnabled()) {
            SyncWorkOrderEntity workOrderSyncEntity = syncEntity.getWorkOrderSyncEntity();
            if (workOrderSyncEntity == null)  log.debug("组装推送的sql对象，非工单表, sql: {} , sqlExecuteTime: {} ", syncEntity.getSql(), syncEntity.getSqlExecuteTime());
            else log.debug("组装推送的sql对象, 工单表， sql: {} , sqlExecuteTime: {} , workOrderId: {} , workOrderStatus: {}",
                    syncEntity.getSql(), syncEntity.getSqlExecuteTime(), workOrderSyncEntity.getWorkOrderId(), workOrderSyncEntity.getWorkOrderStatus());
        }

        try {
            Cache cache = cacheManager.getCache(CachesEnum.compareCache.name());
            ArrayBlockingQueue blockingQueue = cache.get(CachesConstant.COMPARE_CACHE_KEY, ArrayBlockingQueue.class);
            checkDuplicate(blockingQueue, syncEntity);
        } catch (Exception e){
            if (log.isDebugEnabled()) log.debug(e.getMessage());
            return;
        }

        producer.sendData(syncEntity);
    }

    /**
     * 一段时间内当前的sql没有入过库，需要过滤，否则会造成重复入库
     * @param blockingQueue
     * @param syncEntity
     * @throws Exception
     */
    private void checkDuplicate(ArrayBlockingQueue blockingQueue, SyncEntity syncEntity) throws Exception{
        if (blockingQueue != null){
            boolean hasItem = blockingQueue.contains(syncEntity);
            if (hasItem){
                throw new Exception("########## 一段时间内有相同的数据入库 ###########");
            }
        }
    }

    /**
     * 构建推送对象
     * @param sqlDataEntity
     * @param eventType
     * @param header
     */
    private SyncEntity buildSyncEntity(SqlDataEntity sqlDataEntity, CanalEntry.EventType eventType, CanalEntry.Header header){
        SyncEntity syncEntity = new SyncEntity();

        String sql = sqlDataEntity.getSql();
        if (StringUtils.isNotEmpty(sql)){
            syncEntity.setSql(sql);
        } else {
            return syncEntity;
        }

        if (header.hasExecuteTime()) {
            syncEntity.setSqlExecuteTime(header.getExecuteTime());
        } else {
            syncEntity.setSqlExecuteTime(new Date().getTime());
        }

        workOrderAdapter(eventType, header, sqlDataEntity, syncEntity);

        return syncEntity;
    }

    /**
     * 补充同步工单信息
     * @param eventType
     * @param header
     * @param sqlDataEntity
     */
    private void workOrderAdapter(CanalEntry.EventType eventType, CanalEntry.Header header, SqlDataEntity sqlDataEntity, SyncEntity syncEntity){
        if (WorkOrderConstant.DIAGNOSIS_WORK_ORDER_TABLE.equalsIgnoreCase(header.getTableName())){
            if (eventType == CanalEntry.EventType.UPDATE){
                Map<String, Object> columnsMap = sqlDataEntity.getColumnsMap();
                Object statusAndUpdate = columnsMap.get(WorkOrderConstant.WORK_ORDER_STATUS);
                if (statusAndUpdate != null){
                    String[] valueAndUpdate = statusAndUpdate.toString().split(CommonConstant.SPLIT_STRING);
                    String workOrderStatus = valueAndUpdate[0];
                    if (CommonConstant.TRUE_STRING.equalsIgnoreCase(valueAndUpdate[1])){
                        String[] idAndUpdate = columnsMap.get(WorkOrderConstant.WORK_ORDER_ID).toString().split(CommonConstant.SPLIT_STRING);
                        SyncWorkOrderEntity workOrderEntity = new SyncWorkOrderEntity();
                        workOrderEntity.setWorkOrderId(idAndUpdate[0]);
                        workOrderEntity.setWorkOrderStatus(workOrderStatus);
                        syncEntity.setWorkOrderSyncEntity(workOrderEntity);
                    }
                }
            }
        }
    }

}
