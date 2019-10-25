package com.hdvon.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("diagnosis_work_order")
public class DiagnosisWorkOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ID_WORKER_STR)
    private String id;

    private String sn;

    private String title;

    private String diagnosisType;

    private String relateAssetType;

    private String relateAssetId;

    private String relateAssetIp;

    private String problemId;

    private String problemDescription;

    private String processDescription;

    private String status;

    private String orgId;

    private String projectId;

    private String departmentId;

    private String triggerUserId;

    private String currentHandlerId;

    private Long handupTime;

    private Long respondTime;

    private LocalDateTime problemStartDate;

    private LocalDateTime problemEndDate;

    private Boolean handup;

    private LocalDateTime handupDate;

    private String handupReason;

    private String faultSource;

    private String faultType;

    private LocalDateTime receiveDate;

    private LocalDateTime arriveDate;

    private LocalDateTime reportRepairDate;

    private LocalDateTime repairDate;

    private String repairConfirmer;

    private LocalDateTime repairConfirmDate;

    private Boolean readed;

    private String imgUrls;

    private Boolean handupApply;

    private Boolean isAutoDispatch;

    private String handupImgUrls;

    private String dataSource;

    @TableField(fill = FieldFill.INSERT)
    private String createUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private String updateUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String faultGrade;

    private String priority;

    private String solveResult;

    private String processInstanceId;

    private String remark;

    private LocalDateTime dispatchDate;

    private String reportRepairer;

    private String addressId;

    private LocalDateTime diagnosisDate;

    private LocalDateTime processAssignTime;

    private Integer workOrderType;

    private LocalDateTime inspectStartTime;

    private LocalDateTime inspectEndTime;

    private String policyId;
}
