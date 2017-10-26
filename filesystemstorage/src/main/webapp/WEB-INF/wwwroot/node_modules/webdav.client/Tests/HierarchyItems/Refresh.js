/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyItems.Refresh');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyItems.Refresh
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyItems.Refresh', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyItems.Refresh */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFileAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    Refresh: function(webDavSession, sFileAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFileAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            // Return file content length
            console.log('Content length: ' + oFile.ContentLength);

            // Update file content
            oFile.WriteContentAsync('My updated content', null, null, function() {

                // Refresh item from server to read new content length
                oFile.RefreshAsync(function(oAsyncResult) {

                    // Return file content length
                    console.log('Content length: ' + oFile.ContentLength);

                    fCallback(oAsyncResult, oFile);
                });
            });
        });
    }

});

QUnitRunner.test('Refresh item', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/Refresh/myfile.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenFileAsync(Helper.GetAbsolutePath('HierarchyItems/Refresh/myfile.txt'), null, function(oAsyncResult) {
            QUnit.start();

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oAsyncResult.Result;

            test.strictEqual(oFile.ContentLength, 6, 'Check file content length');

            QUnit.stop();
            ITHit.WebDAV.Client.Tests.HierarchyItems.Refresh.Refresh(webDavSession, Helper.GetAbsolutePath('HierarchyItems/Refresh/myfile.txt'), function(oAsyncResult, oFile) {
                QUnit.start();

                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of refresh item request');
                test.strictEqual(oFile.ContentLength, 18, 'Check file content length after refresh');
            });
        });
    });
});
