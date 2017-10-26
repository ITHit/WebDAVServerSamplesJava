/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('Versions.PutUnderVersion');

/**
 * @class ITHit.WebDAV.Client.Tests.Versions.PutUnderVersion
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.Versions.PutUnderVersion', null, {}, /** @lends ITHit.WebDAV.Client.Tests.Versions.PutUnderVersion */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFileAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    EnableVersion: function(webDavSession, sFileAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFileAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            // Enable versioning
            oFile.PutUnderVersionControlAsync(true, null, function(oAsyncResult) {

                if (oAsyncResult.IsSuccess) {
                    console.log('Versioning for file `' + oFile.DisplayName + '` is enabled.');
                }

                fCallback(oAsyncResult);
            });
        });
    }

});

QUnitRunner.test('Enable and disable versioning', function (test) {
    QUnit.stop();
    Helper.Create([
        'Versions/putunderver.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenItemAsync(Helper.GetAbsolutePath('Versions/putunderver.txt'), null, function(oAsyncResult) {
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

                test.strictEqual(oFile.VersionControlled, true, 'Check VersionControlled is true');

                QUnit.stop();
                oFile.PutUnderVersionControlAsync(false, null, function(oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of version control request (set false)');

                    QUnit.stop();
                    webDavSession.OpenItemAsync(Helper.GetAbsolutePath('Versions/putunderver.txt'), null, function(oAsyncResult) {
                        QUnit.start();

                        /** @typedef {ITHit.WebDAV.Client.HierarchyItem} oFile */
                        oFile = oAsyncResult.Result;

                        test.strictEqual(oFile.VersionControlled, false, 'Check VersionControlled is false');

                        QUnit.stop();
                        ITHit.WebDAV.Client.Tests.Versions.PutUnderVersion.EnableVersion(webDavSession, Helper.GetAbsolutePath('Versions/putunderver.txt'), function(oAsyncResult) {
                            QUnit.start();

                            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of version control request (set true)');

                            QUnit.stop();
                            webDavSession.OpenItemAsync(Helper.GetAbsolutePath('Versions/putunderver.txt'), null, function(oAsyncResult) {
                                QUnit.start();

                                /** @typedef {ITHit.WebDAV.Client.HierarchyItem} oFile */
                                oFile = oAsyncResult.Result;

                                test.strictEqual(oFile.VersionControlled, true, 'Check VersionControlled is true');
                            });
                        });
                    });
                });
            });
        });
    });
});
