/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('Versions.ReadContent');

/**
 * @class ITHit.WebDAV.Client.Tests.Versions.ReadContent
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.Versions.ReadContent', null, {}, /** @lends ITHit.WebDAV.Client.Tests.Versions.ReadContent */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    ReadContent: function(webDavSession, sFolderAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFolderAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            oFile.GetVersionsAsync(function(oVersionsAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.Version[]} aVersions */
                var aVersions = oVersionsAsyncResult.Result;

                /** @typedef {ITHit.WebDAV.Client.Version} oVersion */
                var oVersion = aVersions[0];

                oVersion.ReadContentAsync(null, null, function(oAsyncResult) {

                    /** @typedef {String} sContent */
                    var sContent = oAsyncResult.Result;

                    console.log('Version ' + oVersion.VersionName + ', content:' + sContent);

                    fCallback(oAsyncResult);
                });
            });
        });
    }

});

QUnitRunner.test('Read version content', function (test) {
    QUnit.stop();
    Helper.Create([
        'Versions/ver_read.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenFileAsync(Helper.GetAbsolutePath('Versions/ver_read.txt'), null, function(oAsyncResult) {
            QUnit.start();

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oAsyncResult.Result;

            QUnit.stop();
            Helper.CheckVersionsSupported(oFile, function(bIsVersionSupported) {
                QUnit.start();

                if (!bIsVersionSupported) {
                    ITHitTests.skip(test, 'Server does not support versions.');
                    return;
                }

                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of open item request');
                test.strictEqual(oFile instanceof ITHit.WebDAV.Client.HierarchyItem, true, 'Check result is instance of HierarchyItem');

                QUnit.stop();
                ITHit.WebDAV.Client.Tests.Versions.ReadContent.ReadContent(webDavSession, Helper.GetAbsolutePath('Versions/ver_read.txt'), function(oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of read version request');
                    test.strictEqual(oAsyncResult.Result, 'test..', 'Check version content');
                });
            });
        });
    });
});
