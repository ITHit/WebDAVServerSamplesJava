/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyItems.NameCheck');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck */{

    CheckFileName: function(test, sSymbol) {
        QUnit.stop();
        Helper.Create([
            'HierarchyItems/NameCheck/'
        ], function() {
            QUnit.start();

            QUnit.stop();
            Helper.GetFolder(Helper.GetPath('HierarchyItems/NameCheck/').substr(1), function(oRootFolder) {
                QUnit.start();

                var sName = 'sym' + sSymbol + 'bol.txt';

                QUnit.stop();
                oRootFolder.CreateFileAsync(sName, null, 'my content', null, function(oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of create file with symbol: ' + sSymbol);
					if (!oAsyncResult.IsSuccess) {
						return;
					}

                    /** @typedef {ITHit.WebDAV.Client.File} oFile */
                    var oFile = oAsyncResult.Result;

                    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck._CheckItemName(test, oRootFolder, oFile, sSymbol);
                });
            });
        });
    },

    CheckFolderName: function(test, sSymbol) {
        QUnit.stop();
        Helper.Create([
            'HierarchyItems/NameCheck/'
        ], function() {
            QUnit.start();

            QUnit.stop();
            Helper.GetFolder(Helper.GetPath('HierarchyItems/NameCheck/').substr(1), function(oRootFolder) {
                QUnit.start();

                var sName = 'sym' + sSymbol + 'bol';

                QUnit.stop();
                oRootFolder.CreateFolderAsync(sName, null, null, function(oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of create folder with symbol: ' + sSymbol);

                    /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
                    var oFolder = oAsyncResult.Result;

                    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck._CheckItemName(test, oRootFolder, oFolder, sSymbol);
                });
            });
        });
    },

    _CheckItemName: function (test, oRootFolder, oItem, sSymbol) {
        if (oItem) {
            QUnit.stop();
            oItem.CopyToAsync(oRootFolder, 'copy' + oItem.DisplayName, false, false, null, function (oAsyncResult) {
                QUnit.start();

                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of copy request, symbol: ' + sSymbol);

                QUnit.stop();
                oRootFolder.GetItemAsync('copy' + oItem.DisplayName, function (oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get copied item request, symbol: ' + sSymbol);

                    /** @typedef {ITHit.WebDAV.Client.Folder} oCopiedItem */
                    var oCopiedItem = oAsyncResult.Result;
                    test.strictEqual(oCopiedItem.DisplayName, 'copy' + oItem.DisplayName, 'Check copied item name, symbol: ' + sSymbol);
                    if (oCopiedItem instanceof ITHit.WebDAV.Client.File) {
                        test.strictEqual(oCopiedItem.ContentLength, 10, 'Check copied item length, symbol: ' + sSymbol);
                    }

                    QUnit.stop();
                    oCopiedItem.MoveToAsync(oRootFolder, 'move' + oItem.DisplayName, true, null, function (oAsyncResult) {
                        QUnit.start();

                        test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of move request, symbol: ' + sSymbol);

                        QUnit.stop();
                        oRootFolder.GetItemAsync('move' + oItem.DisplayName, function (oAsyncResult) {
                            QUnit.start();

                            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get moved item request, symbol: ' + sSymbol);

                            /** @typedef {ITHit.WebDAV.Client.Folder} oMovedItem */
                            var oMovedItem = oAsyncResult.Result;
                            test.strictEqual(oMovedItem.DisplayName, 'move' + oItem.DisplayName, 'Check renamed item name, symbol: ' + sSymbol);
                            if (oCopiedItem instanceof ITHit.WebDAV.Client.File) {
                                test.strictEqual(oCopiedItem.ContentLength, 10, 'Check renamed item length, symbol: ' + sSymbol);
                            }

                            test.strictEqual(oAsyncResult.Result.DisplayName, 'move' + oItem.DisplayName, 'Check renamed item name, symbol: ' + sSymbol);
                        });
                    });
                });
            });
        }
    }

});

// @todo Todo? (moved from old tests): `?*()-_={}[];:'",.|/\<>

QUnitRunner.test('Symbol & in file name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFileName(test, '&');
});
QUnitRunner.test('Symbol # in file name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFileName(test, '#');
});
QUnitRunner.test('Symbol @ in file name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFileName(test, '@');
});
QUnitRunner.test('Symbol ^ in file name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFileName(test, '^');
});
QUnitRunner.test('Symbol ! in file name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFileName(test, '!');
});
QUnitRunner.test('Symbol ~ in file name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFileName(test, '~');
});
QUnitRunner.test('Symbol $ in file name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFileName(test, '$');
});
QUnitRunner.test('Space in file name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFileName(test, 'a b');
});
/*QUnitRunner.test('Symbol % in file', function (test) { // @todo Symbol % is not worked
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFileName(test, '%');
});*/
QUnitRunner.test('Symbol + in file name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFileName(test, '+');
});


QUnitRunner.test('Symbol & in folder name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFolderName(test, '&');
});
QUnitRunner.test('Symbol # in folder name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFolderName(test, '#');
});
QUnitRunner.test('Symbol @ in folder name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFolderName(test, '@');
});
QUnitRunner.test('Symbol ^ in folder name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFolderName(test, '^');
});
QUnitRunner.test('Symbol ! in folder name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFolderName(test, '!');
});
QUnitRunner.test('Symbol ~ in folder name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFolderName(test, '~');
});
QUnitRunner.test('Symbol $ in folder name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFolderName(test, '$');
});
QUnitRunner.test('Space in folder name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFolderName(test, 'a b');
});
/*QUnitRunner.test('Symbol % in folder name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFolderName(test, '%');
});*/
QUnitRunner.test('Symbol + in folder name', function (test) {
    ITHit.WebDAV.Client.Tests.HierarchyItems.NameCheck.CheckFolderName(test, '+');
});
