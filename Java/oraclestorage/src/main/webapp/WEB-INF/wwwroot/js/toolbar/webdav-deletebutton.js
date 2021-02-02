function HerarhyItemDeleteController(toolbar) {
    this.Toolbar = toolbar;
}

HerarhyItemDeleteController.prototype = {
    Delete: function () {
        var self = this;
        self.Toolbar.ConfirmModal.Confirm('Are you sure want to delete selected items?', function () {
            $.each(self.Toolbar.FolderGrid.selectedItems, function (index) {
                self.Toolbar.FolderGrid.selectedItems[index].DeleteAsync(null);
            });

            self.Toolbar.ResetToolbar();
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