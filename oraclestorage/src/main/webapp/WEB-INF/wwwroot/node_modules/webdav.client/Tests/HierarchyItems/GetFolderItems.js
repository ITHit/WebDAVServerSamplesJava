/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyItems.GetFolderItems');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyItems.GetFolderItems
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyItems.GetFolderItems', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyItems.GetFolderItems */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/']
     * @param {function} [fCallback=function() {}]
     */
    GetChildren: function(webDavSession, sFolderAbsolutePath, fCallback) {
        webDavSession.OpenFolderAsync(sFolderAbsolutePath, null, function(oFolderAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oFolderAsyncResult.Result;

            oFolder.GetChildrenAsync(null, null, function(oItemsAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.HierarchyItems[]} aItems */
                var aItems = oItemsAsyncResult.Result;

                for (var i = 0, l = aItems.length; i < l; i++) {
                    console.log(aItems[i].DisplayName);
                }

                fCallback(oItemsAsyncResult);
            });
        });
    }

});

QUnitRunner.test('List folder content', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/GetChildren/item1.txt',
        'HierarchyItems/GetChildren/item2.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        ITHit.WebDAV.Client.Tests.HierarchyItems.GetFolderItems.GetChildren(webDavSession, Helper.GetAbsolutePath('HierarchyItems/GetChildren/'), function(oAsyncResult) {
            QUnit.start();

            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get children request');
            test.strictEqual(oAsyncResult.Result.length, 2, 'Check children length');

            var aFileNames = [];
            for (var i = 0, l = oAsyncResult.Result.length; i < l; i++) {
                aFileNames.push(oAsyncResult.Result[i].DisplayName);
            }

            aFileNames = aFileNames.sort();
            test.strictEqual(aFileNames[0], 'item1.txt', 'Check first child name');
            test.strictEqual(aFileNames[1], 'item2.txt', 'Check second child name');
        });
    });
});
