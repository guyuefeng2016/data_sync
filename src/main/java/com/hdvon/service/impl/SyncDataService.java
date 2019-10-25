package com.hdvon.service.impl;

import com.alibaba.otter.canal.protocol.CanalEntry.*;
import com.hdvon.event.SyncEntityEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;

/**
 * @Author:huwenfeng
 * @Description:
 * @Date: 18:02 2019/8/31
 */
@Component
@Slf4j
public class SyncDataService extends CanalStatusService{

    @Autowired
    ApplicationContext applicationContext;

    @PostConstruct
    public void init(){
        super.init();
    }

    /**
     * 处理数据
     * @param entrys
     */
    @Override
    void processEntry(List<Entry> entrys) {
        for (Entry entry : entrys) {
            if (entry.getEntryType() == EntryType.TRANSACTIONBEGIN || entry.getEntryType() == EntryType.TRANSACTIONEND) {
                continue;
            }

            RowChange rowChage = null;
            try {
                rowChage = RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                log.error("ERROR ## parser of eromanga-event has an error , data:" + entry.toString(), e);
            }

            final EventType eventType = rowChage.getEventType();
            final Header header = entry.getHeader();

            String schemaName = header.getSchemaName();
            String tableName = header.getTableName();

            if (!syncDatabase().equalsIgnoreCase(schemaName)){
                return;
            }

            boolean syncAllFalg = syncAllTables();
            if (!syncAllFalg){
                Set<String> strings = syncTables();
                if (!strings.contains(tableName)){
                    return;
                }
            }

            List<RowData> rowDatasList = rowChage.getRowDatasList();
            for (RowData rowData : rowDatasList) {
                if (eventType == EventType.DELETE) {
                    applicationContext.publishEvent(new SyncEntityEvent(this, rowData.getBeforeColumnsList(), eventType, header));
                } else if (eventType == EventType.INSERT) {
                    applicationContext.publishEvent(new SyncEntityEvent(this, rowData.getAfterColumnsList(), eventType, header));
                } else if (eventType == EventType.UPDATE){
                    applicationContext.publishEvent(new SyncEntityEvent(this, rowData.getAfterColumnsList(), eventType, header));
                }
            }

        }
    }

}
