/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyItems.CreateFile');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyItems.CreateFile
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyItems.CreateFile', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyItems.CreateFile */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/']
     * @param {function} [fCallback=function() {}]
     */
    CreateAndWriteContent: function(webDavSession, sFolderAbsolutePath, fCallback) {
        webDavSession.OpenFolderAsync(sFolderAbsolutePath, null, function(oFolderAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oFolderAsyncResult.Result;

            oFolder.CreateFileAsync('myfile.txt', null, 'Hello World!', null, function(oAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.File} oFile */
                var oFile = oAsyncResult.Result;

                if (oAsyncResult.Error) {
                    console.log(oAsyncResult.toString());
                }

                if (oAsyncResult.IsSuccess) {
                    console.log(oFile.DisplayName); // myfile.txt
                }

                fCallback(oAsyncResult);
            });
        });
    },

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFileAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    OnlyWriteContent: function(webDavSession, sFileAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFileAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            oFile.WriteContentAsync('Goodbye!', null, 'text/plain', function(oAsyncResult) {

                if (oAsyncResult.IsSuccess) {
                    console.log('Content is saved!');
                }

                fCallback(oAsyncResult);
            });
        });
    },

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFileAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    ReadContent: function(webDavSession, sFileAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFileAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            oFile.ReadContentAsync(null, null, function(oAsyncResult) {

                console.log(oAsyncResult.Result); // Goodbye!

                fCallback(oAsyncResult);
            });
        });
    },

    /**
     * Create folder example
     */
    ReadContentRange: function(oFile, fCallback) {
        oFile.ReadContentAsync(4, 3, function(oAsyncResult) {

            console.log(oAsyncResult.Result); // bye

            fCallback(oAsyncResult);
        });
    }

});

QUnitRunner.test('Create, write and read file content', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/CreateFile/'
    ], function() {
        QUnit.start();

        var sFolderPath = Helper.GetAbsolutePath('HierarchyItems/CreateFile/');
        var sFilePath = Helper.GetAbsolutePath('HierarchyItems/CreateFile/myfile.txt');

        QUnit.stop();
        ITHit.WebDAV.Client.Tests.HierarchyItems.CreateFile.CreateAndWriteContent(webDavSession, sFolderPath, function(oAsyncResult) {
            QUnit.start();

            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of create and write file request');
            test.strictEqual(oAsyncResult.Result.DisplayName, 'myfile.txt', 'Check created file name');

            QUnit.stop();
            webDavSession.OpenFileAsync(sFilePath, null, function(oAsyncResult) {
                QUnit.start();

                var oFile = oAsyncResult.Result;

                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get file request');
                test.strictEqual(oAsyncResult.Result.DisplayName, 'myfile.txt', 'Check loaded file name');

                QUnit.stop();
                oAsyncResult.Result.ReadContentAsync(null, null, function(oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of read content request');
                    test.strictEqual(oAsyncResult.Result, 'Hello World!', 'Check content string');

                    QUnit.stop();
                    ITHit.WebDAV.Client.Tests.HierarchyItems.CreateFile.OnlyWriteContent(webDavSession, sFilePath, function(oAsyncResult) {
                        QUnit.start();

                        test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of write content request');

                        QUnit.stop();
                        ITHit.WebDAV.Client.Tests.HierarchyItems.CreateFile.ReadContent(webDavSession, sFilePath, function(oAsyncResult) {
                            QUnit.start();

                            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of read content request (after change content)');
                            test.strictEqual(oAsyncResult.Result, 'Goodbye!', 'Check updated content');

                            QUnit.stop();
                            ITHit.WebDAV.Client.Tests.HierarchyItems.CreateFile.ReadContentRange(oFile, function(oAsyncResult) {
                                QUnit.start();

                                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of read range content request');
                                test.strictEqual(oAsyncResult.Result, 'bye', 'Check range get content');

                            });
                        });
                    });
                });
            });
        });
    });
});
