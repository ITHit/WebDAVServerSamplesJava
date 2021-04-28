const sRenameItemErrorMessage = "Rename item error.";
const sRenameItemLockedErrorMessage = "Item is locked.";

function RenameItemController(toolbar) {
    this.Toolbar = toolbar;
}

RenameItemController.prototype = {
    /**
   * Renames files or folders.
   */
    Rename: function (newItemName, fCallback) {
        this.Toolbar.FolderGrid.selectedItems[0].MoveToAsync(this.Toolbar.WebDAV.CurrentFolder,
            newItemName, null, null, function (oAsyncResult) {
            fCallback(oAsyncResult);
        });
    },
}

///////////////////
// Create Folder Bootstrap Modal
function RenameItemModal(modalSelector, renameItemController) {

    var self = this;
    this.$modal = $(modalSelector);
    this.$txt = $(modalSelector).find('input[type="text"]');
    this.$submitButton = $(modalSelector).find('.btn-submit');
    this.$alert = $(modalSelector).find('.alert-danger');
    this.oldItemName = "";

    this.$modal.on('shown.bs.modal', function () {
        self.$txt.focus();
    })
    this.$modal.find('form').submit(function () {
        self.$alert.addClass('d-none');
        if (self.$txt.val() == self.oldItemName) {
            self.$modal.modal('hide');
        }
        else if (self.$txt.val() !== null && self.$txt.val().match(/^ *$/) === null) {
            var oValidationMessage = WebdavCommon.Validators.ValidateName(self.$txt.val());
            if (oValidationMessage) {
                self.$alert.removeClass('d-none').text(oValidationMessage);
                return false;
            }

            self.$txt.blur();
            self.$submitButton.attr('disabled', 'disabled');
            renameItemController.Rename(self.$txt.val().trim(), function (oAsyncResult) {
                if (!oAsyncResult.IsSuccess) {
                     if (oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.LockedException) {
                        WebdavCommon.ErrorModal.Show(sRenameItemLockedErrorMessage, oAsyncResult.Error);
                    }
                    else {
                        WebdavCommon.ErrorModal.Show(sRenameItemErrorMessage, oAsyncResult.Error);
                    }
                }
                self.$modal.modal('hide');
                self.$submitButton.removeAttr('disabled');
                renameItemController.Toolbar.ResetToolbar();
                self.$txt.val('');
            });
        }
        else {
            self.$alert.removeClass('d-none').text('Name is required!');
        }
        return false;
    });

}

function ToolbarRenameButton(name, cssClass, toolbar) {
    BaseButton.call(this, name, cssClass);
    var oRenameItemModal = new RenameItemModal('#RenameItemModal', new RenameItemController(toolbar));
    this.Render = function () {
        this.$Button.on('click', function () {
            if (toolbar.FolderGrid.selectedItems.length) {
                oRenameItemModal.$txt.val(toolbar.FolderGrid.selectedItems[0].DisplayName);
                oRenameItemModal.oldItemName = toolbar.FolderGrid.selectedItems[0].DisplayName;
            }
            oRenameItemModal.$alert.addClass('d-none');
            oRenameItemModal.$modal.modal('show');
        })
    }
}