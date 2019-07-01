package com.xpandit.plugins.xrayjenkins.model;

import org.apache.commons.lang.StringUtils;

public enum HostingType {
    SERVER("server"), CLOUD("cloud");
    public final String name;

    HostingType(String name) { this.name = name; }

    public String getName() {
        return name;
    }

    public static String getCloudHostingTypeName(){
        return HostingType.CLOUD.getName();
    }

    public static String getServerHostingTypeName(){
        return HostingType.SERVER.getName();
    }

    /**
     * Method that finds a hosting type by it's name/value
     * @param name of the Hosting type ex:"server"
     * @return HostingType ex: SERVER or null if does no exist
     */
    public static HostingType findByName(String name) {
        for (HostingType type : HostingType.values()) {
            if (StringUtils.equals(type.getName(), name)) {
                return type;
            }
        }
        return null;
    }

    public static HostingType getDefaultType() {
        return HostingType.SERVER;
    }
}
