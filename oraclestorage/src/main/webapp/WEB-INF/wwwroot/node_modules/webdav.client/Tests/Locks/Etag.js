/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('Locks.Etag');

/**
 * @class ITHit.WebDAV.Client.Tests.Locks.Etag
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.Locks.Etag', null, {}, /** @lends ITHit.WebDAV.Client.Tests.Locks.Etag */{

    GetEtag: function (test, sName, fCallback) {
        var sFolderPath = Helper.GetAbsolutePath('Locks/Etag' + sName);
        var sFilePath = Helper.GetAbsolutePath('Locks/Etag' + sName + '/etag.txt');
        var oEtagPropertyName = new ITHit.WebDAV.Client.PropertyName('getetag', ITHit.WebDAV.Client.DavConstants.NamespaceUri);

        var aEtags = [];
        var methodsCount = 4;
        var push = function (method, etag) {
            if (etag === false) {
                methodsCount--;
                console.log('Method ' + method + ' not support for get etag.');
            } else {
                aEtags.push([method, etag]);
            }

            if (aEtags.length === methodsCount) {
                // Check etag equals
                var sEtag = aEtags[0][1];
                var bIsEquals = true;
                for (var i = 0, l = aEtags.length; i < l; i++) {
                    if (sEtag !== aEtags[i][1]) {
                        bIsEquals = false;
                        break;
                    }
                }

                if (!bIsEquals) {
                    for (var i2 = 0, l2 = aEtags.length; i2 < l2; i2++) {
                        console.log(aEtags[i2][0] + ':', aEtags[i2][1]);
                    }
                }

                test.strictEqual(bIsEquals, true, 'Check get tags with different methods');

                fCallback(sEtag);
            }
        };

        // GET
        webDavSession.OpenFileAsync(sFilePath, null, function (oAsyncResult) {
            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oAsyncResult.Result;

            var oRequest = oFile.ReadContentAsync(null, null, function () {
                var etag = oRequest.GetInternalRequests()[0].Response.GetResponseHeader('etag');
                push('GET', etag !== undefined ? etag : false);
            });
        });

        // HEAD
        webDavSession.OpenFolderAsync(sFolderPath, null, function (oAsyncResult) {
            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oAsyncResult.Result;

            var oRequest = oFolder.ItemExistsAsync('etag.txt', function () {
                var etag = oRequest.GetInternalRequests()[0].Response.GetResponseHeader('etag');
                push('HEAD', etag !== undefined ? etag : false);
            });
        });

        // PROPFIND Depth 0
        webDavSession.OpenFileAsync(sFilePath, [oEtagPropertyName], function (oAsyncResult) {
            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oAsyncResult.Result;

            push('PROPFIND Depth 0', oFile.GetProperty(oEtagPropertyName));
        });

        // PROPFIND Depth 1
        webDavSession.OpenFolderAsync(sFolderPath, null, function (oFolderAsyncResult) {
            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oFolderAsyncResult.Result;

            oFolder.GetChildrenAsync(false, [oEtagPropertyName], function (oAsyncResult) {
                /** @typedef {ITHit.WebDAV.Client.File[]} aFiles */
                var aFiles = oAsyncResult.Result;

                push('PROPFIND Depth 1', aFiles[0].GetProperty(oEtagPropertyName));
            });
        });
    }

});

QUnitRunner.test('Lock file, check etag no change', function (test) {
    var sName = 'lock';
    var sFilePath = 'Locks/Etag' + sName + '/etag.txt';
    Helper.Create(sFilePath, QUnitRunner.async(function (oFile) {
        if (oFile.SupportedLocks.length === 0) {
            return ITHitTests.skip(test, 'Server does not support locks.');
        }

        ITHit.WebDAV.Client.Tests.Locks.Etag.GetEtag(test, sName, QUnitRunner.async(function (sEtag) {
            oFile.LockAsync(ITHit.WebDAV.Client.LockScope.Shared, false, 'User 1', -1, QUnitRunner.async(function () {
                ITHit.WebDAV.Client.Tests.Locks.Etag.GetEtag(test, sName, QUnitRunner.async(function (sNewEtag) {

                    test.strictEqual(sEtag, sNewEtag, 'Check that etag did not changed');

                    webDavSession.OpenFileAsync(Helper.GetAbsolutePath(sFilePath), null, QUnitRunner.async(function (oAsyncResult) {
                        /** @typedef {ITHit.WebDAV.Client.File} oFile */
                        var oLockedFile = oAsyncResult.Result;

                        test.strictEqual(String(oFile.LastModified), String(oLockedFile.LastModified), 'Check that LastModified did not changed');
                    }));
                }));
            }));
        }));
    }));
});

QUnitRunner.test('Update file, check etag change', function (test) {
    var sName = 'update';
    var sFilePath = 'Locks/Etag' + sName + '/etag.txt';
    Helper.Create(sFilePath, QUnitRunner.async(function (oFile) {
        if (oFile.SupportedLocks.length === 0) {
            return ITHitTests.skip(test, 'Server does not support locks.');
        }

        ITHit.WebDAV.Client.Tests.Locks.Etag.GetEtag(test, sName, QUnitRunner.async(function (sEtag) {

            oFile.WriteContentAsync('Changes!', null, 'text/plain', QUnitRunner.async(function (oAsyncResult) {

                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of write content to file');

                ITHit.WebDAV.Client.Tests.Locks.Etag.GetEtag(test, sName, QUnitRunner.async(function (sNewEtag) {

                    test.notEqual(sEtag, sNewEtag, 'Check that etag did not changed');

                    webDavSession.OpenFileAsync(Helper.GetAbsolutePath(sFilePath), null, QUnitRunner.async(function (oAsyncResult) {
                        /** @typedef {ITHit.WebDAV.Client.File} oFile */
                        var oLockedFile = oAsyncResult.Result;

                        test.notEqual(String(oFile.LastModified), String(oLockedFile.LastModified), 'Check that etag did not changed');
                    }));
                }));
            }));
        }));
    }));
});

QUnitRunner.test('Lock file, unlock, check etag no change', function (test) {
    var sName = 'lock';
    var sFilePath = 'Locks/Etag' + sName + '/etag.txt';
    Helper.Create(sFilePath, QUnitRunner.async(function (oFile) {
        if (oFile.SupportedLocks.length === 0) {
            return ITHitTests.skip(test, 'Server does not support locks.');
        }

        ITHit.WebDAV.Client.Tests.Locks.Etag.GetEtag(test, sName, QUnitRunner.async(function (sEtag) {
            oFile.LockAsync(ITHit.WebDAV.Client.LockScope.Shared, false, 'User 1', -1, QUnitRunner.async(function (oAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.LockInfo} oLockInfo */
                var oLockInfo = oAsyncResult.Result;

                oFile.UnlockAsync(oLockInfo.LockToken.LockToken, QUnitRunner.async(function () {
                    ITHit.WebDAV.Client.Tests.Locks.Etag.GetEtag(test, sName, QUnitRunner.async(function (sNewEtag) {

                        test.strictEqual(sEtag, sNewEtag, 'Check that etag did not changed');

                        webDavSession.OpenFileAsync(Helper.GetAbsolutePath(sFilePath), null, QUnitRunner.async(function (oAsyncResult) {
                            /** @typedef {ITHit.WebDAV.Client.File} oFile */
                            var oLockedFile = oAsyncResult.Result;

                            test.strictEqual(String(oFile.LastModified), String(oLockedFile.LastModified), 'Check that LastModified did not changed');
                        }));
                    }));
                }));
            }));
        }));
    }));
});

QUnitRunner.test('Add file custom property, check etag no change', function (test) {
    var sName = 'add_property';
    var sFilePath = 'Locks/Etag' + sName + '/etag.txt';
    var oPropertyName = new ITHit.WebDAV.Client.PropertyName('myname', 'mynamespace');
    var oProperty = new ITHit.WebDAV.Client.Property(oPropertyName, 'first value');

    Helper.Create(sFilePath, QUnitRunner.async(function (oFile) {

        ITHit.WebDAV.Client.Tests.Locks.Etag.GetEtag(test, sName, QUnitRunner.async(function (sEtag) {

            oFile.UpdatePropertiesAsync([oProperty], null, null, QUnitRunner.async(function (oAsyncResult) {
                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of add property request');

                ITHit.WebDAV.Client.Tests.Locks.Etag.GetEtag(test, sName, QUnitRunner.async(function (sNewEtag) {
                    test.strictEqual(sEtag, sNewEtag, 'Check that etag did not changed');

                    webDavSession.OpenFileAsync(Helper.GetAbsolutePath(sFilePath), null, QUnitRunner.async(function (oAsyncResult) {
                        /** @typedef {ITHit.WebDAV.Client.File} oFile */
                        var oLockedFile = oAsyncResult.Result;

                        test.strictEqual(String(oFile.LastModified), String(oLockedFile.LastModified), 'Check that LastModified did not changed');
                    }));
                }));
            }));
        }));
    }));
});

QUnitRunner.test('Update file custom property, check etag no change', function (test) {
    var sName = 'update_property';
    var sFilePath = 'Locks/Etag' + sName + '/etag.txt';
    var oPropertyName = new ITHit.WebDAV.Client.PropertyName('myname', 'mynamespace');
    var oProperty = new ITHit.WebDAV.Client.Property(oPropertyName, 'Value');

    Helper.Create(sFilePath, QUnitRunner.async(function (oFile) {

        ITHit.WebDAV.Client.Tests.Locks.Etag.GetEtag(test, sName, QUnitRunner.async(function (sEtag) {

            oFile.UpdatePropertiesAsync([oProperty], null, null, QUnitRunner.async(function (oAsyncResult) {
                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of add property request');

                var oUpdatedProperty = new ITHit.WebDAV.Client.Property(oPropertyName, 'Updated value');

                oFile.UpdatePropertiesAsync([oUpdatedProperty], null, null, QUnitRunner.async(function (oAsyncResult) {
                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of add property request');

                    ITHit.WebDAV.Client.Tests.Locks.Etag.GetEtag(test, sName, QUnitRunner.async(function (sNewEtag) {
                        test.strictEqual(sEtag, sNewEtag, 'Check that etag did not changed');

                        webDavSession.OpenFileAsync(Helper.GetAbsolutePath(sFilePath), null, QUnitRunner.async(function (oAsyncResult) {
                            /** @typedef {ITHit.WebDAV.Client.File} oFile */
                            var oLockedFile = oAsyncResult.Result;

                            test.strictEqual(String(oFile.LastModified), String(oLockedFile.LastModified), 'Check that LastModified did not changed');
                        }));
                    }));
                }));
            }));
        }));
    }));
});


QUnitRunner.test('Delete file custom property, check etag no change', function (test) {
    var sName = 'delete_property';
    var sFilePath = 'Locks/Etag' + sName + '/etag.txt';
    var oPropertyName = new ITHit.WebDAV.Client.PropertyName('myname', 'mynamespace');
    var oProperty = new ITHit.WebDAV.Client.Property(oPropertyName, 'Value');

    Helper.Create(sFilePath, QUnitRunner.async(function (oFile) {

        ITHit.WebDAV.Client.Tests.Locks.Etag.GetEtag(test, sName, QUnitRunner.async(function (sEtag) {

            oFile.UpdatePropertiesAsync([oProperty], null, null, QUnitRunner.async(function (oAsyncResult) {
                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of add property request');

                oFile.UpdatePropertiesAsync(null, [oPropertyName], null, QUnitRunner.async(function (oAsyncResult) {
                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of add property request');

                    ITHit.WebDAV.Client.Tests.Locks.Etag.GetEtag(test, sName, QUnitRunner.async(function (sNewEtag) {
                        test.strictEqual(sEtag, sNewEtag, 'Check that etag did not changed');

                        webDavSession.OpenFileAsync(Helper.GetAbsolutePath(sFilePath), null, QUnitRunner.async(function (oAsyncResult) {
                            /** @typedef {ITHit.WebDAV.Client.File} oFile */
                            var oLockedFile = oAsyncResult.Result;

                            test.strictEqual(String(oFile.LastModified), String(oLockedFile.LastModified), 'Check that LastModified did not changed');
                        }));
                    }));
                }));
            }));
        }));
    }));
});