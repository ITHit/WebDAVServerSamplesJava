
<h1 class="d-xl-block d-none">Spring Boot WebDAV Server Example with File System Back-end, Java</h1>
<p>This sample provides a WebDAV server running on the Spring Boot framework with files being stored in the file system. The WebDAV requests are processed in a dedicated context, while the rest of the website processes regular HTTP requests, serving web pages. Documents are being published from the file system while the locks and custom attributes are being stored in Alternating Data Streams (in case of NTFS) or in Extended Attributes (in case of Linux and macOS).&nbsp;</p>
<p>This sample can be downloaded in the&nbsp;<a title="Download" href="https://www.webdavsystem.com/javaserver/download/">product download area</a>&nbsp;as well as it is published on&nbsp;<a href="https://github.com/ITHit/WebDAVServerSamplesJava/tree/master/Java/springbootfsstorage">GitHub</a>.</p>
<p><span>This sample is using&nbsp;</span><a title="AJAX Library" href="https://www.webdavsystem.com/ajax/">IT Hit WebDAV Ajax Library</a><span>&nbsp;to display and browse server content on a default web page as well as to open documents for editing from a web page and save back directly to the server.</span></p>
<h2>Requirements</h2>
<ul>
<li>Java 1.8.</li>
<li>NTFS, Ext4, Ext3 or any other file system which supports extended file attributes. You can find a complete list of file systems that support extended attributes&nbsp;<a href="https://en.wikipedia.org/wiki/Extended_file_attributes">here</a>. To enable extended file attributes on Linux change fstab to look like:&nbsp;
<pre class="brush:html;auto-links:false;toolbar:false">/dev/sda1  /               ext4    errors=remount-ro,user_xattr   0       1</pre>
</li>
<li>Lombok plug-in should be installed in your favorite IDE otherwise syntax error will be displayed</li>
</ul>
<h2>Running the sample</h2>
<ol>
<li><span><strong>Set the license</strong>.</span>&nbsp;Download your license file&nbsp;<a href="https://www.webdavsystem.com/javaserver/download/">here</a>. To set the license, edit the <span class="code">webdav.license</span>&nbsp;section in <span class="code"><em>\springboot\src\main\resources\application.properties</em></span>&nbsp;and specify the path to the&nbsp;<span class="code"><em>license.lic</em></span>&nbsp;file. <br>
<pre class="brush:html;auto-links:false;toolbar:false">webdav.license=C:\License.lic</pre>
The IT Hit Java WebDAV Server Library is fully functional and does not have any limitations. However, the trial period is limited to 1 month. After the trial period expires the Java WebDAV Server will stop working.<span></span></li>
<li><span><strong>Configure the storage folder</strong>.</span>&nbsp;By default, this sample publishes documents from the&nbsp;<span class="code">springboot\src\main\resources\Storage\</span>&nbsp;folder. For the sake of configuration simplicity, documents are extracted from project resources during the first run. You can publish documents from any other folder specifying a path in the <span class="code">webdav.rootFolder</span>&nbsp;section in&nbsp;<span class="code">application.properties</span>:<br>
<pre class="brush:html;auto-links:false;toolbar:false">webdav.rootFolder=C:\Storage</pre>
</li>
<li><span><span><span><strong>Configure the application server</strong>.</span>&nbsp;Here we will configure the WebDAV server to run on the website non-root context (<span class="code">https://server/DAV/</span>). This setting is located in the <span class="code">webdav.rootContext</span> section in the&nbsp;<em><span class="code">\springboot\src\main\resources\application.properties</span>.<br></em></span></span>
<pre class="brush:html;auto-links:false;toolbar:false">webdav.rootContext=/DAV/</pre>
<span><span class="warn">Note:<span>&nbsp;Some WebDAV clients (such as some old versions or Mini-redirector, Microsoft Web Folders, and MS Office 2007 and earlier) will fail to connect to a non-root server. They submit configuration requests to server root and if they do not get the response they will not be able to connect.&nbsp;</span>For this reason, this sample processes OPTIONS and PROPFIND requests on all folders, including on the site root (https://server/).<span>&nbsp;See also&nbsp;</span><a title="Working with MS Office" href="https://www.webdavsystem.com/javaserver/doc/ms_office_read_only/">Making Microsoft Office to Work with WebDAV Server</a><span>&nbsp;and&nbsp;</span><a title="Opening Docs" href="https://www.webdavsystem.com/ajax/programming/open-doc-webpage/opening_ms_office_docs/">Opening Microsoft Office Documents and Other Types of Files for Editing From a Web Page</a><span>.</span><br><span>This Spring Boot sample supports those configuration requests and works properly on a non-root context.</span></span></span></li>
<li><strong>Running the springboot sample.&nbsp;</strong>To start the sample, change the directory to&nbsp;<em><span class="code">springboot</span>&nbsp;</em>and execute the following command:
<pre class="brush:html;auto-links:false;toolbar:false">mvnw spring-boot:run</pre>
<p>If everything was set up properly you should see a sample web page on&nbsp;&nbsp;<span class="code">https://server/DAV/</span>&nbsp;URL with a list of sample files and folders from your storge folder in the file system, configured in step 2. Now you can open documents for editing, manage documents, as well as&nbsp;<a href="https://www.webdavsystem.com/server/access/">connect to the server with any WebDAV client</a>.</p>
<p>If anything goes wrong examine the log file. For Spring Boot, the log file is usually located at&nbsp;<span class="code">springboot/log/engine.log</span>. You may also need to capture and examine the HTTP requests. See&nbsp;<a title="Troubleshooting" href="https://www.webdavsystem.com/javaserver/server_examples/troubleshooting/">this article</a>&nbsp;for more details.&nbsp;</p>
</li>
</ol>
<h2>Authentication</h2>
<p>This sample supports Anonymous access as well as&nbsp;<strong>MS-OFBA</strong>&nbsp;authentication against&nbsp;<strong>Azure AD</strong>.&nbsp;<span>By default, the authentication is disabled and this sample allows full anonymous access.</span></p>
<p>To enable the MS-OFBA, uncomment&nbsp;<strong>MS-OFBA&nbsp;</strong>settings block in the <span class="code">application.properties</span><strong>&nbsp;</strong>file of the sample.</p>
<pre class="brush:html;auto-links:false;toolbar:false">## This is Azure security configuration section.
## If you want to use Azure login - uncomment configurations bellow.
# Specifies your Active Directory ID:
#azure.activedirectory.tenant-id=
# Specifies your App Registration's Application ID:
#azure.activedirectory.client-id=
# Specifies your App Registration's secret key:
#azure.activedirectory.client-secret=</pre>
<p>To get these properties you must register new (or use existing) application under your Azure Active Directory. To register the new Azure AD application follow these steps:</p>
<ol>
<li>
<p>Navigate to&nbsp;<em>Azure Active Directory</em>&nbsp;-&gt;&nbsp;<em>App Registrations</em>. Select&nbsp;<em>New Registration</em>.</p>
<p><img id="__mcenew" alt="New application registration in Azure AD" src="https://www.webdavsystem.com/media/2049/9azureadappregistrationnew.png" rel="120357"></p>
</li>
<li>
<p>Enter the app name.&nbsp;You MUST also enter the&nbsp;<em>Redirect URI</em>. Confirm registration.</p>
<p><img id="__mcenew" alt="Specify the Redirect URI." src="https://www.webdavsystem.com/media/2124/azuread.png" rel="122226"></p>
</li>
<li>
<p>Open the newly created app registration.</p>
<p><img id="__mcenew" alt="Copy the Application (client) ID and Directory (tenant) ID into settings." src="https://www.webdavsystem.com/media/2059/11azureadclientidtenantid1.png" rel="120368"></p>
<p>Copy the&nbsp;<em>Application (client) ID</em>&nbsp;and&nbsp;<em>Directory (tenant) ID</em>&nbsp;fields and paste them into&nbsp;<span class="code">client-id</span>&nbsp;and&nbsp;<span class="code">tenant-id</span>&nbsp;settings&nbsp;in&nbsp;the&nbsp;<span class="code">application.properties</span><span>&nbsp;</span>file.</p>
</li>
<li>
<p>Navigate to&nbsp;<em>Certificates &amp; secrets</em>. Select&nbsp;<em>New client secret</em>. Enter the secret name and confirm the client secret creation.</p>
<p><img id="__mcenew" alt="Create new client secret" src="https://www.webdavsystem.com/media/2050/12azureadnewclientsecret.png" rel="120359"></p>
<p>Copy the newly created secret value and past it into&nbsp;<span class="code">client-secret</span>&nbsp;setting in&nbsp;the&nbsp;<span class="code">application.properties</span><span>&nbsp;</span>file.</p>
<p><img id="__mcenew" alt="Copy the newly created secret value and past it into client-secret setting." src="https://www.webdavsystem.com/media/2052/14azureadcopyappsecret.png" rel="120360"></p>
</li>
</ol>
<p>&nbsp;</p>
<p>After getting all data put it in properties file and run the sample. You will get&nbsp;<strong>Azure&nbsp;</strong>login screen on attemp to access WebDAV page.&nbsp;</p>
<h2>The Project&nbsp;Classes</h2>
<p>On the diagram below you can see the classes in the WebDAV SpringBoot sample:</p>
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
<a title="Spring Boot framework WebDAV Server Example with Oracle back-end. Can both process WebDAV requests and serve web pages on the rest of the website." href="https://www.webdavsystem.com/javaserver/server_examples/spring_boot_sql/">Spring Boot WebDAV Server Example with Oracle Back-end, Java</a>

