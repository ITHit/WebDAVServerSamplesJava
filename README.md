
<h1>WebDAV Server Examples, Java</h1>
<div class="description"><p style="line-height: 22px; font-size: 15px; font-weight: normal;">IT Hit WebDAV Server Library for Java is provided with several examples that demonstrate how to build a WebDAV server with SQL back-end or with file system storage. You can adapt these samples to utilize almost any back-end storage including storing data in CMS/DMS/CRM, Azure or Amazon storage.</p>
<p style="line-height: 22px; font-size: 15px; font-weight: normal;">A sample HTML page included with samples demonstrates how to use <a title="IT Hit WebDAV Ajax Libray" href="https://www.webdavsystem.com/ajax/" target="_blank">IT Hit WebDAV Ajax Libray</a>&nbsp;to open documents from a web page for editing, list documents and navigate folder structure as well as build search capabilities.</p>
<h2>Online Demo Server</h2>
<p style="line-height: 22px; font-size: 15px; font-weight: normal;"><a title="https://www.WebDAVServer.com" href="https://www.WebDAVServer.com" target="_blank">https://www.WebDAVServer.com</a></p>
<h2>&nbsp;Requirements</h2>
<p style="line-height: 22px; font-size: 15px; font-weight: normal;">The samples are tested with <strong><span>Java 1.8</span></strong> in the following environments:</p>
<ul>
<li style="margin-bottom: 16px;">Tomcat 7 or later</li>
<li style="margin-bottom: 16px;">Glassfish 4.1.1 or later</li>
<li style="margin-bottom: 16px;">JBoss Wildfly 9 or later or respective EAP</li>
<li style="margin-bottom: 16px;">WebLogic 12c or later</li>
<li style="margin-bottom: 16px;">WebSphere 8.5.5.11 or later</li>
<li style="margin-bottom: 16px;">Jetty 9.3.13 or later</li>
</ul>
<h2>Full-text Search and indexing</h2>
<p style="line-height: 22px; font-size: 15px; font-weight: normal;">The samples are provided with full-text search and indexing based on use Apache Lucene as indexing engine and Apache Tika as content analysis toolkit.</p>
<p style="line-height: 22px; font-size: 15px; font-weight: normal;">The server implementation searches both file names and file content including content of Microsoft Office documents as well as any other documents which format is supported by Apache Tika, such as LibreOffice, OpenOffice, PDF, etc.</p></div>
<ul class="list">
<li>
<a class="link-header" href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/springbootfsstorage">
<h2>Spring Boot WebDAV Server Example with File System Back-end, Java</h2>
</a>

<a href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/springbootfsstorage">
<p>
This sample provides a WebDAV server running on the Spring Boot framework with files being stored in the file system. The WebDAV requests are processed in a dedicated context, while the rest of the website processes regular HTTP requests, serving web                                            <span>...</span>
</p>
</a>
</li>
<li>
<a class="link-header" href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/springbootoraclestorage">
<h2>Spring Boot WebDAV Server Example with Oracle Back-end, Java</h2>
</a>

<a href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/springbootoraclestorage">
<p>
This sample provides a WebDAV server running on the Spring Boot framework.&nbsp;All data including file content, document structure, and custom attributes are stored in the Oracle database.&nbsp;The&nbsp;IT Hit WebDAV Ajax Library&nbsp;is used to display and browse serv                                            <span>...</span>
</p>
</a>
</li>
<li>
<a class="link-header" href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/springboots3storage">
<h2>Spring Boot WebDAV Server Example with Amazon S3 Back-end, Java</h2>
</a>

<a href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/springboots3storage">
<p>
This sample&nbsp;is a fully functional Class 2 WebDAV server that runs on the Spring Boot framework and stores all data in the Amazon S3 bucket.&nbsp;The WebDAV requests are processed on a /DAV/ context, while the rest of the website processes regular HTTP req                                            <span>...</span>
</p>
</a>
</li>
<li>
<a class="link-header" href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/oraclestorage">
<h2>WebDAV Server Example with Oracle Back-end, Java</h2>
</a>

<a href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/oraclestorage">
<p>
The sample provides Class 2 WebDAV server implementation that can be hosted in Apache Tomcat, GlassFish, JBoss,&nbsp;WebLogic,&nbsp;WebSphere or other compliant application server. All data including file content, documents structure and custom attributes is s                                            <span>...</span>
</p>
</a>
</li>
<li>
<a class="link-header" href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/filesystemstorage">
<h2>WebDAV Server Example with File System Back-end, Java and Kotlin</h2>
</a>

<a href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/filesystemstorage">
<p>
This sample&nbsp;is a fully functional Class 2 WebDAV server that stores all data in the file system. It utilizes file system Extended Attributes (in case of Linux and macOS) or Alternate Data&nbsp;Streams (in case of Windows/NTFS) to store locks and custom pr                                            <span>...</span>
</p>
</a>
</li>
<li>
<a class="link-header" href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/androidfsstorage">
<h2>Java WebDAV Server Example for Android</h2>
</a>

<a href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/androidfsstorage">
<p>
This sample is a Class 2 WebDAV server that runs on Android. It uses modified&nbsp;NanoHTTPD as an application server and publishes files from a mobile application folder or from media folder. Locks and properties in SQLite database.
To see the documents                                            <span>...</span>
</p>
</a>
</li>
<li>
<a class="link-header" href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/deltav">
<h2>WebDAV Server Example with Versioning, Java</h2>
</a>

<a href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/deltav">
<p>
The sample provides&nbsp;DeltaV WebDAV server implementation that can be hosted in Apache Tomcat, GlassFish, JBoss,&nbsp;WebLogic or&nbsp;WebSphere. The data is stored in Oracle database.&nbsp;The IT Hit WebDAV Ajax Library is used to display and browse server content o                                            <span>...</span>
</p>
</a>
</li>
<li>
<a class="link-header" href="https://www.webdavsystem.com/javaserver/server_examples/running_webdav_samples/">
<h2>Running the WebDAV Samples</h2>
</a>

<a href="https://www.webdavsystem.com/javaserver/server_examples/running_webdav_samples/">
<p>
Once your&nbsp;sample is configured&nbsp;and running you will see the following web page (note that&nbsp;the port that the sample is using may be different from the one on the screenshots):

This web page is a MyCustomHandlerPage.html&nbsp;included in&nbsp;each sample&nbsp;and                                             <span>...</span>
</p>
</a>
</li>
<li>
<a class="link-header" href="https://www.webdavsystem.com/javaserver/server_examples/search/">
<h2>Configuring Full-Text Search for Files Stored in File System or in Oracle Database</h2>
</a>

<a href="https://www.webdavsystem.com/javaserver/server_examples/search/">
<p>
The&nbsp;samples provided with SDK&nbsp;use Apache Lucene&nbsp;as indexing engine and Apache Tika&nbsp;as content analysis toolkit.
The server implementation searches both file names and file content including content of Microsoft Office documents as well as any other                                             <span>...</span>
</p>
</a>
</li>
<li>
<a class="link-header" href="https://www.webdavsystem.com/javaserver/server_examples/troubleshooting/">
<h2>WebDAV Server Samples Problems and Troubleshooting</h2>
</a>

<a href="https://www.webdavsystem.com/javaserver/server_examples/troubleshooting/">
<p>
Examining Logs
If things are not going as planned and you run into issues the first place to look would be the log file&nbsp;&amp;lt;Your Tomcat location&amp;gt;\Tomcat x.x\logs\localhost.xxxx-xx-xx.log&nbsp;. The logs will reflect as to what is going on and it will                                             <span>...</span>
</p>
</a>
</li>
</ul>

