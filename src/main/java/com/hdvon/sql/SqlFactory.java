package com.hdvon.sql;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.google.common.collect.Maps;
import com.hdvon.constant.CommonConstant;
import com.hdvon.entity.SqlDataEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @Author:huwenfeng
 * @Description:
 * @Date: 18:38 2019/8/30
 */
@Component
@Slf4j
public class SqlFactory extends BaseSqlFactory {

    /**
     * 构造sql
     * @param columns
     * @param eventType
     * @param schemaName
     * @param tableName
     * @return
     */
    public SqlDataEntity buildSqlData(List<CanalEntry.Column> columns, CanalEntry.EventType eventType, String schemaName, String tableName){
        SqlDataEntity dataEntity = new SqlDataEntity();
        StringBuffer sb = new StringBuffer();

        sb.append(eventType).append(" ");
        if (eventType == CanalEntry.EventType.INSERT){
            sb.append("INTO").append(" ");
            sb.append(schemaName).append(".").append(tableName).append("(");
            sb.append(buildInsertSqlParameter(columns)).append(")").append(" ");
            sb.append("VALUES(");
            sb.append(buildInsertSqlValue(columns));
            sb.append(");");

            if (log.isDebugEnabled()) log.debug("生成的insert语句sql:  {} ", sb.toString());
        } else if (eventType == CanalEntry.EventType.UPDATE){
            sb.append(schemaName).append(".").append(tableName).append(" ");
            sb.append("SET").append(" ");
            sb.append(buildUpdateSqlPV(columns)).append(" ");
            sb.append("WHERE").append(" ");
            sb.append(buildSqlWhere(columns));
            sb.append(";");

            if (log.isDebugEnabled()) log.debug("生成的update语句sql:  {} ", sb.toString());
        } else if (eventType == CanalEntry.EventType.DELETE){
            sb.append("FROM").append(" ");
            sb.append(schemaName).append(".").append(tableName).append(" ");
            sb.append("WHERE").append(" ");
            sb.append(buildSqlWhere(columns));
            sb.append(";");

            if (log.isDebugEnabled()) log.debug("生成的delete语句sql:  {} ", sb.toString());
        }

        setColumnsMap(dataEntity, columns);
        dataEntity.setSql(sb.toString());
        return dataEntity;
    }

    /**
     * @param dataEntity
     * @param columns
     */
    private void setColumnsMap(SqlDataEntity dataEntity,List<CanalEntry.Column> columns){
        Map<String,Object> columnsMap = Maps.newHashMap();
        for (CanalEntry.Column column : columns) {
            String name = column.getName();
            String value = column.getValue();
            columnsMap.put(name, value+ CommonConstant.SPLIT_STRING +column.getUpdated());
        }
        dataEntity.setColumnsMap(columnsMap);
    }

}
