/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyItems.GetHeadRequests');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyItems.GetHeadRequests
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyItems.GetHeadRequests', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyItems.GetHeadRequests */{

    XhrSend: function(method, sPath, fCallback) {
        var xhr = new XMLHttpRequest();
        xhr.open(method, sPath, true);
        xhr.onreadystatechange = function() {
            if (xhr.readyState === 4) {
                var oHeaders = {};
                var aHeaders = xhr.getAllResponseHeaders().split('\n');
                for (var i = 0; i < aHeaders.length; i++) {
                    var aParts = aHeaders[i].split(':');
                    if (aParts.length > 1) {
                        oHeaders[aParts.shift()] = aParts.join(':').replace(/^\s+|\s+$/g, '');
                    }
                }

                fCallback(xhr, oHeaders);
            }
        };
        xhr.send();
    }

});

QUnitRunner.test('Check GET request support on folder', function (test) {
    var sFilePath = 'HierarchyItems/GetHeadRequests/';
    Helper.Create(sFilePath, QUnitRunner.async(function () {

        ITHit.WebDAV.Client.Tests.HierarchyItems.GetHeadRequests.XhrSend('GET', Helper.GetAbsolutePath(sFilePath), QUnitRunner.async(function (xhr) {

            test.strictEqual(xhr.status, 200, 'Check response status');
        }));
    }));
});


QUnitRunner.test('Check HEAD support on folder', function (test) {
    var sFilePath = 'HierarchyItems/GetHeadRequests/';
    Helper.Create(sFilePath, QUnitRunner.async(function () {

        ITHit.WebDAV.Client.Tests.HierarchyItems.GetHeadRequests.XhrSend('HEAD', Helper.GetAbsolutePath(sFilePath), QUnitRunner.async(function (xhr, oHeadHeaders) {

            test.strictEqual(xhr.status, 200, 'Check response status');

            ITHit.WebDAV.Client.Tests.HierarchyItems.GetHeadRequests.XhrSend('GET', Helper.GetAbsolutePath(sFilePath), QUnitRunner.async(function (xhr, oGetHeaders) {

                test.strictEqual(xhr.status, 200, 'Check response status');

                var  aHeaders = [
                    'Content-Type',
                    'Last-Modified',
                    'etag'
                ];
                for (var i = 0, l = aHeaders.length; i < l; i++) {
                    if (oHeadHeaders[aHeaders[i]] && oGetHeaders[aHeaders[i]]) {
                        test.strictEqual(oHeadHeaders[aHeaders[i]], oGetHeaders[aHeaders[i]], 'Check "' + aHeaders[i] + '" header equals on HEAD and GET requests');
                    } else {
                        console.error('Header "' + aHeaders[i] + '" is not support');
                    }
                }
            }));
        }));
    }));
});
