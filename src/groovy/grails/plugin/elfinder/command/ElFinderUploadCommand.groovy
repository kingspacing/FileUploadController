package grails.plugin.elfinder.command

import org.apache.log4j.Logger
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.commons.CommonsMultipartFile

/**
 * @author Sudhir Nimavat
 */
class ElFinderUploadCommand extends ElfinderBaseCommand {
	private static Logger logger = Logger.getLogger(ElFinderUploadCommand.class.name)
	@Override
	void execute() {
		String target = params['target']

		List<MultipartFile> files = request.getFiles('upload[]')
		List added = []

		files.each {CommonsMultipartFile tempFile ->
			logger.debug "uploading: $tempFile.originalFilename"
			def info = elFinderFileManager.uploadFile(tempFile, unhash(target))
			if(info) {
				added.add(info)
			}
		}
		putResponse("added", added)
	}
}
