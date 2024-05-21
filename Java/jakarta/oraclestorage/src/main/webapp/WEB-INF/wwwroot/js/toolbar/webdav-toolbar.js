    function Toolbar(selectorTableToolbar, folderGrid, confirmModal, webDAVController) {
        this.ToolbarName = selectorTableToolbar;
        this.$Toolbar = $(selectorTableToolbar);
        this.FolderGrid = folderGrid;
        this.ConfirmModal = confirmModal;
        this.WebDAV = webDAVController;
        this.buttons = [];

        var self = this;

        if (typeof ToolbarCreateFolderButton === "function") {
            var createFolderButton = new ToolbarCreateFolderButton('Create Folder', 'btn-create-folder', this);
            this.buttons.push(createFolderButton);
            createFolderButton.Create($(self.$Toolbar).find(".first-section"));
        }

        if (typeof ToolbarDownloadButton == "function") {
            var downloadButton = new ToolbarDownloadButton('Dwonload', 'btn-download-items', this);
            this.buttons.push(downloadButton);
            downloadButton.Create($(self.$Toolbar).find(".second-section"));
        }

        if (typeof ToolbarRenameButton == "function") {
            var renameButton = new ToolbarRenameButton('Rename', 'btn-rename-item', this);
            this.buttons.push(renameButton);
            renameButton.Create($(self.$Toolbar).find(".third-section"));
        }

        if (typeof CopyPasteButtonsControl === "function") {
            var copyPasteButtons = new CopyPasteButtonsControl(this)
            this.buttons.push(copyPasteButtons)
            copyPasteButtons.Create($(self.$Toolbar).find(".fourth-section"));
        }

        if (typeof ToolbarReloadButton == "function") {
            var reloadButton = new ToolbarReloadButton('Reload', 'btn-reload-items', this);
            this.buttons.push(reloadButton);
            reloadButton.Create($(self.$Toolbar).find(".fifth-section"));
        }

        if (typeof ToolbarPrintButton === "function") {
            var printButton = new ToolbarPrintButton('Print', 'btn-print-items', this)
            this.buttons.push(printButton);
            printButton.Create($(self.$Toolbar).find(".sixth-section"));
        }

        if (typeof ToolbarDeleteButton === "function") {
            var deleteButton = new ToolbarDeleteButton('Delete', 'btn-delete-items', this)
            this.buttons.push(deleteButton);
            deleteButton.Create($(self.$Toolbar).find(".sixth-section"));
        }

        $.each(self.buttons, function (index) {
            this.Render();
        });

        this.UpdateToolbarButtons();
    }

Toolbar.prototype = {
    UpdateToolbarButtons: function () {
        var self = this;

        $.each(self.buttons, function (index) {
            if (typeof ToolbarCreateFolderButton === "function" && this instanceof ToolbarCreateFolderButton) {
                self.FolderGrid.selectedItems.length == 0 ? this.ShowOnMobile() : this.HideOnMobile();
            }
            if (typeof ToolbarDeleteButton === "function" && this instanceof ToolbarDeleteButton) {
                if (self.FolderGrid.selectedItems.length == 0) {
                    this.Disable();
                    this.HideOnMobile();
                }
                else {
                    this.Activate();
                    this.ShowOnMobile();
                }
            }
            if (typeof ToolbarRenameButton === "function" && this instanceof ToolbarRenameButton) {
                if (self.FolderGrid.selectedItems.length == 0 ||
                    self.FolderGrid.selectedItems.length != 1) {
                    this.Disable();
                    this.HideOnMobile();
                }
                else {
                    this.Activate();
                    this.ShowOnMobile();
                }
            }
            if (typeof ToolbarDownloadButton === "function" && this instanceof ToolbarDownloadButton) {
                if (self.FolderGrid.selectedItems.length == 0 || !self.FolderGrid.selectedItems.some(el => !el.IsFolder())) {
                    this.Disable();
                    this.HideOnMobile();
                }
                else {
                    this.Activate();
                    this.ShowOnMobile();
                }
            }
            if (typeof CopyPasteButtonsControl === "function" && this instanceof CopyPasteButtonsControl) {
                if (self.FolderGrid.selectedItems.length == 0) {
                    this.CopyButton.Disable();
                    this.CopyButton.HideOnMobile();

                    this.CutButton.Disable();
                    this.CutButton.HideOnMobile();
                }
                else {
                    this.CopyButton.Activate();
                    this.CopyButton.ShowOnMobile();

                    this.CutButton.Activate();
                    this.CutButton.ShowOnMobile();
                }

                if (this.storedItems.length == 0) {
                    this.PasteButton.Disable();
                    this.PasteButton.HideOnMobile();
                }
                else {
                    this.PasteButton.Activate();
                    this.PasteButton.ShowOnMobile();
                }
            }
            if (ITHit.Environment.OS == 'Windows' && typeof ToolbarPrintButton === "function" && this instanceof ToolbarPrintButton) {
                if (self.FolderGrid.selectedItems.filter(function (item) { return !item.IsFolder(); }).length == 0) {
                    this.Disable();
                    this.HideOnMobile();
                }
                else {
                    this.Activate();
                    this.ShowOnMobile();
                }
            }
        });
    },

    ResetToolbar: function () {
        this.FolderGrid.UncheckTableCheckboxs();
        this.UpdateToolbarButtons();
    }
}