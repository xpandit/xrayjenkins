/*
 * XP.RAVEN Project
 * <p/>
 * Copyright (C) 2018 Xpand IT.
 * <p/>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.Utils;

import com.xpandit.plugins.xrayjenkins.model.ServerConfiguration;

public class ConfigurationUtils {

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
