/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyItems.Quota');

QUnitRunner.test('Check quota properties available', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/Quota/'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/Quota/'), null, function(oAsyncResult) {
            QUnit.start();

            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of open folder request');

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oAsyncResult.Result;

            if (oFolder.AvailableBytes === -1 && oFolder.UsedBytes === -1) {
                ITHitTests.skip(test, 'Server does not have quota information.');
                return;
            }

            test.strictEqual(typeof oFolder.AvailableBytes, 'number', 'Check type of property AvailableBytes');
            test.strictEqual(typeof oFolder.UsedBytes, 'number', 'Check type of property UsedBytes');

            test.ok(oFolder.AvailableBytes >= 0, 'Check AvailableBytes more or equal than zero');
            test.ok(oFolder.UsedBytes >= 0, 'Check UsedBytes more or equal than zero');
        });
    });
});
