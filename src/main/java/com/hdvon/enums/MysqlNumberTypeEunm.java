package com.hdvon.enums;

import org.springframework.util.StringUtils;

public enum MysqlNumberTypeEunm {

    BIT("bit"),BOOL("bool"),TINY_INT("tinyint"),SMALL_INT("smallint"),MEDIUM_INT("mediumint"),
    INT("int"),BIG_INT("bigint"),FLOAT("float"),DOUBLE("double"),DECIMAL("decimal");

    String label;
    MysqlNumberTypeEunm(String label){
        this.label = label;
    }

    public static boolean contain(String label){
        if (StringUtils.isEmpty(label)) return false;
        String lowerLabel = label.toLowerCase();
        MysqlNumberTypeEunm[] values = values();
        for(MysqlNumberTypeEunm type : values){
            if (lowerLabel.indexOf(type.label) != -1){
                return true;
            }
        }
        return false;
    }
}
