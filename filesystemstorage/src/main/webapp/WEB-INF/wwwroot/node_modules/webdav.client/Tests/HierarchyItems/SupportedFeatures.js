/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyItems.SupportedFeatures');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyItems.SupportedFeatures
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyItems.SupportedFeatures', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyItems.SupportedFeatures */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/']
     * @param {function} [fCallback=function() {}]
     */
    SupportedFeatures: function(webDavSession, sFolderAbsolutePath, fCallback) {
        webDavSession.OpenFolderAsync(sFolderAbsolutePath, null, function(oAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oAsyncResult.Result;

            oFolder.GetSupportedFeaturesAsync(function(oAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.OptionsInfo} oOptionsInfo */
                var oOptionsInfo = oAsyncResult.Result;

                console.log('Locking support: ' + (oOptionsInfo.Features & ITHit.WebDAV.Client.Features.Class2 !== 0 ? 'yes' : 'no'));
                console.log('Resumable upload support: ' + (oOptionsInfo.Features & ITHit.WebDAV.Client.Features.ResumableUpload !== 0 ? 'yes' : 'no'));

                fCallback(oAsyncResult);
            });
        });
    }

});

QUnitRunner.test('Get supported features', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/'
    ], function() {
        QUnit.start();

        QUnit.stop();
        ITHit.WebDAV.Client.Tests.HierarchyItems.SupportedFeatures.SupportedFeatures(webDavSession, Helper.GetAbsolutePath('HierarchyItems/'), function(oAsyncResult) {
            QUnit.start();

            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get supported features request');
            test.strictEqual(oAsyncResult.Result instanceof ITHit.WebDAV.Client.OptionsInfo, true, 'Check result is instance of OptionsInfo');
        });
    });
});
