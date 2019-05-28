/*
 * XP.RAVEN Project
 * <p/>
 * Copyright (C) 2018 Xpand IT.
 * <p/>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.Utils;

import com.xpandit.plugins.xrayjenkins.model.ServerConfiguration;
import com.xpandit.plugins.xrayjenkins.model.XrayInstance;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationUtils.class);

    /**
     * Utility method to get an XrayInstance
     * @param serverConfigurationId the server configuration ID
     * @return <code>XrayInstance</code> if found, <code>null</code> otherwise
     */
    public static XrayInstance getConfiguration(String serverConfigurationId){
        int idIndex = 1;
        if(serverConfigurationId.startsWith(XrayInstance.HostingType.CLOUD.value)){
            serverConfigurationId = serverConfigurationId.split(XrayInstance.HostingType.CLOUD.value + "-")[idIndex];
        } else {
            serverConfigurationId = serverConfigurationId.split(XrayInstance.HostingType.SERVER.value + "-")[idIndex];
        }

        XrayInstance config =  null;
        List<XrayInstance> serverInstances =  ServerConfiguration.get().getServerInstances();
        for(XrayInstance sc : serverInstances){
            if(sc.getConfigID().equals(serverConfigurationId)){
                config = sc;
                break;
            }
        }
        if(config == null){
            LOG.error("No XrayInstance could be found with configuration id '{}'", serverConfigurationId);
        }
        return config;
    }

    /**
     * Utility method to check if any xray jira server configuration exists
     * @return <code>true</code> if any server configuration is available, <code>false</code> otherwise
     */
    public static boolean anyAvailableConfiguration(){
        ServerConfiguration configuration = ServerConfiguration.get();
        return configuration != null
                && configuration.getServerInstances() != null
                && configuration.getServerInstances().size() > 0;
    }

}
