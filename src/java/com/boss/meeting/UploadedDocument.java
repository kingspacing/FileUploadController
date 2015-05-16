package com.boss.meeting;

import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;

import java.io.File;

/**
 * Created by Administrator on 2014/12/12.
 */
public class UploadedDocument {
    private final static GrailsApplication grailsApplication = new DefaultGrailsApplication();
    private static Logger logger = Logger.getLogger(UploadedDocument.class);

    private final String name;
    private final String md5;
    private String fileType;
    private File uploadedFile;
    private final String room;
    private int numberOfPages = 0;
    private boolean lastStepSuccessful = false;

    public UploadedDocument(String name, String md5, String fileType, File uploadedFile, String room) {
        this.name = name;
        this.md5 = md5;
        this.fileType = fileType;
        this.uploadedFile = uploadedFile;
        this.room = room;
    }

    public String getMD5() { return md5;}

    public int getNumberOfPages() {

        return numberOfPages;
    }

    public void setNumberOfPages(int pages) {
        logger.debug("UploadedDocument setNumberOfPages is " + pages);
        this.numberOfPages = pages;
    }

    public String getFileType() {
        return fileType;
    }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public File getUploadedFile() { return uploadedFile; }
    public void setUploadedFile(File uploadedFile) { this.uploadedFile = uploadedFile;}

    public boolean renameToUploadDir() {
        File lastFile = new File(grailsApplication.getConfig().getProperty("fileUploadRootDir").toString() + File.separatorChar +md5);
        return uploadedFile.renameTo(lastFile);
    }
}
