package com.hdvon.service.impl;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.alibaba.otter.canal.protocol.exception.CanalClientException;
import com.hdvon.config.CanalConfig;
import com.hdvon.config.HdvonThreadFactory;
import com.hdvon.enums.CachesEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @Author:huwenfeng
 * @Description: 
 * @Date: 18:32 2019/8/31
 */
@Slf4j
public abstract class CanalStatusService  implements DisposableBean {

    @Resource(name = "caffeineCacheManager")
    private CacheManager cacheManager;

    @Autowired
    private CanalConfig canalConfig;

    private ScheduledExecutorService scheduledExecutorService;

    private ExecutorService canalExecutor;

    private static volatile boolean start;

    private CanalConnector connector;

    abstract void processEntry(List<CanalEntry.Entry> entrys);

    public void init(){
        //初始化
        initCanal();
        // 连接Canal
        canalExecutor.execute(() -> connectCanal());
        // 定时检测Canal状态
        scheduledExecutorService.scheduleAtFixedRate(() -> checkCanalStatus(), 1, 4, TimeUnit.MINUTES);
    }


    /**
     * 初始化Canal连接信息
     */
    private void initCanal(){
        if (log.isDebugEnabled()) log.debug("######### 初始化Canal连接信息 ########");
        int threads = Runtime.getRuntime().availableProcessors();

        scheduledExecutorService = new ScheduledThreadPoolExecutor(threads, HdvonThreadFactory.create("check_canal_status", true));

        canalExecutor = Executors.newSingleThreadExecutor(HdvonThreadFactory.create("canal_servers", true));

        if (canalConfig.isCluster()){
            if (log.isDebugEnabled()) log.debug("开始连接ZK集群Canal信息：{}", canalConfig.getZkServers());
            connector = CanalConnectors.newClusterConnector(canalConfig.getZkServers(), canalConfig.getDestination(),
                    canalConfig.getUsername(), canalConfig.getPassword());
        }else {
            if (log.isDebugEnabled()) log.debug("开始连接单个Canal服务器：{}", canalConfig.getCanalHostIp());
            connector = CanalConnectors.newSingleConnector(new InetSocketAddress(canalConfig.getCanalHostIp(),
                    canalConfig.getCanalHostPort()), canalConfig.getDestination(), canalConfig.getUsername(), canalConfig.getPassword());
        }
    }

    /**
     * Canal连接
     */
    private void connectCanal(){
        try {
            connector.connect();
            connector.subscribe(); //统一放在canal服务器配置
            start = true;

            if (log.isDebugEnabled()) log.debug("######## Canal 连接成功 ########");
        }catch (CanalClientException e){
            log.error("Canal服务连接失败：", e);
            start = false;
        }

        if (start){
            connectGetData();
        }
    }

    /**
     * 获取数据
     */
    private void connectGetData() {
        int batchSize = 1000;
        while (true) {
            try {
                Message message = connector.getWithoutAck(batchSize); // 获取指定数量的数据
                long batchId = message.getId();
                int size = message.getEntries().size();
                if (batchId == -1 || size == 0) {
                    Thread.sleep(2000); //没有获取到数据，就休眠2秒钟
                } else {
                    processEntry(message.getEntries());
                }
                connector.ack(batchId); // 提交确认
            } catch (Exception e){
                log.error(e.getMessage());
                closeCanal();
                break;
            }
        }
    }

    /**
     * 检测Canal状态
     */
    private void checkCanalStatus(){
        if (log.isDebugEnabled()) log.debug("######## 开始执行检测Canal连接状态 ########");
        if (!start){
            log.warn("######## 开始重连Canal服务 ######## ");
            this.connectCanal();
        }
        if (log.isDebugEnabled())  log.debug("######## Canal is ok  ########");
    }

    @Override
    public void destroy() throws Exception {
        log.error("####### 即将关闭Canal连接，销毁线程池 ########");
        if (start) {
            try {
                connector.disconnect();
            } catch (CanalClientException e) {
                log.error("关闭Canal连接异常：", e);
            }
        }
        canalExecutor.shutdown();
        scheduledExecutorService.shutdown();
    }

    /**
     * 关闭Canal
     */
    private void closeCanal() {
        try {
            if (start) {
                connector.disconnect();
            }
        } catch (CanalClientException e) {
            log.error("关闭Canal连接错误 {}", e);
        }  finally {
            start = false;
        }
    }

    /**
     * 同步的数据库
     */
    protected String syncDatabase(){
        String database = canalConfig.getSyncDatabase();
        if (StringUtils.isEmpty(database)) {
            throw new RuntimeException("同步数据库的名字不能为空");
        }
        return database;
    }


    /**
     * 获取所有的同步的表
     */
    public Set<String> syncTables(){
        String cacheName = CachesEnum.syncTables.name();
        return cacheManager.getCache(cacheName).get(cacheName, new Callable<Set<String>>() {
            @Override
            public Set<String> call() throws Exception {
                Set<String> tableSet = new HashSet<>();
                String tables[] = canalConfig.getSyncTables();
                for (String table : tables){
                    tableSet.add(table);
                }
                return tableSet;
            }
        });
    }

    /**
     * 获取所有的同步的表
     */
    public boolean syncAllTables(){
        String tables[] = canalConfig.getSyncTables();
        if (tables == null || (tables.length == 1 && tables[0].equalsIgnoreCase("*"))) {
            return true;
        }
        return false;
    }
}
