
<h1 class="d-xl-block d-none">Spring Boot WebDAV Server Example with Oracle Back-end, Java</h1>
<p>This sample provides a WebDAV server running on the Spring Boot framework.&nbsp;All data including file content, document structure, and custom attributes are stored in the Oracle database.&nbsp;The&nbsp;<a title="AJAX Library" href="https://www.webdavsystem.com/ajax/">IT Hit WebDAV Ajax Library</a>&nbsp;is used to display and browse server content on a default web page as well as to open documents for editing from a web page and save them back directly to the server.</p>
<p>This sample can be downloaded in the&nbsp;<a title="Download" href="https://www.webdavsystem.com/javaserver/download/">product download area</a>&nbsp;as well as it is published on&nbsp;<a href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/springbootoraclestorage">GitHub</a>.</p>
<h2>Requirements</h2>
<ul>
<li>Java 1.8.</li>
<li>Oracle Database 10g or later version.&nbsp;Express, Standard, or Enterprise Edition.</li>
</ul>
<h2>Running the sample</h2>
<ol>
<li><strong>Set the license.</strong>&nbsp;Download your license file&nbsp;<a href="https://www.webdavsystem.com/javaserver/download/">here</a>. To set the license, edit the <span class="code">webdav.license</span>&nbsp;section in&nbsp;<span class="code"><em>\springboot\src\main\resources\application.properties</em></span>&nbsp;and specify the path to the&nbsp;<span class="code"><em>license.lic</em></span>&nbsp;file.&nbsp;<br>
<pre class="brush:html;auto-links:false;toolbar:false">webdav.license=C:\License.lic</pre>
The IT Hit Java WebDAV Server Library is fully functional and does not have any limitations. However, the trial period is limited to 1 month. After the trial period expires the Java WebDAV Server will stop working.<span><br></span></li>
<li><strong>Configure the application server.</strong>&nbsp;Here we will configure the WebDAV server to run on the website non-root context (<span class="code">https://server/DAV/</span>). This setting is located in the <span class="code">webdav.rootContext</span>&nbsp;section in the&nbsp;<em><span class="code">\springboot\src\main\resources\application.properties</span>.<br></em>
<pre class="brush:html;auto-links:false;toolbar:false">webdav.rootContext=/</pre>
<span class="warn"><span>Note:</span>&nbsp;Some WebDAV clients (such as some old versions or Mini-redirector, Microsoft Web Folders, and MS Office 2007 and earlier) will fail to connect to a non-root server. They submit configuration requests to server root and if they do not get the response they will not be able to connect.&nbsp;<span>For this reason, this sample processes OPTIONS and PROPFIND requests on all folders, including on the site root (https://server/).</span>&nbsp;See also&nbsp;<a title="Working with MS Office" href="https://www.webdavsystem.com/javaserver/doc/ms_office_read_only/">Making Microsoft Office to Work with WebDAV Server</a>&nbsp;and&nbsp;<a title="Opening Docs" href="https://www.webdavsystem.com/ajax/programming/open-doc-webpage/opening_ms_office_docs/">Opening Microsoft Office Documents and Other Types of Files for Editing From a Web Page</a>.<br>This Spring Boot sample supports those configuration requests and works properly on a non-root context.</span></li>
<li><strong>Set Oracle BD connection string.</strong> Provide you connection string and credentials in the&nbsp;<em>\springboot\src\main\resources\application.properties.</em><br>
<pre class="brush:java;auto-links:false;toolbar:false">spring.datasource.url=jdbc:oracle:thin:@localhost:1521:xe
spring.datasource.username=system
spring.datasource.password=pwd</pre>
<span></span><span>Database structure for your WebDAV server will be created during application startup from the file&nbsp;<em>springboot\src\main\resources\db\OracleStorage.sql</em></span></li>
<li><span><strong>Running the springboot sample.</strong>&nbsp;</span>To start the sample, change the directory to&nbsp;<em><span class="code">springboot</span>&nbsp;</em>and execute the following command:
<pre class="brush:html;auto-links:false;toolbar:false">mvnw spring-boot:run</pre>
<p>If everything was set up properly you should see a sample web page on&nbsp;&nbsp;<span class="code">https://server/DAV/</span>&nbsp;URL. Now you can upload documents, open documents for editing, manage documents, as well as&nbsp;<a href="https://www.webdavsystem.com/server/access/">connect to the server with any WebDAV client</a>.</p>
<p>If anything goes wrong examine the log file. For Spring Boot, the log file is usually located at&nbsp;<span class="code">springboot/log/engine.log</span>. You may also need to capture and examine the HTTP requests. See&nbsp;<a title="Troubleshooting" href="https://www.webdavsystem.com/javaserver/server_examples/troubleshooting/">this article</a>&nbsp;for more details.&nbsp;</p>
</li>
</ol>
<h2>The Project&nbsp;Classes</h2>
<p>On the diagram below you can see the classes in the WebDAV SpringBoot SQL sample:</p>
<p><img id="__mcenew" alt="Class diagram of the sample Java WebDAV Server running on Spring Boot for Oracle" src="https://www.webdavsystem.com/media/1899/springbootoraclediagram.png" rel="116337"></p>
<p>To adapt the sample to your needs, you will modify these classes to read and write data from and into your storage. You can find more about this in&nbsp;<a title="Creating WebDAV Server" href="https://www.webdavsystem.com/javaserver/doc/">Creating a Class 1 WebDAV Server</a>&nbsp;and&nbsp;<a title="Class 2 / 3 Server" href="https://www.webdavsystem.com/javaserver/doc/create_class_2_webdav_server/">Creating Class 2 WebDAV Server</a>&nbsp;article as well as in the&nbsp;<a href="http://java.webdavsystem.com/">class reference documentation</a>.</p>
<p>&nbsp;</p>
<h3>See Also:</h3>
<ul>
<li><a title="Running" href="https://www.webdavsystem.com/javaserver/server_examples/running_webdav_samples/">Running the WebDAV Samples</a></li>
<li><a title="Troubleshooting" href="https://www.webdavsystem.com/javaserver/server_examples/troubleshooting/">WebDAV Server Samples Problems and Troubleshooting</a></li>
<li><a title="Creating WebDAV Server" href="https://www.webdavsystem.com/javaserver/doc/">Creating a Class 1 WebDAV Server</a>&nbsp;</li>
<li><a title="Class 2 / 3 Server" href="https://www.webdavsystem.com/javaserver/doc/create_class_2_webdav_server/">Creating a Class 2 WebDAV Server</a></li>
</ul>
<p>&nbsp;</p>
<h3 class="para d-inline next-article-heading">Next Article:</h3>
<a title="WebDAV server running on the Spring Boot framework on Amazon S3 bucket" href="https://www.webdavsystem.com/javaserver/server_examples/spring_boot_s3/">Spring Boot WebDAV Server Example with Amazon S3 Back-end, Java</a>

