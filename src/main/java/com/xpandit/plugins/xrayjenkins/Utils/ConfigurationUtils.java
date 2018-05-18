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
import jline.internal.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationUtils.class);

    public static XrayInstance getConfiguration(String configID){
        XrayInstance config =  null;
        List<XrayInstance> serverInstances =  ServerConfiguration.get().getServerInstances();
        for(XrayInstance sc : serverInstances){
            if(sc.getConfigID().equals(configID)){
                config = sc;
                break;
            }
        }
        if(config == null){
            LOG.error("No XrayInstance could be found with configuration id '{}'", configID);
        }
        return config;
    }

}
