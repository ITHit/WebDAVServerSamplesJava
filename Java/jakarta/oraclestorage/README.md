
<h1 class="d-xl-block d-none">WebDAV Server Example with Oracle Back-end, Java</h1>
<p><span>The sample provides Class 2 WebDAV server implementation that can be hosted in Apache Tomcat, GlassFish, JBoss,&nbsp;WebLogic,&nbsp;WebSphere or other compliant application server. All data including file content, documents structure and custom attributes is stored in Oracle database.&nbsp;The&nbsp;<a title="AJAX Library" href="https://www.webdavsystem.com/ajax/">IT Hit WebDAV Ajax Library</a>&nbsp;is used to display and browse server content on a default web page as well as to open documents for editing from a web page and save back directly to server.</span></p>
<h2>Requirements</h2>
<ul>
<li>Oracle Database 10g or later version.&nbsp;Express, Standard or Enterprise Edition.</li>
<li>Apache Tomcat 7.0+ or GlassFish v4.1.1+ or&nbsp;WebLogic 12c+ or JBoss Wildfly 9+ or WebSphere&nbsp;16.0.0.2+.</li>
<li>Java 1.8.</li>
</ul>
<p>You will also need the <a title="AJAX Library" href="https://www.webdavsystem.com/ajax/">IT Hit WebDAV Ajax Library</a> to display JavaScript UI on a default web page. You can install it from NPM using NPM command line tool, see below. Node.js is <strong>ONLY</strong> required to download the JavaScript files used in the project. Node.js is <strong>NOT</strong> required to run the server.</p>
<h2>Running the sample</h2>
<ol>
<li><strong>Create the database.</strong>&nbsp;The Oracle database script is located in&nbsp;<em>\samples\oraclestorage\db\OracleStorage.sql</em>&nbsp;file. This script creates tables and populates them with data so your WebDAV server initially has several folders and files. To run the script login to Oracle administration web interface, go to&nbsp;<em>SQL-&gt;SQL Scripts-&gt;Create</em>, then paste content of OracleStorage.sql to script field, specify a script name and click Run.<br>
<p>It is also recommended to increase the datafile. After creating the database run the following command:</p>
<div class="code">
<pre class="brush:csharp;auto-links:false;toolbar:false">alter database datafile ' C:\oraclexe\oradata\XE\SYSTEM.DBF' resize 4g;</pre>
</div>
<p>&nbsp;This is especially required if you would like to test WebDAV server running Oracle XE with&nbsp;<a href="https://www.webdavsystem.com/ajaxfilebrowser/">IT Hit AJAX File Browser</a>.</p>
</li>
<li><strong>Set license.</strong>&nbsp;Download your license file&nbsp;<a href="https://www.webdavsystem.com/javaserver/download/">here</a>. To set the license edit license section in&nbsp;<em>\oraclestorage\WEB-INF\web.xml</em>&nbsp;and specify the path to the&nbsp;<em>license.lic</em>&nbsp;file.
<pre class="brush:xml;auto-links:false;toolbar:false">&lt;init-param&gt;
&lt;param-name&gt;license&lt;/param-name&gt;
&lt;param-value&gt;C:\License.lic&lt;/param-value&gt;
&lt;/init-param&gt;</pre>
The IT Hit Java WebDAV Server Library is fully functional and does not have any limitations. However, the trial period is limited to 1 month. After the trial period expires the Java WebDAV Server will stop working.</li>
<li><strong>Download the IT Hit WebDAV Ajax Library.</strong>&nbsp;You can do this with NPM command line tool, which is included with Node.js.&nbsp;Install the&nbsp;<a href="https://nodejs.org/en/download/">Node.js</a>&nbsp;and navigate to&nbsp;<code class="code">\oraclestorage\WEB-INF\wwwroot\js\</code>&nbsp;folder. Run:&nbsp;
<pre class="brush:html;auto-links:false;toolbar:false">npm install&nbsp;webdav.client</pre>
This will download IT Hit WebDAV Ajax Library files into your project. Note that Node.js itself is&nbsp;<strong>NOT</strong>&nbsp;required to run the server, it is used&nbsp;<strong>ONLY</strong>&nbsp;to install the required JavaScript files.</li>
<li><strong>Configure the application server.</strong>&nbsp;Here we will configure WebDAV server to run on the website root (<code class="code">http://server.com/</code>). <span class="warn"><strong>Note:</strong> While you can configure WebDAV server to run on site non-root (for instance on&nbsp;<code class="code">http://server.com/webdavroot/</code>) some WebDAV clients (such as some old versions or Mini-redirector, Microsoft Web Folders and MS Office 2007 and earlier) will fail to connect to non-root server. They submit configuration requests to server root and if they does not get the response they will not be able to connect.&nbsp;See also&nbsp;<a title="Working with MS Office" href="https://www.webdavsystem.com/javaserver/doc/ms_office_read_only/">Making Microsoft Office to Work with WebDAV Server</a> and&nbsp;<a title="Opening Docs" href="https://www.webdavsystem.com/ajax/programming/open-doc-webpage/opening_ms_office_docs/">Opening Microsoft Office Documents and Other Types of Files for Editing From a Web Page</a>.</span><br><ol>
<li><span>In the case of Tomcat:</span><br>Copy&nbsp;<em>\oraclestorage</em>&nbsp;folder to&nbsp;<em>&lt;Your Tomcat location&gt;\Tomcat x.x\webapps</em>&nbsp;folder. Add the following lines under the &lt;Host&gt; tag in&nbsp;<em>&lt;Your Tomcat location&gt;</em><em>\Tomcat x.x\conf\server.xml</em>:<br>
<pre class="brush:csharp;auto-links:false;toolbar:false">&lt;Context path="" debug="0" docBase="oraclestorage"&gt;
&nbsp;&lt;Resource name="jdbc/Oracle" auth="Container"
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; type="javax.sql.DataSource" username="system" password="pwd"
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; driverClassName="oracle.jdbc.OracleDriver" url="jdbc:oracle:thin:@localhost:1521:XE"
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; maxActive="8" maxIdle="4" factory="org.apache.commons.dbcp.BasicDataSourceFactory"/&gt;
&lt;/Context&gt;</pre>
<p>Specify Oracle database login credentials in Context tag. Check you service instance Id in server url.&nbsp;<br>Finally, restart the Tomcat for configuration changes to take effect.</p>
<p>To see if your server is running type the root URL of your WebDAV site in a browser and you will see the list of folders. Now&nbsp;<a href="https://www.webdavsystem.com/server/access/">connect to the server with any WebDAV client</a>.</p>
<p>If you experience any problems examine the log created by tomcat:&nbsp;<code class="code">&lt;Your Tomcat location&gt;\Tomcat x.x\logs\localhost.xxxx-xx-xx.log</code>.</p>
</li>
<li><span>In the case of Glassfish:</span><ol>
<li><span>Create oracle connection pool.</span>
<p>Copy&nbsp;<em>\oraclestorage\WEB-INF\lib\oracle-driver-ojdbc6-x.x.x.x.jar</em>&nbsp;to&nbsp;&nbsp;<em>&lt;GLASSFISH_HOME&gt;/domains/domain1/lib/ext</em>&nbsp;folder. Note that&nbsp;"domain1" is a default Glassfish domain. The&nbsp; domain may be different for specific deployments.</p>
<p>Restart GlassFish.</p>
<p>Open administrative console of the Glassfish server.</p>
<p>From the main tree (<em>Common Tasks</em>) expand&nbsp;<em>Resources</em>&nbsp;and go to&nbsp;<em>JDBC &gt; Connection Pools</em>. Create a Connection pool:</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- JNDI name = Oracle</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- resource_type = javax.sql.ConnectionPoolDataSource</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Database Vendor = Oracle&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</p>
<p>Click Next.</p>
<p>Specify following additional properties (replace following values with your specific):</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- url = jdbc:oracle:thin:@localhost:1521:XE</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- user = system</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- password = password</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- xa-driver-does-not-support-non-tx-operations = true</p>
<p>Test connection with&nbsp;<em>Ping</em>&nbsp;button.</p>
</li>
<li><span>Create DataSource.</span>
<p>From the main tree (<em>Common Tasks</em>) expand&nbsp;<em>Resources</em>&nbsp;and go to&nbsp;<em>JDBC &gt; JDBC Resources</em>.</p>
<p>Press&nbsp;<em>New</em>&nbsp;and provide the following information:</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- JNDI Name: JDBC/Oracle (must be called exactly like this).</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Pool Name: The pool name created in the previous section.</p>
<p>Press&nbsp;<em>OK</em>, JDBC-resource will be created.</p>
</li>
<li><span>Deploy oracle storage application.</span>
<p>From the main tree (<em>Common Tasks</em>)&nbsp;goto&nbsp;<em>Applications</em>.</p>
<p>Press&nbsp;<em>Deploy</em>&nbsp;and specify following properties:</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Local Packaged File or Directory That Is Accessible from the Enterprise Server = &lt;path to oracle storage directory&gt;</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Type = Web Application</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Context Root = /</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span>Note:&nbsp;</span>sample can be deployed to a non-root context, but some clients work only with servers deployed to root the context.</p>
<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Application Name = oraclestorage</p>
</li>
<li><span>Launch sample.</span>
<p>From the main tree (<em>Common Tasks</em>)&nbsp;go to&nbsp;<em>Applications</em>.</p>
<p>Press&nbsp;<em>Launch</em>&nbsp;on&nbsp;<em>oraclestorage</em>&nbsp;application.</p>
<p>If everything was set up properly you should see a sample page with a list of sample files and folders. Now&nbsp;<a href="https://www.webdavsystem.com/server/access/">connect to the server with any WebDAV client</a>.</p>
<p>If anything goes wrong please consult log usually located at&nbsp;<em>&lt;GLASSFISH_INSTANCE_ROOT&gt;/logs/server.log.</em></p>
</li>
</ol></li>
</ol></li>
</ol>
<h2>The Project&nbsp;Classes</h2>
<p>On the diagram below you can see the classes in&nbsp;WebDAV OracleStorage project.</p>
<p><img id="__mcenew" alt="Oracle class diagram" src="https://www.webdavsystem.com/media/1562/oraclediagram.png" rel="109211"></p>
<p>To adapt the sample to your needs, you will modify these classes to read and write data from and into your storage. You can find more about this in&nbsp;<a title="Creating WebDAV Server" href="https://www.webdavsystem.com/javaserver/doc/">Creating a Class 1 WebDAV Server</a>&nbsp;and&nbsp;<a title="Class 2 / 3 Server" href="https://www.webdavsystem.com/javaserver/doc/create_class_2_webdav_server/">Creating Class 2 WebDAV Server</a>&nbsp;article as well as in the&nbsp;<a href="http://java.webdavsystem.com/">class reference documentation</a>.</p>
<h2>How Things Get Stored – Overview of the Oracle Back-end</h2>
<p>The database consists of 3 entities as depicted in the figure below.</p>
<p><img id="__mcenew" alt="Oracle DB diagram" src="https://www.webdavsystem.com/media/1559/oraclestoragedb.jpg" rel="109205"></p>
<h3>Repository Table</h3>
<p>All the information about files and folders along with their content is stored here. Following is the list of columns with a brief description of each field.</p>
<h3>Lock Table</h3>
<p>The Lock table stores lock applied to items.&nbsp;<span>You can find more about locking in the&nbsp;</span><a title="Class 2 / 3 Server" href="https://www.webdavsystem.com/javaserver/doc/create_class_2_webdav_server/">Creating Class 2 WebDAV Server</a><span>&nbsp;article.</span></p>
<h3>Properties Table</h3>
<p>All the information about the properties pertaining to each item is stored in Properties table.</p>
<p>&nbsp;</p>
<h3>See Also:</h3>
<ul>
<li><a title="Running" href="https://www.webdavsystem.com/javaserver/server_examples/running_webdav_samples/">Running the WebDAV Samples</a></li>
<li><a title="Troubleshooting" href="https://www.webdavsystem.com/javaserver/server_examples/troubleshooting/">WebDAV Server Samples Problems and Troubleshooting</a></li>
</ul>
<h3 class="para d-inline next-article-heading">Next Article:</h3>
<a title="WebDAV Server Example with File System Back-end in Java and Kotlin" href="https://www.webdavsystem.com/javaserver/server_examples/storage_file_system/">WebDAV Server Example with File System Back-end, Java and Kotlin</a>

