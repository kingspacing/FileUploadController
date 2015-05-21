package com.boss.meeting

import grails.transaction.Transactional

@Transactional
class FileService {
    def redisService
    def processUploadedFile = {uploadedFile ->
        // Run conversion on another thread.
        new Timer().runAfter(100) {
            FileConversionService fileConversionService = new FileConversionService(redisService);
            fileConversionService.processFile(uploadedFile, false)
        }
    }

    def syncProcessUploadFile(uploadedFile) {
        FileConversionService fileConversionService = new FileConversionService(redisService);
        fileConversionService.processFile(uploadedFile, true)
    }
}
