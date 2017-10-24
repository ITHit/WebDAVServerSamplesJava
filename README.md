# Java WebDAV Server Examples
IT Hit WebDAV Server Library for Java is provided with several examples that demonstrate how to build a WebDAV server with SQL back-end or with file system storage. You can adapt these samples to publish data from virtually any back-end storage including CMS/DMS/CRM, Azure or Amazon storage. 

A sample HTML page included with samples demonstrates how to use [IT Hit WebDAV Ajax Libray](https://www.webdavsystem.com/ajax/) to open documents from a web page for editing, list documents and navigate folder structure as well as build search capadilities.

#### Online Demo Server
https://www.WebDAVServer.com

#### Requirements
The samples are tested with _Java 1.8_ in the following environments:
- [x] Tomcat 7 or later
- [x] Glassfish 4.1.1 or later
- [x] JBoss Wildfly 9 or later or respective EAP
- [x] WebLogic 12c or later
- [x] WebSphere 8.5.5.11 or later
- [x] Jetty 9.3.13 or later

#### Full-text Search and indexing
The samples are provided with full-text search and indexing based on use Apache Lucene as indexing engine and Apache Tika as content analysis toolkit.

The server implementation searches both file names and file content including content of Microsoft Office documents as well as any other documents which format is supported by Apache Tika, such as LibreOffice, OpenOffice, PDF, etc.

## WebDAV Server with Oracle Back-end Example
WebDAV server with Oracle back-end example is a Class 2 server that stores all data including locks, file content and custom properties in Oracle database. 
This example is a fully-functional WebDAV server that can be used to open, edit and save Microsoft Office documents as well as any other types of files directly to server, without download/upload steps. [more...](https://www.webdavsystem.com/javaserver/server_examples/sql_storage/)

## WebDAV Server with File System Back-end Example
WebDAV server with file system back-end storage is a fully functional Class 2 server that stores all data in file system. It utilizes File Extended Attributes to store locks and custom properties. This sample can be configured to use Basic or Digest authentication. [more...](https://www.webdavsystem.com/javaserver/server_examples/storage_file_system/)

## WebDAV Server with Versioning Example
This example demonstrates how you can implement file versioning support in your WebDAV server. It is using auto-versioning and each time you save a file the new version is created. Your client application does not need to know anything about versioning support on a server side. While capable of handling any WebDAV clients, this DeltaV sample is optimized to work with Microsoft Office. It is using Lock/Unlock commands to minimize an amount of versions created. [more...](https://www.webdavsystem.com/javaserver/server_examples/deltav_storage/)

## Android WebDAV Server on NanoHTTPD Example
 A modified NanoHTTPD mobile WebDAV server that runs on Android. It stores all files in mobile device file system while locks and properties in SQLite. This sample provides access to the documents inside a mobile app folder. To see the documents a user opens a sample web page served by this server in a web browser on any machine in the local network. A user can open, edit and save documents back to the mobile device as well as can upload, download and manage documents using any WebDAV client. The sample requires license. 
 Its text should be added to the **webdavsettings.json** file (see **assets** folder) in the **License** section. Please change all double quotes to single quotes in the license text or remove **xml** header.
 To build example execute **buildAndroid.bat** or **buildAndroid**  in the root folder.