/*
 * XP.RAVEN Project
 * <p/>
 * Copyright (C) 2018 Xpand IT.
 * <p/>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.Utils;

import com.xpandit.plugins.xrayjenkins.model.HostingType;
import com.xpandit.plugins.xrayjenkins.model.ServerConfiguration;
import com.xpandit.plugins.xrayjenkins.model.XrayInstance;
import hudson.util.ListBoxModel;
import java.util.List;

public class FormUtils {

    /**
     * Utility method to get the list of configured Xray Instances to be used by
     * Build Step and post Built Action forms
     * @return ListBoxModel
     */
    public static ListBoxModel getServerInstanceItems(){
        ListBoxModel items = new ListBoxModel();
        List<XrayInstance> serverInstances =  ServerConfiguration.get().getServerInstances();
        if(serverInstances == null){
            return items;
        }
        for(XrayInstance sc : serverInstances){
            if (sc.getHosting() == null){
                items.add(sc.getAlias(), HostingType.getServerHostingType() + "-" + sc.getConfigID());
            } else {
                items.add(sc.getAlias(),sc.getHosting().getName() + "-" + sc.getConfigID());
            }
        }
        return items;
    }

}
