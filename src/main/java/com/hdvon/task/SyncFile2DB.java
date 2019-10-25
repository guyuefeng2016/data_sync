package com.hdvon.task;

import com.alibaba.fastjson.JSONArray;
import com.hdvon.config.HdvonThreadFactory;
import com.hdvon.config.ReadSyncFile2DBConfig;
import com.hdvon.constant.FileConstant;
import com.hdvon.constant.WorkOrderConstant;
import com.hdvon.disruptor.service.ISyncEntityExecutorService;
import com.hdvon.entity.DiagnosisWorkOrder;
import com.hdvon.entity.SyncEntity;
import com.hdvon.entity.SyncWorkOrderEntity;
import com.hdvon.enums.SystemEnvironmentEunm;
import com.hdvon.service.IDiagnosisWorkOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @Author:huwenfeng
 * @Description: 读取文件，同步文件SQL到数据库
 * @Date: 15:29 2019/9/2
 */
@Slf4j
@Component
public class SyncFile2DB {

    @Value("${spring.profiles.active}")
    private String systemEnvironment;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IDiagnosisWorkOrderService diagnosisWorkOrderService;

    @Autowired
    private ISyncEntityExecutorService syncEntityExecutorService;


    @Autowired
    private ReadSyncFile2DBConfig readSyncFile2DBConfig;

    private static final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), HdvonThreadFactory
            .create("read_file_task", false), new ThreadPoolExecutor.AbortPolicy());

    private volatile boolean canWrite = true;
    private volatile boolean discardFile = false;

    @Scheduled(cron = "${task.read.schedule-corn}")
    public void processTask() {
        while (!canWrite){
            LockSupport.parkNanos(1000);
        }
        canWrite = false;
        try {
            executor.execute(() -> {
                try {
                    if (log.isDebugEnabled()) log.debug("######## 寻找可以解析的文件 ########");
                    String remoteSyncFileDir = readSyncFile2DBConfig.getRemoteSyncFileDir();
                    File fileDir = new File(remoteSyncFileDir);
                    if (!fileDir.isDirectory()) fileDir.mkdirs();
                    Collection<File> files = FileUtils.listFiles(fileDir, new String[]{FileConstant.syncFileNameSuffix}, false);
                    files.stream().max((f1, f2) -> {
                        String fName1 = f1.getName();
                        String fName2 = f2.getName();
                        Long fN1 = Long.parseLong(fName1.substring(0,fName1.indexOf(FileConstant.dot)));
                        Long fN2 = Long.parseLong(fName2.substring(0,fName2.indexOf(FileConstant.dot)));

                        return (int)(fN2 - fN1);
                    }).ifPresent(file -> {
                        try {
                            discardFile = false;
                            if (log.isDebugEnabled()) log.debug("######## 开始解析文件 {} ########",file.getPath());
                            String s = FileUtils.readFileToString(file, "UTF-8");
                            List<SyncEntity> syncEntityList = JSONArray.parseArray(s, SyncEntity.class);
                            canSyncData2DB(syncEntityList);

                            copy2RemoteBackFileDir(file);
                            move2RemoteFailBackFileDir(file);
                        } catch (Exception e){
                            log.error("解析文件失败: "+e.getMessage());
                            canWrite = true;
                            discardFile = false;
                        }
                    });
                } catch (Exception e) {
                    log.warn("暂时没有解析的文件: "+e.getMessage());
                    canWrite = true;
                    discardFile = false;
                }
                canWrite = true;
            });
        }catch (Exception e){
            log.error(e.getMessage());
            canWrite = true;
            discardFile = false;
        }

    }

    /**
     * 检测入库、更新、删除条件
     * @param syncEntityList
     */
    private void canSyncData2DB(List<SyncEntity> syncEntityList){
        if (syncEntityList != null){
            List<SyncEntity> canSyncDBContainWorkOrderSyncEntityList = new LinkedList<>();
            for (SyncEntity syncEntity: syncEntityList){
                boolean canExecuteFlag = false;
                SyncWorkOrderEntity workOrderSyncEntity = syncEntity.getWorkOrderSyncEntity();

                if (workOrderSyncEntity == null){
                    //非工单直接入库
                    canExecuteFlag = true;
                } else {
                    String workOrderId = workOrderSyncEntity.getWorkOrderId();

                    if (SystemEnvironmentEunm.GOV.getLabel().equalsIgnoreCase(systemEnvironment)) {
                        //如果当前环境是平台
                        canExecuteFlag = platFormEnvironment(workOrderId, syncEntity);
                    } else if (SystemEnvironmentEunm.APP.getLabel().equalsIgnoreCase(systemEnvironment)){
                        //如果当前环境是互联网
                        canExecuteFlag = appEnvironment(workOrderSyncEntity, syncEntity);
                    }
                }
                if (discardFile) break;
                if (canExecuteFlag) canSyncDBContainWorkOrderSyncEntityList.add(syncEntity);
            }

            if (!discardFile){
                canSyncDBContainWorkOrderSyncEntityList.forEach(syncEntity -> {
                    syncData2DB(syncEntity);
                });
            }
            discardFile = false;
        }
    }


    /**
     * 当前环境为平台环境时， 检测是否 入库、更新、删除
     * @param workOrderId
     * @param syncEntity
     * @return
     */
    private boolean platFormEnvironment(String workOrderId, SyncEntity syncEntity){
        DiagnosisWorkOrder workOrder = diagnosisWorkOrderService.getById(workOrderId);
        if (workOrder == null){
            //平台不存在这个工单，直接同步
            return true;
        } else {
            String status = workOrder.getStatus();
            //平台工单未关闭
            if (!WorkOrderConstant.WORK_ORDER_CLOSE_STATUS.equalsIgnoreCase(status)){
                //比较同步工单和平台工单谁的操作时间更新，同步工单更新，则执行
                return judgeByTime(syncEntity, workOrder);
            } else {
                discardFile = true;
                return false;
            }
        }
    }

    /**
     * 当前环境是互联网环境时， 检测是否 入库、更新、删除
     * @param workOrderSyncEntity
     * @param syncEntity
     * @return
     */
    private boolean appEnvironment(SyncWorkOrderEntity workOrderSyncEntity, SyncEntity syncEntity){
        //平台工单关闭，直接同步
        if (WorkOrderConstant.WORK_ORDER_CLOSE_STATUS.equalsIgnoreCase(workOrderSyncEntity.getWorkOrderStatus())){
            return true;
        } else {
            DiagnosisWorkOrder workOrder = diagnosisWorkOrderService.getById(workOrderSyncEntity.getWorkOrderId());
            if (workOrder == null) {
                //互联网不存在这个工单，直接同步
                return true;
            }
            //比较同步工单和互联网工单谁的操作时间更新，同步工单更新，则执行
            return judgeByTime(syncEntity, workOrder);
        }
    }

    /**
     * 判断工单更新时间哪一端更新，则决定是否 入库、更新、删除
     * @param syncEntity
     * @param workOrder
     * @return
     */
    private boolean judgeByTime(SyncEntity syncEntity, DiagnosisWorkOrder workOrder){
        long syncWorkOrderTime = syncEntity.getSqlExecuteTime();
        long localWorkOrderTime = 0;
        LocalDateTime localUpdateTime = workOrder.getUpdateTime();
        if (localUpdateTime == null) {
            return true;
        } else {
            localWorkOrderTime = localUpdateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (syncWorkOrderTime > localWorkOrderTime){
                return true;
            } else {
                discardFile = true;
                return false;
            }
        }
    }


    /**
     * 执行入库、更新、删除
     * @param syncEntity
     */
    private void syncData2DB(SyncEntity syncEntity){
        String sql = syncEntity.getSql();
        if (log.isDebugEnabled()) log.debug("######### 开始同步数据, sql: {}  #########", sql);

        try {
            jdbcTemplate.execute(sql);
            if (log.isDebugEnabled()) log.debug("######### 同步数据成功, sql: {}  #########", sql);
            //将数据同时写入比较缓存
            syncEntityExecutorService.writeCompareCache(syncEntity);
        } catch (Exception e){
            log.error(e.getMessage());
        }
    }

    /**
     * 从远程网络同步过来入库成功之后，写到文件备份的目录
     * @param srcFile
     */
    private void copy2RemoteBackFileDir(File srcFile){
        String remoteBackFileDir = readSyncFile2DBConfig.getRemoteBackFileDir();
        String fileName = srcFile.getName();
        try {
            if (StringUtils.isEmpty(remoteBackFileDir)){
                log.warn("从远程网络同步过来入库成功之后，写到文件备份的目录没有配置！，请检查您的配置 task.read.remote-back-file-dir");
                return;
            }
            File destFile = new File(remoteBackFileDir, fileName);
            FileUtils.copyFile(srcFile,destFile);
        } catch (Exception e){
            log.error("从远程网络同步过来的文件备份失败 , 写到目录:{} , 文件为：{} ，系统错误消息：{}",remoteBackFileDir,srcFile.getPath(),e.getMessage());
        }
    }

    /**
     * 从远程网络同步过来入库失败之后，写到文件备份的目录
     * @param srcFile
     */
    private void move2RemoteFailBackFileDir(File srcFile){
        String remoteFailBackFileDir = readSyncFile2DBConfig.getRemoteFailBackFileDir();
        String fileName = srcFile.getName();
        try {
            if (StringUtils.isEmpty(remoteFailBackFileDir)){
                log.warn("从远程网络同步过来入库失败的备份目录没有配置！，请检查您的配置 task.read.remote-fail-back-file-dir");
                return;
            }
            File destFile = new File(remoteFailBackFileDir, fileName);
            FileUtils.moveFile(srcFile,destFile);
        } catch (Exception e){
            log.error("本地同步写目录失败 , 写到目录:{} , 文件为：{}  ，系统错误消息：{}",remoteFailBackFileDir,srcFile.getPath(),e.getMessage());
        }
    }

}
