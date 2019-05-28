package com.xpandit.plugins.xrayjenkins.model;

public enum HostingType {
    SERVER("server"), CLOUD("cloud");
    public String value;

    HostingType(String value) { this.value = value; }

    public static String getCloudHostingType(){
        return HostingType.CLOUD.value;
    }

    public static String getServerHostingType(){
        return HostingType.SERVER.value;
    }
}
