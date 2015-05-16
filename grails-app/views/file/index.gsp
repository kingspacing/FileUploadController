<%--
  Created by IntelliJ IDEA.
  User: Administrator
  Date: 2014/12/12
  Time: 13:10
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>上传文件</title>
    <r:require module="uploadify"/>
    <r:layoutResources/>
</head>

<body>
<input type="file" name="file_upload" id="file_upload" />
<r:script disposition="defer">
        $(function() {
            $('#file_upload').uploadify({
                'multi'    : true,
                'checkExisting' : "${createLink(controller: 'file', action: 'checkexisting')}",
                'swf'      : '${createLinkTo(dir: "images", file: "uploadify.swf")}',
                'uploader' : "${createLink(controller: 'file', action: 'upload')}",
                'onUploadSuccess' : function(file, data, response) {
                    alert('The file ' + file.name + ' was successfully uploaded with a response of ' + response + ':' + data);
                }
                // Your options here
            });
        });
</r:script>
<r:layoutResources/>
</body>
</html>