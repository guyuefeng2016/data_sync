package com.hdvon.event;

import com.alibaba.otter.canal.protocol.CanalEntry;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class SyncEntityEvent extends ApplicationEvent {

    List<CanalEntry.Column> columns;
    CanalEntry.EventType eventType;
    CanalEntry.Header header;

    public SyncEntityEvent(Object source, List<CanalEntry.Column> columns, CanalEntry.EventType eventType, CanalEntry.Header header) {
        super(source);
        this.columns = columns;
        this.eventType = eventType;
        this.header = header;
    }

}
