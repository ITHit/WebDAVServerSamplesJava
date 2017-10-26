/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('Locks.CopyLock');

QUnitRunner.test('Copy locked file', function (test) {
    Helper.Create([
        'Locks/CopyLock/',
        'Locks/CopyLock/copyfile.txt'
    ], QUnitRunner.async(function(oRootFolder, oFile) {
        if (oFile.SupportedLocks.length === 0) {
            ITHitTests.skip(test, 'Server does not support locks.');
            return;
        }

        oFile.LockAsync(ITHit.WebDAV.Client.LockScope.Shared, false, 'User 1', -1, QUnitRunner.async(function(oAsyncResult) {
            var sLockToken = oAsyncResult.Result.LockToken.LockToken;

            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of lock file');

            oFile.CopyToAsync(oRootFolder, 'copyfile_2.txt', false, false, sLockToken, QUnitRunner.async(function (oAsyncResult) {

                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of copy file');

                webDavSession.OpenFileAsync(Helper.GetAbsolutePath('Locks/CopyLock/copyfile_2.txt'), null, QUnitRunner.async(function (oAsyncResult) {
                    /** @typedef {ITHit.WebDAV.Client.File} oNewFile */
                    var oNewFile = oAsyncResult.Result;

                    test.strictEqual(oNewFile.ActiveLocks.length, 0, 'Verify that locks did not copy');

                }));
            }));
        }));
    }));
});

QUnitRunner.test('Copy locked folder', function (test) {
    Helper.Create([
        'Locks/CopyLock/',
        'Locks/CopyLock/copyfolder/'
    ], QUnitRunner.async(function(oRootFolder, oFolder) {
        if (oFolder.SupportedLocks.length === 0) {
            ITHitTests.skip(test, 'Server does not support locks.');
            return;
        }

        oFolder.LockAsync(ITHit.WebDAV.Client.LockScope.Shared, false, 'User 1', -1, QUnitRunner.async(function(oAsyncResult) {
            var sLockToken = oAsyncResult.Result.LockToken.LockToken;

            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of lock folder');

            oFolder.CopyToAsync(oRootFolder, 'copyfolder_2', false, false, sLockToken, QUnitRunner.async(function (oAsyncResult) {

                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of copy folder');

                webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('Locks/CopyLock/copyfolder_2/'), null, QUnitRunner.async(function (oAsyncResult) {
                    /** @typedef {ITHit.WebDAV.Client.Folder} oNewFolder */
                    var oNewFolder = oAsyncResult.Result;

                    test.strictEqual(oNewFolder.ActiveLocks.length, 0, 'Verify that locks did not copy');

                }));
            }));
        }));
    }));
});
