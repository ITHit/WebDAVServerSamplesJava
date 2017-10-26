/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('Locks.RefreshLock');

/**
 * @class ITHit.WebDAV.Client.Tests.Locks.RefreshLock
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.Locks.RefreshLock', null, {}, /** @lends ITHit.WebDAV.Client.Tests.Locks.RefreshLock */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFileAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    RefreshLock: function(webDavSession, sFileAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFileAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            oFile.LockAsync(ITHit.WebDAV.Client.LockScope.Shared, false, 'User 1', 60, function(oLockAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.LockInfo} oLockInfo */
                var oLockInfo = oLockAsyncResult.Result;

                if (oLockAsyncResult.IsSuccess) {
                    console.log('Lock timeout is: ' + oLockInfo.TimeOut);
                }

                oFile.RefreshLockAsync(oLockInfo.LockToken.toString(), 300, function(oRefreshAsyncResult) {

                    /** @typedef {ITHit.WebDAV.Client.LockInfo} oLockInfo */
                    var oRefreshedLockInfo = oRefreshAsyncResult.Result;

                    if (oRefreshAsyncResult.IsSuccess) {
                        console.log('Lock timeout is: ' + oRefreshedLockInfo.TimeOut);
                    }

                    fCallback(oLockAsyncResult, oRefreshAsyncResult);
                });
            });
        });
    }

});

QUnitRunner.test('Refresh lock on a file', function (test) {
    QUnit.stop();
    Helper.Create([
        'Locks/refreshlockfile.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenFileAsync(Helper.GetAbsolutePath('Locks/refreshlockfile.txt'), null, function(oAsyncResult) {
            QUnit.start();

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oAsyncResult.Result;

            if (oFile.SupportedLocks.length === 0) {
                ITHitTests.skip(test, 'Server does not support locks.');
                return;
            }

            QUnit.stop();
            ITHit.WebDAV.Client.Tests.Locks.RefreshLock.RefreshLock(webDavSession, Helper.GetAbsolutePath('Locks/refreshlockfile.txt'), function(oLockAsyncResult, oRefreshAsyncResult) {
                QUnit.start();

                test.strictEqual(oLockAsyncResult.IsSuccess, true, 'Check success of lock request');
                test.strictEqual(oLockAsyncResult.Result.TimeOut, 60, 'Check previous lock timeout');
                test.strictEqual(oRefreshAsyncResult.IsSuccess, true, 'Check success of refresh lock request');
                test.strictEqual(oRefreshAsyncResult.Result.TimeOut, 300, 'Check current lock timeout');

                QUnit.stop();
                webDavSession.OpenFileAsync(Helper.GetAbsolutePath('Locks/refreshlockfile.txt'), null, function(oAsyncResult) {
                    QUnit.start();

                    /** @typedef {ITHit.WebDAV.Client.File} oFile */
                    var oFile = oAsyncResult.Result;

                    QUnit.stop();
                    oFile.UnlockAsync(oRefreshAsyncResult.Result.LockToken.toString(), function(oAsyncResult) {
                        QUnit.start();

                        test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of unlock request');
                    });
                });
            });
        });
    });
});
