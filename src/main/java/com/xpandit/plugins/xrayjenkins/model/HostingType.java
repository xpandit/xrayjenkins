package com.xpandit.plugins.xrayjenkins.model;

import org.apache.commons.lang.StringUtils;

public enum HostingType {
    SERVER("server"), CLOUD("cloud");
    private final String typeName;

    HostingType(String typeName) { this.typeName = typeName; }

    public String getTypeName() {
        return typeName;
    }

    public static String getCloudHostingTypeName(){
        return HostingType.CLOUD.getTypeName();
    }

    public static String getServerHostingTypeName(){
        return HostingType.SERVER.getTypeName();
    }

    /**
     * Method that finds a hosting type by it's name/value
     * @param name of the Hosting type ex:"server"
     * @return HostingType ex: SERVER or null if does no exist
     */
    public static HostingType findByName(String name) {
        for (HostingType type : HostingType.values()) {
            if (StringUtils.equals(type.getTypeName(), name)) {
                return type;
            }
        }
        return null;
    }

    public static HostingType getDefaultType() {
        return HostingType.SERVER;
    }
}
