package grails.plugin.elfinder.filemanager

import com.boss.meeting.FileService
import com.boss.meeting.SupportedFileTypes
import com.boss.meeting.UploadedDocument
import grails.plugin.elfinder.MimeTypeMappings
import org.apache.commons.io.FilenameUtils
import org.apache.log4j.Logger
import org.springframework.web.multipart.commons.CommonsMultipartFile

import java.security.MessageDigest

/**
 * @author Sudhir Nimavat
 */
class ElfinderLocalFileSystemFileManager implements ElFinderFileManager {
	private static Logger logger = Logger.getLogger(ElfinderLocalFileSystemFileManager.class.name)
	static final String VOLUME_ID = "v1_"

	FileService fileService
	String root

	Map cwd(String path) {
		return cwd(toFile(path))
	}

	Map cwd(File file) {
		Map info = [:]
		info.name = file.name
		info.encode = file.name.encodeAsBase64()
		info.hash = hash(getPathRelativeToRoot(file))
		if(!isRoot(file)) {
			info.phash = hash(getPathRelativeToRoot(file.parent))
		} else {
			info.volumeid = VOLUME_ID
		}
		info.mime = getMimeTypeForFile(file)
		info.ts = file.lastModified()
		info.size = file.size()
		info.dirs = 1
		info.read = 1
		info.write = 1
		info.locked = 0

		return info
	}

	List<Map> scanDir(String path) {
		File dir = toFile(path)
		return scanDir(dir)
	}

	List<Map> scanDir(File dir) {
		List files = []
		if(dir.isDirectory()) {
			dir.eachFile {child ->
				files << cwd(child)
			}
		}
		return files
	}

	List<Map> getTree(String path, int deep) {
		return getTree(toFile(path), deep)
	}

	List<Map> getTree(File f, int deep) {
		List dirs = []
		f.eachFile {File child ->
			if(child.isDirectory()) {
				def info = cwd(child)
				dirs << info
				if(deep > 0) {
					dirs.addAll(getTree(child, deep -1))
				}
			}
		}
		return dirs
	}

	List parents(String path) {
		File f = toFile(path)
		return parents(f)
	}

	List parents(File current) {
		List tree = []
		File dir = current
		while(!isRoot(dir)) {
			dir = dir.parentFile
			def info = cwd(dir)
			tree.add(0, info)
			if(!isRoot(dir)) {
				getTree(dir, 0).each {
					if(!tree.contains(it)) {
						tree << it
					}
				}
			}
		}

		return tree ?: [cwd(current)]
	}

	List mkdir(String name, String target) {
		String parent = toFile(target)
		File newDir = new File(parent, name)
		def success = newDir.mkdir()

		def resp = []

		if(success) {
			resp << cwd(newDir)
		}
	}

	List mkfile(String name, String target) {
		String parent = toFile(target)
		File newFile = new File(parent, name)
		def success = newFile.createNewFile()
		def resp = []
		if(success) {
			resp << cwd(newFile)
		}
	}

	String rename(String name, String target) {
		File old = toFile(target)
		File newFile = new File(old.parentFile, name)
		old.renameTo(newFile)
		return hash(getPathRelativeToRoot(newFile))
	}

	List delete(String path) {
		logger.info("Deleting : $path")
		List deleted = []
		File file = toFile(path)

		if(!file.isDirectory()) {
			String hash = hash(getPathRelativeToRoot(file))
			if(file.delete()) {
				deleted << hash
			}
		} else {
			deleted.addAll(deleteDir(file))
		}

		return deleted
	}

	List deleteDir(File dir) {
		List deleted = []
		dir.eachFile {File child ->
			if(child.isDirectory()) {
				deleted.addAll(deleteDir(child))
			} else {
				if(child.delete()) {
					deleted << hash(getPathRelativeToRoot(child))
				}
			}
		}

		if(dir.delete()) {
			deleted << hash(getPathRelativeToRoot(dir))
		}

		return deleted
	}

	Map options(String path) {
		Map options = [:]
		options.seperator = "/"
		options.path = (getRootDir().name) + (isRoot(toFile(path)) ? "" : "/"+getPathRelativeToRoot(toFile(path)))
		//options.url = "http://localhost/files/1"
		options.disabled = [
			'tmb',
			'size',
			'dim',
			'duplicate',
			'paste',
			'get',
			'put',
			'archive',
			'extract',
			'search',
			'resize',
			'netmount']
		return options
	}

	InputStream getFileInputStream(String path) {
		File file = toFile(path)
		return new FileInputStream(file)
	}

	def generateMD5(final File file) {
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
		return bigInt.toString(16).padLeft(32, '0')
	}

	Map uploadFile(CommonsMultipartFile multiPartFile, String targetDir) {
		logger.debug("uploadFile targetDir $targetDir")
		String name = multiPartFile.getOriginalFilename()
		String directory = toFile(targetDir)

		File f = new File(directory, name)
		boolean success = f.createNewFile()
		if(success) {
			multiPartFile.transferTo(f)
			Map result = cwd(f)

			String md5 = generateMD5(f)

			File md5File = new File(directory + File.separatorChar +md5);
			if(md5File.exists()) {
				f.delete();
			}
			else {
				def fileType = ""
				int extPos = name.lastIndexOf(".")

				if(extPos != -1)
				{
					fileType = name.toLowerCase().substring(extPos+1)// 不存在则解析文件类型
				}
				if(fileType != "" && SupportedFileTypes.isFileSupported(fileType))
				{
					def renamedFile = new File(f.parent + File.separatorChar + md5 + "." + fileType);
					if(renamedFile.exists()) {
						renamedFile.delete();
					}
					if(f.renameTo(renamedFile))
					{
						logger.debug("rename upload file ok, begin to convert, fileService is null? ${fileService == null}")

						UploadedDocument doc = new UploadedDocument(name, md5, fileType, renamedFile, "");
						fileService.processUploadedFile(doc);
					};
				}
				else {
					f.renameTo(md5File);
				}
			}
			result.md5 = md5;
			return result
		}
		return null
	}

	String hash(String str) {
		String hashed = str.encodeAsBase64()
		hashed = hashed.replace("=", "")
		hashed = hashed.replace("+", "-")
		hashed = hashed.replace("/", "_")

		hashed = VOLUME_ID+hashed

		return hashed
	}

	String unhash(String hashed) {

		hashed = hashed.substring(3)

		hashed = hashed.replace(".", "=")
		hashed = hashed.replace("-", "+")
		hashed = hashed.replace("_", "/")
		return new String(hashed.decodeBase64())
	}

	private File toFile(String path) {
		if(path.trim() == root || path == "root") {
			return rootDir
		} else {
			return new File(root, path)
		}
	}

	boolean isRoot(File f) {
		f == rootDir
	}

	File getRootDir() {
		return new File(root)
	}

	String getPathRelativeToRoot(File file) {
		String path = rootDir.toURI().relativize(file.toURI()).getPath()
		if(path.endsWith("/")) {
			path = path.substring(0, path.length() - 1)
		}

		if(path.trim() == "") {
			return "root"
		}
		return path
	}

	String getPathRelativeToRoot(String path) {
		File file = new File(path)
		return getPathRelativeToRoot(file)
	}

	String getMimeTypeForFile(File file) {
		if(file.isDirectory()) {
			return "directory"
		}
		return MimeTypeMappings.forExtension(FilenameUtils.getExtension(file.name))
	}
}
