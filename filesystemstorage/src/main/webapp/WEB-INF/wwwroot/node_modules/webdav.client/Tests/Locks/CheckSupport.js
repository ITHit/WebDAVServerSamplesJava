/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('Locks.CheckSupport');

/**
 * @class ITHit.WebDAV.Client.Tests.Locks.CheckSupport
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.Locks.CheckSupport', null, {}, /** @lends ITHit.WebDAV.Client.Tests.Locks.CheckSupport */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFileAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    CheckLockSupport: function(webDavSession, sFileAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFileAbsolutePath, null, function(oAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oAsyncResult.Result;

            if (oFile.SupportedLocks.length === 0) {
                console.log('Locks are not supported.');
            }

            for (var i = 0, l = oFile.SupportedLocks.length; i < l; i++) {
                if (oFile.SupportedLocks[i] === ITHit.WebDAV.Client.LockScope.Exclusive) {
                    console.log('Item supports exclusive locks.');
                }
                if (oFile.SupportedLocks[i] === ITHit.WebDAV.Client.LockScope.Shared) {
                    console.log('Item supports shared locks.');
                }
            }

            fCallback(oAsyncResult);
        });
    }

});

QUnitRunner.test('Check locks types support', function (test) {
    QUnit.stop();
    Helper.Create([
        'Locks/myfile.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        ITHit.WebDAV.Client.Tests.Locks.CheckSupport.CheckLockSupport(webDavSession, Helper.GetAbsolutePath('Locks/myfile.txt'), function(oAsyncResult) {
            QUnit.start();

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oAsyncResult.Result;

            if (oFile.SupportedLocks.length === 0) {
                ITHitTests.skip(test, 'Server does not support locks.');
                return;
            }

            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of open item request');
            test.strictEqual(oAsyncResult.Result.SupportedLocks[0], ITHit.WebDAV.Client.LockScope.Exclusive, 'Check item is supported Exclusive lock');
            test.strictEqual(oAsyncResult.Result.SupportedLocks[1], ITHit.WebDAV.Client.LockScope.Shared, 'Check item is supported Shared lock');
        });
    });
});
