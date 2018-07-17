/*
 * XP.RAVEN Project
 * <p/>
 * Copyright (C) 2018 Xpand IT.
 * <p/>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.task.compatibility;

import com.xpandit.plugins.xrayjenkins.Utils.ConfigurationUtils;
import com.xpandit.plugins.xrayjenkins.task.XrayExportBuilder;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class XrayExportBuilderCompatibilityDelegate implements CompatibilityDelegate {

    private XrayExportBuilder exportBuilder;

    public XrayExportBuilderCompatibilityDelegate(XrayExportBuilder exportBuilder) {
        this.exportBuilder = exportBuilder;
    }

    @Override
    public void applyCompatibility() {
        //only backward compatibility is needed for now
        exportBuilder.setXrayInstance(ConfigurationUtils.getConfiguration(exportBuilder.getServerInstance()));
        exportBuilder.setFields(getFields());

    }

    private Map<String, String> getFields(){
        Map<String, String> fields = new HashMap<>();
        if(StringUtils.isNotBlank(exportBuilder.getIssues())){
            fields.put("issues", exportBuilder.getIssues());
        }
        if(StringUtils.isNotBlank(exportBuilder.getFilter())){
            fields.put("filter", exportBuilder.getFilter());
        }
        if(StringUtils.isNotBlank(exportBuilder.getIssues())){
            fields.put("filePath", exportBuilder.getFilePath());
        }
        return fields;
    }


}
