/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyItems.CreateFolder');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyItems.CreateFolder
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyItems.CreateFolder', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyItems.CreateFolder */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/']
     * @param {function} [fCallback=function() {}]
     */
    CreateFolder: function (webDavSession, sFolderAbsolutePath, sFolderName, fCallback) {
        webDavSession.OpenFolderAsync(sFolderAbsolutePath, null, function (oAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oAsyncResult.Result;

            oFolder.CreateFolderAsync(sFolderName, null, null, function (oAsyncResult) {

                if (oAsyncResult.IsSuccess) {
                    /** @typedef {ITHit.WebDAV.Client.Folder} oNewFolder */
                    var oNewFolder = oAsyncResult.Result;

                    console.log(oNewFolder.Href);
                } else if (oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.MethodNotAllowedException) {
                    console.log('Folder already exists.');
                }

                fCallback(oAsyncResult);
            });
        });
    }

});

QUnitRunner.test('Create folder and open it', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/'
    ], function () {
        QUnit.start();

        QUnit.stop();
        ITHit.WebDAV.Client.Tests.HierarchyItems.CreateFolder.CreateFolder(webDavSession, Helper.GetAbsolutePath('HierarchyItems/'), 'my_folder', function (oAsyncResult) {
            QUnit.start();

            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of create folder request');
            test.strictEqual(oAsyncResult.Result.Href, Helper.GetAbsolutePath('HierarchyItems/my_folder/'), 'Check created folder name');

            QUnit.stop();
            webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/my_folder/'), null, function (oAsyncResult) {
                QUnit.start();

                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get folder request');
                test.strictEqual(oAsyncResult.Result.Href, Helper.GetAbsolutePath('HierarchyItems/my_folder/'), 'Check name of loaded folder');
            });
        });
    });
});

QUnitRunner.test('Check Conflict exception on create in removed folder', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/toDelete/'
    ], function () {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/toDelete/'), null, function (oAsyncResult) {
            QUnit.start();

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oAsyncResult.Result;

            QUnit.stop();
            oFolder.DeleteAsync(null, function (oAsyncResult) {
                QUnit.start();

                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of delete request');

                QUnit.stop();
                oFolder.CreateFolderAsync('test', null, null, function (oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, false, 'Check failure on create in not founded folder');
                    test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.ConflictException, true, 'Check error is ConflictException');
                });
            });
        });
    });
});

QUnitRunner.test('Check Duplicate exception on create already exists folder', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/duplicate/'
    ], function () {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/'), null, function (oAsyncResult) {
            QUnit.start();

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oAsyncResult.Result;

            QUnit.stop();
            oFolder.CreateFolderAsync('duplicate', null, null, function (oAsyncResult) {
                QUnit.start();

                test.strictEqual(oAsyncResult.IsSuccess, false, 'Check failure on request create already exists folder');
                test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.MethodNotAllowedException, true, 'Check error is MethodNotAllowedException');
            });
        });
    });
});

QUnitRunner.test('Check long paths support', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/'
    ], function () {
        var numFolder = 9;
        var folderPath = 'HierarchyItems';
        var folderName = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
        function createFolder(sFolderPath, sFolderName, index) {
            ITHit.WebDAV.Client.Tests.HierarchyItems.CreateFolder.CreateFolder(webDavSession, Helper.GetAbsolutePath(sFolderPath), sFolderName, function (oAsyncResult) {

                var nFolderPath = sFolderPath + '/' + sFolderName;
                var message = 'Path ' + nFolderPath + ': check success of create folder request';
                if (oAsyncResult.IsSuccess) {
                    test.ok(true, message);
                    test.strictEqual(oAsyncResult.Result.Href, Helper.GetAbsolutePath(nFolderPath + '/'), 'Path ' + nFolderPath + ': check created folder name');
                }
                else {
                    ITHitTests.skip(test, 'Server does not support long paths.');
                    QUnit.start();
                    return;
                }

                if (index++ < numFolder) {
                    createFolder(nFolderPath, folderName, index);
                }
                else {
                    QUnit.start();
                }
            });
        }
        createFolder(folderPath, folderName, 0)
    });
});