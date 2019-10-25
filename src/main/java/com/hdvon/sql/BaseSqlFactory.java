package com.hdvon.sql;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.hdvon.enums.MysqlNumberTypeEunm;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public abstract class BaseSqlFactory {
    /**
     * 构造update条件: 参数=值
     * @param columns
     * @return
     */
    protected String buildUpdateSqlPV(List<CanalEntry.Column> columns){
        StringBuilder sb = new StringBuilder();

        for (CanalEntry.Column column : columns) {
            if (column.getUpdated()){
                String mysqlType = column.getMysqlType();

                sb.append(column.getName()).append(" ");
                sb.append("=").append(" ");

                if (MysqlNumberTypeEunm.contain(mysqlType)) {
                    sb.append(column.getValue()).append(" , ");
                } else {
                    sb.append("\"").append(column.getValue()).append("\"").append(" , ");
                }
            }
        }

        int len = sb.length();
        return len == 0 ? "" : sb.delete(len - 2, len).toString();
    }

    /**
     * 构造 wehere条件:  id=1 and name='hwf'
     * @param columns
     * @return
     */
    protected String buildSqlWhere(List<CanalEntry.Column> columns){
        StringBuilder sb = new StringBuilder();

        for (CanalEntry.Column column : columns) {
            if (!column.getUpdated()){
                String value = column.getValue();
                if (StringUtils.isNotEmpty(value)){
                    String mysqlType = column.getMysqlType();

                    sb.append(column.getName()).append(" ");
                    sb.append("=").append(" ");

                    if (MysqlNumberTypeEunm.contain(mysqlType)) {
                        sb.append(value).append(" ");
                    } else {
                        sb.append("\"").append(value).append("\"").append(" ");
                    }
                    sb.append("AND").append(" ");
                }
            }
        }

        int len = sb.length();
        return len == 0 ? "" : sb.delete(len - 4, len).toString();
    }

    /**
     * 构造insert值
     * @param columns
     * @return
     */
    protected String buildInsertSqlValue(List<CanalEntry.Column> columns){
        StringBuilder sb = new StringBuilder();

        for (CanalEntry.Column column : columns) {
            String value = column.getValue();
            if (column.getUpdated() && StringUtils.isNotEmpty(value)){
                String mysqlType = column.getMysqlType();
                if (MysqlNumberTypeEunm.contain(mysqlType)) {
                    sb.append(value).append(",");
                } else {
                    sb.append("\"").append(value).append("\"").append(",");
                }
            }
        }

        int len = sb.length();
        return len == 0 ? "" : sb.delete(len - 1, len).toString();
    }

    /**
     * 构造insert参数
     * @param columns
     * @return
     */
    protected String buildInsertSqlParameter(List<CanalEntry.Column> columns){
        StringBuilder sb = new StringBuilder();
        for (CanalEntry.Column column : columns) {
            String value = column.getValue();
            if (column.getUpdated() && StringUtils.isNotEmpty(value)){
                sb.append(column.getName()).append(",");
            }
        }
        int len = sb.length();
        return len == 0 ? "" : sb.delete(len - 1, len).toString();
    }
}
