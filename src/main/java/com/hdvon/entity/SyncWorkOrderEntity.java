package com.hdvon.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class SyncWorkOrderEntity implements Serializable{

    private String workOrderId; //工单id
    private String workOrderStatus; //工单状态

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncWorkOrderEntity that = (SyncWorkOrderEntity) o;

        if (workOrderId != null ? !workOrderId.equals(that.workOrderId) : that.workOrderId != null) return false;
        return workOrderStatus != null ? workOrderStatus.equals(that.workOrderStatus) : that.workOrderStatus == null;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (workOrderId != null ? workOrderId.hashCode() : 0);
        result = 31 * result + (workOrderStatus != null ? workOrderStatus.hashCode() : 0);
        return result;
    }
}
