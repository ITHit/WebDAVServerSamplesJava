/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('Locks.Lock');

/**
 * @class ITHit.WebDAV.Client.Tests.Locks.Lock
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.Locks.Lock', null, {}, /** @lends ITHit.WebDAV.Client.Tests.Locks.Lock */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFileAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    SetLock: function(webDavSession, sFileAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFileAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            if (oFile.SupportedLocks.length === 0) {
                console.log('Locks are not supported.');
                return;
            }

            oFile.LockAsync(ITHit.WebDAV.Client.LockScope.Shared, false, 'User 1', -1, function(oAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.LockInfo} oLockInfo */
                var oLockInfo = oAsyncResult.Result;

                if (oAsyncResult.IsSuccess) {
                    console.log('Locks token is: ' + oLockInfo.LockToken.LockToken);
                }

                fCallback(oAsyncResult);
            });
        });
    },

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFileAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {string} [sLockToken='f36726d1-0671-4f6f-8445-c7d10e42a08e']
     * @param {function} [fCallback=function() {}]
     */
    SetUnLock: function(webDavSession, sFileAbsolutePath, sLockToken, fCallback) {
        webDavSession.OpenFileAsync(sFileAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            oFile.UnlockAsync(sLockToken, function(oAsyncResult) {

                if (oAsyncResult.IsSuccess) {
                    console.log('File is unlocked');
                }

                fCallback(oAsyncResult);
            });
        });
    }

});

QUnitRunner.test('Lock file and check that it can not moving', function (test) {
    QUnit.stop();
    Helper.Create([
        'Locks/MoveDir/',
        'Locks/lockfile.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenFileAsync(Helper.GetAbsolutePath('Locks/lockfile.txt'), null, function(oAsyncResult) {
            QUnit.start();

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oAsyncResult.Result;

            if (oFile.SupportedLocks.length === 0) {
                ITHitTests.skip(test, 'Server does not support locks.');
                return;
            }

            QUnit.stop();
            webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('Locks/MoveDir/'), null, function(oAsyncResult) {
                QUnit.start();

                /** @typedef {ITHit.WebDAV.Client.Folder} oMoveFolder */
                var oMoveFolder = oAsyncResult.Result;

                QUnit.stop();
                ITHit.WebDAV.Client.Tests.Locks.Lock.SetLock(webDavSession, Helper.GetAbsolutePath('Locks/lockfile.txt'), function(oAsyncResult) {
                    QUnit.start();

                    /** @typedef {ITHit.WebDAV.Client.LockInfo} oLockInfo */
                    var oLockInfo = oAsyncResult.Result;

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of set lock request');
                    test.strictEqual(oLockInfo instanceof ITHit.WebDAV.Client.LockInfo, true, 'Check result is instance of LockInfo');
                    test.strictEqual(oLockInfo.Deep, false, 'Check lock info deep is false');
                    test.strictEqual(oLockInfo.LockScope, ITHit.WebDAV.Client.LockScope.Shared, 'Check lock is shared');
                    test.strictEqual(oLockInfo.LockToken.Href, Helper.GetAbsolutePath('Locks/lockfile.txt'), 'Check lock href');

                    // Try move
                    QUnit.stop();
                    oFile.MoveToAsync(oMoveFolder, oFile.DisplayName, false, null, function(oAsyncResult) {
                        QUnit.start();

                        test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.LockedException, true, 'Check error is LockedException on try move file without lock token');

                        // Try move with lock
                        QUnit.stop();
                        oFile.MoveToAsync(oMoveFolder, oFile.DisplayName, false, [oLockInfo], function(oAsyncResult) {
                            QUnit.start();

                            // Try move
                            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of move file request with lock token');

                            QUnit.stop();
                            ITHit.WebDAV.Client.Tests.Locks.Lock.SetUnLock(webDavSession, Helper.GetAbsolutePath('Locks/MoveDir/lockfile.txt'), oLockInfo.LockToken.LockToken, function(oAsyncResult) {
                                QUnit.start();

                                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of unlock request');
                            });
                        });
                    });
                });
            });
        });
    });
});
