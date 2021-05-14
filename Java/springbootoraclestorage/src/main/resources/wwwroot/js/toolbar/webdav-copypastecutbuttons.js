const sCopyItemsErrorMessage = "Copy items error.";
const sCutItemsErrorMessage = "Cut items error.";
const sCutItemsSameNameErrorMessage = "The source and destination file names are the same.";
const sCutItemsLockedErrorMessage = "Items are locked.";

function HerarhyItemsCopyPasteController(toolbar, storedItems) {
    //Copied or cut items
    this.storedItems = storedItems;
    this.isCopiedItems = false;
    this.Toolbar = toolbar;
}

HerarhyItemsCopyPasteController.prototype = {
    /**
    * Copies files or folders.
    */
    Copy: function (oItem, oItemName, fCallback) {
        oItem.CopyToAsync(this.Toolbar.WebDAV.CurrentFolder, oItemName, true, null, null, function (oAsyncResult) {
            fCallback(oAsyncResult);
        });
    },

    /**
    * Moves files or folders.
    */
    Move: function (oItem, fCallback) {
        oItem.MoveToAsync(this.Toolbar.WebDAV.CurrentFolder, oItem.DisplayName, null, null, function (oAsyncResult) {
            fCallback(oAsyncResult);
        });
    },

    /**
    * Adds items to storeItems array.
    */
    _PushStoreItems: function () {
        var self = this;
        if (self.storedItems.length != 0) {
            $.each(self.storedItems, function (index) {
                self.storedItems.pop(this);
            });
        }
        $.each(self.Toolbar.FolderGrid.selectedItems, function (index) {
            self.storedItems.push(self.Toolbar.FolderGrid.selectedItems[index]);
        });

        self.Toolbar.UpdateToolbarButtons();
    },

    /**
    * Moves or pastes files or folders.
    */
    _MoveOrPasteItems: function () {
        var self = this;
        if (self.isCopiedItems) {
            $.each(self.storedItems, function (index) {
                self._ExecuteCopy(self.storedItems[index]);
            });
        } else {
            $.each(self.storedItems, function (index) {
                self.Move(self.storedItems[index], function (oAsyncResult) {
                    if (!oAsyncResult.IsSuccess) {
                        if (oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.ForbiddenException) {
                            WebdavCommon.ErrorModal.Show(sCutItemsSameNameErrorMessage, oAsyncResult.Error);
                        }
                        else if (oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.LockedException) {
                            WebdavCommon.ErrorModal.Show(sCutItemsLockedErrorMessage, oAsyncResult.Error);
                        }
                        else {
                            WebdavCommon.ErrorModal.Show(sCutItemsErrorMessage, oAsyncResult.Error);
                        }
                    }
                });
            });
            $.each(self.storedItems, function (index) {
                self.storedItems.pop(this);
            });
        }
        this.Toolbar.UpdateToolbarButtons();
    },

    _ExecuteCopy: function (oItem) {
        var self = this;
        self._DoCopy(oItem, self._GetCopySuffix(oItem.DisplayName, false));
    },

    /**
    * Copies files or folders or shows error modal.
    */
    _DoCopy: function (oItem, oItemName) {
        var self = this;
        self.Copy(oItem, oItemName, function (oAsyncResult) {
            if (!oAsyncResult.IsSuccess) {
                if (
                    oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.PreconditionFailedException ||
                    oAsyncResult.Error instanceof window.ITHit.WebDAV.Client.Exceptions.ForbiddenException) {
                    self._DoCopy(oItem, self._GetCopySuffix(oItemName, true));
                }
                else {
                    WebdavCommon.ErrorModal.Show(sCopyItemsErrorMessage, oAsyncResult.Error);
                }
            }
        });
    },

    /**
    * Gets 'Copy' suffix.
    */
    _GetCopySuffix: function (oItemName, bWithCopySuffix) {
        var sCopyPrefixName = 'Copy';

        var aExtensionMatches = /\.[^\.]+$/.exec(oItemName);
        var sName = aExtensionMatches !== null ? oItemName.replace(aExtensionMatches[0], '') : oItemName;
        var sDotAndExtension = aExtensionMatches !== null ? aExtensionMatches[0] : '';

        var sLangCopy = sCopyPrefixName;
        var oSuffixPattern = new RegExp('- ' + sLangCopy + '( \\(([0-9]+)\\))?$', 'i');

        var aSuffixMatches = oSuffixPattern.exec(sName);
        if (aSuffixMatches === null && bWithCopySuffix) {
            sName += " - " + sLangCopy;
        } else if (aSuffixMatches !== null && !aSuffixMatches[1]) {
            sName += " (2)";
        } else if (aSuffixMatches !== null) {
            var iNextNumber = parseInt(aSuffixMatches[2]) + 1;
            sName = sName.replace(
                oSuffixPattern,
                "- " + sLangCopy + " (" + iNextNumber + ")"
            );
        }

        oItemName = sName + sDotAndExtension;
        return oItemName;
    },
}

function CopyPasteButtonsControl(toolbar) {
    this.CopyButton = new BaseButton('Copy', 'btn-copy-items', toolbar);
    this.PasteButton = new BaseButton('Paste', 'btn-paste-items', toolbar);
    this.CutButton = new BaseButton('Cut', 'btn-cut-items', toolbar);
    this.storedItems = [];

    var oHerarhyItemsCopyPasteController = new HerarhyItemsCopyPasteController(toolbar, this.storedItems);

    this.Create = function (tolbarSection) {

        this.CopyButton.Create(tolbarSection);
        this.CutButton.Create(tolbarSection);
        this.PasteButton.Create(tolbarSection);
    }
    this.Disable = function () {
        this.CopyButton.Disable();
        this.CutButton.Disable();
        this.PasteButton.Disable();
    }

    this.Activate = function () {
        this.CopyButton.Activate();
        this.CutButton.Activate();
        this.PasteButton.Activate();
    }

    this.Render = function () {
        this.CopyButton.$Button.on('click', function () {
            oHerarhyItemsCopyPasteController.isCopiedItems = true;
            oHerarhyItemsCopyPasteController._PushStoreItems();
        })

        this.CutButton.$Button.on('click', function () {
            oHerarhyItemsCopyPasteController.isCopiedItems = false;
            oHerarhyItemsCopyPasteController._PushStoreItems();
        })

        this.PasteButton.$Button.on('click', function () {
            oHerarhyItemsCopyPasteController._MoveOrPasteItems();
        })
    }


}