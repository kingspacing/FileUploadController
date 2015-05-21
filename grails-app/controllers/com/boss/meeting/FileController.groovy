package com.boss.meeting

import grails.converters.JSON
import grails.plugin.elfinder.command.ElFinderUploadCommand
import grails.plugin.elfinder.command.ElfinderBaseCommand
import org.apache.commons.lang.StringEscapeUtils
import org.apache.commons.lang.StringUtils
import org.apache.log4j.Logger
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.commons.CommonsMultipartFile
import redis.clients.jedis.Jedis

import java.security.MessageDigest

class FileController {
    private static Logger logger = Logger.getLogger(FileController.class.name)

    def fileService;
    def redisService
    def redisMessagingService
    def elfinderFileManager

    def index() {
        logger.debug("the redis is start test .............")
    }

    def generateMD5(final file) {
        MessageDigest digest = MessageDigest.getInstance("MD5")
        file.withInputStream(){is->
            byte[] buffer = new byte[8192]
            int read = 0
            while( (read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] md5sum = digest.digest()
        BigInteger bigInt = new BigInteger(1, md5sum)
        bigInt.toString(16).padLeft(32, '0')
    }

    def uploadByClient() {
        logger.debug("uploadByClient ${params}")
        def file = params.Filedata;
        if(file && !file.empty) {
            // 保存文件
//            File tmpDir = new File(grailsApplication.config.getProperty("fileUploadTmpDir").toString());
            File uploadDir = new File(grailsApplication.config.getProperty("fileUploadRootDir").toString());

//            if (!tmpDir.exists()) {
//                tmpDir.mkdirs();
//            }

            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            def fileName = file.getOriginalFilename()
            logger.debug("the file name is ${fileName}")
            def savingFile = new File( uploadDir.absolutePath + File.separatorChar + fileName )
            file.transferTo(savingFile)
            logger.debug("the file path is ${savingFile.absolutePath}")

            def fileType = ""
            int extPos = fileName.lastIndexOf(".")

            if(extPos != -1)
            {
                fileType = fileName.toLowerCase().substring(extPos+1)// 不存在则解析文件类型
            }

            // 获取文件的md5值
            String md5 = generateMD5(savingFile)

            // 检查文件是否已经存在
            def md5File = new File(uploadDir.absolutePath + File.separatorChar +md5);

            def message = ["name":fileName,
                           "encode":fileName.encodeAsBase64(),
                           "ts":savingFile.lastModified(),
                           "hash":elfinderFileManager.hash(elfinderFileManager.getPathRelativeToRoot(md5File)),
                           "md5":md5,
                           "write":1,
                           "mime": elfinderFileManager.getMimeTypeForFile(savingFile),
                           "phash":elfinderFileManager.hash(elfinderFileManager.getPathRelativeToRoot(md5File.parent)),
                           "read":1,
                           "lock":0,
                           "dirs": 1,
                           "size":savingFile.size()];

            // 通知所有事件侦听者，文件上传成功，文件md5以及上传的文件名称
            if(md5File.exists()) {
                savingFile.delete();
            }
            else {
                // 如果是支持的文档类型，则进行文件转换
                if(fileType != "" && SupportedFileTypes.isFileSupported(fileType))
                {
                    logger.debug("upload file path is ${savingFile.parent}, md5 = ${md5} and filetype = ${fileType}")
                    def renamedFile = new File(savingFile.parent + File.separatorChar + md5 + "." + fileType);

                    if(renamedFile.exists()) {
                        renamedFile.delete();
                    }

                    if(savingFile.renameTo(renamedFile))
                    {
                        UploadedDocument doc = new UploadedDocument(fileName, md5, fileType, renamedFile, "");
//                        fileService.processUploadedFile(doc);
                        int pages= fileService.syncProcessUploadFile(doc);

                        message.pages = pages;
                        logger.debug("rename upload file ok, begin to convert, pages is ${pages}, message is ${message}.");
                    };
                    // 转换完成时自动重命名，所以与无需命名，
                }
                else {
                    // 将文件重命名为md5
                    savingFile.renameTo(md5File);
                }
            }

            redisService.withRedis { Jedis redis ->
                redis.publish("bm:file:add", (message as JSON).toString());
            }

            render message as JSON
        }
        else {
            logger.debug("cann't find file, filedata is null ? ${params.Filedata == null}.............")
            render "0"
        }
    }

    def upload(String cmd) {
        logger.debug("upload by command ${params}")
        cmd = cmd ?: "Unknown"
        String commandName = "elfinder${StringUtils.capitalize(cmd)}Command"

        logger.debug("Executing command $commandName, $cmd")

        ElfinderBaseCommand command = grailsApplication.mainContext[commandName]

        if(command == null) {
            logger.error("Unknown command received : $commandName")
            render(status:400, text:"unknown command")
            return
        }

        command.params = params
        command.request = request
        command.response = response

        try {
            command.execute()
            if(!command.responseOutputDone) {
                def resp = command.responseMap as JSON
                render resp
            }
        }catch(Exception e) {
            logger.error("Error encountered while executing command $commandName", e)
            def resp = [error:e.message]
            render resp as JSON
        }
    }

//    def uploadSub() {
//        logger.debug("the upload start test, params is ${params} .............")
//        def file = params.Filedata;
//        if(file && !file.empty) {
//            // 保存文件
//            File tmpDir = new File(grailsApplication.config.getProperty("fileUploadTmpDir").toString());
//            File uploadDir = new File(grailsApplication.config.getProperty("fileUploadRootDir").toString());
//
//            if (!tmpDir.exists()) {
//                tmpDir.mkdirs();
//            }
//
//            if (!uploadDir.exists()) {
//                uploadDir.mkdirs();
//            }
//
//            def fileName = file.getOriginalFilename()
//            logger.debug("the file name is ${fileName}")
//            def savingFile = new File( tmpDir.absolutePath + File.separatorChar + fileName )
//            file.transferTo(savingFile)
//            logger.debug("the file path is ${savingFile.absolutePath}")
//
//            def fileType = ""
//            int extPos = fileName.lastIndexOf(".")
//
//            if(extPos != -1)
//            {
//                fileType = fileName.toLowerCase().substring(extPos+1)// 不存在则解析文件类型
//            }
//
//            // 获取文件的md5值
//            String md5 = generateMD5(savingFile)
//
//            // 检查文件是否已经存在
//            def md5File = new File(uploadDir.absolutePath + File.separatorChar +md5);
//
//            // 通知所有事件侦听者，文件上传成功，文件md5以及上传的文件名称
//            Random random = new Random();
//            def message = ["md5":md5,
//                           "checkvalue":(int)(100000+Math.random()*9900000),
//                           "type":fileType, "size":savingFile.size()];
//
//            if(md5File.exists()) {
//                savingFile.delete();
//                message["exists"] = true; // 文件已存在
//            }
//            else {
//                // 如果是支持的文档类型，则进行文件转换
//                if(fileType != "" && SupportedFileTypes.isFileSupported(fileType))
//                {
//                        logger.debug("upload file path is ${savingFile.parent}, md5 = ${md5} and filetype = ${fileType}")
//                        def renamedFile = new File(savingFile.parent + File.separatorChar + md5 + "." + fileType);
//
//                        if(renamedFile.exists()) {
//                            renamedFile.delete();
//                        }
//
//                        if(savingFile.renameTo(renamedFile))
//                        {
//                            logger.debug("rename upload file ok, begin to convert.")
//
//                            UploadedDocument doc = new UploadedDocument(fileName, md5, fileType, renamedFile, "");
//                            fileService.processUploadedFile(doc);
//                        };
//                        // 转换完成时自动重命名，所以与无需命名，
//                }
//                else {
//                    // 将文件重命名为md5
//                    savingFile.renameTo(md5File);
//                }
//            }
//            redisService.withRedis { Jedis redis ->
//                redis.publish("bm:file:add", (message as JSON).toString());
//            }
//            render message as JSON
//        }
//        else {
//            logger.debug("cann't find file .............")
//            render "0"
//        }
//    }

    def checkexisting() {
        // 检查是否存在
        logger.debug("the checkexisting start test, params is ${params} .............")

        render "0"
    }

    def uploaded() {

    }

    def downloadByClient() {
        logger.debug("${params}")

        redisService.withRedis { Jedis redis ->
            def index = params.uuid;
            if (redis.exists(index)) {
                String files = redis.get(index)
                logger.debug("file list index is " + index + ", filelist is " + files)

                if(files.indexOf(params.md5) >= 0) {
                    FileInputStream hFile

                    if(params.thumdid != null) {
                        // 下载缩略图
                        hFile = new FileInputStream(grailsApplication.config.fileUploadImageDir +  File.separatorChar + params.md5 + File.separatorChar + grailsApplication.config.fileUploadThumbnailDir + File.separatorChar + params.thumdid + grailsApplication.config.fileUploadImageExt); // 以byte流的方式打开文件 d:\1.gif
                    }
                    else if(params.pageid != null) {
                        // 下载图片页
                        hFile = new FileInputStream(grailsApplication.config.fileUploadImageDir +  File.separatorChar + params.md5 + File.separatorChar + params.pageid + grailsApplication.config.fileUploadImageExt); // 以byte流的方式打开文件 d:\1.gif
                    }
                    else {
                        // 下载文件
                        hFile = new FileInputStream(grailsApplication.config.fileUploadRootDir +  File.separatorChar + params.md5); // 以byte流的方式打开文件 d:\1.gif
                   }
                    if(hFile) {
                        int i=hFile.available(); //得到文件大小

                        byte[] data=new byte[i];

                        hFile.read(data);  //读数据
                        response.setContentType(params.type); //设置返回的文件类型
                        OutputStream toClient=response.getOutputStream(); //得到向客户端输出二进制数据的对象
                        toClient.write(data);  //输出数据

                        toClient.flush();
                        toClient.close();
                        hFile.close();
                    }
                }
            }
            else {
                logger.debug("redis uuid ${index} don't exist!");
            }
        }
    }

    def download() {
        logger.debug("${params}")

        redisService.withRedis { Jedis redis ->
            def index = params.md5 + "&" + params.name + "&" + params.id;
            if (redis.exists(index)) {
                def mime = redis.get(index)
                redis.del(index)

                def file = new File(grailsApplication.config.fileUploadRootDir + File.separatorChar + params.md5)
                logger.debug("download file path is ${file.absolutePath}")
                if (file.exists()) {
                    def bytes = file.readBytes()
                    response.addHeader("Cache-Control", "no-cache")
                    String filename=URLEncoder.encode(new String(params.name.decodeBase64()),"utf-8");
                    response.addHeader("Content-Disposition", "attachment;filename="+filename);
                    response.contentType = mime != null ? mime: 'application/octet-stream'
                    response.outputStream << bytes;
                    return
                }
            }
        }
    }
}
