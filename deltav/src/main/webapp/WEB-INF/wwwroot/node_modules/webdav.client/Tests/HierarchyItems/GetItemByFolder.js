/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyItems.GetItemByFolder');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyItems.GetItemByFolder
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyItems.GetItemByFolder', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyItems.GetItemByFolder */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/Products/']
     * @param {string} [sFileName='myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    GetFile: function(webDavSession, sFolderAbsolutePath, sFileName, fCallback) {
        webDavSession.OpenFolderAsync(sFolderAbsolutePath, null, function(oFolderAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oFolderAsyncResult.Result;

            oFolder.GetFileAsync(sFileName, function(oFileAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.File} oFile */
                var oFile = oFileAsyncResult.Result;

                console.log('File `' + oFile.DisplayName + '` successful loaded from folder `' + oFolder.DisplayName + '`.');

                fCallback(oFileAsyncResult);
            });
        });
    }

});

QUnitRunner.test('Get item using Folder', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/GetItemByFolder/my_folder/',
        'HierarchyItems/GetItemByFolder/my_file.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/GetItemByFolder/'), null, function(oAsyncResult) {
            QUnit.start();

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oAsyncResult.Result;

            QUnit.stop();
            oFolder.GetItemAsync('my_folder', function(oAsyncResult) {
                QUnit.start();
                test.strictEqual(oAsyncResult.Result instanceof ITHit.WebDAV.Client.Folder, true, 'Check result is instance of Folder');

                QUnit.stop();
                oFolder.GetItemAsync('my_file.txt', function(oAsyncResult) {
                    QUnit.start();
                    test.strictEqual(oAsyncResult.Result instanceof ITHit.WebDAV.Client.File, true, 'Check result is instance of File');

                    QUnit.stop();
                    oFolder.GetFolderAsync('my_folder', function(oAsyncResult) {
                        QUnit.start();
                        test.strictEqual(oAsyncResult.Result instanceof ITHit.WebDAV.Client.Folder, true, 'Check result is instance of Folder');

                        QUnit.stop();
                        ITHit.WebDAV.Client.Tests.HierarchyItems.GetItemByFolder.GetFile(webDavSession, Helper.GetAbsolutePath('HierarchyItems/GetItemByFolder/'), 'my_file.txt', function(oAsyncResult) {
                            QUnit.start();

                            test.strictEqual(oAsyncResult.Result instanceof ITHit.WebDAV.Client.File, true, 'Check result is instance of File');

                            QUnit.stop();
                            oFolder.GetFileAsync('my_folder', function(oAsyncResult) {
                                QUnit.start();
                                test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.WebDavException, true, 'Check error is WebDavException on get folder by GetFile method');

                                QUnit.stop();
                                oFolder.GetFolderAsync('my_file.txt', function(oAsyncResult) {
                                    QUnit.start();
                                    test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.WebDavException, true, 'Check error is WebDavException on get file by GetFolder method');
                                });
                            });
                        });
                    });
                });
            });
        });
    });
});
