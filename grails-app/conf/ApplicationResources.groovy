modules = {
    application {
        resource url:'js/application.js'
    }

    "uploadify" {
        dependsOn 'jquery'
        resource id:'css', url: [dir:'css', file: 'uploadify.css']
        resource id:'js', url:[dir:'js', file:'jquery.uploadify.min.js'], disposition: 'defer'
    }
}