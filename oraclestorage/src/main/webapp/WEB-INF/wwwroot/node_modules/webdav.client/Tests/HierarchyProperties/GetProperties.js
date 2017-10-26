/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyProperties.GetProperties');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyProperties.GetProperties
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyProperties.GetProperties', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyProperties.GetProperties */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFileAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    GetAllProperties: function(webDavSession, sFileAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFileAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            oFile.GetAllPropertiesAsync(function(oAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.Property[]} aProperties */
                var aProperties = oAsyncResult.Result;

                for (var i = 0, l = aProperties.length; i < l; i++) {
                    console.log(aProperties[i].Name + ': ' + aProperties[i].StringValue());
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
    GetPropertyValues: function(webDavSession, sFileAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFileAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            var oPropertyName = new ITHit.WebDAV.Client.PropertyName('myname', 'mynamespace');

            oFile.GetPropertyValuesAsync([oPropertyName], function(oAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.Property[]} aProperties */
                var aProperties = oAsyncResult.Result;

                console.log('Value of `mynamespace:myname`: ' + aProperties[0].StringValue());

                fCallback(oAsyncResult);
            });
        });
    },

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFileAbsolutePath='http://localhost:87654/myfile.txt']
     * @param {function} [fCallback=function() {}]
     */
    GetPropertyNames: function(webDavSession, sFileAbsolutePath, fCallback) {
        webDavSession.OpenFileAsync(sFileAbsolutePath, null, function(oFileAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.File} oFile */
            var oFile = oFileAsyncResult.Result;

            oFile.GetPropertyNamesAsync(function(oAsyncResult) {

                /** @typedef {ITHit.WebDAV.Client.Property[]} aProperties */
                var aPropertyNames = oAsyncResult.Result;

                if (oAsyncResult.IsSuccess) {
                    console.log('Properties: ' + aPropertyNames.join(', '));
                }

                fCallback(oAsyncResult);
            });
        });
    }

});

QUnitRunner.test('Get all properties, get property values and get property names', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyProperties/myfile.txt'
    ], function() {
        QUnit.start();

        var aProperties = [
            new ITHit.WebDAV.Client.Property(new ITHit.WebDAV.Client.PropertyName('myname', 'mynamespace'), 'QQ 11')
            //new ITHit.WebDAV.Client.Property('aaa', 'AS 11', 'sss') // @todo uncomment, when server fixed multiple set properties
        ];

        // Create properties
        QUnit.stop();
        webDavSession.OpenFileAsync(Helper.GetAbsolutePath('HierarchyProperties/myfile.txt'), null, function(oAsyncResult) {

            oAsyncResult.Result.UpdatePropertiesAsync(aProperties, null, null, function(oAsyncResult) {
                QUnit.start();

                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of update properties request');

                QUnit.stop();
                ITHit.WebDAV.Client.Tests.HierarchyProperties.GetProperties.GetAllProperties(webDavSession, Helper.GetAbsolutePath('HierarchyProperties/myfile.txt'), function(oAsyncResult) {
                    QUnit.start();

                    test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get all properties request');
                    test.strictEqual(oAsyncResult.Result.length >= aProperties.length, true, 'Check result items length equal or more than added properties');
                    test.strictEqual(oAsyncResult.Result[0] instanceof ITHit.WebDAV.Client.Property, true, 'Check result item is instance of Property');
                    test.strictEqual(oAsyncResult.Result[0].Name.NamespaceUri, 'mynamespace', 'Check result item namespace');
                    test.strictEqual(oAsyncResult.Result[0].Name.Name, 'myname', 'Check result item name');
                    test.strictEqual(oAsyncResult.Result[0].StringValue(), 'QQ 11', 'Check result item value');
                    //test.strictEqual(oAsyncResult.Result[1].Name.toString(), 'aaa:sss');
                    //test.strictEqual(oAsyncResult.Result[1].StringValue(), 'AS 11');

                    QUnit.stop();
                    ITHit.WebDAV.Client.Tests.HierarchyProperties.GetProperties.GetPropertyValues(webDavSession, Helper.GetAbsolutePath('HierarchyProperties/myfile.txt'), function(oAsyncResult) {
                        QUnit.start();

                        test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get property values request');
                        test.strictEqual(oAsyncResult.Result.length, 1, 'Check GetPropertyValue result length is 1');
                        test.strictEqual(oAsyncResult.Result[0] instanceof ITHit.WebDAV.Client.Property, true, 'Check result (by GetPropertyValue) item is instance of Property');
                        test.strictEqual(oAsyncResult.Result[0].Name.NamespaceUri, 'mynamespace', 'Check result (by GetPropertyValue) item namespace');
                        test.strictEqual(oAsyncResult.Result[0].Name.Name, 'myname', 'Check result (by GetPropertyValue) item name');
                        test.strictEqual(oAsyncResult.Result[0].StringValue(), 'QQ 11', 'Check result (by GetPropertyValue) item value');

                        QUnit.stop();
                        ITHit.WebDAV.Client.Tests.HierarchyProperties.GetProperties.GetPropertyNames(webDavSession, Helper.GetAbsolutePath('HierarchyProperties/myfile.txt'), function(oAsyncResult) {
                            QUnit.start();

                            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get property names request');
                            test.strictEqual(oAsyncResult.Result.length > 0, true, 'Check request (by GetPropertyNames) length more than zero');
                            test.strictEqual(oAsyncResult.Result[0] instanceof ITHit.WebDAV.Client.PropertyName, true, 'Check request (by GetPropertyNames) item is instance of PropertyName');

                            QUnit.stop();
                            webDavSession.OpenFileAsync(Helper.GetAbsolutePath('HierarchyProperties/myfile.txt'), null, function(oAsyncResult) {
                                QUnit.start();

                                /** @typedef {ITHit.WebDAV.Client.File} oFile */
                                var oFile = oAsyncResult.Result;

                                // Get non-exists property
                                var oUnknownProperty = new ITHit.WebDAV.Client.PropertyName("unexistProp", "myNamespace");

                                QUnit.stop();
                                oFile.GetPropertyValuesAsync([oUnknownProperty], function(oAsyncResult) {
                                    QUnit.start();

                                    test.strictEqual(oAsyncResult.IsSuccess, false, 'Check failure on get values of unknown property');
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

QUnitRunner.test('PropertyList check', function (test) {
    QUnit.stop();
    Helper.Create([
        'HierarchyProperties/myfile2.txt'
    ], function() {
        QUnit.start();

        // Create properties
        QUnit.stop();
        webDavSession.OpenFileAsync(Helper.GetAbsolutePath('HierarchyProperties/myfile2.txt'), null, function(oAsyncResult) {
            QUnit.start();

            var oFile = oAsyncResult.Result;
            var oProperty = ITHit.WebDAV.Client.DavConstants.DisplayName;
            var oNotExistsProperty = new ITHit.WebDAV.Client.PropertyName('myname', 'mynamespace');

            test.strictEqual(oFile.Properties.Has(oProperty), true, 'Check has property method');
            test.strictEqual(oFile.Properties.Find(oProperty), 'myfile2.txt', 'Check property value');
            test.strictEqual(oFile.Properties.Has(oNotExistsProperty), false, 'Check not exists property');
            test.strictEqual(oFile.Properties.Find(oNotExistsProperty), null, 'Check null of not exists property');
            test.strictEqual(oFile.Properties instanceof Array, true, 'Check instanceof Array');
            test.strictEqual(oFile.Properties.length > 0, true, 'Check array length');
        });
    });
});
