/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('HierarchyItems.Progress');

/**
 * @class ITHit.WebDAV.Client.Tests.HierarchyItems.Progress
 */
ITHit.DefineClass('ITHit.WebDAV.Client.Tests.HierarchyItems.Progress', null, {}, /** @lends ITHit.WebDAV.Client.Tests.HierarchyItems.Progress */{

    /**
     * @param {ITHit.WebDAV.Client.WebDavSession} [webDavSession=new ITHit.WebDAV.Client.WebDavSession()]
     * @param {string} [sFolderAbsolutePath='http://localhost:87654/']
     * @param {function} [fCallback=function() {}]
     */
    Progress: function(webDavSession, sFolderAbsolutePath, fCallback) {
        webDavSession.OpenFolderAsync(sFolderAbsolutePath, null, function(oAsyncResult) {

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFolder = oAsyncResult.Result;

            var oRequest = oFolder.GetChildrenAsync(true, null, function(oAsyncResult) {

                if (oAsyncResult.IsSuccess) {
                    console.log('Count of children: ' + oAsyncResult.Result.length);
                }

            });

            // Subscribe on progress event
            oRequest.AddListener('OnProgress', function(oEvent) {
                /** @typedef {ITHit.WebDAV.Client.RequestProgress} oProgress */
                var oProgress = oEvent.Progress;

                var aOfText = oProgress.LengthComputable ?
                    [oProgress.BytesLoaded, 'of', oProgress.BytesTotal, 'bytes'] :
                    [oProgress.CountComplete, 'of', oProgress.CountTotal];

                console.log('Progress: ' + oProgress.Percent + '% (' + aOfText.join(' ') + ')');
            });

            fCallback(oRequest);
        });
    }

});

QUnitRunner.test('Subscribe on progress event and get progress info', function (test) {
    if (ITHit.DetectBrowser.IE && ITHit.DetectBrowser.IE <= 9) {
        ITHitTests.skip(test, 'Internet Explorer 9 and lower versions does not support `onprogress` event.');
        return;
    }

    QUnit.stop();
    Helper.Create([
        'HierarchyItems/Progress/item1.txt',
        'HierarchyItems/Progress/item2.txt',
        'HierarchyItems/Progress/item3.txt',
        'HierarchyItems/Progress/item4.txt',
        'HierarchyItems/Progress/item5.txt',
        'HierarchyItems/Progress/item6.txt',
        'HierarchyItems/Progress/item7.txt',
        'HierarchyItems/Progress/item8.txt',
        'HierarchyItems/Progress/item9.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        ITHit.WebDAV.Client.Tests.HierarchyItems.Progress.Progress(webDavSession, Helper.GetAbsolutePath('HierarchyItems/Progress/'), function(oRequest) {
            var aProgresses = [];

            oRequest.AddListener('OnProgress', function(oEvent) {
                aProgresses.push(oEvent.Progress);
            });

            oRequest.AddListener('OnFinish', function() {
                QUnit.start();

                test.strictEqual(oRequest.Progress.Percent, 100, 'Check progress percent is 100 of request link');
                test.strictEqual(aProgresses.length >= 1, true, 'Check count events >= 1');

                var oLastProgress = aProgresses[aProgresses.length - 1];
                test.strictEqual(oLastProgress instanceof ITHit.WebDAV.Client.RequestProgress, true, 'Check last progress is instance of RequestProgress');
                test.strictEqual(oLastProgress.Percent, 100, 'Check progress percent is 100 of last RequestProgress instance');
            });
        });
    });
});
