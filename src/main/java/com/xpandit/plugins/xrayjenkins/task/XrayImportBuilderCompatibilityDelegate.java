/*
 * XP.RAVEN Project
 * <p/>
 * Copyright (C) 2018 Xpand IT.
 * <p/>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.task;

import com.xpandit.plugins.xrayjenkins.Utils.ConfigurationUtils;
import com.xpandit.plugins.xrayjenkins.model.XrayInstance;
import com.xpandit.xray.model.Endpoint;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;


/**
 * Delegate class that performs forward and backward compatibility processing
 * between previous versions from 1.3.0 to 1.3.0 and upper.
 */
public class XrayImportBuilderCompatibilityDelegate {

    private static final String INPUT_INFO_SWITCHER = "inputInfoSwitcher";
    private static final String TEST_ENVIRONMENTS = "testEnvironments";
    private static final String PROJECT_KEY = "projectKey";
    private static final String TEST_PLAN_KEY = "testPlanKey";
    private static final String FIX_VERSION = "fixVersion";
    private static final String IMPORT_FILE_PATH = "importFilePath";
    private static final String TEST_EXEC_KEY = "testExecKey";
    private static final String REVISION_FIELD = "revision";
    private static final String IMPORT_INFO = "importInfo";

    XrayImportBuilder xrayImportBuilder;

    public XrayImportBuilderCompatibilityDelegate(XrayImportBuilder xrayImportBuilder) {
        this.xrayImportBuilder = xrayImportBuilder;
    }

    public void applyCompatibility(){
        if(needForwardCompatibility()){
            applyForwardCompatibility();
        } else{
            applyBackwardCompatibility();
        }
    }

    /**
     * This method applies some compatibility processing so the pre-configured jobs are compatible
     * between pre 1.3.0 and current versions of the plugin.
     */
    private void applyForwardCompatibility(){
        xrayImportBuilder.setEndpointName(xrayImportBuilder.getEndpoint().getSuffix());
        xrayImportBuilder.setProjectKey(xrayImportBuilder.getDynamicFields().get(PROJECT_KEY));
        xrayImportBuilder.setTestEnvironments(xrayImportBuilder.getDynamicFields().get(TEST_ENVIRONMENTS));
        xrayImportBuilder.setTestPlanKey(xrayImportBuilder.getDynamicFields().get(TEST_PLAN_KEY));
        xrayImportBuilder.setFixVersion(xrayImportBuilder.getDynamicFields().get(FIX_VERSION));
        xrayImportBuilder.setImportFilePath(xrayImportBuilder.getDynamicFields().get(IMPORT_FILE_PATH));
        xrayImportBuilder.setTestExecKey(xrayImportBuilder.getDynamicFields().get(TEST_EXEC_KEY));
        xrayImportBuilder.setRevision(xrayImportBuilder.getDynamicFields().get(REVISION_FIELD));
        xrayImportBuilder.setImportInfo(xrayImportBuilder.getDynamicFields().get(IMPORT_INFO));
        xrayImportBuilder.setImportToSameExecution("true");//true is by default
    }

    private boolean needForwardCompatibility(){
        return xrayImportBuilder.getEndpointName() == null
                && xrayImportBuilder.getProjectKey() == null
                && xrayImportBuilder.getTestEnvironments()  == null
                && xrayImportBuilder.getTestPlanKey() == null
                && xrayImportBuilder.getFixVersion() == null
                && xrayImportBuilder.getImportFilePath() == null
                && xrayImportBuilder.getTestExecKey() == null
                && xrayImportBuilder.getRevision() == null
                && xrayImportBuilder.getImportInfo() == null
                && xrayImportBuilder.getImportToSameExecution() == null;
    }

    private void applyBackwardCompatibility(){
        XrayInstance instance = ConfigurationUtils.getConfiguration(xrayImportBuilder.getServerInstance());
        xrayImportBuilder.setXrayInstance(instance);
        xrayImportBuilder.setEndpoint(lookupForEndpoint(xrayImportBuilder.getEndpointName()));
        xrayImportBuilder.setDynamicFields(getDynamicFieldsMap());
    }

    private Map<String,String> getDynamicFieldsMap(){
        Map<String,String> fields = new HashMap<>();
        putNotBlank(fields, PROJECT_KEY, xrayImportBuilder.getProjectKey());
        putNotBlank(fields, TEST_ENVIRONMENTS, xrayImportBuilder.getTestEnvironments());
        putNotBlank(fields,TEST_PLAN_KEY, xrayImportBuilder.getTestPlanKey());
        putNotBlank(fields,FIX_VERSION, xrayImportBuilder.getFixVersion());
        putNotBlank(fields, IMPORT_FILE_PATH, xrayImportBuilder.getImportFilePath());
        putNotBlank(fields,TEST_EXEC_KEY,xrayImportBuilder.getTestExecKey());
        putNotBlank(fields, REVISION_FIELD, xrayImportBuilder.getRevision());
        putNotBlank(fields,IMPORT_INFO, xrayImportBuilder.getImportInfo());
        putNotBlank(fields, INPUT_INFO_SWITCHER,xrayImportBuilder.getInputInfoSwitcher());
        return fields;
    }

    private void putNotBlank(Map<String,String> fields, String key, String val){
        if(StringUtils.isNotBlank(val)){
            fields.put(key,val);
        }
    }

    @Nullable
    private Endpoint lookupForEndpoint(String endpointName){
        Endpoint targetedEndpoint = Endpoint.lookupByName(endpointName);
        return targetedEndpoint != null ? targetedEndpoint : Endpoint.lookupBySuffix(endpointName);
    }

}
