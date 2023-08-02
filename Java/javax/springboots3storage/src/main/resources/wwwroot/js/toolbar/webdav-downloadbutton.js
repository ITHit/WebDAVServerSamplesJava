function HerarhyItemDownloadController(toolbar) {
    this.Toolbar = toolbar;
}

HerarhyItemDownloadController.prototype = {
    DownloadFiles: function () {
        var self = this;
        $.each(self.Toolbar.FolderGrid.selectedItems, function (index) {
            if (!this.IsFolder()) {
                self._Delay(index * 1000);
                self._Download(this.Href + "?download", '');
            }
        });
    },
    _Download: function (url, name) {
        const a = document.createElement('a');
        a.download = name;
        a.href = url;
        a.style.display = 'none';
        document.body.append(a);
        a.click();

        // Chrome requires the timeout
        this._Delay(100);
        a.remove();
    },
    _Delay: function () {
        return ms => new Promise(resolve => setTimeout(resolve, ms));
    }
}



function ToolbarDownloadButton(name, cssClass, toolbar) {
    BaseButton.call(this, name, cssClass);
    this.InnerHtmlContent = '<span class="d-none d-xl-inline d-xxl-inline">Download</span>';

    this.Render = function () {
        this.$Button.on('click', function () {
            new HerarhyItemDownloadController(toolbar).DownloadFiles();
        })
    }
}