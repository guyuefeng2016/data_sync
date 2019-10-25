package com.hdvon.task;

import com.alibaba.fastjson.JSONArray;
import com.hdvon.config.HdvonThreadFactory;
import com.hdvon.config.WriteSyncFileTaskConfig;
import com.hdvon.constant.CachesConstant;
import com.hdvon.constant.FileConstant;
import com.hdvon.disruptor.service.impl.SyncEntityExecutorServiceImpl;
import com.hdvon.entity.SyncEntity;
import com.hdvon.enums.CachesEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @Author:huwenfeng
 * @Description: 定时将缓存写入批次分段文件
 * @Date: 15:19 2019/9/2
 */
@Slf4j
@Component
public class WriteSyncFileTask {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private WriteSyncFileTaskConfig writeSyncFileTaskConfig;

    @Value("${task.filename-date-pattern}")
    private String fileNameDatePattern;

    private static final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), HdvonThreadFactory
            .create("write_file_task", false), new ThreadPoolExecutor.AbortPolicy());

    private volatile boolean canWrite = true;

    private static FastDateFormat fastDateFormat;

    @PostConstruct
    public void init(){
        try {
            fastDateFormat = FastDateFormat.getInstance(fileNameDatePattern);
            if (log.isDebugEnabled()) log.debug("####### 生成写文件名策略，时间格式：{}{}{} ########", fileNameDatePattern,FileConstant.dot, FileConstant.fileNameSuffix);
            if (fileNameDatePattern.indexOf("ss") == -1) log.warn("您的文件名策略时间粒度太大，建议至少精确到秒级别！！请修改 task.filename-date-pattern 配置 ！！！！");
        } catch (Exception e){
            log.error("task.filenameDatePattern配置错误，文件名生成策略将默认按照 {} 策略生成", FileConstant.defaultFileNameDatePattern);
            fastDateFormat = FastDateFormat.getInstance(FileConstant.defaultFileNameDatePattern);
        }
    }

    @Scheduled(cron = "${task.write.schedule-corn}")
    public void processTask() {
        while (!canWrite){
            LockSupport.parkNanos(1000);
        }
        canWrite = false;
        try {
            executor.execute(() -> {
                try {
                    Cache cache = cacheManager.getCache(CachesEnum.writeFileCache.name());
                    List list = cache.get(CachesConstant.WRITE_CACHE_KEY, LinkedList.class);
                    if (list != null && list.size() > 0){
                        String fileName = fastDateFormat.format(new Date())+ FileConstant.dot +FileConstant.fileNameSuffix;
                        File file = new File(writeSyncFileTaskConfig.getFileDir(), fileName);

                        if (log.isDebugEnabled()) {
                            log.debug("####### 开始写文件路径： {} #######", file.getPath());
                        }

                        try {
                            //防止因为写过快，导致文件自身覆盖现象
                            String s = FileUtils.readFileToString(file, "UTF-8");
                            if (StringUtils.isNotEmpty(s)){
                                List<SyncEntity> syncEntityList = JSONArray.parseArray(s, SyncEntity.class);
                                list.addAll(0,syncEntityList);
                            }
                        } catch (Exception e){
                            if(log.isDebugEnabled()) log.warn("{}文件第一次开始写，{}",file.getPath(),e.getMessage());
                        }

                        FileUtils.write(file, JSONArray.toJSONString(list), "UTF-8", false);

                        copy2SyncBackFileDir(file, fileName);

                        move2SyncFileDir(file, fileName);
//                        cache.evict(CachesConstant.WRITE_CACHE_KEY);
                        //移除已经处理的数据
                        removeOldKey(list);
                    }
                    canWrite = true;
                } catch (Exception e){
                    log.error("写入本地文件失败,"+e.getMessage());
                    canWrite = true;
                }
            });
        } catch (Exception e){
            log.error(e.getMessage());
            canWrite = true;
        }
    }

    /**
     * 移除已经处理的key
     * @param list
     */
    private void removeOldKey(List list){
        SyncEntityExecutorServiceImpl.canWriteCache = false;
        Cache cache = cacheManager.getCache(CachesEnum.writeFileCache.name());
        List newList = cache.get(CachesConstant.WRITE_CACHE_KEY, LinkedList.class);
        newList.removeAll(list);
        cache.put(CachesConstant.WRITE_CACHE_KEY, newList);
        SyncEntityExecutorServiceImpl.canWriteCache = true;
    }

    /**
     * 本地写备份文件
     * @param srcFile
     * @param fileName
     */
    private void copy2SyncBackFileDir(File srcFile, String fileName){
        String syncBackFileDir = writeSyncFileTaskConfig.getSyncBackFileDir();
        try {
            if (StringUtils.isEmpty(syncBackFileDir)){
                log.warn("本地写备份文件目录没有配置！，请检查您的配置 task.write.sync-back-file-dir");
                return;
            }
            File destFile = new File(syncBackFileDir, fileName);
            FileUtils.copyFile(srcFile,destFile);
        } catch (Exception e){
            log.error("本地写备份文件失败 , 写到目录:{} , 文件为：{} , 系统错误消息: {}",syncBackFileDir,srcFile.getPath(),e.getMessage());
        }
    }

    /**
     * 本地同步写目录，python监控，发送给远程FTP
     * @param srcFile
     * @param oldFileName
     */
    private void move2SyncFileDir(File srcFile, String oldFileName){
        String syncFileDir = writeSyncFileTaskConfig.getSyncFileDir();
        try {
            if (StringUtils.isEmpty(syncFileDir)){
                log.warn("本地同步写目录，python监控，发送给远程FTP没有配置！，请检查您的配置 task.write.sync-file-dir");
                return;
            }
            String newFileName = oldFileName.substring(0, oldFileName.indexOf(FileConstant.dot)) +FileConstant.dot+ FileConstant.syncFileNameSuffix;
            File destFile = new File(syncFileDir, newFileName);
            FileUtils.moveFile(srcFile,destFile);
        } catch (Exception e){
            log.error("本地写发送给FTP目录文件失败 , 写到目录:{} , 文件：{} , 系统错误消息: {}",syncFileDir,srcFile.getPath(),e.getMessage());
        }
    }



}
