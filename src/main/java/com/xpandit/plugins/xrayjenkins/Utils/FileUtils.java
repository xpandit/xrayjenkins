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
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import java.util.Collections;

public class FileUtils {

    private FileUtils(){
    }

    /**
     * Utility method that returns all .features files from a folder, including those contained in sub folders.
     * This method works for master node and for slave (remote) nodes
     * @param workspace the Jenkins project workspace
     * @param path the folder path
     * @param listener the TaskListener
     * @return the list of the filepath's
     */
    public static List<FilePath> getFeatureFilesFromWorkspace(FilePath workspace,
                                                              String path,
                                                              TaskListener listener) throws IOException, InterruptedException {
        String errors = getErrors(workspace, path, listener);
        if(errors != null){
            throw new XrayJenkinsGenericException(errors);
        }
        List<FilePath> paths = new ArrayList<>();
        FilePath folder = readFile(workspace, path, listener);
        if(folder.isDirectory()){
            paths.addAll(Arrays.asList(folder.list("*.feature","", false)));
            List<FilePath> children = folder.list();
            for(FilePath child : children){
                if(child.isDirectory()){
                    paths.addAll(getFeatureFilesFromWorkspace(workspace, child.toString(), listener));
                }
            }
        } else{
            throw new XrayJenkinsGenericException("The path is not a folder");
        }
        return paths;
    }

    private static String getErrors(FilePath workspace,
                           String path,
                           TaskListener listener){
        List<String> errors = new LinkedList<>();
        if(workspace == null){
            errors.add("workspace cannot be null");
        }
        if(StringUtils.isBlank(path)){
            errors.add("The folder path cannot be null nor empty");
        }
        if(listener == null){
            errors.add("The task listener cannot be null");
        }
        return errors.isEmpty() ? null : StringUtils.join(errors, "\n");
    }


    /**
     * Returns a list of files that matches the glob expression relatively to the workspace or that matches a file path.
     * The glob expression is relative, any attempt to match an absolute path will not work.
     * @param workspace the workspace
     * @param globExpression the glob expression. Must be relative to the workspace
     */
    public static List<FilePath> getFiles(FilePath workspace,
                                          String globExpression,
                                          TaskListener listener)
            throws IOException, InterruptedException {
        if(workspace == null){
            throw new XrayJenkinsGenericException("workspace cannot be null");
        }
        if(StringUtils.isBlank(globExpression)){
            throw new XrayJenkinsGenericException("The file path cannot be null nor empty");
        }
        FilePath [] pathArray = workspace.list(globExpression, "", false);
        if(pathArray.length == 0){
            //If the path was not considered a glob expression, we now need to try to get the file by it's path using readFile method
            FilePath filePath = readFile(workspace, globExpression, listener);
            if(!filePath.exists()){
                throw new XrayJenkinsGenericException("No file matching the glob expression or file path was found.");
            } else if(filePath.isDirectory()){
                throw new XrayJenkinsGenericException("The matching path represents a directory instead of a file");
            }
            return Collections.singletonList(filePath);

        }
        return Arrays.asList(pathArray);
    }

    /**
     * Given the Jenkins project workspace FilePath and the file path, will resolve the FilePath of the file
     * @param workspace the Jenkins workspace
     * @param filePath the file path of the file
     * @param listener the task listener
     * @return the <code>FilePath</code>
     */
    public static FilePath readFile(FilePath workspace, String filePath, TaskListener listener){
        FilePath f = new FilePath(workspace, filePath);
        listener.getLogger().println("File: " + f.getRemote());
        return f;
    }


}
