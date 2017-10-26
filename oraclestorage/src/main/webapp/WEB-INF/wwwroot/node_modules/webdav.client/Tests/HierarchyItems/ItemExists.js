/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyItems.ItemExists');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyItems.ItemExists
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyItems.ItemExists', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyItems.ItemExists */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/']
     * @param {function} [fCallback=function() {}]
     */
    ItemExists: function(webDavSession, sFolderAbsolutePath, fCallback) {
        webDavSession.OpenFolderAsync(sFolderAbsolutePath, null, function(oAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oAsyncResult.Result;

            oFolder.ItemExistsAsync('my_folder', function(oAsyncResult) {

                if (oAsyncResult.Result) {
                    console.log('Item exists');
                } else {
                    console.log('Item not found');
                }

                fCallback(oAsyncResult);
            });
        });
    }

});

QUnitRunner.test('Item exists', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/ItemExists/'
    ], function() {
        QUnit.start();

        QUnit.stop();
        ITHit.WebDAV.Client.Tests.HierarchyItems.ItemExists.ItemExists(webDavSession, Helper.GetAbsolutePath('HierarchyItems/ItemExists/'), function(oAsyncResult) {
            QUnit.start();

            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of item exists request');
            test.strictEqual(oAsyncResult.Result, false, 'Check result is false - not found folder');

            // Create folder
            QUnit.stop();
            webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/ItemExists/'), null, function(oAsyncResult) {
                var oFolder = oAsyncResult.Result;
                oFolder.CreateFolderAsync('my_folder', null, null, function(oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of create folder request');

                    QUnit.stop();
                    ITHit.WebDAV.Client.Tests.HierarchyItems.ItemExists.ItemExists(webDavSession, Helper.GetAbsolutePath('HierarchyItems/ItemExists/'), function(oAsyncResult) {
                        QUnit.start();

                        test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of item exists request');
                        test.strictEqual(oAsyncResult.Result, true, 'Check result is true - folder is exists');

                    });
                });
            });
        });
    });
});
