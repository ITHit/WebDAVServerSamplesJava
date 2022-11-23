
<h1 class="d-xl-block d-none">WebDAV Server Example with File System Back-end, Java and Kotlin</h1>
<p><span>This sample&nbsp;is a fully functional Class 2 WebDAV server that stores all data in the file system. It utilizes file system Extended Attributes (in case of Linux and macOS) or Alternate Data&nbsp;Streams (in case of Windows/NTFS) to store locks and custom properties. The&nbsp;<a title="AJAX Library" href="https://www.webdavsystem.com/ajax/">IT Hit WebDAV Ajax Library</a>&nbsp;is used to display and browse server content on a default web page as well as to open documents for editing from a web page and save back directly to the server. It is provided in Java and Kotlin, with identical functionality.</span></p>
<h2>Requirements</h2>
<ul>
<li>Java 1.8.</li>
<li>Apache Tomcat 7.0+ or GlassFish v4.1.1+ or&nbsp;WebLogic 12c+ or JBoss WildFly 9+ or WebSphere&nbsp;16.0.0.2+.</li>
<li>NTFS, Ext4, Ext3 or any other file system which supports extended file attributes. You can find a complete list of file systems that support extended attributes&nbsp;<a href="https://en.wikipedia.org/wiki/Extended_file_attributes">here</a>. To enable extended file attributes on Linux change fstab to look like:&nbsp;
<pre class="brush:html;auto-links:false;toolbar:false">/dev/sda1  /               ext4    errors=remount-ro,user_xattr   0       1</pre>
</li>
</ul>
<p>You will also need the&nbsp;<a title="AJAX Library" href="https://www.webdavsystem.com/ajax/">IT Hit WebDAV Ajax Library</a>&nbsp;to display JavaScript UI on a default web page. You can install it from NPM using NPM command line tool, see below. Node.js is <strong>ONLY</strong> required to download the JavaScript files used in the project. Node.js is <strong>NOT</strong> required to run the server.</p>
<h2>Running the sample</h2>
<ol>
<li><strong>Set license.</strong>&nbsp;Download your license file&nbsp;<a href="https://www.webdavsystem.com/javaserver/download/">here</a>. To set the license, edit the 'license' section in \filesystemstorage<em>\WEB-INF\web.xml</em>&nbsp;and specify the path to the&nbsp;<em>license.lic</em>&nbsp;file. <br>
<pre class="brush:xml;auto-links:false;toolbar:false">&lt;init-param&gt;
&lt;param-name&gt;license&lt;/param-name&gt;
&lt;param-value&gt;C:\License.lic&lt;/param-value&gt;
&lt;/init-param&gt;</pre>
The IT Hit Java WebDAV Server Library is fully functional and does not have any limitations. However, the trial period is limited to 1 month. After the trial period expires the Java WebDAV Server will stop working.</li>
<li><strong>Download the IT Hit WebDAV Ajax Library.</strong>&nbsp;You can do this with NPM command-line tool, which is included with Node.js.&nbsp;Install the&nbsp;<a href="https://nodejs.org/en/download/">Node.js</a>&nbsp;and navigate to&nbsp;<span class="code">\filesystemstorage\WEB-INF\wwwroot\js\</span>&nbsp;folder. Run:&nbsp;
<pre class="brush:html;auto-links:false;toolbar:false">npm install&nbsp;webdav.client</pre>
This will download IT Hit WebDAV Ajax Library files into your project. Note that Node.js itself is <strong>NOT</strong> required to run the server, it is used <strong>ONLY</strong> to install the required JavaScript files.</li>
<li><strong>Configure the storage folder.</strong> By default, this sample publishes documents from the <span class="code">WEB-INF/Storage</span>&nbsp;folder. For the sake of configuration simplicity, documents are extracted from project resources during the first run. You can publish documents from any other folder specifying a path in the 'root' section in&nbsp;<span class="code">web.xml</span>:<br>
<pre class="brush:xml;auto-links:false;toolbar:false">&lt;init-param&gt;
&lt;param-name&gt;root&lt;/param-name&gt;
&lt;param-value&gt;C:\Storage\&lt;/param-value&gt;
&lt;/init-param&gt;</pre>
</li>
<li><strong>Configure the application server.</strong>&nbsp;Here we will configure WebDAV server to run on the website root (<span class="code">http://server.com/</span>). <span class="warn"><strong>Note:</strong> While you can configure WebDAV server to run on site non-root (for instance on&nbsp;<span class="code">http://server.com/webdavroot/</span>) some WebDAV clients (such as some old versions or Mini-redirector, Microsoft Web Folders and MS Office 2007 and earlier) will fail to connect to non-root server. They submit configuration requests to server root and if they do not get the response they will not be able to connect.&nbsp;See also&nbsp;<a title="Working with MS Office" href="https://www.webdavsystem.com/javaserver/doc/ms_office_read_only/">Making Microsoft Office to Work with WebDAV Server</a> and&nbsp;<a title="Opening Docs" href="https://www.webdavsystem.com/ajax/programming/open-doc-webpage/opening_ms_office_docs/">Opening Microsoft Office Documents and Other Types of Files for Editing From a Web Page</a>.</span>
<ul>
<li><strong>In the case of Tomcat:</strong><br>Copy&nbsp;<em>\filesystemstorage</em> folder to&nbsp;<em>&lt;Your Tomcat location&gt;\Tomcat x.x\webapps</em>&nbsp;folder. Add the following lines under the &lt;Host&gt; tag in&nbsp;<em>&lt;Your Tomcat location&gt;</em><em>\Tomcat x.x\conf\server.xml</em>:<br>
<pre class="brush:csharp;auto-links:false;toolbar:false">&lt;Context path="" debug="0" docBase="filesystemstorage"&gt;
&lt;/Context&gt;</pre>
<p><span>To see if your server is running type the root URL of your WebDAV site in a browser and you will see the list of folders. Now&nbsp;</span><a href="https://www.webdavsystem.com/server/access/">connect to the server with any WebDAV client</a><span>.</span></p>
<p>If you experience any problems examine the log created by tomcat:&nbsp;<span class="code">&lt;Your Tomcat location&gt;\Tomcat x.x\logs\localhost.xxxx-xx-xx.log</span>.</p>
</li>
<li><strong>In the case of Glassfish:</strong><ol type="a">
<li>Deploy the filesystem storage application.
<p>From the main tree (<em>Common Tasks</em>)&nbsp;goto&nbsp;<em>Applications</em>.</p>
<p>Press&nbsp;<em>Deploy</em>&nbsp;and specify following properties:</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Local Packaged File or Directory That Is Accessible from the Enterprise Server = &lt;path to filesystem&nbsp;storage directory&gt;</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Type = Web Application</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Context Root = /</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Note:&nbsp;sample can be deployed to a non-root context, but some clients work only with servers deployed to root the context.</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Application Name =&nbsp;filesystemstorage</p>
</li>
<li>Launch the sample.
<p>From the main tree (<em>Common Tasks</em>)&nbsp;go to&nbsp;<em>Applications</em>.</p>
<p>Press&nbsp;<em>Launch</em>&nbsp;on <em>filesystemstorage</em>&nbsp;application.</p>
<p>If everything was set up properly you should see a sample page with a list of sample files and folders. Now&nbsp;<a href="https://www.webdavsystem.com/server/access/">connect to the server with any WebDAV client</a>.</p>
<p>If anything goes wrong please consult log usually located at&nbsp;<em>&lt;GLASSFISH_INSTANCE_ROOT&gt;/logs/server.log.</em></p>
</li>
</ol></li>
<li><strong>In the case of JBoss WildFly:</strong><ol type="a">
<li>By default WildFly restricts access to certain packages from Java SDK. So you need to allow&nbsp;<strong>com.sun.nio.file</strong>&nbsp;package in the&nbsp;<strong>sun/jdk&nbsp;</strong>module by adding the following line in the&nbsp;module.xml&nbsp;file:
<pre class="brush:xml;auto-links:false;toolbar:false">&lt;path name="com/sun/nio/file"/&gt;</pre>
<p>Restart WildFly.</p>
</li>
<li>Deploy the filesystem storage application.
<p>Create folder <em>filsystemstorage.war </em>under&nbsp;<em><em>&lt;WILDFLY_ROOT&gt;/deployments.</em></em></p>
<p>Copy content of <em>samples/filesystemstorage </em>to&nbsp;<em><em><em>&lt;<em><em>WILDFLY_ROOT</em></em>&gt;/deployments/<em>filsystemstorage.war.</em></em></em></em></p>
<p>Create file&nbsp;<em>filsystemstorage.war.dodeploy in&nbsp;<em><em>&lt;<em><em>WILDFLY_ROOT</em></em>&gt;/deployments/<em>filsystemstorage.war.</em></em></em></em></p>
<p><span>If everything was set up properly you should see a sample page on the WildFly root context with a list of sample files and folders. Now&nbsp;</span><a href="https://www.webdavsystem.com/server/access/">connect to the server with any WebDAV client</a><span>.</span></p>
<p>If anything goes wrong please consult log usually located at&nbsp;<em>&lt;<em><em>WILDFLY_ROOT</em></em>&gt;/log/server.log.</em></p>
</li>
</ol></li>
</ul>
</li>
</ol>
<h2>The Project&nbsp;Classes</h2>
<p>On the diagram below you can see the classes in the WebDAV File System&nbsp;project:</p>
<p><img id="__mcenew" alt="File system diagram" src="https://www.webdavsystem.com/media/1563/filesystemdiagram.png" rel="109213"></p>
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
<a title="Java WebDAV Server for Android" href="https://www.webdavsystem.com/javaserver/server_examples/android/">Java WebDAV Server Example for Android</a>