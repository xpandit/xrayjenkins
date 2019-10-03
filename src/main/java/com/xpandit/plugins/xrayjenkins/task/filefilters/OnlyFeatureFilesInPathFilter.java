package com.xpandit.plugins.xrayjenkins.task.filefilters;

import com.xpandit.plugins.xrayjenkins.exceptions.XrayJenkinsGenericException;
import hudson.FilePath;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

/**
 * This class will it's a FileFilter implementation that will only "accept" files that are both:
 * 1) In the Set of "valid" files.
 * 2) Has been modified in the last <i>"lastModified"</i> minutes. (if null, we considered all files has been modified)
 *
 */
public class OnlyFeatureFilesInPathFilter implements FileFilter {

    private final Set<String> validFilePaths;
    private final String lastModified;
    
    public OnlyFeatureFilesInPathFilter(Set<String> validFilePaths, String lastModified) {
        this.validFilePaths = validFilePaths;
        this.lastModified = lastModified;
    }

    @Override
    public boolean accept(File pathname) {
        try {
            return validFilePaths.contains(pathname.getAbsolutePath()) &&
                    isApplicableAsModifiedFile(pathname);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isApplicableAsModifiedFile(File file) throws InterruptedException, IOException {
        return file != null && isApplicableAsModifiedFile(new FilePath(file));
    }

    private boolean isApplicableAsModifiedFile(FilePath filePath) throws InterruptedException, IOException{
        if(StringUtils.isBlank(lastModified)){
            //the modified field is not used so we return true
            return true;
        }
        int lastModifiedIntValue = getLastModifiedIntValue();
        long diffInMillis = new Date().getTime() - filePath.lastModified();
        long diffInHour = diffInMillis / DateUtils.MILLIS_PER_HOUR;
        
        return diffInHour <= lastModifiedIntValue;
    }

    private int getLastModifiedIntValue(){
        try{
            int m = Integer.parseInt(this.lastModified);
            if(m <= 0){
                throw new XrayJenkinsGenericException("last modified value must be a positive integer");
            }
            return m;
        } catch (NumberFormatException e){
            throw new XrayJenkinsGenericException("last modified value is not an integer");
        }
    }
}