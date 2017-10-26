/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyItems.GetParent');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyItems.GetParent
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyItems.GetParent', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyItems.GetParent */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/my_folder/']
     * @param {function} [fCallback=function() {}]
     */
    GetParent: function(webDavSession, sFolderAbsolutePath, fCallback) {
        webDavSession.OpenFolderAsync(sFolderAbsolutePath, null, function(oFolderAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oFolderAsyncResult.Result;

            oFolder.GetParentAsync(null, function(oParentAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.Folder} oParentFolder */
                var oParentFolder = oParentAsyncResult.Result;

                console.log('Parent folder: ' + oParentFolder.DisplayName);

                oParentFolder.GetParentAsync(null, function(oParentParentAsyncResult) {

                    /** @typedef {ITHit.WebDAV.Client.Folder} oParentFolder */
                    var oParentParentFolder = oParentParentAsyncResult.Result;

                    if (oParentParentFolder === null) {
                        console.log('Folder is root!');
                    }

                    fCallback(oParentAsyncResult, oParentParentAsyncResult);
                });
            });
        });
    }

});

QUnitRunner.test('Get parent folder', function (test) {
    QUnit.stop();
    ITHit.WebDAV.Client.Tests.HierarchyItems.GetParent.GetParent(webDavSession, Helper.GetAbsolutePath(''), function(oParentAsyncResult, oParentParentAsyncResult) {
        QUnit.start();

        test.strictEqual(oParentAsyncResult.IsSuccess, true, 'Check success of first get parent folder request');
        test.strictEqual(oParentAsyncResult.Result instanceof ITHit.WebDAV.Client.Folder, true, 'Check result is instance of Folder');
        test.strictEqual(oParentParentAsyncResult.IsSuccess, true, 'Check success of second get parent folder request');

        //test.strictEqual(oParentParentAsyncResult.Result, null, 'Check result is null - root folder'); // Root folder can be is folder
    });
});
