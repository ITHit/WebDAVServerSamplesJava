/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyItems.Delete');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyItems.Delete
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyItems.Delete', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyItems.Delete */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/my_folder/']
     * @param {function} [fCallback=function() {}]
     */
    Delete: function(webDavSession, sFolderAbsolutePath, fCallback) {
        webDavSession.OpenFolderAsync(sFolderAbsolutePath, null, function(oAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oAsyncResult.Result;

            oFolder.DeleteAsync(null, function(oAsyncResult) {

                if (oAsyncResult.IsSuccess) {
                    console.log('Folder successfully deleted.');
                } else if (oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.WebDavHttpException) {
                    var sErrorText = oAsyncResult.Error.Message + ' ' + oAsyncResult.Error.Status.Code + ' ' +
                        oAsyncResult.Error.Status.Description;

                    // Find which items failed to delete.
                    for(var i = 0, l = oAsyncResult.Error.Multistatus.Responses.length; i < l; i++) {
                        var oResponse = oAsyncResult.Error.Multistatus.Responses[i];
                        sErrorText += '\n' + oResponse.Href + ' ' + oResponse.Status.Code + ' ' +
                        oResponse.Status.Description;
                    }

                    console.log('Delete error: ' + sErrorText);
                } else {
                    console.log('Delete error: ' + String(oAsyncResult.Error));
                }

                fCallback(oAsyncResult);
            });
        });
    }

});

QUnitRunner.test('Delete folder and check it not found', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyItems/Delete/my_folder/'
    ], function() {
        QUnit.start();

        QUnit.stop();
        ITHit.WebDAV.Client.Tests.HierarchyItems.Delete.Delete(webDavSession, Helper.GetAbsolutePath('HierarchyItems/Delete/my_folder/'), function(oAsyncResult) {
            QUnit.start();

            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of delete request');

            QUnit.stop();
            webDavSession.OpenFolderAsync(Helper.GetAbsolutePath('HierarchyItems/Delete/my_folder/'), null, function(oAsyncResult) {
                QUnit.start();

                test.strictEqual(oAsyncResult.IsSuccess, false, 'Check failure request on get deleted folder');
                test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.NotFoundException, true, 'Check error is NotFoundException');

            });
        });
    });
});
