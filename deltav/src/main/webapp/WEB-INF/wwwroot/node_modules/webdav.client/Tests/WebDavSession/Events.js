/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('WebDavSession.Events');

/**
 * @class ITHit.WebDAV.Client.Tests.WebDavSession.Events
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.WebDavSession.Events', null, {}, /** @lends ITHit.WebDAV.Client.Tests.WebDavSession.Events */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {function} [fCallback=function() {}]
     */
    BeforeRequestSend: function(webDavSession, fCallback) {
        webDavSession.AddListener('OnBeforeRequestSend', function(oEvent) {

            // Add new header
            //oEvent.Headers['My-Header'] = oEvent.Method;

            // Show request info
            console.log(oEvent.Method + ' ' + oEvent.Href);
            for (var sKey in oEvent.Headers) {
                if (oEvent.Headers.hasOwnProperty(sKey)) {
                    console.log(sKey + ': ' + oEvent.Headers[sKey]);
                }
            }

            // Show request body
            console.log(oEvent.Body);

            fCallback(oEvent);
        });
    },

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {function} [fCallback=function() {}]
     */
    Response: function(webDavSession, fCallback) {
        webDavSession.AddListener('OnResponse', function(oEvent) {

            // Show HTTP status and description
            console.log(oEvent.Status + ' ' + oEvent.StatusDescription);

            // Show headers
            for (var sKey in oEvent.Headers) {
                if (oEvent.Headers.hasOwnProperty(sKey)) {
                    console.log(sKey + ': ' + oEvent.Headers[sKey]);
                }
            }

            // Show response body
            console.log(oEvent.BodyText);

            fCallback(oEvent);
        });
    }

});

QUnitRunner.test('Subscribe on WebDavSession events: OnBeforeRequestSend, OnResponse', function (test) {
    QUnit.stop();
    Helper.Create([
        'WebDavSession/event.txt'
    ], function() {
        QUnit.start();

        var oRequestData = null;
        var oResponseData = null;

        ITHit.WebDAV.Client.Tests.WebDavSession.Events.BeforeRequestSend(webDavSession, function(oEvent) {
            oRequestData = oEvent;
        });
        ITHit.WebDAV.Client.Tests.WebDavSession.Events.Response(webDavSession, function(oEvent) {
            oResponseData = oEvent;
        });

        QUnit.stop();
        webDavSession.OpenFileAsync(Helper.GetAbsolutePath('WebDavSession/event.txt'), null, function(oAsyncResult) {
            QUnit.start();


            test.strictEqual(oRequestData.Method, 'PROPFIND', 'Check method in request event data');
            test.strictEqual(oRequestData.Href.replace(/\?.*$/, ''), Helper.GetAbsolutePath('WebDavSession/event.txt'), 'Check href in request event data');
            test.strictEqual(oRequestData.Headers.Depth, 0, 'Check header depth in request event data');
            test.strictEqual(oRequestData.Headers['Content-Type'].replace(/; ?/g, '; '), 'text/xml; charset="utf-8"', 'Check content type header in request event data');


			var xml = new ITHit.XMLDoc();
			xml.load(oRequestData.Body);
			var nodes = xml.childNodes()[0].childNodes()[0].childNodes();
			var propNames = [];
			for (var i = 0, l = nodes.length; i < l; i++) {
				propNames.push(nodes[i].nodeName());
			}

            test.strictEqual(propNames.join(), [
				"resourcetype",
				"displayname",
				"creationdate",
				"getlastmodified",
				"getcontenttype",
				"getcontentlength",
				"supportedlock",
				"lockdiscovery",
				"quota-available-bytes",
				"quota-used-bytes",
				"checked-in",
				"checked-out"
			].join(), 'Check body in request event data');

            test.strictEqual(oResponseData.Status, ITHit.WebDAV.Client.HttpStatus.MultiStatus.Code, 'Check status in response event data');
            test.strictEqual(oResponseData.StatusDescription, ITHit.WebDAV.Client.HttpStatus.MultiStatus.Description, 'Check status description in response event data');
            test.strictEqual(oResponseData.Headers['Content-Type'].replace(/; ?/g, '; '), 'application/xml; charset=utf-8', 'Check content type header in response event data');
            test.strictEqual(oResponseData.BodyText.indexOf('event.txt') !== -1, true, 'Check body text in response event data');
        });
    });
});