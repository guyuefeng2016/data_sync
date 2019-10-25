package com.hdvon.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class SyncEntity implements Serializable {

    private long sqlExecuteTime; //sql执行时间
    private String sql;

    private SyncWorkOrderEntity workOrderSyncEntity;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncEntity that = (SyncEntity) o;

        if (sql != null ? !sql.equals(that.sql) : that.sql != null) return false;
        return workOrderSyncEntity != null ? workOrderSyncEntity.equals(that.workOrderSyncEntity) : that.workOrderSyncEntity == null;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (int) (sqlExecuteTime ^ (sqlExecuteTime >>> 32));
        result = 31 * result + (sql != null ? sql.hashCode() : 0);
        result = 31 * result + (workOrderSyncEntity != null ? workOrderSyncEntity.hashCode() : 0);
        return result;
    }
}
