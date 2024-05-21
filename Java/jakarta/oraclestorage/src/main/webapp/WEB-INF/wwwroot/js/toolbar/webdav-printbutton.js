function HerarhyItemPrintController(toolbar) {
    this.Toolbar = toolbar;
}

HerarhyItemPrintController.prototype = {
    /**
        * Print documents.
        * @param {string} sDocumentUrls Array of document URLs
     */
    PrintDocs: function (sDocumentUrls) {
        ITHit.WebDAV.Client.DocManager.DavProtocolEditDocument(sDocumentUrls, this.Toolbar.WebDAV.GetMountUrl(),
            this.Toolbar.WebDAV._ProtocolInstallMessage.bind(this.Toolbar.WebDAV), null, webDavSettings.EditDocAuth.SearchIn,
            webDavSettings.EditDocAuth.CookieNames, webDavSettings.EditDocAuth.LoginUrl, 'Print');
    },
    ExecutePrint: function () {
        self = this;
        self.Toolbar.ConfirmModal.Confirm('Are you sure want to print selected items?', function () {
            var filesUrls = [];
            $.each(self.Toolbar.FolderGrid.selectedItems, function () {
                if (!this.IsFolder()) {
                    filesUrls.push(this.Href);
                }
            });

            self.PrintDocs(filesUrls);
        });
    }
}

function ToolbarPrintButton(name, cssClass, toolbar) {
    BaseButton.call(this, name, cssClass);
    this.InnerHtmlContent = '<span class="d-none d-xl-inline">Print</span>';

    this.Render = function () {
        this.$Button.on('click', function () {
            new HerarhyItemPrintController(toolbar).ExecutePrint();
        })
    }
}