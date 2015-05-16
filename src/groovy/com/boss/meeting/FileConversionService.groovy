package com.boss.meeting

import com.boss.meeting.Office2PdfPageConverter;
import com.boss.meeting.PageConverter;
import com.boss.meeting.SupportedFileTypes;
import com.boss.meeting.UploadedDocument
import grails.converters.JSON
import grails.plugin.redis.RedisService;
import groovy.lang.Closure;
import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication
import redis.clients.jedis.Jedis;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2014/12/12.
 */
public class FileConversionService {
    private static Logger logger = Logger.getLogger(FileConversionService.class);
    private final static GrailsApplication grailsApplication = new DefaultGrailsApplication();
    private final RedisService redisService;

    public FileConversionService(RedisService redisService) {
        this.redisService = redisService;
    }

    public void processFile(UploadedDocument file)
    {
        logger.debug("processFile document name is " + file.getUploadedFile().getAbsolutePath());
        if (SupportedFileTypes.isOfficeFile(file.getFileType()))
        {
            convertOfficeToPdf(file); // 将word文档转换为pdf,并重命名原始文件
            convertPdf2Images(file); // 将pdf转换为image
            file.getUploadedFile().delete();// 删除中间pdf文件
        }
        else if (SupportedFileTypes.isPdfFile(file.getFileType()))
        {
            convertPdf2Images(file);
            file.renameToUploadDir(); // 将原始的pdf文件重命名为md5保存
        }
        else if (SupportedFileTypes.isImageFile(file.getFileType()))
        {
            file.renameToUploadDir(); // 保存原始文件
        }
        else
        {
            logger.error("unsupport file tye, file type is " + file.getFileType());
        }
    }

    public UploadedDocument convertPdf2Images(UploadedDocument document)
    {
        determineNumberOfPages(document);
        Process p = null;

        if(document.getNumberOfPages() > 0)
        {
            // 创建存储目录，开始转换为图片
            File destDir = new File(grailsApplication.getConfig().getProperty("fileUploadImageDir") + File.separatorChar + document.getMD5());

            String COMMAND = String.format(grailsApplication.getConfig().getProperty("pdfConvertCommand").toString(), destDir.getAbsolutePath() +  File.separatorChar, document.getUploadedFile().getAbsolutePath());
            try {
                logger.debug("convertPdf2Images cmd is " + COMMAND);
                p = Runtime.getRuntime().exec(COMMAND);
                StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "Error");
                StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "Output");

                errorGobbler.start();
                outputGobbler.start();

                p.waitFor();
            } catch (Exception e) {
                logger.info("run excuting error : " + COMMAND + ", " + e.getMessage());
                p.destroy();
                e.printStackTrace();
            }
            finally {
                logger.debug("run excuting finished, cmd is " + COMMAND + ". ");
//                Thread.interrupted();   // We need to clear the interrupt flag on the current thread just in case
                // interrupter executed after waitFor had already returned but before timer.cancel
                // took effect.
            }
        }
        return document;
    }

    private void determineNumberOfPages(UploadedDocument document)
    {
        File destDir = new File(grailsApplication.config.fileUploadImageDir + File.separatorChar + document.getMD5() + File.separatorChar + grailsApplication.config.fileUploadThumbnailDir);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        String COMMAND = String.format(grailsApplication.config.pdfThumbnailsCommand, destDir.getAbsolutePath() + File.separatorChar, document.getUploadedFile().getAbsolutePath());

        Process proc = null;
        StreamGobbler outputGobbler = null;
        StreamGobbler errorGobbler = null;

        try {
            logger.debug("Pdf2SwfPageCounter cmd is " + COMMAND);
            proc = Runtime.getRuntime().exec(COMMAND);
            errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR");
            outputGobbler = new StreamGobbler(proc.getInputStream(), "Output");

            errorGobbler.start();
            outputGobbler.start();

            int exitVal = proc.waitFor();
            logger.info("TIMEDOUT excuting : " + exitVal);
        } catch(Exception e) {
            logger.info("TIMEDOUT excuting : " + COMMAND + ", exception message is " + e.getMessage());
            proc.destroy();
        } finally {
            if(outputGobbler != null) {
                outputGobbler.join();
                def message = ["md5":document.getMD5(),
                               "size":document.getUploadedFile().size(),
                               "pages":outputGobbler.numPages];
                redisService.withRedis { Jedis redis ->
                    redis.publish("bm:file:update", (message as JSON).toString());
                }
                document.setNumberOfPages(outputGobbler.numPages);
                logger.info("excuting finished, cmd is " + COMMAND + ", pages is " + document.getNumberOfPages() +
                        ", outputGobbler.numPages is " + outputGobbler.numPages + ", message is " +  (message as JSON).toString());
            }
        }
    }

    public UploadedDocument convertOfficeToPdf(UploadedDocument file)
    {
        File output = setupOutputPdfFile(file);
        PageConverter converter = new Office2PdfPageConverter();
        if(converter.convert(file.getUploadedFile(), output, 0))
        {
            file.renameToUploadDir(); // 将原始文件重命名为upload根目录下的md5文件，作为原始文件记录
            file.setUploadedFile(output); // 将转换后的PDF设置为新的需要转换的文件
            file.setFileType("pdf");
        }
        else
        {
            logger.error("failed to convert the file to pdf.");
        }
        return file;
    }

    private File setupOutputPdfFile(UploadedDocument file)
    {
        File orgFile = file.getUploadedFile();
        String pdfFileName = orgFile.getAbsolutePath() + ".pdf";
        return new File(pdfFileName);
    }

    class StreamGobbler extends Thread {
//        private final Pattern PAGE_NUMBER_PATTERN = Pattern.compile("total/s+[0-9]+ms/s+///s([0-9]+)/s+pages");
//        private final Pattern PAGE_NUMBER_PATTERN = Pattern.compile("^total.*/.*\\s([0-9]+)\\s.*pages.*");

        private InputStream is;
        private String type;
        public int numPages = 0; //total numbers of this pdf

        StreamGobbler(InputStream is, String type) {
            this.is = is;
            this.type = type;
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = "";

                while ((line = br.readLine()) != null) {
                    if (type.equals("Error")) {
                        logger.error(line);
                    }
                    else {
                        if (line.indexOf("total") != -1 && line.indexOf("pages for an average of") != -1) {
                            String pages = line.substring(line.indexOf("/") + 2);
                            pages = pages.substring(0, pages.indexOf("ages for an average of") -2);
                            numPages = Integer.parseInt(pages);
                            logger.debug("output pages is " + pages + ", numPages is " + numPages);
                        }
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
