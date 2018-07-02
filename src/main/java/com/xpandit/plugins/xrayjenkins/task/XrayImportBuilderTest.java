/*
 * XP.RAVEN Project
 * <p/>
 * Copyright (C) 2018 Xpand IT.
 * <p/>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.task;

import hudson.FilePath;
import hudson.model.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class XrayImportBuilderTest {

    private static final Logger LOG = LoggerFactory.getLogger(XrayImportBuilderTest.class);

    @Rule
    public TemporaryFolder workspace = new TemporaryFolder();

    @Mock
    TaskListener taskListener;

    private void prepareFolders() throws IOException{
        File fa = workspace.newFolder("xrayjenkins","work", "workspace", "dummyproject", "joaquina");
        File.createTempFile("potatoe", ".txt", fa).deleteOnExit();
        File.createTempFile("hello", ".xml", fa).deleteOnExit();
        File fa1b = workspace.newFolder("xrayjenkins","work", "workspace", "dummyproject","joaquina","a1","results");
        File.createTempFile("antonio", ".jpg", fa1b).deleteOnExit();
        workspace.newFolder("xrayjenkins","work", "workspace","dummyproject", "joaquina","a1","results","b1");
        File fa1b2 = workspace.newFolder("xrayjenkins","work", "workspace","dummyproject", "joaquina","a1","results","b2");
        File.createTempFile("vacations", ".jpg", fa1b2).deleteOnExit();
        File.createTempFile("result_fa1b2_1", ".xml", fa1b2).deleteOnExit();//this is a result file
        File.createTempFile("result_fa1b2_3", ".xml", fa1b2).deleteOnExit();//this is a result file
        workspace.newFolder("xrayjenkins","work", "workspace","dummyproject", "joaquina","a1","results","b3");
        workspace.newFolder("xrayjenkins","work", "workspace","dummyproject", "joaquina","a2","results","b1");
        workspace.newFolder("xrayjenkins","work", "workspace","dummyproject", "joaquina","a2","results","b2");
        File fa2b3 = workspace.newFolder("xrayjenkins","work", "workspace","dummyproject", "joaquina","a2","results","b3");
        File.createTempFile("result_fa2b3_1", ".xml", fa2b3).deleteOnExit();//this is a result file
        File.createTempFile("result_fa2b3_2", ".xml", fa2b3).deleteOnExit();//this is a result file
        File.createTempFile("rockyII", ".avi", fa2b3).deleteOnExit();
        File fa3b = workspace.newFolder( "xrayjenkins","work", "workspace", "dummyproject", "joaquina","a3","results");
        File.createTempFile("movie1", ".zip", fa3b).deleteOnExit();
        File.createTempFile("backup",".xml", fa3b).deleteOnExit();
        workspace.newFolder("xrayjenkins","work", "workspace","dummyproject", "joaquina","a3","results","b1");
        workspace.newFolder("xrayjenkins","work", "workspace","dummyproject", "joaquina","a3","results","b2");
        File fa3b3 = workspace.newFolder("xrayjenkins","work", "workspace","dummyproject", "joaquina","a3","results","b3");
        File.createTempFile("result_fa3b3_1",".xml", fa3b3).deleteOnExit(); //this is a result file
        File.createTempFile("february_05_fa3b3",".xml", fa3b3).deleteOnExit(); //this is a result file
        File.createTempFile("february_17_fa3b3",".xml", fa3b3).deleteOnExit(); //this is a result file
    }

    @Test
    public void testGetFileWithAbsolutePath(){
        try{
            when(taskListener.getLogger()).thenReturn(System.out);
            prepareFolders();
            String basePath = workspace.getRoot().toPath().toString();
            File workspaceFile = new File(basePath + "\\xrayjenkins\\work\\workspace\\dummyproject");
            FilePath workspace = new FilePath(workspaceFile);
            String resultsPath = basePath + "\\xrayjenkins\\work\\workspace\\dummyproject\\joaquina\\**\\results\\**\\*.xml";
            XrayImportBuilder builder = new XrayImportBuilder(null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);

            List<FilePath> matchingFiles = builder.getFilePaths(workspace, resultsPath, taskListener);
            Assert.assertTrue(matchingFiles.size() == 7);

        } catch (IOException e){
            Assert.fail("An exception occured when performing the Test: " + e.getMessage());
        }
        workspace.delete();
    }

    @Test
    public void testGetFileWithRelativePath(){
        try{
            when(taskListener.getLogger()).thenReturn(System.out);
            prepareFolders();
            String basePath = workspace.getRoot().toPath().toString();
            File workspaceFile = new File(basePath + "\\xrayjenkins\\work\\workspace\\dummyproject");
            FilePath workspace = new FilePath(workspaceFile);
            String resultsPath = "\\joaquina\\**\\results\\**\\*.xml";
            XrayImportBuilder builder = new XrayImportBuilder(null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);

            List<FilePath> matchingFiles = builder.getFilePaths(workspace, resultsPath, taskListener);
            Assert.assertTrue(matchingFiles.size() == 7);

        } catch (IOException e){
            Assert.fail("An exception occured when performing the Test: " + e.getMessage());
        }
        workspace.delete();
    }

    @Test
    public void testGetFileWithSofisticatedGlobExpression(){
        try{
            when(taskListener.getLogger()).thenReturn(System.out);
            prepareFolders();
            String basePath = workspace.getRoot().toPath().toString();
            File workspaceFile = new File(basePath + "\\xrayjenkins\\work\\workspace\\dummyproject");
            FilePath workspace = new FilePath(workspaceFile);
            String resultsPath = basePath + "\\xrayjenkins\\work\\workspace\\dummyproject\\joaquina\\**\\results\\**\\feb*.xml";
            XrayImportBuilder builder = new XrayImportBuilder(null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);

            List<FilePath> matchingFiles = builder.getFilePaths(workspace, resultsPath, taskListener);
            Assert.assertTrue(matchingFiles.size() == 2);

        } catch (IOException e){
            Assert.fail("An exception occured when performing the Test: " + e.getMessage());
        }
        workspace.delete();
    }

}
