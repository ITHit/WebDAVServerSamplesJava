(function (WebdavCommon) {
    var sOverwriteDialogueFormat = 'The following item(s) exist on the server:<br/><br/>{0}<br/><br/>Overwrite?';
    var sFailedCheckExistsMessage = "Check for already exists item failed with error.";
    var sRetryMessageFormat = 'Retry in: {0}';
    var sWrongFileSizeFormat = 'File size should be less than {0}.';
    var sForbiddenExtensionFormat = 'Upload files with "{0}" extension is forbidden.';
    var sValidationError = 'Validation Error';
    var iMaxFileSize = 10485760; //10MB
    var aForbiddenExtensions = ['BAT', 'BIN', 'CMD', 'COM', 'EXE'];


    ///////////////////
    // Confirm Bootstrap Modal
    var ConfirmRewriteModal = function (selector) {
        this.$el = $(selector);
        this.$el.find('.btn-ok').click(this._onOkClickHandler.bind(this));
        this.$el.find('.btn-no').click(this._onNoClickHandler.bind(this));
        this.$el.on('hide.bs.modal',this._onModalHideHandler.bind(this));
    }
    ConfirmRewriteModal.prototype = {
        Confirm: function (message, successfulCallback, discardCallback, cancelCallback) {
            this.isConfirmed = false;
            this.successfulCallback = successfulCallback || $.noop;
            this.discardCallback = discardCallback || $.noop;
            this.cancelCallback = cancelCallback || $.noop;
            this.$el.find('.message').html(message);
            this.$el.find('.modal-dialog').addClass('modal-lg');
            this.$el.modal('show');
        },

        _onOkClickHandler: function (e) {
            this.isConfirmed = true;
            this.successfulCallback();
            this.$el.modal('hide');
        },

        _onNoClickHandler: function (e) {
            this.isDiscarded = true;
            this.discardCallback();
            this.$el.modal('hide');
        },

        _onModalHideHandler: function () {
            if (!this.isConfirmed && !this.isDiscarded) {
                this.cancelCallback();
            }
        }
    };

    /**
     * This class represents error that occured on client.
     * @class
     * @param {string} sMessage - The message will be displayed as error's short description.
     * @param {string} sUri - This url will be displayed as item's URL caused error.
     * @property {string} Message
     * @property {string} Uri
     */
    function ClientError(sMessage, sUri) {
        this.Message = sMessage;
        this.Uri = sUri;
    }

    ////////////////
    // Uploader Grid View
     /** @class  */
    function UploaderGridView(sSelector) {

        this.Uploader = new ITHit.WebDAV.Client.Upload.Uploader();
        this._dropCounter = 0;

        this.Uploader.Inputs.AddById('ithit-button-input');
        this._dropZone = this.Uploader.DropZones.AddById('ithit-dropzone');
        this._dropZone.HtmlElement.addEventListener('dragenter', this._OnDragEnter.bind(this), false);
        this._dropZone.HtmlElement.addEventListener('dragleave', this._OnDragLeave.bind(this), false);
        this._dropZone.HtmlElement.addEventListener('drop', this._OnDrop.bind(this), false);

        this.Uploader.SetUploadUrl(ITHit.WebDAV.Client.Encoder.Decode(window.location.href.split("#")[0]));
        this.Uploader.Queue.AddListener('OnQueueChanged', '_QueueChange', this);
        this.Uploader.Queue.AddListener('OnUploadItemsCreated', this._OnUploadItemsCreated, this);

        var $container = this.$container = $(sSelector);
        this.$uploadingBlock= this.$container.find('.uploading-block');
        this.$uploadingDetails = this.$container.find('.uploading-details');
        this.$uploadingDetails.draggable();

        this.rows = [];
        this.fileLoadCompleted = function () {
            if (this.$container.find('.uploading-item').length == 0) {
                this.$container.addClass('d-none');
                this.$container.find('.progress-wrapper .progress-bar').attr('aria-valuenow', 0).css('width', 0 + '%');
                this.$uploadingBlock.find('.persent').text(0 + '%');
            }
            window.WebDAVController.Reload();
        }

        window.addEventListener('beforeunload', function (event) {
            if ($container.find('.uploading-item').length != 0) {
                var warnMessage = 'Uploader is running!';
                (event || window.event).returnValue = warnMessage;
                return warnMessage;
            }
        });

        this._DataBindUploaderBlock();
    };

    UploaderGridView.prototype.SetUploadUrl = function(sPath) {
        this.Uploader.SetUploadUrl(sPath);
    };

    /** Called when a user selects items for upload or drops items into a drop area. 
     * In this function, you can validate files selected for upload and present user interface 
     * if user interaction is necessary. 
     * You can check if each item exists on the server, submitting additional requests to the 
     * server, and specify if each item should be overwritten or skipped. You can also specify 
     * if the item should be deleted in case of upload cancelation (typically if the item did 
     * not exist on the server before upload). 
     * In addition you can validate file size, file extension, file upload path, and file name.
     *
     * As soon as you may perform asynchronous calls in this function you must signal that all 
     * asynchronous checks are completed and upload can be started calling 
     * UploadItemsCreated.Upload() function passing a list of UploadItems to be uploaded.
     * 
     * @param {ITHit.WebDAV.Client.Upload.Events.UploadItemsCreated} oUploadItemsCreated - Contains 
     * a list of items selected by the user for upload in UploadItemsCreated.Items property.
     * @memberof UploaderGridView.prototype
     */
    UploaderGridView.prototype._OnUploadItemsCreated = function (oUploadItemsCreated) {

        /* Validate file extensions, size, name, etc. here. */
        var oValidationError = this._ValidateUploadItems(oUploadItemsCreated.Items);
        if(oValidationError) {
            window.ErrorModal.Show(sValidationError, oValidationError);
            return;
        }

        /* Below we will check if each file exists on the server 
        and ask a user if files should be overwritten or skipped. */
        this._GetExistsAsync(oUploadItemsCreated.Items, function (oAsyncResult) {
            if(oAsyncResult.IsSuccess && oAsyncResult.Result.length === 0) {
                // No items exists on the server.                
                // Add all items to the upload queue.
                oUploadItemsCreated.Upload(oUploadItemsCreated.Items); 
                return;
            }

            if(!oAsyncResult.IsSuccess) {
                // Some error occurred during item existence verification requests.
                // Show error dialog with error description.
                // Mark all items as failed and add to the upload list.
                this._ShowExistsCheckError(oAsyncResult.Error,
                    function() {
                        oUploadItemsCreated.Items.forEach(function(oUploadItem) {
                        
                            // Move an item into the error state. 
                            // Upload of this item will NOT start when added to the queue.
                            oUploadItem.SetFailed(oAsyncResult.Error);
                        });

                        // Add all items to the upload queue, so a user can start the upload later.
                        oUploadItemsCreated.Upload(oUploadItemsCreated.Items);
                    });
                return;
            }

            var sItemsList = ''; // List of items to be displayed in Overwrite / Skip / Cancel dialog.

            /** @type {ITHit.WebDAV.Client.Upload.UploadItem[]} aExistsUploadItems */
            var aExistsUploadItems = [];
            oAsyncResult.Result.forEach(function(oUploadItem) {
                
                // For the sake of simplicity folders are never deleted when upload canceled.
                if (!oUploadItem.IsFolder()) {
                    
                    // File exists so we should not delete it when file's upload canceled.
                    oUploadItem.SetDeleteOnCancel(false); 
                } 
                
                // Mark item as verified to avoid additional file existence verification requests.
                oUploadItem.CustomData.FileExistanceVerified = true;
                
                sItemsList += oUploadItem.GetRelativePath() + '<br/>';
                aExistsUploadItems.push(oUploadItem);
            });

            /* One or more items exists on the server. Show Overwrite / Skip / Cancel dialog.*/
            oConfirmModal.Confirm(WebdavCommon.PasteFormat(sOverwriteDialogueFormat, sItemsList),

                /* A user selected to overwrite existing files. */
                function onOverwrite() {
                
                    // Mark all items that exist on the server with overwrite flag.
                    aExistsUploadItems.forEach(function(oUploadItem) {
                        if(oUploadItem.IsFolder()) return;
                        
                        // The file will be overwritten if it exists on the server.
                        oUploadItem.SetOverwrite(true);
                    });

                    // Add all items to the upload queue.
                    oUploadItemsCreated.Upload(oUploadItemsCreated.Items);
                },

                /* A user selected to skip existing files. */
                function onSkipExists() {
                
                    // Create list of items that do not exist on the server.
                    /** @type {ITHit.WebDAV.Client.Upload.UploadItem[]} aNotExistsUploadItems */
                    var aNotExistsUploadItems = $.grep(oUploadItemsCreated.Items,
                        function(oUploadItem) {
                            return !ITHit.Utils.Contains(aExistsUploadItems, oUploadItem);
                        });

                    // Add only items that do not exist on the server to the upload queue.
                    oUploadItemsCreated.Upload(aNotExistsUploadItems);
                });
        }.bind(this));
    };

    /**
     * @param {ITHit.WebDAV.Client.Upload.UploadItem[]} aUploadItems - Array of items to check.
     * @memberof UploaderGridView.prototype
     */
    UploaderGridView.prototype._ValidateUploadItems = function(aUploadItems) {
        for(var i = 0; i < aUploadItems.length; i++) {
            var oUploadItem = aUploadItems[i];
            //Max file size validation
            //var oExtensionError = this._ValidateExtension(oUploadItem);

            //File extension validation
            //var oSizeError = this._ValidateSize(oUploadItem);

            //Special characters validation
            //var oNameError = this._ValidateName(oUploadItem);

            //var oValidationError = oExtensionError || oSizeError || oNameError;
            //if(oValidationError) {
            //    return oValidationError;
            //}

            var oValidationError = this._ValidateName(oUploadItem);
            if(oValidationError) {
                return oValidationError;
            }
        }
    };

    /**
     * @param {ITHit.WebDAV.Client.Upload.UploadItem} oUploadItem - The item to check.
     * @memberof UploaderGridView.prototype
     * @returns {undefined | WebdavCommon.ClientError} - Undefined if item valid or error object.
     */
    UploaderGridView.prototype._ValidateSize = function(oUploadItem) {
        if(oUploadItem.GetSize() > iMaxFileSize) {
            var sMessage = WebdavCommon.PasteFormat(sWrongFileSizeFormat, WebdavCommon.Formatters.FileSize(iMaxFileSize));
            return new ClientError(sMessage, oUploadItem.GetUrl());
        }
    };

    /**
     * @param {ITHit.WebDAV.Client.Upload.UploadItem} oUploadItem - The item to check.
     * @memberof UploaderGridView.prototype
     * @returns {undefined | WebdavCommon.ClientError} - Undefined if item valid or error object.
     */
    UploaderGridView.prototype._ValidateExtension = function(oUploadItem) {
        var sExtension = WebdavCommon.Formatters.GetExtension(oUploadItem.GetUrl());
        if(aForbiddenExtensions.indexOf(sExtension.toUpperCase()) >= 0) {
            var sMessage = WebdavCommon.PasteFormat(sForbiddenExtensionFormat, sExtension);
            return new ClientError(sMessage, oUploadItem.GetUrl());
        }
    };

    /**
     * @param {ITHit.WebDAV.Client.Upload.UploadItem} oUploadItem - Array of items to check.
     * @memberof UploaderGridView.prototype
     */
    UploaderGridView.prototype._ValidateName = function(oUploadItem) {
        var sValidationMessage = WebdavCommon.Validators.ValidateName(oUploadItem.GetName());
        if(sValidationMessage) {
            return new ClientError(sValidationMessage, oUploadItem.GetUrl());
        }
    };


    /**
     * Verifies if each item in the list exists on the server and returns list of existing items.
     * @callback UploaderGridView~GetExistsAsyncCallback
     * @param {ITHit.WebDAV.Client.AsyncResult} oAsyncResult - The result of operation.
     * @param {ITHit.WebDAV.Client.Upload.UploadItem[]} oAsyncResult.Result - The array of items 
     * that exists on server.
     */

    /**
     * @param {ITHit.WebDAV.Client.Upload.UploadItem[]} aUploadItems - Array of items to check.
     * @param {UploaderGridView~GetExistsAsyncCallback} fCallback - The function to be called when
     * all checks are completed.
     * @memberof UploaderGridView.prototype
     */
    UploaderGridView.prototype._GetExistsAsync = function(aUploadItems, fCallback) {
        this._OpenItemsCollectionAsync(aUploadItems,
            function(aResultCollection) {
                var oFailedResult = ITHit.Utils.FindBy(aResultCollection,
                    function(oResult) {
                        return !(oResult.AsyncResult.IsSuccess || oResult.AsyncResult.Status.Code === 404);
                    },
                    this);

                if(oFailedResult) {
                    fCallback(oFailedResult.AsyncResult);
                    return;
                }

                var aExistsItems = aResultCollection.filter(function(oResult) {
                        return oResult.AsyncResult.IsSuccess;
                    })
                    .map(function(oResult) {
                        return oResult.UploadItem;
                    });

                fCallback(new ITHit.WebDAV.Client.AsyncResult(aExistsItems, true, null));
            });

    };


     /**
      * @typedef {Object} UploaderGridView~OpenItemsCollectionResult
      * @property {ITHit.WebDAV.Client.Upload.UploadItem} UploadItem
      * @property {ITHit.WebDAV.Client.AsyncResult} oAsyncResult - The result of operation.
      */

    /**
     * @callback UploaderGridView~OpenItemsCollectionAsyncCallback
     * @param {UploaderGridView~OpenItemsCollectionResult[]} oResult - The result of operation.
     */

    /**
     * @param {ITHit.WebDAV.Client.Upload.UploadItem[]} aUploadItems - Array of items to check.
     * @param {UploaderGridView~OpenItemsCollectionAsyncCallback} fCallback - The function to 
     * be called when all requests completed.
     * @memberof UploaderGridView.prototype
     */
    UploaderGridView.prototype._OpenItemsCollectionAsync = function(aUploadItems, fCallback) {
        var iCounter = aUploadItems.length;

        /**@type {UploaderGridView~OpenItemsCollectionResult} */
        var aResults = [];
        if(iCounter === 0) {
            fCallback(aResults);
            return;
        }

        aUploadItems.forEach(function(oUploadItem) {
            window.WebDAVController.WebDavSession.OpenItemAsync(ITHit.EncodeURI(oUploadItem.GetUrl()),
                [],
                function(oAsyncResult) {
                    iCounter--;
                    aResults.push({
                        UploadItem: oUploadItem,
                        AsyncResult: oAsyncResult
                    });

                    if(iCounter === 0) {
                        fCallback(aResults);
                    }
                });
        });
    };


    /** 
     * Called when items are added or deleted from upload queue.
     * @param {ITHit.WebDAV.Client.Upload.Queue#event:OnQueueChanged} oQueueChanged - Contains 
     * lists of items added to the upload queue in oQueueChanged.AddedItems property and removed 
     * from the upload queue in oQueueChanged.RemovedItems property.
     */
    UploaderGridView.prototype._QueueChange = function (oQueueChanged) {
    
        // Display each ited added to the upload queue in the grid.
        oQueueChanged.AddedItems.forEach(function (value) {
            var row = new UploaderGridRow(value, this.fileLoadCompleted.bind(this), this._ShowExistsCheckError.bind(this), this._DataBindAllProgress.bind(this));
            this.rows.push(row);
            this.$container.find('.uploading-items').append(row.$el);
        }.bind(this));

        // Remove items deleted from upload queue from the grid.
        oQueueChanged.RemovedItems.forEach(function (value) {
            var aRows = $.grep(this.rows, function (oElem) { return value === oElem.UploadItem; });
            if (aRows.length === 0) return;
            var rowIndex = this.rows.indexOf(aRows[0]);
            this.rows.splice(rowIndex, 1);
            aRows[0].$el.remove();
        }.bind(this));

        if (this.rows.length == 0) {
            this.$container.addClass('d-none');
        } else {
            this.$container.removeClass('d-none');
            this.$uploadingBlock.addClass('show');
            var $uploading = this.$uploadingBlock;
            setTimeout(function() {
                    $uploading.removeClass('show');
                }, 3000);
        }
    };

    UploaderGridView.prototype._DataBindAllProgress = function () {
        var currProgress = 0;
        var count = 0;
        this.rows.forEach(function (value) {
            if (value.UploadItem.GetState() !== 'Canceled') {
                var valueProgress = value.UploadItem.GetProgress().Completed;
                if (valueProgress < 100) {
                    currProgress += valueProgress;
                }
                else {
                    currProgress += 100;
                }
                count++;
            }
        });
        currProgress /= count;
        if (currProgress > 0) {
            var $progress = this.$container.find('.progress-wrapper .progress-bar');
            $progress.attr('aria-valuenow', currProgress).css('width', currProgress + '%');
            this.$uploadingBlock.find('.persent').text(Math.round(currProgress) + '%');
        }
    }

    UploaderGridView.prototype._DataBindUploaderBlock = function () {
        this.$container.find('.pause-all-button').click(this._PauseAllClickHandler.bind(this));
        this.$container.find('.play-all-button').click(this._StartAllClickHandler.bind(this));
        this.$uploadingBlock.find('.details-button').click(this._DetailsClickHandler.bind(this));
        this.$uploadingDetails.find('.close-button').click(this._CloseClickHandler.bind(this));
        this.$container.find('.cancel-all-button').click(this._CancelAllClickHandler.bind(this));
        this._SwitchActions();
    }

    UploaderGridView.prototype._SwitchActions = function () {
        var $playButton = this.$container.find(".play-all-button");
        var $pauseButton = this.$container.find(".pause-all-button");
        if ($playButton.hasClass('d-none')) {
            $pauseButton.addClass('d-none');
            $playButton.removeClass('d-none');
        }
        else {
            $pauseButton.removeClass('d-none');
            $playButton.addClass('d-none');
        }
    };

    UploaderGridView.prototype._DetailsClickHandler = function () {
        this.$uploadingBlock.addClass('hide');
        this.$uploadingDetails.removeClass('d-none');
    }

    UploaderGridView.prototype._CloseClickHandler = function () {
        this.$uploadingBlock.removeClass('hide');
        this.$uploadingDetails.addClass('d-none');
    }


    UploaderGridView.prototype._DisableActions = function () {
        this.$container.find('.cancel-all-button').attr("disabled", 'disabled');
        this.$container.find('.play-all-button').attr("disabled", 'disabled');
        this.$container.find('.pause-all-button').attr("disabled", 'disabled');
    };

    UploaderGridView.prototype._EnableActions = function () {
        this.$container.find('.cancel-all-button').removeAttr("disabled");
        this.$container.find('.play-all-button').removeAttr("disabled");
        this.$container.find('.pause-all-button').removeAttr("disabled");
    };

    UploaderGridView.prototype._StartAllClickHandler = function () {
        this._DisableActions();
        this.rows.forEach(function (value) {
            if (value.UploadItem.GetState() === 'Paused') {
                value._StartClickHandler();
            }
        });
        this._SwitchActions();
        this._EnableActions();
    };

    UploaderGridView.prototype._PauseAllClickHandler = function () {
        this._DisableActions();
        this.rows.forEach(function (value) {
                value._PauseClickHandler();
        });
        this._SwitchActions();
        this._EnableActions();
    };

    UploaderGridView.prototype._CancelAllClickHandler = function () {
        this._DisableActions();
        this.rows.forEach(function (value) {
            value._CancelClickHandler();
        });
        this._SwitchActions();
        this._EnableActions();
    };

    /** 
     * Drag-and-Drop area visual effects.
     */    
    UploaderGridView.prototype._OnDragEnter = function (oEvent) {
        this._dropCounter++;
        $(oEvent.target).closest('#ithit-dropzone').addClass('dropzone');
    };

    UploaderGridView.prototype._OnDragLeave = function (oEvent) {
        this._dropCounter--;
        if (this._dropCounter <= 0) {
            this._dropCounter = 0;
            oEvent.currentTarget.classList.remove('dropzone');
        }
    };

    UploaderGridView.prototype._OnDrop = function (oEvent) {
        this._dropCounter = 0;
        this._dropZone.HtmlElement.classList.remove('dropzone');
        this._dropZone.HtmlElement.querySelectorAll("*").forEach(function (value) {
            value.classList.remove('dropzone');
        });
    };

    UploaderGridView.prototype._ShowExistsCheckError = function(oError, fCallback) {
        window.ErrorModal.Show(sFailedCheckExistsMessage,  oError, fCallback);
    };

    /**
     * Represents uploader grid row and subscribes for upload changes.
     * @param {ITHit.WebDAV.Client.Upload.UploadItem} oUploadItem - Upload item.
     */    
    function UploaderGridRow(oUploadItem, fileLoadCompletedCallback, fileUploadFailedCallback, progressChangedCallback) {
        this.$el = $('<div class="row uploading-item" />');
        this.UploadItem = oUploadItem;
        this.UploadItem.AddListener('OnProgressChanged', '_OnProgress', this);
        this.UploadItem.AddListener('OnStateChanged', '_OnStateChange', this);
        this.UploadItem.AddListener('OnBeforeUploadStarted', this._OnBeforeUploadStarted, this);
        this.UploadItem.AddListener('OnUploadError', this._OnUploadError, this);
        this._Render(oUploadItem);
        this._MaxRetry = 10;
        this._CurrentRetry = 0;
        this._RetryDelay = 10;
        this.fileLoadCompletedCallback = fileLoadCompletedCallback;
        this.fileUploadFailedCallback = fileUploadFailedCallback;
        this.progressChangedCallback = progressChangedCallback;
    };

    /**
     * Creates upload row details view.
     * @param {ITHit.WebDAV.Client.Upload.UploadItem} oUploadItem - Upload item to render details.
     */
    UploaderGridRow.prototype._Render = function (oUploadItem) {

        var $cancelBlock = $('<div class="col-auto px-0" />')
            .append($('<button class="cancel-button float-left" title="Cancel" />').
                click(this._CancelClickHandler.bind(this)));

        var $itemIcon = $('<div class="col-auto px-0 file-wrapper" />').append($('<div class="file-icon" />'));

        var $itemData = $('<div class="col" />')
            .append($('<div class="row align-items-center" />')
                .html(
                    '<div class="col item-name ellipsis"></div>' +
                    '<div class="col-auto item-size"></div>' +
                    '<div class="col-auto retry-message d-none"></div>'
                    ))
            .append($('<div class="row" />')
                .html(
                    '<div class="col">' +
                    '<div class="progress">' +
                    '<div class= "progress-bar" rol="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100"></div>' +
                    '</div>' +
                    '</div>'
                    ))
            .append($('<div class="row justify-content-between mt-1" />')
               .html(
                    '<div class="col-auto item-progress"></div>' +
                    '<div class="col-auto item-speed"></div>'
                    ));

        var $actions = $('<div class="col-auto px-0" />')
            .append($('<button class="pause-button float-right" title="Pause" />').
                click(this._PauseClickHandler.bind(this)))
            .append($('<button class="play-button float-right" title="Resume" />').
                click(this._StartClickHandler.bind(this)));

        this.$el.empty();
        this.$el.append($cancelBlock).append($itemIcon).append($itemData).append($actions);

        this._DataBind(oUploadItem);
    };

    UploaderGridRow.prototype._DataBindActions = function (oUploadItem) {
        if (oUploadItem.GetState() === 'Paused') {
            this.$el.find('.play-button').show();
            this.$el.find('.pause-button').hide();
        }
        else {
            this.$el.find('.play-button').hide();
            this.$el.find('.pause-button').show();
        }
    };

    UploaderGridRow.prototype._DataBind = function (oUploadItem) {
        var $container = this.$el;

        var sFileExtansion = WebdavCommon.Formatters.GetFileExtension(oUploadItem.GetName());
        var $itemIcon = $container.find(".file-icon");
        var oProgress = oUploadItem.GetProgress();
        if (sFileExtansion.length < 5) {
            $itemIcon.addClass('file-' + sFileExtansion);
            $itemIcon.html('<span class="file-extension">' + sFileExtansion.toUpperCase() + '</span>');
        }
        $container.find(".item-name").html('<span>' + oUploadItem.GetName() + '</span>');
        $container.find(".item-size").text(WebdavCommon.Formatters.FileSize(oProgress.TotalBytes));
        $container.find(".item-speed").text(oProgress.Completed + ' % done');
        $container.find(".item-progress").text(WebdavCommon.Formatters.FileSize(oProgress.Speed) + '/sec');

        this._DataBindActions(oUploadItem); 
        var sCurrentState = oUploadItem.GetState();
        if (sCurrentState === 'Completed' || sCurrentState === 'Canceled') {
            this.$el.remove();
            this.fileLoadCompletedCallback();
        }
    };

    UploaderGridRow.prototype._DataBindProgressRow = function (oUploadItem) {
        var oProgress = oUploadItem.GetProgress();
        this.$el.find('.progress-bar').attr('aria-valuenow', oProgress.Completed).css('width', oProgress.Completed + '%');
        this.progressChangedCallback();
    };

    /**
     * Called when upload item state changes.
     * @param {ITHit.WebDAV.Client.Upload.Events.StateChanged} oStateChangedEvent - Provides state change event data such as new state and old state.
     */
    UploaderGridRow.prototype._OnStateChange = function (oStateChanged) {
        this._EnableActions();
        this._RemoveRetryMessage();
        this._DataBindProgressRow(oStateChanged.Sender);
        this._DataBind(oStateChanged.Sender);
    };

    /**
     * Called when upload item progress changes.
     * @param {ITHit.WebDAV.Client.Upload.Events.ProgressChanged} oProgressEvent - Provides progress change event data such as new progress value and old progress value.
     */
    UploaderGridRow.prototype._OnProgress = function (oProgressEvent) {
        this._DataBindProgressRow(oProgressEvent.Sender);
        this._DataBind(oProgressEvent.Sender);
    };

    UploaderGridRow.prototype._StartClickHandler = function () {
        this._DisableActions();
        this._CurrentRetry = 0;
        this.UploadItem.StartAsync(this._EnableActions.bind(this));
    };

    UploaderGridRow.prototype._PauseClickHandler = function () {
        this._DisableActions();
        this._CancelRetry();
        this.UploadItem.PauseAsync(this._EnableActions.bind(this));
    };

    UploaderGridRow.prototype._CancelClickHandler = function () {
        this._CancelRetry();
        this._DisableActions();
        this.UploadItem.CancelAsync(null, null, this._EnableActions.bind(this));
    };

    UploaderGridRow.prototype._DisableActions = function () {
        this.$el.find('.cancel-button').attr("disabled", 'disabled');
        this.$el.find('.play-button').attr("disabled", 'disabled');
        this.$el.find('.pause-button').attr("disabled", 'disabled');
    };

    UploaderGridRow.prototype._EnableActions = function () {
        this.$el.find('.cancel-button').removeAttr("disabled");
        this.$el.find('.play-button').removeAttr("disabled");
        this.$el.find('.pause-button').removeAttr("disabled");
    };

    
    /**
     * Called before item upload starts.
     * Here you can make additional checks and validation.
     * @param {ITHit.WebDAV.Client.Upload.UploadItem#event:OnBeforeUploadStarted} oBeforeUploadStarted 
     */
    UploaderGridRow.prototype._OnBeforeUploadStarted = function (oBeforeUploadStarted) {

        // If the file does not exists on the server (verified when item was selected for upload) 
        // or it must be overwritten we start the upload.  
        /** @type {ITHit.WebDAV.Client.Upload.UploadItem} oItem */
        var oItem = oBeforeUploadStarted.Sender;
        if (oItem.GetOverwrite() || oItem.IsFolder() || oItem.CustomData.FileExistanceVerified) {
            oBeforeUploadStarted.Upload();
            return;
        }

        // Otherwise (item exitence verification failed, the server was down or network 
        // connection error orrured when item was selected for upload), 
        // below we verify that item does not exist on the server and upload can be started.
        var sHref = ITHit.EncodeURI(oItem.GetUrl());
        window.WebDAVController.WebDavSession.OpenItemAsync(sHref,
            [],
            function (oAsyncResult) {
                if (!oAsyncResult.IsSuccess && oAsyncResult.Status.Code === 404) {
                
                    // The file does not exist on the server, start the upload.
                    oBeforeUploadStarted.Upload();
                    return;
                }

                if(!oAsyncResult.IsSuccess) {
                
                    // An error during the request occured, do not upload file, set item error state.
                    this.fileUploadFailedCallback(oAsyncResult.Error,
                        function() {
                            oBeforeUploadStarted.Sender.SetFailed(oAsyncResult.Error);
                        });

                    return;
                }

                var sMessage = WebdavCommon.PasteFormat(sOverwriteDialogueFormat, oItem.GetRelativePath());

                // The file exists on the server, ask a user if it must be overwritten. 
                oConfirmModal.Confirm(sMessage,
                    
                    /* A user selected to overwrite existing file. */
                    function onOverwrite() {
                    
                        // Do not delete item if upload canceled (it existed before the upload).
                        oBeforeUploadStarted.Sender.SetDeleteOnCancel(false);
                        
                        // The item will be overwritten if it exists on the server.
                        oBeforeUploadStarted.Sender.SetOverwrite(true); 
                        
                        // All async requests completed - start upload.
                        oBeforeUploadStarted.Upload();
                    });
 
            }.bind(this));
    };
    
    UploaderGridRow.prototype._SetRetryMessage = function (timeLeft) {
        var sMessage = WebdavCommon.PasteFormat(sRetryMessageFormat, WebdavCommon.Formatters.TimeSpan(Math.ceil(timeLeft / 1000)));
        this.$el.find('.retry-message').html(sMessage).addClass('text-danger d-block');
        this.$el.find('.progress-bar').addClass('bg-danger');
    };

    UploaderGridRow.prototype._RemoveRetryMessage = function () {
        this.$el.find('.retry-message').html("");
        this.$el.find('.progress-bar').removeClass('bg-danger d-none');
        this._DataBind(this.UploadItem);
    };

    UploaderGridRow.prototype._CancelRetry = function () {
        if (this.CancelRetryCallback) this.CancelRetryCallback.call(this);
    };
    
    /**
     * Called when upload error occurs.
     * Here you can retry upload or analyze error returned by the server and show error UI 
     * to the user, for example if upload validation failed on the server-side.
     * @param {ITHit.WebDAV.Client.Upload.Events.UploadError} oUploadError - Contains 
     * WebDavException in UploadError.Error property as well as functions to restart the 
     * upload or stop the upload.
     */
    UploaderGridRow.prototype._OnUploadError = function (oUploadError) {
        
        // Here you can verify error code returned by the server and show error UI, 
        // for example if server-side validation failed.
    
        // Stop upload if max upload retries reached.
        if (this._MaxRetry <= this._CurrentRetry) {
            oUploadError.Skip();
            return;
        }
        
        // Retry upload.
        var retryTime = (new Date()).getTime() + (this._RetryDelay * 1000);
        var retryTimerId = setInterval(function () {
            var timeLeft = retryTime - (new Date()).getTime();
            if (timeLeft > 0) {
                this._SetRetryMessage(timeLeft);
                return;
            }
            clearInterval(retryTimerId);
            this._CurrentRetry++;
            this._RemoveRetryMessage();
            
            // Request number of bytes succesefully saved on the server 
            // and retry upload from next byte.
            oUploadError.Retry();
            
        }.bind(this), 1000);
        this.CancelRetryCallback = function () {
            clearInterval(retryTimerId);
            this._RemoveRetryMessage();
        }
    };

    var oConfirmModal = new ConfirmRewriteModal('#ConfirmRewriteModal');
    window.WebDAVUploaderGridView = new UploaderGridView('.ithit-grid-uploads');
})(WebdavCommon);