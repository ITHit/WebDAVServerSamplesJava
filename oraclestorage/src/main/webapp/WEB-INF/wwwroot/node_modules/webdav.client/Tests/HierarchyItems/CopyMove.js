/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyItems.CopyMove');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyItems.CopyMove
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyItems.CopyMove', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyItems.CopyMove */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/Products/']
     * @param {string} [sFileAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    Copy: function(webDavSession, sFolderAbsolutePath, sFileAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFileAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            webDavSession.OpenFolderAsync(sFolderAbsolutePath, null, function(oFolderAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
                var oFolder = oFolderAsyncResult.Result;

                oFile.CopyToAsync(oFolder, 'myproduct.txt', true, null, null, function(oAsyncResult) {

                    if (oAsyncResult.IsSuccess) {
                        console.log('Copy successfully completed.');
                    } else if (oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.PreconditionFailedException) {
                        console.log('The item with such name exists and `overwrite` was `false`.');
                    } else if (oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.WebDavHttpException) {
                        var sErrorText = oAsyncResult.Error.Message + ' ' + oAsyncResult.Error.Status.Code + ' ' +
                            oAsyncResult.Error.Status.Description;

                        // Find which items failed to copy.
                        for(var i = 0, l = oAsyncResult.Error.Multistatus.Responses.length; i < l; i++) {
                            var oResponse = oAsyncResult.Error.Multistatus.Responses[i];
                            sErrorText += '\n' + oResponse.Href + ' ' + oResponse.Status.Code + ' ' +
                                oResponse.Status.Description;
                        }

                        console.log('Copy error: ' + sErrorText);
                    } else {
                        console.log('Copy error: ' + String(oAsyncResult.Error));
                    }

                    fCallback(oAsyncResult);
                });
            });
        });
    },

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sDestinationFolderAbsolutePath='http://localhost:87654/Sales/']
     * @param {string} [sSourceFolderAbsolutePath='http://localhost:87654/Products/']
     * @param {function} [fCallback=function() {}]
     */
    Move: function(webDavSession, sDestinationFolderAbsolutePath, sSourceFolderAbsolutePath, fCallback) {
        webDavSession.OpenFolderAsync(sSourceFolderAbsolutePath, null, function(oSourceFolderAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.Folder} oSourceFolder */
            var oSourceFolder = oSourceFolderAsyncResult.Result;

            webDavSession.OpenFolderAsync(sDestinationFolderAbsolutePath, null, function(oDestinationFolderAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.Folder} oDestinationFolder */
                var oDestinationFolder = oDestinationFolderAsyncResult.Result;

                oSourceFolder.MoveToAsync(oDestinationFolder, oSourceFolder.DisplayName, false, null, function(oAsyncResult) {

                    if (oAsyncResult.IsSuccess) {
                        console.log('Move successfully completed.');
                    } else if (oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.PreconditionFailedException) {
                        console.log('The item with such name exists and `overwrite` was `false`.');
                    } else if (oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.WebDavHttpException) {
                        var sErrorText = oAsyncResult.Error.Message + ' ' + oAsyncResult.Error.Status.Code + ' ' +
                            oAsyncResult.Error.Status.Description;

                        // Find which items failed to move.
                        for(var i = 0, l = oAsyncResult.Error.Multistatus.Responses.length; i < l; i++) {
                            var oResponse = oAsyncResult.Error.Multistatus.Responses[i];
                            sErrorText += '\n' + oResponse.Href + ' ' + oResponse.Status.Code + ' ' +
                            oResponse.Status.Description;
                        }

                        console.log('Move error: ' + sErrorText);
                    } else {
                        console.log('Move error: ' + String(oAsyncResult.Error));
                    }

                    fCallback(oAsyncResult);
                });
            });
        });
    }

});

QUnitRunner.test('Copy file and check error on duplicate', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/Copy/Products/',
        'HierarchyItems/Copy/Duplicate/myfile.txt',
        'HierarchyItems/Copy/myfile.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        ITHit.WebDAV.Client.Tests.HierarchyItems.CopyMove.Copy(webDavSession, Helper.GetAbsolutePath('HierarchyItems/Copy/Products/'), Helper.GetAbsolutePath('HierarchyItems/Copy/myfile.txt'), function(oAsyncResult) {
            QUnit.start();

            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of copy request');

            QUnit.stop();
            ITHit.WebDAV.Client.Tests.HierarchyItems.CopyMove.Copy(webDavSession, Helper.GetAbsolutePath('HierarchyItems/Copy/Products/'), Helper.GetAbsolutePath('HierarchyItems/Copy/Duplicate/myfile.txt'), function(oAsyncResult) {
                QUnit.start();

                test.strictEqual(oAsyncResult.IsSuccess, false, 'Check failed copy request on duplicate');
                test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.PreconditionFailedException, true, 'Check error is PreconditionFailedException on duplicate copy request');
            });
        });
    });
});

QUnitRunner.test('Copy folder', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/CopyFolder/Destination/',
        'HierarchyItems/CopyFolder/Source/file.txt',
        'HierarchyItems/CopyFolder/Source/file2.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/CopyFolder/Destination/'), null, function(oAsyncResult) {
            QUnit.start();

            /** @typedef {ITHit.WebDAV.Client.Folder} oDestinationFolder */
            var oDestinationFolder = oAsyncResult.Result;

            QUnit.stop();
            webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/CopyFolder/Source/'), null, function(oAsyncResult) {
                QUnit.start();

                /** @typedef {ITHit.WebDAV.Client.Folder} oSourceFolder */
                var oSourceFolder = oAsyncResult.Result;

                QUnit.stop();
                oSourceFolder.CopyToAsync(oDestinationFolder, oSourceFolder.DisplayName + '/', true, null, null, function(oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of copy folder request');

                    QUnit.stop();
                    webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/CopyFolder/Destination/Source/'), null, function(oAsyncResult) {
                        QUnit.start();

                        test.strictEqual(oAsyncResult.Result.DisplayName, 'Source', 'Check new copied folder is `Source`');
                    });
                });
            });
        });
    });
});

QUnitRunner.test('Check Forbidden exception on copy to self', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/CopyForbidden/'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/CopyForbidden/'), null, function(oAsyncResult) {
            QUnit.start();

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oAsyncResult.Result;

            QUnit.stop();
            oFolder.CopyToAsync(oFolder, oFolder.DisplayName + '/', true, null, null, function(oAsyncResult) {
                QUnit.start();

                test.strictEqual(oAsyncResult.IsSuccess, false, 'Check failed copy request on copy to self');
                test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.ForbiddenException, true, 'Check error is ForbiddenException');
            });
        });
    });
});

QUnitRunner.test('Check NotFound exception on copy removed file', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/CopyNotFound/res.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/'), null, function(oAsyncResult) {
            QUnit.start();

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oAsyncResult.Result;

            QUnit.stop();
            webDavSession.OpenFileAsync(Helper.GetAbsolutePath('HierarchyItems/CopyNotFound/res.txt'), null, function(oAsyncResult) {
                QUnit.start();

                /** @typedef {ITHit.WebDAV.Client.File} oFile */
                var oFile = oAsyncResult.Result;

                QUnit.stop();
                oFile.DeleteAsync(null, function(oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of delete request');

                    QUnit.stop();
                    oFile.CopyToAsync(oFolder, oFile.DisplayName + '/', true, null, null, function(oAsyncResult) {
                        QUnit.start();

                        test.strictEqual(oAsyncResult.IsSuccess, false, 'Check failed request on copy removed file');
                        test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.NotFoundException, true, 'Check error is NotFoundException');
                    });
                });
            });
        });
    });
});

QUnitRunner.test('Move folder and check already exists error', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/Move/Products/',
        'HierarchyItems/Move/Duplicate/Sales/',
        'HierarchyItems/Move/Sales/'
    ], function() {
        QUnit.start();

        QUnit.stop();
        ITHit.WebDAV.Client.Tests.HierarchyItems.CopyMove.Move(webDavSession, Helper.GetAbsolutePath('HierarchyItems/Move/Products/'), Helper.GetAbsolutePath('HierarchyItems/Move/Sales/'), function(oAsyncResult) {
            QUnit.start();

            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of move request');

            QUnit.stop();
            ITHit.WebDAV.Client.Tests.HierarchyItems.CopyMove.Move(webDavSession, Helper.GetAbsolutePath('HierarchyItems/Move/Products/'), Helper.GetAbsolutePath('HierarchyItems/Move/Duplicate/Sales/'), function(oAsyncResult) {
                QUnit.start();

                test.strictEqual(oAsyncResult.IsSuccess, false, 'Check failed move request on already exists folder');
                test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.PreconditionFailedException, true, 'Check error is PreconditionFailedException');
            });
        });
    });
});

QUnitRunner.test('Move folder', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/MoveFolder/Destination/',
        'HierarchyItems/MoveFolder/Source/file.txt',
        'HierarchyItems/MoveFolder/Source/file2.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/MoveFolder/Destination/'), null, function(oAsyncResult) {
            QUnit.start();

            /** @typedef {ITHit.WebDAV.Client.Folder} oDestinationFolder */
            var oDestinationFolder = oAsyncResult.Result;

            QUnit.stop();
            webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/MoveFolder/Source/'), null, function(oAsyncResult) {
                QUnit.start();

                /** @typedef {ITHit.WebDAV.Client.Folder} oSourceFolder */
                var oSourceFolder = oAsyncResult.Result;

                QUnit.stop();
                oSourceFolder.MoveToAsync(oDestinationFolder, oSourceFolder.DisplayName + '/', true, null, function(oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of move request');

                    QUnit.stop();
                    webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/MoveFolder/Destination/Source/'), null, function(oAsyncResult) {
                        QUnit.start();

                        test.strictEqual(oAsyncResult.Result.DisplayName, 'Source', 'Check moved folder name is `Source`');
                    });
                });
            });
        });
    });
});

QUnitRunner.test('Check NotFound exception on move removed file', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/MoveNotFound/res.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/'), null, function(oAsyncResult) {
            QUnit.start();

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oAsyncResult.Result;

            QUnit.stop();
            webDavSession.OpenFileAsync(Helper.GetAbsolutePath('HierarchyItems/MoveNotFound/res.txt'), null, function(oAsyncResult) {
                QUnit.start();

                /** @typedef {ITHit.WebDAV.Client.File} oFile */
                var oFile = oAsyncResult.Result;

                QUnit.stop();
                oFile.DeleteAsync(null, function(oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of delete request');

                    QUnit.stop();
                    oFile.MoveToAsync(oFolder, oFile.DisplayName + '/', true, null, function(oAsyncResult) {
                        QUnit.start();

                        test.strictEqual(oAsyncResult.IsSuccess, false, 'Check failed on move not found file');
                        test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.NotFoundException, true, 'Check error is NotFoundException');
                    });
                });
            });
        });
    });
});
