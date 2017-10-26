/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('Locks.GetLocks');

/**
 * @class ITHit.WebDAV.Client.Tests.Locks.GetLocks
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.Locks.GetLocks', null, {}, /** @lends ITHit.WebDAV.Client.Tests.Locks.GetLocks */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFileAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    GetList: function(webDavSession, sFileAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFileAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            // Infinite lock
            oFile.LockAsync(ITHit.WebDAV.Client.LockScope.Shared, false, "User 1", -1, function(oInfiniteLockAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.LockInfo} oInfiniteLockInfo */
                var oInfiniteLockInfo = oInfiniteLockAsyncResult.Result;

                // Minute lock
                oFile.LockAsync(ITHit.WebDAV.Client.LockScope.Shared, false, "User 2", 60, function(oMinuteLockAsyncResult) {

                    /** @typedef {ITHit.WebDAV.Client.LockInfo} oMinuteLockInfo */
                    var oMinuteLockInfo = oMinuteLockAsyncResult.Result;

                    // Refresh item from server to read locks
                    oFile.RefreshAsync(function(oAsyncResult) {

                        for (var i = 0, l = oFile.ActiveLocks.length; i < l; i++) {

                            /** @typedef {ITHit.WebDAV.Client.LockInfo} oLockInfo */
                            var oLockInfo = oFile.ActiveLocks[i];
                            var sTimeOut = oLockInfo.TimeOut === -1 ? "Infinite" : oLockInfo.TimeOut + ' sec';

                            // Show item locks
                            console.log([
                                oLockInfo.Owner,
                                oLockInfo.LockToken.Href,
                                oLockInfo.LockToken.LockToken,
                                oLockInfo.LockScope,
                                oLockInfo.Deep,
                                sTimeOut
                            ].join(' '));
                        }

                        fCallback(oFile, oInfiniteLockInfo, oMinuteLockInfo);
                    });

                });
            });
        });
    }

});

QUnitRunner.test('Lock file, refresh and get it locks list', function (test) {
    QUnit.stop();
    Helper.Create([
        'Locks/getlockfile.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        ITHit.WebDAV.Client.Tests.Locks.GetLocks.GetList(webDavSession, Helper.GetAbsolutePath('Locks/getlockfile.txt'), function(oFile, oInfiniteLockInfo, oMinuteLockInfo) {
            QUnit.start();

            if (oFile.SupportedLocks.length === 0) {
                ITHitTests.skip(test, 'Server does not support locks.');
                return;
            }

            var oLocalInfiniteLockInfo = null;
            var oLocalMinuteLockInfo = null;
            for (var i = 0, l = oFile.ActiveLocks.length; i < l; i++) {
                if (oFile.ActiveLocks[i].LockToken.toString() === oInfiniteLockInfo.LockToken.toString()) {
                    oLocalInfiniteLockInfo = oFile.ActiveLocks[i];
                }
                if (oFile.ActiveLocks[i].LockToken.toString() === oMinuteLockInfo.LockToken.toString()) {
                    oLocalMinuteLockInfo = oFile.ActiveLocks[i];
                }
            }

            test.strictEqual(oFile.ActiveLocks.length, 2, 'Check active locks length');
            test.strictEqual(oLocalInfiniteLockInfo.Owner, 'User 1', 'Infinite lock: check owner');
            test.strictEqual(oLocalInfiniteLockInfo.LockToken.Href, Helper.GetAbsolutePath('Locks/getlockfile.txt'), 'Infinite lock: check href');
            test.strictEqual(oLocalInfiniteLockInfo.LockToken.LockToken, oInfiniteLockInfo.LockToken.toString(), 'Infinite lock: check token');
            test.strictEqual(oLocalInfiniteLockInfo.LockToken.toString(), oInfiniteLockInfo.LockToken.toString(), 'Infinite lock: check token by toString()');
            test.strictEqual(oLocalMinuteLockInfo.Owner, 'User 2', 'Minute lock: check owner');
            test.strictEqual(oLocalMinuteLockInfo.LockToken.Href, Helper.GetAbsolutePath('Locks/getlockfile.txt'), 'Minute lock: check href');
            test.strictEqual(oLocalMinuteLockInfo.LockToken.LockToken, oMinuteLockInfo.LockToken.toString(), 'Minute lock: check token');
            test.strictEqual(oLocalMinuteLockInfo.LockToken.toString(), oMinuteLockInfo.LockToken.toString(), 'Minute lock: check token by toString()');

            QUnit.stop();
            oFile.UnlockAsync(oInfiniteLockInfo.LockToken, function(oAsyncResult) {
                QUnit.start();

                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of unlock first lock request');

                QUnit.stop();
                oFile.UnlockAsync(oMinuteLockInfo.LockToken, function(oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of unlock second lock request');
                });
            });
        });
    });
});
