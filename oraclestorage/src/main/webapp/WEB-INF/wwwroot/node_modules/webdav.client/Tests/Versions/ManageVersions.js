/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('Versions.ManageVersions');

/**
 * @class ITHit.WebDAV.Client.Tests.Versions.ManageVersions
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.Versions.ManageVersions', null, {}, /** @lends ITHit.WebDAV.Client.Tests.Versions.ManageVersions */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    UpdateToThis: function(webDavSession, sFolderAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFolderAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            oFile.GetVersionsAsync(function(oAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.Version[]} aVersions */
                var aVersions = oAsyncResult.Result;

                /** @typedef {ITHit.WebDAV.Client.Version} oLastVersion */
                var oLastVersion = aVersions[aVersions.length - 1];

                oLastVersion.UpdateToThisAsync(function(oAsyncResult) {

                    if (oAsyncResult.IsSuccess) {
                        console.log('File `' + oFile.DisplayName + '` successfully updated to version `' + oLastVersion.VersionName + '`');
                    }

                    fCallback(oAsyncResult);
                });
            });
        });
    },

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {string} [sVersionHref='http://localhost:87654/myfile.txt?version=2']
     * @param {function} [fCallback=function() {}]
     */
    UpdateToVersion: function(webDavSession, sFolderAbsolutePath, sVersionHref, fCallback) {
        webDavSession.OpenFileAsync(sFolderAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            oFile.UpdateToVersionAsync(sVersionHref, function(oAsyncResult) {

                if (oAsyncResult.Result === true) {
                    console.log('File `' + oFile.DisplayName + '` successfully updated to version `' + sVersionHref + '`');
                }

                fCallback(oAsyncResult);
            });
        });
    }

});

QUnitRunner.test('Update and delete versions', function (test) {
    QUnit.stop();
    Helper.Create([
        'Versions/man_ver.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenItemAsync(Helper.GetAbsolutePath('Versions/man_ver.txt'), null, function(oAsyncResult) {
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

                QUnit.stop();
                oFile.GetVersionsAsync(function(oAsyncResult) {
                    QUnit.start();

                    var oVersion = oAsyncResult.Result[0];
                    var iPrevLength = oAsyncResult.Result.length;

                    QUnit.stop();
                    ITHit.WebDAV.Client.Tests.Versions.ManageVersions.UpdateToThis(webDavSession, Helper.GetAbsolutePath('Versions/man_ver.txt'), function(oAsyncResult) {
                        QUnit.start();

                        test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of update to version request');

                        QUnit.stop();
                        ITHit.WebDAV.Client.Tests.Versions.ManageVersions.UpdateToVersion(webDavSession, Helper.GetAbsolutePath('Versions/man_ver.txt'), oVersion.Href, function(oAsyncResult) {
                            QUnit.start();

                            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of update to version request (2)');

                            QUnit.stop();
                            oFile.GetVersionsAsync(function(oAsyncResult) {
                                QUnit.start();

                                test.strictEqual(oAsyncResult.Result.length, iPrevLength + 2, 'Check versions count');

                                QUnit.stop();
                                oVersion.DeleteAsync(function(oAsyncResult) {
                                    QUnit.start();

                                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of delete version request');

                                    QUnit.stop();
                                    oFile.GetVersionsAsync(function(oAsyncResult) {
                                        QUnit.start();

                                        test.strictEqual(oAsyncResult.Result.length, iPrevLength + 1, 'Check versions count');
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    });
});

QUnitRunner.test('Update version by write content', function (test) {
    QUnit.stop();
    Helper.Create([
        'Versions/upd_wr.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenFileAsync(Helper.GetAbsolutePath('Versions/upd_wr.txt'), null, function(oAsyncResult) {
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

                QUnit.stop();
                oFile.GetVersionsAsync(function(oAsyncResult) {
                    var iPrevLength = oAsyncResult.Result.length;
                    oFile.WriteContentAsync('test', null, null, function() {
                        oFile.GetVersionsAsync(function(oAsyncResult) {
                            QUnit.start();

                            var iNowLength = oAsyncResult.Result.length;
                            var oNewVersion = oAsyncResult.Result[oAsyncResult.Result.length - 1];

                            test.strictEqual(iPrevLength, iNowLength - 1, 'Check versions count');
                            test.strictEqual(oFile.ContentLength, 6, 'Check file v1 content length');
                            test.strictEqual(oNewVersion.ContentLength, 4, 'Check file v2 content length');
                        });
                    });
                });
            });
        });
    });
});
