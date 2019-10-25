package com.hdvon.enums;

public enum SystemEnvironmentEunm {

    GOV("gov"),APP("app");

    String label;
    SystemEnvironmentEunm(String label){
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
