/*
 * XP.RAVEN Project
 * <p/>
 * Copyright (C) 2018 Xpand IT.
 * <p/>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.Utils;

import hudson.FilePath;
import hudson.model.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FileUtilsTest {

    @Rule
    public TemporaryFolder workspace = new TemporaryFolder();

    @Mock
    private TaskListener taskListener;

    private static final String XRAYJENKINS = "xrayjenkins";
    private static final String WORK = "work";
    private static final String WORKSPACEFOLDER = "workspace";
    private static final String DUMMYPROJECT = "dummyproject";
    private static final String UNITTESTING = "unittesting";
    private static final String RESULTS = "results";
    private static final String FOLDERMATCHER = "**";
    private static final String EXCEPTION_MESSAGE = "An exception occurred when performing the Test: ";
    private static final String LOGGER_NAME = "log.txt";

    private void prepareFolders() throws IOException{
        File fa = workspace.newFolder(XRAYJENKINS,WORK, WORKSPACEFOLDER, DUMMYPROJECT, UNITTESTING);
        File.createTempFile("potatoe", ".txt", fa).deleteOnExit();
        File.createTempFile("hello", ".xml", fa).deleteOnExit();
        File fa1b = workspace.newFolder(XRAYJENKINS,WORK, WORKSPACEFOLDER, DUMMYPROJECT,UNITTESTING,"a1",RESULTS);
        File.createTempFile("antonio", ".jpg", fa1b).deleteOnExit();
        workspace.newFolder(XRAYJENKINS,WORK, WORKSPACEFOLDER,DUMMYPROJECT, UNITTESTING,"a1",RESULTS,"b1");
        File fa1b2 = workspace.newFolder(XRAYJENKINS,WORK, WORKSPACEFOLDER,DUMMYPROJECT, UNITTESTING,"a1",RESULTS,"b2");
        File.createTempFile("vacations", ".jpg", fa1b2).deleteOnExit();
        File.createTempFile("result_fa1b2_1", ".xml", fa1b2).deleteOnExit();//this is a result file
        File.createTempFile("result_fa1b2_3", ".xml", fa1b2).deleteOnExit();//this is a result file
        workspace.newFolder(XRAYJENKINS,WORK, WORKSPACEFOLDER,DUMMYPROJECT, UNITTESTING,"a1",RESULTS,"b3");
        workspace.newFolder(XRAYJENKINS,WORK, WORKSPACEFOLDER,DUMMYPROJECT, UNITTESTING,"a2",RESULTS,"b1");
        workspace.newFolder(XRAYJENKINS,WORK, WORKSPACEFOLDER,DUMMYPROJECT, UNITTESTING,"a2",RESULTS,"b2");
        File fa2b3 = workspace.newFolder(XRAYJENKINS,WORK, WORKSPACEFOLDER,DUMMYPROJECT, UNITTESTING,"a2",RESULTS,"b3");
        File.createTempFile("result_fa2b3_1", ".xml", fa2b3).deleteOnExit();//this is a result file
        File.createTempFile("result_fa2b3_2", ".xml", fa2b3).deleteOnExit();//this is a result file
        File.createTempFile("rockyII", ".avi", fa2b3).deleteOnExit();
        File fa3b = workspace.newFolder( XRAYJENKINS,WORK, WORKSPACEFOLDER, DUMMYPROJECT, UNITTESTING,"a3",RESULTS);
        File.createTempFile("movie1", ".zip", fa3b).deleteOnExit();
        File.createTempFile("backup",".xml", fa3b).deleteOnExit();
        workspace.newFolder(XRAYJENKINS,WORK, WORKSPACEFOLDER,DUMMYPROJECT, UNITTESTING,"a3",RESULTS,"b1");
        workspace.newFolder(XRAYJENKINS,WORK, WORKSPACEFOLDER,DUMMYPROJECT, UNITTESTING,"a3",RESULTS,"b2");
        File fa3b3 = workspace.newFolder(XRAYJENKINS,WORK, WORKSPACEFOLDER,DUMMYPROJECT, UNITTESTING,"a3",RESULTS,"b3");
        File.createTempFile("result_fa3b3_1",".xml", fa3b3).deleteOnExit(); //this is a result file
        File.createTempFile("february_05_fa3b3",".xml", fa3b3).deleteOnExit(); //this is a result file
        File.createTempFile("february_17_fa3b3",".xml", fa3b3).deleteOnExit(); //this is a result file
    }

    @Test
    public void testGetFileWithAbsolutePath(){
        try{
            try(PrintStream logger = new PrintStream(workspace.newFile(LOGGER_NAME))){
                when(taskListener.getLogger()).thenReturn(logger);
            }
            prepareFolders();
            String resultsPath = workspace.getRoot().toPath().toString() + getAbsoluteDirectoryPath() + "*.xml";
            List<FilePath> matchingFiles = FileUtils.getFilePaths(new FilePath(getWorkspaceFile()), resultsPath, taskListener);
            Assert.assertTrue(matchingFiles.size() == 7);
        } catch (IOException e){
            Assert.fail(EXCEPTION_MESSAGE + e.getMessage());
        }
        workspace.delete();
    }

    @Test
    public void testGetFileWithRelativePath(){
        try{
            try(PrintStream logger = new PrintStream(workspace.newFile(LOGGER_NAME))){
                when(taskListener.getLogger()).thenReturn(logger);
            }
            prepareFolders();
            String resultsPath = getRelativeDirectoryPath() + "*.xml";
            List<FilePath> matchingFiles = FileUtils.getFilePaths(new FilePath(getWorkspaceFile()), resultsPath, taskListener);
            Assert.assertTrue(matchingFiles.size() == 7);
        } catch (IOException e){
            Assert.fail(EXCEPTION_MESSAGE + e.getMessage());
        }
        workspace.delete();
    }

    private File getWorkspaceFile(){
        return new File(workspace.getRoot().toPath().toString() + getWorkSpaceSuffix());
    }

    private String getWorkSpaceSuffix(){
        return File.separator
                + XRAYJENKINS
                + File.separator
                + WORK
                + File.separator
                + WORKSPACEFOLDER
                + File.separator
                + DUMMYPROJECT;
    }

    @Test
    public void testGetFileWithSofisticatedGlobExpression(){
        try{
            try(PrintStream logger = new PrintStream(workspace.newFile(LOGGER_NAME))){
                when(taskListener.getLogger()).thenReturn(logger);
            }
            prepareFolders();
            String resultsPath = workspace.getRoot().toPath().toString() + getAbsoluteDirectoryPath() + "feb*.xml";
            List<FilePath> matchingFiles = FileUtils.getFilePaths(new FilePath(getWorkspaceFile()), resultsPath, taskListener);
            Assert.assertTrue(matchingFiles.size() == 2);
        } catch (IOException e){
            Assert.fail(EXCEPTION_MESSAGE + e.getMessage());
        }
        workspace.delete();
    }

    private String getAbsoluteDirectoryPath(){
        return  File.separator
                + XRAYJENKINS
                + File.separator
                + WORK
                + File.separator
                + WORKSPACEFOLDER
                + File.separator
                + DUMMYPROJECT
                + File.separator
                + UNITTESTING
                + File.separator
                + FOLDERMATCHER
                + File.separator
                + RESULTS
                + File.separator
                + FOLDERMATCHER
                + File.separator;
    }

    private String getRelativeDirectoryPath(){
        /*"\\unittesting\\**\\results\\**\\*.xml";*/
        return File.separator
                + UNITTESTING
                + File.separator
                + FOLDERMATCHER
                + File.separator
                + RESULTS
                + File.separator
                + FOLDERMATCHER
                + File.separator;
    }

}
