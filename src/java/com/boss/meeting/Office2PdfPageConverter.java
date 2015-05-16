package com.boss.meeting;

import org.apache.log4j.Logger;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeConnectionProtocol;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;

import java.io.File;

/**
 * Created by Administrator on 2014/12/13.
 */
public class Office2PdfPageConverter implements PageConverter {
    private static Logger logger = Logger.getLogger(Office2PdfPageConverter.class);
    private final OfficeManager officeManager;
    private final static GrailsApplication grailsApplication = new DefaultGrailsApplication();

    public Office2PdfPageConverter()
    {
        DefaultOfficeManagerConfiguration configuration = new DefaultOfficeManagerConfiguration();
        configuration.setConnectionProtocol(OfficeConnectionProtocol.SOCKET);
        configuration.setPortNumber(Integer.valueOf(String.valueOf(grailsApplication.getConfig().getProperty("officePort"))));

        logger.debug("the officeToolsDir is " + String.valueOf(grailsApplication.getConfig().getProperty("officeToolsDir")));
        configuration.setOfficeHome(new File(String.valueOf(grailsApplication.getConfig().getProperty("officeToolsDir"))));

        logger.debug("the officeTempDir is " + String.valueOf(grailsApplication.getConfig().getProperty("officeTempDir")));
        File userDir = new File(String.valueOf(grailsApplication.getConfig().getProperty("officeTempDir")) + File.separator + "user");
        if(!userDir.exists()) {
            userDir.mkdirs();
        }
        configuration.setTemplateProfileDir(new File(String.valueOf(grailsApplication.getConfig().getProperty("officeTempDir"))));


        configuration.setWorkDir(new File(String.valueOf(grailsApplication.getConfig().getProperty("officeTempDir"))));

        officeManager = configuration.buildOfficeManager();
        officeManager.start();
    }

    public static String toUrl(File file) {
        String path = file.toURI().getRawPath();
        String url = path.startsWith("//") ? "file:" + path : "file://" + path;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @Override
    public boolean convert(File document, File output, int page) {
        logger.debug("input file is " + document.getAbsolutePath() + ", output file is " + output.getAbsolutePath());
        OfficeDocumentConverter converter = new OfficeDocumentConverter(officeManager);

        logger.debug("output'path is " + toUrl(output));
        try
        {
            converter.convert(document, output);
        }
        catch(Exception exception)
        {
            logger.debug("exception convert(File presentationFile, File output, int page)  ...............");
        }
        finally {
            logger.debug("stop convert(File presentationFile, File output, int page)  ...............");
//            officeManager.stop();
        }

        if (output.exists())
        {
            logger.debug("succeed convert(File presentationFile, File output, int page)  ...............");
            return true;
        }
        else
        {
            logger.warn("Failed to convert: " + output.getAbsolutePath() + " does not exist.");
            return false;
        }
    }
}
