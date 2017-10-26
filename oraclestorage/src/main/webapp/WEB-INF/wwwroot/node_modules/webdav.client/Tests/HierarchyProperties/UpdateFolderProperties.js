/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyProperties.UpdateFolderProperties');

QUnitRunner.test('Add, update and delete folder custom properties', function (test) {
    Helper.Create([
        'HierarchyProperties/updatefolder/'
    ], QUnitRunner.async(function() {

        var sFolderPath = Helper.GetAbsolutePath('HierarchyProperties/updatefolder');
        var oPropertyName = new ITHit.WebDAV.Client.PropertyName('myname', 'mynamespace');
        var oProperty = new ITHit.WebDAV.Client.Property(oPropertyName, 'first value');

        // Create properties
        webDavSession.OpenFolderAsync(sFolderPath, null, QUnitRunner.async(function(oAsyncResult) {
            var oFolder = oAsyncResult.Result;
            oAsyncResult.Result.UpdatePropertiesAsync([oProperty], null, null, QUnitRunner.async(function(oAsyncResult) {

                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of add property request');

                oFolder.GetPropertyValuesAsync([oPropertyName], QUnitRunner.async(function(oAsyncResult) {

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get property values request');
                    test.strictEqual(oAsyncResult.Result[0].StringValue(), 'first value', 'Check value of loaded first property');

                    var oPropertyName = new ITHit.WebDAV.Client.PropertyName('myname', 'mynamespace');
                    var oProperty = new ITHit.WebDAV.Client.Property(oPropertyName, 'Test value');

                    oFolder.UpdatePropertiesAsync([oProperty], null, null, QUnitRunner.async(function(oAsyncResult) {

                        test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of update property request');

                        oFolder.GetPropertyValuesAsync([oPropertyName], QUnitRunner.async(function(oAsyncResult) {

                            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get property values request (after update)');
                            test.strictEqual(oAsyncResult.Result[0].StringValue(), 'Test value', 'Check value of loaded first property (after update)');

                            var oPropertyName = new ITHit.WebDAV.Client.PropertyName('myname', 'mynamespace');

                            oFolder.UpdatePropertiesAsync(null, [oPropertyName], null, QUnitRunner.async(function(oAsyncResult) {

                                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of delete property request');

                                oFolder.GetPropertyValuesAsync([oPropertyName], QUnitRunner.async(function(oAsyncResult) {

                                    test.strictEqual(oAsyncResult.IsSuccess, false, 'Check failure on get removed property');
                                    test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.PropertyNotFoundException, true, 'Check error is PropertyNotFoundException');
                                }));
                            }));
                        }));
                    }));
                }));
            }));
        }));
    }));
});


QUnitRunner.test('Add, update and delete folder multiple custom properties', function (test) {
    Helper.Create([
        'HierarchyProperties/updatefolder_multiple/'
    ], QUnitRunner.async(function() {

        var sFolderPath = Helper.GetAbsolutePath('HierarchyProperties/updatefolder_multiple');
        var oPropertyName1 = new ITHit.WebDAV.Client.PropertyName('myname1', 'mynamespace');
        var oPropertyName2 = new ITHit.WebDAV.Client.PropertyName('myname2', 'mynamespace');
        var oPropertyName3 = new ITHit.WebDAV.Client.PropertyName('foo', 'bar');
        var aPropertyNames = [oPropertyName1, oPropertyName2, oPropertyName3];
        var oProperty1 = new ITHit.WebDAV.Client.Property(oPropertyName1, '1 first value');
        var oProperty2 = new ITHit.WebDAV.Client.Property(oPropertyName2, '2 second value');
        var oProperty3 = new ITHit.WebDAV.Client.Property(oPropertyName3, '3 foobar value');
        var aProperties = [oProperty1, oProperty2, oProperty3];

        // Create properties
        webDavSession.OpenFolderAsync(sFolderPath, null, QUnitRunner.async(function(oAsyncResult) {
            var oFolder = oAsyncResult.Result;
            oAsyncResult.Result.UpdatePropertiesAsync(aProperties, null, null, QUnitRunner.async(function(oAsyncResult) {

                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of add property request');

                oFolder.GetPropertyValuesAsync(aPropertyNames, QUnitRunner.async(function(oAsyncResult) {

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get property values request');

                    var values = oAsyncResult.Result.length
                        ? $.map(oAsyncResult.Result, function(oResult) {
                            return oResult.StringValue();
                        })
                        : [];
                    values.sort();
                    test.strictEqual(oAsyncResult.Result.length, 3, 'Check properties length');
                    test.strictEqual(values[0], '1 first value', 'Check value of loaded property 1');
                    test.strictEqual(values[1], '2 second value', 'Check value of loaded property 2');
                    test.strictEqual(values[2], '3 foobar value', 'Check value of loaded property 3');

                    var oNewProperty1 = new ITHit.WebDAV.Client.Property(oPropertyName1, '1 NEW first value');
                    var oNewProperty2 = new ITHit.WebDAV.Client.Property(oPropertyName2, '2 NEW second value');
                    var oNewProperty3 = new ITHit.WebDAV.Client.Property(oPropertyName3, '3 NEW foobar value');
                    var aNewProperties = [oNewProperty1, oNewProperty2, oNewProperty3];

                    oFolder.UpdatePropertiesAsync(aNewProperties, null, null, QUnitRunner.async(function(oAsyncResult) {

                        test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of update property request');

                        oFolder.GetPropertyValuesAsync(aPropertyNames, QUnitRunner.async(function(oAsyncResult) {

                            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get property values request (after update)');

                            var values = oAsyncResult.Result.length
                                ? $.map(oAsyncResult.Result, function(oResult) {
                                    return oResult.StringValue();
                                })
                                : [];
                            values.sort();
                            test.strictEqual(oAsyncResult.Result.length, 3, 'Check properties length (after update)');
                            test.strictEqual(values[0], '1 NEW first value', 'Check value of loaded property 1 (after update)');
                            test.strictEqual(values[1], '2 NEW second value', 'Check value of loaded property 2 (after update)');
                            test.strictEqual(values[2], '3 NEW foobar value', 'Check value of loaded property 3 (after update)');

                            oFolder.UpdatePropertiesAsync(null, aPropertyNames, null, QUnitRunner.async(function(oAsyncResult) {

                                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of delete property request');

                                oFolder.GetPropertyValuesAsync(aPropertyNames, QUnitRunner.async(function(oAsyncResult) {

                                    test.strictEqual(oAsyncResult.IsSuccess, false, 'Check failure on get removed property');
                                    test.strictEqual(oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.PropertyNotFoundException, true, 'Check error is PropertyNotFoundException');
                                }));
                            }));
                        }));
                    }));
                }));
            }));
        }));
    }));
});
