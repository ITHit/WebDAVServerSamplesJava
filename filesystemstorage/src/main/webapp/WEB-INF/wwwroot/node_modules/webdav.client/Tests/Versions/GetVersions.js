/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('Versions.GetVersions');

/**
 * @class ITHit.WebDAV.Client.Tests.Versions.GetVersions
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.Versions.GetVersions', null, {}, /** @lends ITHit.WebDAV.Client.Tests.Versions.GetVersions */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    GetVersions: function(webDavSession, sFolderAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFolderAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            oFile.GetVersionsAsync(function(oAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.Version[]} aVersions */
                var aVersions = oAsyncResult.Result;

                for (var i = 0, l = aVersions.length; i < l; i++) {
                    var oVersion = aVersions[i];

                    console.log([
                        'Version Name: ' + oVersion.VersionName,
                        'Comment: ' + oVersion.Comment,
                        'Author: ' + oVersion.CreatorDisplayName,
                        'Created: ' + oVersion.CreationDate
                    ].join('\n'));
                }

                fCallback(oAsyncResult);
            });
        });
    }

});

QUnitRunner.test('Get file versions list', function (test) {
    QUnit.stop();
    Helper.Create([
        'Versions/ver.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenFileAsync(Helper.GetAbsolutePath('Versions/ver.txt'), null, function(oAsyncResult) {
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
                ITHit.WebDAV.Client.Tests.Versions.GetVersions.GetVersions(webDavSession, Helper.GetAbsolutePath('Versions/ver.txt'), function(oAsyncResult) {
                    QUnit.start();

                    var oVersion = oAsyncResult.Result[0];

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get versions request');
                    test.strictEqual(oVersion instanceof ITHit.WebDAV.Client.Version, true, 'Check result item is instance of Version');
                });
            });
        });
    });
});
