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
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class FileUtils {

    /**
     * Utility method that support the usage of glob expression within a filepath.
     * Also supports folder matching using "**"
     * @param workspace the jenkins workspace
     * @param filePath the filepath
     * @param listener the task listener
     * @return a list of matching files.
     * @throws IOException
     */
    public static List<FilePath> getFilePaths(FilePath workspace, String filePath, TaskListener listener) throws IOException {
        List<FilePath> filePaths = new ArrayList<>();
        if(workspace == null){
            throw new XrayJenkinsGenericException("No workspace in this current node");
        }
        if(StringUtils.isBlank(filePath)){
            throw new XrayJenkinsGenericException("No file path was specified");
        }
        FilePath file = readFile(workspace,filePath.trim(),listener);
        List<File> files = getFileList(file.getRemote(), listener);
        if(files.isEmpty()){
            throw new XrayJenkinsGenericException("File path is a directory or no matching file exists");
        }
        for(File f : files){
            filePaths.add(new FilePath(f));
        }
        return filePaths;
    }

    private static FilePath readFile(FilePath workspace, String filePath, TaskListener listener) throws IOException{
        FilePath f = new FilePath(workspace, filePath);
        listener.getLogger().println("File: " + f.getRemote());
        return f;
    }

    private static List<File> getFileList(String globExpression, TaskListener listener) {
        List<File> files = new ArrayList<>();
        String [] parts = globExpression.split("\\\\");
        String glob = parts[parts.length - 1];
        PathMatcher matcher;
        try{
            matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        } catch (IllegalArgumentException e){
            listener.getLogger().println("pattern is invalid");
            throw e;
        } catch (UnsupportedOperationException e){
            listener.getLogger().println("the pattern syntax is not known");
            throw e;
        }
        for(File file : getFileList(resolveFolders(globExpression))){
            if (matcher.matches(file.toPath().getFileName())) {
                files.add(file);
            }
        }
        return files;
    }

    private static List<File> getFileList(List<String> paths){
        List<File> files = new ArrayList<>();
        for(String path : paths){
            File folder = new File(rebuildPath(path.split("\\\\")));
            File [] folderFiles = folder.listFiles();
            if(folderFiles != null){
                files.addAll(Arrays.asList(folderFiles));
            }
        }
        return files;
    }

    private static String rebuildPath(String [] parts){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < parts.length - 1; i ++){
            sb.append(parts[i]).append("\\");
        }
        return sb.toString();
    }

    private static List<String> resolveFolders(String path) {
        List<String> paths = new ArrayList<>();
        String [] parts = path.split("\\*\\*");
        if(parts.length == 1){
            paths.add(path);
            return paths;
        }
        File folder = new File(parts[0]);
        if(!folder.exists()){
            return new ArrayList<>();
        }
        File [] files = folder.listFiles();
        if(files == null){
            return new ArrayList<>();
        }
        for(File f : files){
            if(f.isDirectory()){
                String newPath = f.getPath() + mergeParts(Arrays.copyOfRange(parts, 1, parts.length));
                paths.addAll(resolveFolders(newPath));
            }
        }
        return paths;
    }

    private static String mergeParts(String [] parts){
        if(parts.length == 1){
            return parts[0];
        }
        StringBuilder sb = new StringBuilder();
        sb.append(parts[0]);
        for(int i = 1 ; i < parts.length; i ++){
            sb.append("**").append(parts[i]);
        }
        return sb.toString();
    }

}
