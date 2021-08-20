function CreateFolderController(toolbar) {
    this.Toolbar = toolbar;
}

CreateFolderController.prototype = {
    CreateFolder: function (sFolderName, fCallback) {
        this.Toolbar.WebDAV.CurrentFolder.CreateFolderAsync(sFolderName, null, null, function (oAsyncResult) {
            fCallback(oAsyncResult);
        });
    },
}

///////////////////
// Create Folder Bootstrap Modal
function CreateFolderModal(modalSelector, createFolderController) {
    var sCreateFolderErrorMessage = "Create folder error.";

    var self = this;
    this.$modal = $(modalSelector);
    this.$txt = $(modalSelector).find('input[type="text"]');
    this.$submitButton = $(modalSelector).find('.btn-submit');
    this.$alert = $(modalSelector).find('.alert-danger');

    this.$modal.on('shown.bs.modal', function () {
        self.$txt.focus();
    })
    this.$modal.find('form').submit(function () {
        self.$alert.addClass('d-none');
        if (self.$txt.val() !== null && self.$txt.val().match(/^ *$/) === null) {
            var oValidationMessage = WebdavCommon.Validators.ValidateName(self.$txt.val());
            if (oValidationMessage) {
                self.$alert.removeClass('d-none').text(oValidationMessage);
                return false;
            }

            self.$txt.blur();
            self.$submitButton.attr('disabled', 'disabled');
            createFolderController.CreateFolder(self.$txt.val().trim(), function (oAsyncResult) {
                if (!oAsyncResult.IsSuccess) {
                    if (oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.MethodNotAllowedException) {
                        self.$alert.removeClass('d-none').text(oAsyncResult.Error.Error.Description ? oAsyncResult.Error.Error.Description : 'Folder already exists.');
                    }
                    else {
                        WebdavCommon.ErrorModal.Show(sCreateFolderErrorMessage, oAsyncResult.Error);
                    }
                }
                else {
                    self.$modal.modal('hide');
                }
                self.$submitButton.removeAttr('disabled');
            });
        }
        else {
            self.$alert.removeClass('d-none').text('Name is required!');
        }
        return false;
    });
}

function ToolbarCreateFolderButton(name, cssClass, toolbar) {
    BaseButton.call(this, name, cssClass);
    var oCreateFolderModal = new CreateFolderModal('#CreateFolderModal', new CreateFolderController(toolbar));
    this.InnerHtmlContent = '<span class="d-none d-lg-inline text-nowrap">Create Folder</span>';
    this.Render = function () {
        this.$Button.on('click', function () {
            oCreateFolderModal.$txt.val('');
            oCreateFolderModal.$alert.addClass('d-none');
            oCreateFolderModal.$modal.modal('show');
        })
    }
}