function HerarhyItemDeleteController(toolbar) {
    this.Toolbar = toolbar;
}

HerarhyItemDeleteController.prototype = {
    Delete: function () {
        var self = this;
        self.Toolbar.ConfirmModal.Confirm('Are you sure want to delete selected items?', function () {
            var countDeleted = 0;
            self.Toolbar.WebDAV.AllowReloadGrid = false;
            $.each(self.Toolbar.FolderGrid.selectedItems, function (index) {
                self.Toolbar.FolderGrid.selectedItems[index].DeleteAsync(null, function () {
                    if (++countDeleted == self.Toolbar.FolderGrid.selectedItems.length) {
                        self.Toolbar.WebDAV.AllowReloadGrid = true;
                        self.Toolbar.WebDAV.Reload();
                        self.Toolbar.ResetToolbar();
                    }
                });
            });
        });
    }
}

function ToolbarDeleteButton(name, cssClass, toolbar) {
    BaseButton.call(this, name, cssClass);
    this.InnerHtmlContent = '<span class="d-none d-lg-inline">Delete</span>';

    this.Render = function () {
        this.$Button.on('click', function () {
            new HerarhyItemDeleteController(toolbar).Delete();
        })
    }
}