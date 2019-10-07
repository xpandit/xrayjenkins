/*
 * XP.RAVEN Project
 * <p/>
 * Copyright (C) 2018 Xpand IT.
 * <p/>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.Utils;

import com.xpandit.plugins.xrayjenkins.exceptions.XrayJenkinsGenericException;
import com.xpandit.xray.model.Endpoint;
import hudson.matrix.MatrixProject;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;

import java.util.Objects;

public class BuilderUtils {

    private static final String SLASH_SEPARATED_REGEX = "/";

    /**
     * Utility method to check if the project type is supported by XrayJenkins plugin
     * @param jobType the project type
     * @return <code>true</code> if the project type is supported, <code>false</code> otherwise
     */
    public static boolean isSupportedJobType(Class<? extends AbstractProject> jobType){
        /*MatrixProject is the jobType used by Multi Configuration Project
         *MavenModuleSet is the jobType used by Maven IntegrationPlugin
         */
        return FreeStyleProject.class.isAssignableFrom(jobType)
                || MatrixProject.class.isAssignableFrom(jobType)
                || MavenModuleSet.class.isAssignableFrom(jobType);
    }


    /**
     * Utility method to check if the endpoint supports glob expressions
     * @param endpointValue the endpoint value
     * @return <code>true</code> if the endpoint supports glob expressions , <code>false </code> otherwise
     */
    public static boolean isGlobExpressionsSupported(Endpoint endpointValue) {
        return (Endpoint.JUNIT.equals(endpointValue)
                || Endpoint.NUNIT.equals(endpointValue)
                || Endpoint.TESTNG.equals(endpointValue)
                || Endpoint.ROBOT.equals(endpointValue)
                || Endpoint.XUNIT.equals(endpointValue)
                || Endpoint.JUNIT_MULTIPART.equals(endpointValue)
                || Endpoint.ROBOT_MULTIPART.equals(endpointValue)
                || Endpoint.TESTNG_MULTIPART.equals(endpointValue)
                || Endpoint.NUNIT_MULTIPART.equals(endpointValue));
    }

    /**
     * Utility method to get the generic endpoint from the corresponding multipart endpoint.
     * @param multipartEndpointSuffix the corresponding multipart endpoint suffix
     * @return the new generic endpoint
     */
    public static Endpoint getGenericEndpointFromMultipartSuffix(String multipartEndpointSuffix) {
            String[] multipartEndpointSuffixSeparated = multipartEndpointSuffix.split(SLASH_SEPARATED_REGEX);

            //Build the suffix of the generic endpoint by extracting the endpoint name from the multipart one and adding a slash before.
            String genericEndpointName = "/" + multipartEndpointSuffixSeparated[1];
            Endpoint newEndpoint = Endpoint.lookupBySuffix(genericEndpointName);

            if(Objects.isNull(newEndpoint)){
                throw new XrayJenkinsGenericException("No file path was specified");
            }
            return newEndpoint;
    }
}
