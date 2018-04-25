(function () {
    var Formatters = {

        /**
         *
         * @param {number} iSize
         * @returns {string}
         */
        FileSize: function (iSize) {
            if (!iSize) {
                return '0.00 B';
            }
            var i = Math.floor(Math.log(iSize) / Math.log(1024));
            return (iSize / Math.pow(1024, i)).toFixed(2) + ' ' + ['B', 'kB', 'MB', 'GB', 'TB'][i];
        },


        /**
         *
         * @param {number} iSeconds
         * @returns {string}
         */
        TimeSpan: function (iSeconds) {
            var hours = Math.floor(iSeconds / 3600);
            var minutes = Math.floor((iSeconds - hours * 3600) / 60);
            var seconds = iSeconds - (hours * 3600) - (minutes * 60)
            var sResult = '';
            if (hours) sResult += hours + 'h ';
            if (minutes) sResult += minutes + 'm ';
            sResult += seconds + 's ';
            return sResult;
        }

    };

    ////////////////
    // Uploader Grid View
    function UploaderGridView(sSelector) {

        this.Uploader = new ITHit.WebDAV.Client.Upload.Uploader();
        this._dropCounter = 0;

        var input = this.Uploader.Inputs.AddById('ithit-hidden-input');
        this._dropZone = this.Uploader.DropZones.AddById('ithit-dropzone');
        this._dropZone.HtmlElement.addEventListener('dragenter', this._OnDragEnter.bind(this), false);
        this._dropZone.HtmlElement.addEventListener('dragleave', this._OnDragLeave.bind(this), false);
        this._dropZone.HtmlElement.addEventListener('drop', this._OnDrop.bind(this), false);
        $(this._dropZone.HtmlElement).on('click', function (event) {
            $(input.HtmlElement).trigger('click');
        });

        this.Uploader.SetUploadUrl(ITHit.WebDAV.Client.Encoder.Decode(window.location.toString()));
        this.Uploader.Queue.AddListener('OnQueueChanged', '_QueueChange', this);
        this.$table = $(sSelector);
        this.rows = [];
        this.fileLoadCompleted = function () {
            if (this.$table.find('td').length == 0)
                this.$table.addClass('d-none');
            window.WebDAVController.Reload();
        }
    };

    UploaderGridView.prototype._QueueChange = function (oQueueChanged) {

        $.each(oQueueChanged.AddedItems, function (index, value) {
            var row = new UploaderGridRow(value, this.fileLoadCompleted.bind(this));
            this.rows.push(row);
            this.$table.append(row.$el);
            this.$table.append(row.$progressBarRow);
        }.bind(this));

        $.each(oQueueChanged.RemovedItems, function (index, value) {
            var aRows = $.grep(this.rows, function (oElem) { return value === oElem.UploadItem; });
            if (aRows.length === 0) return;
            var rowIndex = this.rows.indexOf(aRows[0]);
            this.rows.splice(rowIndex, 1);
            aRows[0].$el.remove();
            aRows[0].$progressBarRow.remove();
        }.bind(this));

        if (this.rows.length == 0) {
            this.$table.addClass('d-none');
        } else {
            this.$table.removeClass('d-none');
        }
    };

    UploaderGridView.prototype._OnDragEnter = function (oEvent) {
        this._dropCounter++;
        $(oEvent.target).closest('#ithit-dropzone').addClass('bg-info');
    };

    UploaderGridView.prototype._OnDragLeave = function (oEvent) {
        this._dropCounter--;
        if (this._dropCounter <= 0) {
            this._dropCounter = 0;
            oEvent.currentTarget.classList.remove('bg-info');
        }
    };

    UploaderGridView.prototype._OnDrop = function (oEvent) {
        this._dropCounter = 0;
        this._dropZone.HtmlElement.classList.remove('bg-info');
        $.each(this._dropZone.HtmlElement.querySelectorAll("*"), function (index, value) {
            value.classList.remove('bg-info');
        });
    };

    ////////////////
    // Uploader Grid Row
    function UploaderGridRow(oUploadItem, fileLoadCompletedCallback) {
        this.$el = $('<tr></tr>');
        this.$progressBarRow = $('<tr></tr>');
        this.UploadItem = oUploadItem;
        this.UploadItem.AddListener('OnProgressChanged', '_OnProgress', this);
        this.UploadItem.AddListener('OnStateChanged', '_OnStateChange', this);
        this._Render(oUploadItem);
        this._RenderProgressRow(oUploadItem);
        this.fileLoadCompletedCallback = fileLoadCompletedCallback;
    };

    UploaderGridRow.prototype._Render = function (oUploadItem) {

        var oProgress = oUploadItem.GetProgress();
        var columns = [
            'ellipsis',
            'd-none d-sm-table-cell text-right',
            'd-none d-sm-table-cell text-right',
            'd-none d-sm-table-cell text-right',
            'd-none d-md-table-cell custom-hidden text-right',
            'text-right',
            'd-none d-md-table-cell text-right',
            'd-none d-md-table-cell custom-hidden'
        ];

        var $columns = [];
        columns.forEach(function (item) {
            var $column = $('<td></td>');
            $column.addClass(item);
            $columns.push($column);
        });

        var $actions = $('<td class="column-action"></td>');
        this._RenderActions(oUploadItem).forEach(function (item) {
            $actions.append(item);
        });

        $columns.push($actions);
        this.$el.empty();
        this.$el.append($columns);

        this._DataBind(oUploadItem);
    };

    UploaderGridRow.prototype._RenderActions = function (oUploadItem) {
        var actions = [];
        actions.push($('<button class="btn btn-transparent" />').
        html('<span class="fas fa-play text-primary"></span>').
        click(this._StartClickHandler.bind(this)));

        actions.push($('<button class="btn btn-transparent" />').
        html('<span class="fas fa-pause text-primary"></span>').
        click(this._PauseClickHandler.bind(this)));

        actions.push($('<button class="btn btn-transparent .lnk-cancel" />').
        html('<span class="fas fa-trash-alt text-primary"></span>').
        click(this._CancelClickHandler.bind(this)));

        return actions;
    };

    UploaderGridRow.prototype._DataBindActions = function (oUploadItem) {
        if (oUploadItem.GetState() !== 'Uploading') {
            this.$el.children().last().children().eq(1).hide();
            this.$el.children().last().children().eq(0).show();
        }
        else {
            this.$el.children().last().children().eq(0).hide();
            this.$el.children().last().children().eq(1).show();
        }
    };

    UploaderGridRow.prototype._DataBind = function (oUploadItem) {
        var oProgress = oUploadItem.GetProgress();
        var $tr = this.$el;
        var columns = [
            '<span>' + oUploadItem.GetName() + '</span>',
            Formatters.FileSize(oUploadItem.GetSize()),
            Formatters.FileSize(oProgress.UploadedBytes),
            oProgress.Completed + ' %',
            Formatters.TimeSpan(oProgress.ElapsedTime),
            Formatters.TimeSpan(oProgress.RemainingTime),
            Formatters.FileSize(oProgress.Speed) + '/s',
            oUploadItem.GetState()
        ];

        columns.forEach(function (item, index) {
            $tr.children().eq(index).html(item);
        });

        this._DataBindActions(oUploadItem);
        var sCurrentState = oUploadItem.GetState();
        if (sCurrentState === 'Completed' || sCurrentState === 'Canceled') {
            this.$el.remove();
            this.fileLoadCompletedCallback();
        }
    };

    UploaderGridRow.prototype._RenderProgressRow = function () {
        var $td = $('<td colspan="9" class="progress-container"></td>');
        var $progressBar = $('<div class="progress"><div class="progress-bar" role="progressbar" aria-valuenow="2" aria-valuemin="0" aria-valuemax="100"></div></div>');
        $td.append($progressBar);

        this.$progressBarRow.empty();
        this.$progressBarRow.append($td);
    };

    UploaderGridRow.prototype._DataBindProgressRow = function (oUploadItem) {
        var oProgress = oUploadItem.GetProgress();
        this.$progressBarRow.find('.progress-bar').css('width', oProgress.Completed + '%');

        var sCurrentState = oUploadItem.GetState();
        if (sCurrentState === 'Completed' || sCurrentState === 'Canceled') {
            this.$progressBarRow.remove();
        }
    };

    UploaderGridRow.prototype._OnStateChange = function (oStateChanged) {
        this._EnableActions();
        this._DataBindProgressRow(oStateChanged.Sender);
        this._DataBind(oStateChanged.Sender);
    };

    UploaderGridRow.prototype._OnProgress = function (oProgressEvent) {
        this._DataBindProgressRow(oProgressEvent.Sender);
        this._DataBind(oProgressEvent.Sender);
    };

    UploaderGridRow.prototype._OnLoadEnd = function (oSender, oEvent) {
        this.$el.remove();
        this.$progressBarRow.remove();
    };

    UploaderGridRow.prototype._StartClickHandler = function () {
        this._DisableActions();
        this.UploadItem.StartAsync();
    };

    UploaderGridRow.prototype._PauseClickHandler = function () {
        this._DisableActions();
        this.UploadItem.PauseAsync();
    };

    UploaderGridRow.prototype._CancelClickHandler = function () {
        this._DisableActions();
        this.UploadItem.CancelAsync();
    };

    UploaderGridRow.prototype._DisableActions = function () {
        this.$el.children().last().children().slice(-3).attr("disabled", 'disabled');
    };

    UploaderGridRow.prototype._EnableActions = function () {
        this.$el.children().last().children().slice(-3).removeAttr("disabled");
    };


    var oUploaderGrid = new UploaderGridView('.ithit-grid-uploads');
})();