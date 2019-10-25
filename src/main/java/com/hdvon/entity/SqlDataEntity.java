package com.hdvon.entity;

import lombok.Data;

import java.util.Map;

@Data
public class SqlDataEntity {

    private String sql; //返回出来的sql
    private Map<String,Object> columnsMap; //返回出来的所有字段map
}
