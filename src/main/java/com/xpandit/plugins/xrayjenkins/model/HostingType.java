package com.xpandit.plugins.xrayjenkins.model;

import org.apache.commons.lang.StringUtils;

public enum HostingType {
    SERVER("server"), CLOUD("cloud");
    public final String name;

    HostingType(String name) { this.name = name; }

    public String getName() {
        return name;
    }

    public static String getCloudHostingType(){
        return HostingType.CLOUD.getName();
    }

    public static String getServerHostingType(){
        return HostingType.SERVER.getName();
    }

    public static HostingType findByName(String name) {
        for (HostingType type : HostingType.values()) {
            if (StringUtils.equals(type.getName(), name)) {
                return type;
            }
        }
        return null;
    }
}
