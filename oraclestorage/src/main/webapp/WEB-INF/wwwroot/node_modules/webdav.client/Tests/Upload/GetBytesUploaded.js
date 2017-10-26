/**
 * @typedef {ITHit.WebDAV.Client.WebDavSession} webDavSession
 */

QUnit.module('Upload.GetBytesUploaded');

QUnitRunner.test('Get bytes uploaded on created file', function (test) {
    QUnit.stop();
    Helper.Create([
        'Upload/bytes_file.txt'
    ], function() {
        QUnit.start();

        QUnit.stop();
        webDavSession.OpenItemAsync(Helper.GetAbsolutePath('Upload/bytes_file.txt'), null, function(oAsyncResult) {
            QUnit.start();

            /** @typedef {ITHit.WebDAV.Client.Folder} oFolder */
            var oFile = oAsyncResult.Result;

            test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of open item request');

            // Async variant
            QUnit.stop();
            oFile.ResumableUpload.GetBytesUploadedAsync(function(oAsyncResult) {
                QUnit.start();

                test.strictEqual(oAsyncResult.IsSuccess, true, 'Check success of get bytes request');
                test.strictEqual(oAsyncResult.Result, 6, 'Check content length in result');
            });
        });
    });
});
