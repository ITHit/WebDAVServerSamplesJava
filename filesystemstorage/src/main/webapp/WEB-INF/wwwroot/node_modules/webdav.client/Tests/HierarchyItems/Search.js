/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyItems.Search');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyItems.Search
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyItems.Search', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyItems.Search */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/']
     * @param {function} [fCallback=function() {}]
     */
    SearchByString: function(webDavSession, sFolderAbsolutePath, fCallback) {
        webDavSession.OpenFolderAsync(sFolderAbsolutePath, null, function(oFolderAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oFolderAsyncResult.Result;

            oFolder.SearchAsync('my_file', null, function(oAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.HierarchyItems[]} aItems */
                var aItems = oAsyncResult.Result;

                for (var i = 0, l = aItems.length; i < l; i++) {
                    console.log(aItems[i].DisplayName);
                }

                fCallback(oAsyncResult);
            });
        });
    },

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/Products/']
     * @param {function} [fCallback=function() {}]
     */
    SearchByQuery: function(webDavSession, sFolderAbsolutePath, fCallback) {
        webDavSession.OpenFolderAsync(sFolderAbsolutePath, null, function(oFolderAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oFolderAsyncResult.Result;

            // Build search query
            var oSearchQuery = new ITHit.WebDAV.Client.SearchQuery('my_%');

            // By default WebDAV Ajax Client search by DisplayName property.
            // You can add other properties to this list.
            oSearchQuery.LikeProperties.push(new ITHit.WebDAV.Client.PropertyName('creator-displayname', 'DAV:'));
            oSearchQuery.LikeProperties.push(new ITHit.WebDAV.Client.PropertyName('comment', 'DAV:'));

            // Disable search by file content
            oSearchQuery.EnableContains = false;

            oFolder.SearchByQueryAsync(oSearchQuery, function(oAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.HierarchyItems[]} aItems */
                var aItems = oAsyncResult.Result;

                for (var i = 0, l = aItems.length; i < l; i++) {
                    console.log(aItems[i].DisplayName);
                }

                fCallback(oAsyncResult);
            });
        });
    }

});

// @todo No full testing, because search indexes is not dynamically

QUnitRunner.test('Search file by string or query object', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/Search/my_folder/',
        'HierarchyItems/Search/my_file.txt',
        'HierarchyItems/Search/my_image.jpg'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/Search/'), null, function(oAsyncResult) {
            QUnit.start();

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oAsyncResult.Result;

            QUnit.stop();
            oFolder.GetSupportedFeaturesAsync(function(oAsyncResult) {
                QUnit.start();

                /** @typedef {ITHit.WebDAV.Client.OptionsInfo} oOptionsInfo */
                var oOptionsInfo = oAsyncResult.Result;

                if (!oOptionsInfo.Search) {
                    ITHitTests.skip(test, 'Server does not support search.');
                    return;
                }

                QUnit.stop();
                ITHit.WebDAV.Client.Tests.HierarchyItems.Search.SearchByString(webDavSession, Helper.GetAbsolutePath('HierarchyItems/Search/'), function(oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of search by string request');
                    /*test.strictEqual(oAsyncResult.Result.length, 1);
                     test.strictEqual(oAsyncResult.Result[0] instanceof ITHit.WebDAV.Client.File, true);
                     test.strictEqual(oAsyncResult.Result[0].DisplayName, 'my_file');*/

                    QUnit.stop();
                    ITHit.WebDAV.Client.Tests.HierarchyItems.Search.SearchByQuery(webDavSession, Helper.GetAbsolutePath('HierarchyItems/Search/'), function(oAsyncResult) {
                        QUnit.start();

                        test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of search by query object request');
                        /*test.strictEqual(oAsyncResult.Result.length, 1);
                         test.strictEqual(oAsyncResult.Result[0] instanceof ITHit.WebDAV.Client.File, true);*/
                    });
                });
            });
        });
    });
});
