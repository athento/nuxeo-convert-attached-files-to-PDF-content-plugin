# nuxeo-convert-attached-files-to-PDF-content-plugin
This plugin contributes an operation, a converter and a operation chain that can be invoked vía REST API to set the file contents as the result of concatenate attached files of Document in PDF format.

## Installation

Just download & compile the pom.xml using Maven and deploy the plugin in
 
```{r, engine='bash', count_lines}
cd nuxeo-convert-attached-files-to-PDF-content-plugin
mvn clean install
cp target/nuxeo-convert-attached-files-to-PDF-content-plugin-*.jar $NUXEO_HOME/nxserver/plugins
```

## Use
Just invoke the following REST service using your favourite REST client

POST URL: http://localhost:8080/nuxeo/site/automation/ATHENTO_ConcatenateAttachedFilesToContent


{
    "input":"95f6b969-fa5b-46e9-9525-1893b52eda75",
    "context":{}
} 


The "input" value is your Document UUID. 
