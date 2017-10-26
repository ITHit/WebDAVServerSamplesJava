# IT Hit WebDAV AJAX Library

[Product Website](https://www.webdavsystem.com/ajax/) |
[Demo Server](https://www.webdavserver.com/)


## Cross-browser JavaScript library for opening documents from a web page and managing WebDAV servers.


### API for Opening Docs from a Web Page

Using WebDAV Ajax Library you can open documents from a web page and save back directly to server without download/upload steps. The library opens any document with associated application in Chrome, FireFox, Safari, Edge and IE on Windows, Mac OS X and Linux.

```html
<script src="ITHitWebDAVClient.js"></script>

<script type="text/javascript">
   
    // Get your license ID here: https://www.webdavsystem.com/ajax/download/
    ITHit.WebDAV.Client.LicenseId = 'XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX';

    function edit() {
        ITHit.WebDAV.Client.DocManager.EditDocument(
		    "https://server/folder/file.docx", "/");
    }
</script>

<input type="button" value="Edit Document" onclick="edit()" />
```
[more...](https://www.webdavsystem.com/ajax/programming/open-doc-webpage/opening_ms_office_docs/)

### Opening Document from a Website with Cookies Authentication 

The library can open documents from a WebDAV server with cookies authentication: 

```javascript
function edit() {
    ITHit.WebDAV.Client.DocManager.DavProtocolEditDocument(
        'https://server/folder/file.ext', // Document URL(s)
        'https://server/',                // Mount URL
        protocolInstallMessage,           // Function to call if protocol app is not installed
        null,                             // Reserved
        'Current',                        // Which browser to copy cookies from: 'Current', 'All', 'None'
        '.AspNet.ApplicationCookie',      // Cookie(s) to copy. Must be persistent.
        '/Account/Login',                 // URL to navigate to if any cookie from the list is not found.
        'Edit'                            // Command to execute: 'Edit', 'OpenWith'
    );
}
     
function protocolInstallMessage(message) {
    var installerFilePath = "/Plugins/" + ITHit.WebDAV.Client.DocManager.GetInstallFileName();
 
    if (confirm("Opening this type of file requires a protocol installation. Select OK to download the protocol installer.")){
        window.open(installerFilePath);
    }
}
```
[more...](https://www.webdavsystem.com/ajax/programming/open-doc-webpage/cookies-authentication/)

### Using Ajax API for Managing WebDAV Server

The library provides a high-level JavaScript API for managing WebDAV server content and building Ajax file managers.

See how to list files on a WebDAV server, copy, move and delete items, read and set custom properties, lock items and discover locks.

[more...](https://www.webdavsystem.com/ajax/programming/managing_hierarchy/)


### Programming WebDAV Search Capabilities
This article describes how to detect DASL search support, submit search queries, specify which properties to search and request custom properties to be returned in search results, such as snippet of text around search phrase.

[more...](https://www.webdavsystem.com/ajax/programming/search/)

### Re-branding and Building from Source Codes

The Source Codes License is provided with complete Protocol Applications source codes. You can fully re-brand, localize and customize the protocol applications for Windows, OS X and Linux. The source codes are provided with build scripts that you can use to compile and build installers for each OS "in one click".

[more...](https://www.webdavsystem.com/ajax/programming/open-doc-webpage/rebranding-building/)

### Opening OS File Manager from a Web Page

See how to jump from a web page to managing files using familiar desktop interface.

The custom protocol provided with Ajax Library can mount WebDAV server to local file system and start OS File Manager  on Windows, Mac OS X and Linux.

[more...](https://www.webdavsystem.com/ajax/programming/open-doc-webpage/opening_os_file_manager/)

### Managing Locks on Your WebDAV Server

Locks protect the document while being edited from concurrent modifications. Locks support on your WebDAV server is vital for Microsoft Office editing.

While in most cases your WebDAV client, including Microsoft Office, will manage locks automatically you may need to discover locks support, enumerate locks and unlock a file if needed.

[more...](https://www.webdavsystem.com/ajax/programming/locking/)

### License 

[License Agreement](https://www.webdavsystem.com/media/1175/it-hit-webdav-ajax-library-license-agreement.rtf)
