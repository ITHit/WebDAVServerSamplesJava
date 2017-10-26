/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyProperties.UpdateProperties');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyProperties.UpdateProperties
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyProperties.UpdateProperties', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyProperties.UpdateProperties */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFileAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    Update: function(webDavSession, sFileAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFileAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            var oPropertyName = new ITHit.WebDAV.Client.PropertyName('myname', 'mynamespace');
            var oProperty = new ITHit.WebDAV.Client.Property(oPropertyName, 'Test value');

            oFile.UpdatePropertiesAsync([oProperty], null, null, function(oAsyncResult) {

                if (oAsyncResult.IsSuccess) {
                    console.log('Property `mynamespace:myname` successfully added to file!');
                } else if (oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.PropertyException) {
                    var sErrorText = oAsyncResult.Error.Message + ' ' + oAsyncResult.Error.Status.Code + ' ' +
                        oAsyncResult.Error.Status.Description;

                    // Find which properties failed to add/update/delete.
                    for(var i = 0, l = oAsyncResult.Error.Multistatus.Responses.length; i < l; i++) {
                        var oResponse = oAsyncResult.Error.Multistatus.Responses[i];
                        sErrorText += '\n' + oResponse.PropertyName.NamespaceUri + ':' + oResponse.PropertyName.Name + ' ' +
                            oResponse.Status.Code + ' ' + oResponse.Status.Description;
                    }

                    console.log('Update properties error: ' + sErrorText);
                } else {
                    console.log('Update properties error: ' + String(oAsyncResult.Error));
                }

                fCallback(oAsyncResult);
            });
        });
    },

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFileAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    Delete: function(webDavSession, sFileAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFileAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            var oPropertyName = new ITHit.WebDAV.Client.PropertyName('myname', 'mynamespace');

            oFile.UpdatePropertiesAsync(null, [oPropertyName], null, function(oAsyncResult) {

                if (oAsyncResult.IsSuccess) {
                    console.log('Property `mynamespace:myname` successfully deleted from file!');
                } else if (oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.PropertyException) {
                    var sErrorText = oAsyncResult.Error.Message + ' ' + oAsyncResult.Error.Status.Code + ' ' +
                        oAsyncResult.Error.Status.Description;

                    // Find which properties failed to add/update/delete.
                    for(var i = 0, l = oAsyncResult.Error.Multistatus.Responses.length; i < l; i++) {
                        var oResponse = oAsyncResult.Error.Multistatus.Responses[i];
                        sErrorText += '\n' + oResponse.PropertyName.NamespaceUri + ':' + oResponse.PropertyName.Name + ' ' +
                        oResponse.Status.Code + ' ' + oResponse.Status.Description;
                    }

                    console.log('Update properties error: ' + sErrorText);
                } else {
                    console.log('Update properties error: ' + String(oAsyncResult.Error));
                }

                fCallback(oAsyncResult);
            });
        });
    }

});

QUnitRunner.test('Add, update and delete file custom properties', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyProperties/updatefile.txt'
    ], function() {
        QUnit.start();

        var sFilePath = Helper.GetAbsolutePath('HierarchyProperties/updatefile.txt');
        var oPropertyName = new ITHit.WebDAV.Client.PropertyName('myname', 'mynamespace');
        var oProperty = new ITHit.WebDAV.Client.Property(oPropertyName, 'first value');

        // Create properties
        QUnit.stop();
        webDavSession.OpenFileAsync(sFilePath, null, function(oAsyncResult) {
            var oFile = oAsyncResult.Result;
            oAsyncResult.Result.UpdatePropertiesAsync([oProperty], null, null, function(oAsyncResult) {
                QUnit.start();

                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of add property request');

                QUnit.stop();
                oFile.GetPropertyValuesAsync([oPropertyName], function(oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get property values request');
                    test.strictEqual(oAsyncResult.Result[0].StringValue(), 'first value', 'Check value of loaded first property');

                    QUnit.stop();
                    ITHit.WebDAV.Client.Tests.HierarchyProperties.UpdateProperties.Update(webDavSession, sFilePath, function(oAsyncResult) {
                        QUnit.start();

                        test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of update property request');

                        QUnit.stop();
                        oFile.GetPropertyValuesAsync([oPropertyName], function(oAsyncResult) {
                            QUnit.start();

                            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get property values request (after update)');
                            test.strictEqual(oAsyncResult.Result[0].StringValue(), 'Test value', 'Check value of loaded first property (after update)');

                            QUnit.stop();
                            ITHit.WebDAV.Client.Tests.HierarchyProperties.UpdateProperties.Delete(webDavSession, sFilePath, function(oAsyncResult) {
                                QUnit.start();

                                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of delete property request');

                                QUnit.stop();
                                oFile.GetPropertyValuesAsync([oPropertyName], function(oAsyncResult) {
                                    QUnit.start();

                                    test.strictEqual(oAsyncResult.IsSuccess, false, 'Check failure on get removed property');
                                    test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.PropertyNotFoundException, true, 'Check error is PropertyNotFoundException');
                                });
                            });
                        });
                    });
                });
            });
        });
    });
});

QUnitRunner.test('Check Property exception when try set properties with DAV: namespace', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyProperties/CheckDavNsRestriction.txt'
    ], function() {
        QUnit.start();

        var propertiesToAdd = [
            new ITHit.WebDAV.Client.Property(new ITHit.WebDAV.Client.PropertyName("myProp", "OK"), "value1"),
            new ITHit.WebDAV.Client.Property(new ITHit.WebDAV.Client.PropertyName("myProp1", ITHit.WebDAV.Client.DavConstants.NamespaceUri), "value2")
        ];
        var propertiesToDelete = [
            new ITHit.WebDAV.Client.PropertyName("myProp", "OK"),
            new ITHit.WebDAV.Client.PropertyName("myProp1", ITHit.WebDAV.Client.DavConstants.NamespaceUri)
        ];

        QUnit.stop();
        webDavSession.OpenFileAsync(Helper.GetAbsolutePath('HierarchyProperties/CheckDavNsRestriction.txt'), null, function(oAsyncResult) {
            QUnit.start();

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oAsyncResult.Result;

            // Create properties
            test.throws(function() {
                oFile.UpdatePropertiesAsync(propertiesToAdd, null, null, function (oAsyncResult) {
                });
            }, ITHit.WebDAV.Client.Exceptions.PropertyException, 'Check PropertyException on add properties with DAV: namespace');
            test.throws(function() {
                oFile.UpdatePropertiesAsync(null, propertiesToDelete, null, function (oAsyncResult) {
                });
            }, ITHit.WebDAV.Client.Exceptions.PropertyException, 'Check PropertyException on remove properties with DAV: namespace');
        });
    });
});
