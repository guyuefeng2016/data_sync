package com.hdvon.event;

import com.hdvon.entity.SyncEntity;
import lombok.Data;

@Data
public class DisruptorSyncEntityEvent {

    private SyncEntity syncEntity;

}
