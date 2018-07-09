/*
 * XP.RAVEN Project
 * <p/>
 * Copyright (C) 2018 Xpand IT.
 * <p/>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.Utils;

import com.xpandit.plugins.xrayjenkins.exceptions.XrayJenkinsGenericException;
import hudson.FilePath;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class FileUtils {

    private FileUtils(){
    }

    /**
     * Returns a list of files that matches the glob expression relatively to the workspace.
     * The glob expression is relative, any attempt to match an absolute path will not work.
     * @param workspace the workspace
     * @param globExpression the glob expression. Must be relative to the workspace
     */
    public static List<FilePath> getFiles(FilePath workspace,
                                          String globExpression)
            throws IOException, InterruptedException {
        if(workspace == null){
            throw new XrayJenkinsGenericException("workspace cannot be null");
        }
        if(StringUtils.isBlank(globExpression)){
            throw new XrayJenkinsGenericException("The file path cannot be null nor empty");
        }
        List<FilePath> paths = new ArrayList<>();
        FilePath [] pathArray = workspace.list(globExpression, "", false);
        if(pathArray.length == 0){
            throw new XrayJenkinsGenericException("No file matching the glob expression was found.");
        }
        paths.addAll(Arrays.asList(pathArray));
        return paths;
    }


}
