/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('DocManager.MsOfficeEditExtensions');

/**
 * @class ITHit.WebDAV.Client.Tests.DocManager.MsOfficeEditExtensions
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.DocManager.MsOfficeEditExtensions', null, {}, /** @lends ITHit.WebDAV.Client.Tests.DocManager.MsOfficeEditExtensions */{

    /**
     * @param {string} [sDocumentUrl='http://localhost:87654/myfile.rtf']
     */
    EditRtfDocumentWithWord: function(sDocumentUrl) {
		ITHit.WebDAV.Client.DocManager.IsMicrosoftOfficeDocument(sDocumentUrl);	// false
		ITHit.WebDAV.Client.DocManager.MsOfficeEditExtensions.Word.push('rtf');
		ITHit.WebDAV.Client.DocManager.IsMicrosoftOfficeDocument(sDocumentUrl);	// true
		// Now .rtf file is opened with Microsoft Word.
    },

	/**
     * @param {string} [sDocumentUrl='http://localhost:87654/myfile.rtf']
     * @param {string} [sMountPoint='http://localhost:87654/']
     * @param {function} [fProtocolInstallCallback]
	 */
	EditDocumentTest: function(sDocumentUrl, sMountPoint, fProtocolInstallCallback) {
		ITHit.WebDAV.Client.DocManager.MsOfficeEditExtensions.Word.push("rtf");

		// Now .rtf files are opened with Microsoft Word.
		ITHit.WebDAV.Client.DocManager.EditDocument(sDocumentUrl, sMountPoint, fProtocolInstallCallback);

		// or call
		// ITHit.WebDAV.Client.DocManager.MicrosoftOfficeEditDocument(sDocumentUrl, fProtocolInstallCallback);
	}

});


QUnit.test('MsOfficeEditExtensions test', function(test){
	var oDocManager = ITHit.WebDAV.Client.DocManager;
	test.ok(oDocManager.MsOfficeEditExtensions !== undefined, 'Check if ..DocManager.MsOfficeEditExtensions is defined');
	test.ok(oDocManager.MsOfficeEditExtensions.Word !== undefined, 'Check if ..MsOfficeEditExtensions.Word is defined');
	test.strictEqual(oDocManager.GetMsOfficeSchemaByExtension('docx'), 'ms-word', 'Check if schema for "docx" is "ms-word" (DocManager.GetMsOfficeSchemaByExtension)');
	test.ok(oDocManager.IsMicrosoftOfficeDocument('filename.VDW'), 'Check "VDW" if it is in MsOfficeEditExtensions (IsMicrosoftOfficeDocument)');
	test.ok(!oDocManager.IsMicrosoftOfficeDocument('filename.rtf'), 'Check "rtf" if it is not in MsOfficeEditExtensions');
	// add 'rtf' to MsOffice file extensions
	oDocManager.MsOfficeEditExtensions.Word.push('rtf');
	test.ok(oDocManager.IsMicrosoftOfficeDocument('filename.rtf'), 'Check "rtf" if it is in MsOfficeEditExtensions now');

	oDocManager.MsOfficeEditExtensions.Word.pop();
	ITHit.WebDAV.Client.Tests.DocManager.MsOfficeEditExtensions.EditRtfDocumentWithWord('filename.rtf');
});