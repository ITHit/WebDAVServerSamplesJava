
<h1 class="d-xl-block d-none">Spring Boot WebDAV Server Example with Amazon S3 Back-end, Java</h1>
<p>This sample&nbsp;is a fully functional Class 2 WebDAV server that runs on the Spring Boot framework and stores all data in the Amazon S3 bucket.&nbsp;The WebDAV requests are processed on a /DAV/ context, while the rest of the website processes regular HTTP requests, serving web pages. Documents are being published from the Amazon S3 bucket with locks and custom attributed being stored in S3 Metadata.&nbsp;</p>
<p>This sample can be downloaded in the <a title="Download" href="https://www.webdavsystem.com/javaserver/download/">product download area</a> as well as it is published on&nbsp;<a href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/springboots3storage">GitHub</a>.</p>
<p><span>This sample is using&nbsp;</span><a title="AJAX Library" href="https://www.webdavsystem.com/ajax/">IT Hit WebDAV Ajax Library</a><span>&nbsp;to display and browse server content on a default web page as well as to open documents for editing from a web page and save back directly to the server.</span><span></span></p>
<p>&nbsp;</p>
<h2>Requirements</h2>
<ul>
<li>Java 1.8.</li>
</ul>
<h2>Running the sample</h2>
<ol>
<li>
<p><strong>Set the license</strong>.&nbsp;Download your license file&nbsp;<a href="https://www.webdavsystem.com/javaserver/download/">here</a>. To set the license, edit the <span class="code">webdav.license</span>&nbsp;section in <span class="code"><em>\springboot\src\main\resources\application.properties</em></span>&nbsp;and specify the path to the&nbsp;<span class="code"><em>license.lic</em></span>&nbsp;file.</p>
<pre class="brush:html;auto-links:false;toolbar:false">webdav.license=C:\License.lic</pre>
The IT Hit Java WebDAV Server Library is fully functional and does not have any limitations. However, the trial period is limited to 1 month. After the trial period expires the Java WebDAV Server will stop working.<span></span></li>
<li>
<p><strong>Configure the Amazon S3 storage</strong>. You can either use an existing Amazon S3 bucket or create a new one. To create a bucket you can use the Amazon S3 <a title="web console" href="https://s3.console.aws.amazon.com/s3/home">web console</a>.&nbsp;</p>
<p><img id="__mcenew" alt="" src="https://www.webdavsystem.com/media/2127/createb.jpg" rel="122335"></p>
<p>After creating the S3 bucket you can create some folders and upload files for testing purposes.</p>
</li>
<li>
<p><strong>Configure the Amazon S3 project settings.</strong>&nbsp;In&nbsp;<span class="code">application.properties</span>&nbsp;set the following properties:</p>
<pre class="brush:html;auto-links:false;toolbar:false">
## Amazon S3 region
webdav.s3.region=

## Amazon S3 access key
webdav.s3.access-key=

## Amazon S3 secret access key
webdav.s3.secret-access-key=

## Amazon S3 bucket name
webdav.s3.bucket=
</pre>
</li>
<li>
<p><strong>Configure the application server</strong>.&nbsp;Here we will configure the WebDAV server to run on the website non-root context (<span class="code">https://server/DAV/</span>). This setting is located in the <span class="code">webdav.rootContext</span>&nbsp;section in the&nbsp;<em><span class="code">\springboot\src\main\resources\application.properties</span>.</em></p>
<pre class="brush:html;auto-links:false;toolbar:false">webdav.rootContext=/DAV/</pre>
<span><span class="warn"><span>Note:</span>&nbsp;Some WebDAV clients (such as some old versions or Mini-redirector, Microsoft Web Folders, and MS Office 2007 and earlier) will fail to connect to a non-root server. They submit configuration requests to server root and if they do not get the response they will not be able to connect.&nbsp;<span>For this reason, this sample processes OPTIONS and PROPFIND requests on all folders, including on the site root (https://server/).</span> See also&nbsp;<a title="Working with MS Office" href="https://www.webdavsystem.com/javaserver/doc/ms_office_read_only/">Making Microsoft Office to Work with WebDAV Server</a>&nbsp;and&nbsp;<a title="Opening Docs" href="https://www.webdavsystem.com/ajax/programming/open-doc-webpage/opening_ms_office_docs/">Opening Microsoft Office Documents and Other Types of Files for Editing From a Web Page</a>.<br>This Spring Boot sample supports those configuration requests and works properly on a non-root context.<br></span></span></li>
<li>
<p><strong>Running the springboot sample.&nbsp;</strong>To start the sample, change the directory to&nbsp;<em><span class="code">springboot</span>&nbsp;</em>and execute the following command:</p>
<pre class="brush:html;auto-links:false;toolbar:false">mvnw spring-boot:run</pre>
<p>If everything was set up properly you should see a sample web page on&nbsp;&nbsp;<span class="code">https://server/DAV/</span>&nbsp;URL with a list of sample files and folders previously created in S3. Now you can open documents for editing, manage documents, as well as&nbsp;<a href="https://www.webdavsystem.com/server/access/">connect to the server with any WebDAV client</a>.</p>
<p>If anything goes wrong examine the log file. For Spring Boot, the log file is usually located at <span class="code">springboot/log/engine.log</span>. You may also need to capture and examine the HTTP requests. See <a title="Troubleshooting" href="https://www.webdavsystem.com/javaserver/server_examples/troubleshooting/">this article</a> for more details.&nbsp;</p>
</li>
</ol>
<h2>The Project&nbsp;Classes</h2>
<p>On the diagram below you can see the classes in the WebDAV SpringBoot S3 sample:</p>
<p><img id="__mcenew" alt="Class diagram of the sample Java WebDAV Server running on Spring Boot" src="https://www.webdavsystem.com/media/1879/springbootdiagram.png" rel="115963"></p>
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
<a title="WebDAV Server Example with Oracle Back-end, Java" href="https://www.webdavsystem.com/javaserver/server_examples/sql_storage/">WebDAV Server Example with Oracle Back-end, Java</a>

