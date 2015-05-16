import grails.plugin.elfinder.command.ElFinderUploadCommand
import grails.plugin.elfinder.filemanager.ElfinderLocalFileSystemFileManager
import com.boss.meeting.FileService

// Place your Spring DSL code here
beans = {
    //configure fileManager
    elfinderFileManager(ElfinderLocalFileSystemFileManager) {
        root = grailsApplication.config.fileUploadRootDir
        fileService = ref("fileService")
    }

    //configure commands

    def configurationClosure = { bean ->
        bean.scope = "prototype"
        elFinderFileManager = ref("elfinderFileManager")
    }

    elfinderUploadCommand(ElFinderUploadCommand, configurationClosure)
}
