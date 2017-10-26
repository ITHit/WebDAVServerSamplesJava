/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyItems.GetItemBySession');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyItems.GetItemBySession
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyItems.GetItemBySession', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyItems.GetItemBySession */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/Documents/']
     * @param {function} [fCallback=function() {}]
     */
    GetFolder: function(webDavSession, sFolderAbsolutePath, fCallback) {
        webDavSession.OpenFolderAsync(sFolderAbsolutePath, null, function(oAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oAsyncResult.Result;

            console.log('Loaded folder `' + oFolder.DisplayName + '`.');

            fCallback(oAsyncResult);
        });
    }

});

QUnitRunner.test('Get item using WebDavSession', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/GetItemBySession/item1.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenItemAsync(Helper.GetAbsolutePath('HierarchyItems/GetItemBySession/'), null, function(oAsyncResult) {
            QUnit.start();
            test.strictEqual(oAsyncResult.Result instanceof ITHit.WebDAV.Client.Folder, true, 'Check result is instance of Folder');

            QUnit.stop();
            webDavSession.OpenItemAsync(Helper.GetAbsolutePath('HierarchyItems/GetItemBySession/item1.txt'), null, function(oAsyncResult) {
                QUnit.start();
                test.strictEqual(oAsyncResult.Result instanceof ITHit.WebDAV.Client.File, true, 'Check result is instance of File');

                QUnit.stop();
                ITHit.WebDAV.Client.Tests.HierarchyItems.GetItemBySession.GetFolder(webDavSession, Helper.GetAbsolutePath('HierarchyItems/GetItemBySession/'), function(oAsyncResult) {
                    QUnit.start();
                    test.strictEqual(oAsyncResult.Result instanceof ITHit.WebDAV.Client.Folder, true, 'Check result is instance of Folder');

                    QUnit.stop();
                    webDavSession.OpenFileAsync(Helper.GetAbsolutePath('HierarchyItems/GetItemBySession/item1.txt'), null, function(oAsyncResult) {
                        QUnit.start();
                        test.strictEqual(oAsyncResult.Result instanceof ITHit.WebDAV.Client.File, true, 'Check result is instance of File');

                        QUnit.stop();
                        webDavSession.OpenFileAsync(Helper.GetAbsolutePath('HierarchyItems/GetItemBySession/'), null, function(oAsyncResult) {
                            QUnit.start();
                            test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.WebDavException, true, 'Check error is WebDavException on get folder by OpenFile method');

                            QUnit.stop();
                            webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/GetItemBySession/item1.txt'), null, function(oAsyncResult) {
                                QUnit.start();
                                test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.WebDavException, true, 'Check error is WebDavException on get file by OpenFolder method');
                            });
                        });
                    });
                });
            });
        });
    });
});

QUnitRunner.test('Check NotFound exception by session calls', function (test) {
    QUnit.stop();
    webDavSession.OpenItemAsync(Helper.GetAbsolutePath('HierarchyItems/GetItemBySession/not-found/'), null, function(oAsyncResult) {
        QUnit.start();

        test.strictEqual(oAsyncResult.IsSuccess, false, 'Check failure on open not found item request');
        test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.NotFoundException, true, 'Check error is NotFoundException');

        QUnit.stop();
        webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/GetItemBySession/not-found/'), null, function(oAsyncResult) {
            QUnit.start();

            test.strictEqual(oAsyncResult.IsSuccess, false, 'Check failure on open not found folder request');
            test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.NotFoundException, true, 'Check error is NotFoundException');


            QUnit.stop();
            webDavSession.OpenFileAsync(Helper.GetAbsolutePath('HierarchyItems/GetItemBySession/not-found.txt'), null, function(oAsyncResult) {
                QUnit.start();

                test.strictEqual(oAsyncResult.IsSuccess, false, 'Check failure on open not found file request');
                test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.NotFoundException, true, 'Check error is NotFoundException');
            });
        });
    });
});

QUnitRunner.test('Get item using WebDavSession, with parameters in URL', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/GetItemBySession/param1.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenItemAsync(Helper.GetAbsolutePath('HierarchyItems/GetItemBySession/param1.txt?param1=1&param2=2'), null, function(oAsyncResult) {
            QUnit.start();

            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of open item with params request');
            test.strictEqual(oAsyncResult.Result instanceof ITHit.WebDAV.Client.File, true, 'Check result is instance of File');

            QUnit.stop();
            webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/GetItemBySession/?param1=1&param2=2'), null, function(oAsyncResult) {
                QUnit.start();

                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of open folder with params request');
                test.strictEqual(oAsyncResult.Result instanceof ITHit.WebDAV.Client.Folder, true, 'Check result is instance of Folder');


                QUnit.stop();
                webDavSession.OpenFileAsync(Helper.GetAbsolutePath('HierarchyItems/GetItemBySession/param1.txt?param1=1&param2=2'), null, function(oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of open file with params request');
                    test.strictEqual(oAsyncResult.Result instanceof ITHit.WebDAV.Client.File, true, 'Check result is instance of File');
                });
            });
        });
    });
});
