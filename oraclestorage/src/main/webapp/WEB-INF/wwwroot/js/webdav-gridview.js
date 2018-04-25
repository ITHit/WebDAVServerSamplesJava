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
         * @param {Date} oDate
         * @returns {string}
         */
        Date: function (oDate) {
            return [
                    ('0' + (oDate.getMonth() + 1)).slice(-2),
                    ('0' + oDate.getDate()).slice(-2),
                    oDate.getFullYear()
                ].join('/') +
                ' ' +
                [
                    ('0' + oDate.getHours() % 12 || 12).slice(-2),
                    ('0' + oDate.getMinutes()).slice(-2),
                    ('0' + oDate.getSeconds()).slice(-2)
                ].join(':') +
                ' ' +
                (oDate.getHours() > 12 ? 'PM' : 'AM');
        },


        /**
         *
         * @param {string} html
         * @returns {string}
         */
        Snippet: function (html) {
            if (html) {
                var safePrefix = '__b__tag' + (new Date()).getTime();
                html = html.replace(/<b>/g, safePrefix + '_0').replace(/<\/b>/g, safePrefix + '_1');
                html = $('<div />').text(html).text();
                html = html.replace(new RegExp(safePrefix + '_0', 'g'), '<b>').
                replace(new RegExp(safePrefix + '_1', 'g'), '</b>');
            }
            return $('<div />').addClass('snippet').html(html);
        }

    };

    ///////////////////
    // Folder Grid View
    var FolderGridView = function (selector) {
        this.$el = $(selector);
        this.IsSearchMode = false;

        $(selector).on({
            mouseenter: function () {
                if ($(this).hasClass('tr-snippet-url'))
                    $(this).addClass('hover').prev().addClass('hover');
                else
                    $(this).addClass('hover').next().addClass('hover');
            },
            mouseleave: function () {
                if ($(this).hasClass('tr-snippet-url'))
                    $(this).removeClass('hover').prev().removeClass('hover');
                else
                    $(this).removeClass('hover').next().removeClass('hover');
            }
        }, 'tr');
    };
    FolderGridView.prototype = {

        Render: function (aItems, bIsSearchMode) {
            this.IsSearchMode = bIsSearchMode || false;

            // Sort by display name
            if (''.localeCompare) {
                aItems.sort(function (a, b) {
                    return a.DisplayName.localeCompare(b.DisplayName);
                });
            } else {
                aItems.sort(function (a, b) {
                    return a.DisplayName < b.DisplayName ? -1 : (a.DisplayName > b.DisplayName ? 1 : 0);
                });
            }

            // Folders at first
            aItems = [].concat(aItems.filter(function (oItem) {
                return oItem.IsFolder();
            })).concat(aItems.filter(function (oItem) {
                return !oItem.IsFolder();
            }));

            this.$el.find('tbody').html(
                aItems.map(function (oItem, i) {
                    var locked = oItem.ActiveLocks.length > 0
                        ? '<span class="ithit-grid-icon-locked fas fa-lock"></span>'
                        : '';
                    /** @type {ITHit.WebDAV.Client.HierarchyItem} oItem */
                    return $('<div/>').html([
                        $('<tr />').html([
                            $('<td class="d-none d-sm-table-cell" />').text(i + 1),
                            $('<td />').
                            html(oItem.IsFolder() ? '<span class="fas fa-folder">' + locked +
                                '</span>' : locked),
                            this._RenderDisplayName(oItem),
                            $('<td />').
                            text(!oItem.IsFolder() ? Formatters.FileSize(oItem.ContentLength) : '').
                            css('text-align', 'right'),
                            $('<td class="d-none d-sm-table-cell" />').text(Formatters.Date(oItem.LastModified)),
                            $('<td class="column-action" />').html(this._RenderActions(oItem))
                        ]),
                        $('<tr class="tr-snippet-url"/>').html([
                            $('<td class="d-none d-sm-table-cell" />'),
                            $('<td class="d-none d-sm-table-cell" />'),
                            this._RenderSnippetAndUrl(oItem)])]).children();
                }.bind(this))
            );
        },

        /**
         * @param {ITHit.WebDAV.Client.HierarchyItem} oItem
         **/
        _RenderDisplayName: function (oItem) {
            var oElement = oItem.IsFolder() ?
                $('<td class="ellipsis" />').html($('<a />').text(oItem.DisplayName).attr('href', oItem.Href)) :
                $('<td class="ellipsis" />').html($('<span />').text(oItem.DisplayName));

            return oElement;
        },
        _RenderSnippetAndUrl: function (oItem) {
            var oElement = $('<td colspan="10"/>');
            // Append path on search mode
            if (this.IsSearchMode) {
                new BreadcrumbsView($('<ol />').addClass('breadcrumb').appendTo(oElement)).SetHierarchyItem(oItem);

                // Append snippet to name
                oElement.append(Formatters.Snippet(oItem.Properties.Find(oWebDAV.SnippetPropertyName)));
            }

            return oElement;
        },

        /**
         * @param {ITHit.WebDAV.Client.HierarchyItem} oItem
         * @returns string
         **/
        _RenderActions: function (oItem) {
            var actions = [];
            var isDavProtocolSupported = ITHit.WebDAV.Client.DocManager.IsDavProtocolSupported();
            var isMicrosoftOfficeDocument = ITHit.WebDAV.Client.DocManager.IsMicrosoftOfficeDocument(oItem.Href);

            if (oItem.IsFolder()) {
                actions.push($('<button class="btn btn-transparent browse-lnk" type="button"/>').
                html('<span class="fas fa-hdd"></span> <span class="d-none d-md-inline">Browse</span>').
                attr('title', 'Open this folder in Operating System file manager.').
                on('click', function () {
                    oWebDAV.OpenFolderInOsFileManager(oItem.Href);
                }).prop("disabled", !isDavProtocolSupported));
            } else {
                actions.push($('<button class="btn btn-transparent"/>').
                html('<span class="fas fa-edit"></span> <span class="d-none d-md-inline">Edit</span>').
                attr('title', 'Edit document in associated application.').
                on('click', function () {
                    oWebDAV.EditDoc(oItem.Href);
                }).prop("disabled", !isDavProtocolSupported && !isMicrosoftOfficeDocument));
                actions.push($('<button class="btn btn-transparent"/>')
                    .html('<span class="fas fa-window-maximize"></span>')
                    .attr('title', 'Select application to open this file with.')
                    .on('click', function () {
                        oWebDAV.OpenDocWith(oItem.Href);
                    }).prop("disabled", !isDavProtocolSupported));
            }

            actions.push($('<button class="btn btn-transparent"/>')
                .html('<span class="fas fa-trash-alt"></span>')
                .attr('title', 'Delete Document')
                .on('click', function () {
                    oWebDAV.DeleteHierarchyItem(oItem);
                }));

            return actions;
        }
    };

    ///////////////////
    // Search Form View
    var SearchFormView = function (selector) {
        this.$el = $(selector);
        this.Init();
    };
    SearchFormView.prototype = {

        Init: function () {
            this.$el.find('button').on('click', this._OnSubmit.bind(this));
            this.$el.find('input').typeahead({},
                {
                    name: 'states',
                    display: 'DisplayName',
                    limit: 6,
                    templates: {
                        suggestion: this._RenderSuggestion.bind(this)
                    },
                    async: true,
                    source: this._Source.bind(this)
                }
            ).on('keyup', this._OnKeyUp.bind(this)).on('typeahead:select', this._OnSelect.bind(this));
        },

        SetDisabled: function (bIsDisabled) {
            this.$el.find('button').prop('disabled', bIsDisabled);
            this.$el.find('input').
            prop('disabled', bIsDisabled).
            attr('placeholder', !bIsDisabled ? '' : 'The server does not support search');
        },

        GetValue: function () {
            return this.$el.find('input.tt-input').val();
        },

        _Source: function (sPhrase, c, fCallback) {
            oWebDAV.NavigateSearch(sPhrase, false, function (aItems) {
                fCallback(aItems);
            });
        },

        _OnKeyUp: function (oEvent) {
            if (oEvent.keyCode === 13) {
                oWebDAV.NavigateSearch(oSearchForm.GetValue(), false, function (aItems) {
                    oFolderGrid.Render(aItems, true);
                });
                this.$el.find('input').typeahead('close');
                this._HideKeyboard(this.$el.find('input'));
            }
        },

        _OnSelect: function (oEvent, oItem) {
            oFolderGrid.Render([oItem], true);
        },

        _OnSubmit: function () {
            oWebDAV.NavigateSearch(oSearchForm.GetValue(), false, function (aItems) {
                oFolderGrid.Render(aItems, true);
            });
        },

        /**
         * @param {ITHit.WebDAV.Client.HierarchyItem} oItem
         **/
        _RenderSuggestion: function (oItem) {
            var oElement = $('<div />').text(oItem.DisplayName);

            // Append path
            new BreadcrumbsView($('<ol />').addClass('breadcrumb').appendTo(oElement)).SetHierarchyItem(oItem);

            // Append snippet
            oElement.append(Formatters.Snippet(oItem.Properties.Find(oWebDAV.SnippetPropertyName)));

            return oElement;
        },

        /**
         * @param {JQuery obeject} element
         **/
        _HideKeyboard: function (element) {
            element.attr('readonly', 'readonly'); // Force keyboard to hide on input field.
            element.attr('disabled', 'true'); // Force keyboard to hide on textarea field.
            setTimeout(function () {
                element.blur();  //actually close the keyboard
                // Remove readonly attribute after keyboard is hidden.
                element.removeAttr('readonly');
                element.removeAttr('disabled');
            }, 100);
        }

    };

    ///////////////////
    // Breadcrumbs View
    var BreadcrumbsView = function (selector) {
        this.$el = $(selector);
    };
    BreadcrumbsView.prototype = {

        /**
         * @param {ITHit.WebDAV.Client.HierarchyItem} oItem
         */
        SetHierarchyItem: function (oItem) {
            var aParts = oItem.Href
                .split('/')
                .slice(2)
                .filter(function (v) {
                    return v;
                });

            this.$el.html(aParts.map(function (sPart, i) {
                var bIsLast = aParts.length === i + 1;
                var oLabel = i === 0 ? $('<span />').addClass('fas fa-home') : $('<span />').text(decodeURIComponent(sPart));
                return $('<li class="breadcrumb-item"/>').toggleClass('active', bIsLast).append(
                    bIsLast ?
                        $('<span />').html(oLabel) :
                        $('<a />').attr('href', location.protocol + '//' + aParts.slice(0, i + 1).join('/') + '/').html(oLabel)
                );
            }));
        }

    };

    /////////////////////////
    // History Api Controller
    var HistoryApiController = function (selector) {
        this.$container = $(selector);
        this.Init();
    };
    HistoryApiController.prototype = {

        Init: function () {
            if (!this._IsBrowserSupport()) {
                return;
            }

            window.addEventListener('popstate', this._OnPopState.bind(this), false);
            this.$container.on('click', this._OnLinkClick.bind(this));
        },

        _OnPopState: function (oEvent) {
            var sUrl = oEvent.state && oEvent.state.Url || location.href;
            oWebDAV.NavigateFolder(sUrl);
        },

        _OnLinkClick: function (oEvent) {
            var sUrl = $(oEvent.target).closest('a').attr('href');
            if (!sUrl) {
                return;
            }

            if (sUrl.indexOf((location.origin || location.href.replace(location.pathname, ''))) !== 0) {
                return;
            }

            oEvent.preventDefault();

            history.pushState({ Url: sUrl }, '', sUrl);
            oWebDAV.NavigateFolder(sUrl);
        },

        _IsBrowserSupport: function () {
            return !!(window.history && history.pushState);
        }

    };

    ///////////////////
    // Confirm Bootstrap Modal
    var ConfirmModal = function (selector) {
        var self = this;
        this.$el = $(selector);
        this.$el.find('.btn-ok').click(function (e) {
            if (self.successfulCallback) {
                self.successfulCallback();
            }
            self.$el.modal('hide');
        });
    }
    ConfirmModal.prototype = {
        Confirm: function (htmlMessage, successfulCallback, options) {
            var $modalDialog = this.$el.find('.modal-dialog');
            this.successfulCallback = successfulCallback;
            this.$el.find('.message').html(htmlMessage);
            if (options && options.size == 'lg')
                $modalDialog.removeClass('modal-sm');
            else
                $modalDialog.addClass('modal-sm');

            this.$el.modal('show');
        }
    }

    ///////////////////
    // Create Folder Bootstrap Modal
    var CreateFolderModal = function (modalSelector, buttonSelector) {
        var self = this;
        this.$modal = $(modalSelector);
        this.$txt = $(modalSelector).find('input[type="text"]');
        this.$submitButton = $(modalSelector).find('.btn-submit');
        this.$alert = $(modalSelector).find('.alert-danger');

        $(buttonSelector).click(function () {
            self.$txt.val('');
            self.$alert.addClass('d-none');
            self.$modal.modal('show');
        });

        this.$modal.on('shown.bs.modal', function () {
            self.$txt.focus();
        })
        this.$modal.find('form').submit(function () {
            self.$alert.addClass('d-none');
            if (self.$txt.val() !== null && self.$txt.val().match(/^ *$/) === null) {
                self.$txt.blur();
                self.$submitButton.attr('disabled', 'disabled');
                oWebDAV.CreateFolder(self.$txt.val().trim(), function (oAsyncResult) {
                    if (oAsyncResult.Error instanceof ITHit.WebDAV.Client.Exceptions.MethodNotAllowedException) {
                        self.$alert.removeClass('d-none').text(oAsyncResult.Error.Error.Description ? oAsyncResult.Error.Error.Description : 'Folder already exists.');
                    }
                    else {
                        self.$modal.modal('hide');
                    }
                    self.$submitButton.removeAttr('disabled');
                })
            }
            else {
                self.$alert.removeClass('d-none').text('Name is required!');
            }
            return false;
        });
    }

    var WebDAVController = function () {
        this.CurrentFolder = null;
        this.WebDavSession = new ITHit.WebDAV.Client.WebDavSession();
        this.SnippetPropertyName = new ITHit.WebDAV.Client.PropertyName('snippet', 'ithit');
    };

    WebDAVController.prototype = {

        Reload: function () {
            if (this.CurrentFolder) {
                this.NavigateFolder(this.CurrentFolder.Href);
            }
        },

        NavigateFolder: function (sPath) {
            this.WebDavSession.OpenFolderAsync(sPath, [], function (oResponse) {
                this.CurrentFolder = oResponse.Result;
                oBreadcrumbs.SetHierarchyItem(this.CurrentFolder);

                // Detect search support. If search is not supported - disable search field.
                this.CurrentFolder.GetSupportedFeaturesAsync(function (oResult) {
                    /** @typedef {ITHit.WebDAV.Client.OptionsInfo} oOptionsInfo */
                    var oOptionsInfo = oResult.Result;

                    oSearchForm.SetDisabled(!(oOptionsInfo.Features & ITHit.WebDAV.Client.Features.Dasl));
                });

                this.CurrentFolder.GetChildrenAsync(false, [], function (oResult) {
                    /** @type {ITHit.WebDAV.Client.HierarchyItem[]} aItems */
                    var aItems = oResult.Result;

                    oFolderGrid.Render(aItems, false);
                })
            }.bind(this));
        },

        NavigateSearch: function (sPhrase, bIsDynamic, fCallback) {
            if (!this.CurrentFolder) {
                fCallback && fCallback([]);
                return;
            }

            if (sPhrase === '') {
                this.Reload();
                return;
            }

            // The DASL search phrase can contain wildcard characters and escape according to DASL rules:
            //   ‘%’ – to indicate one or more character.
            //   ‘_’ – to indicate exactly one character.
            // If ‘%’, ‘_’ or ‘\’ characters are used in search phrase they are escaped as ‘\%’, ‘\_’ and ‘\\’.
            var searchQuery = new ITHit.WebDAV.Client.SearchQuery();
            searchQuery.Phrase = sPhrase.replace(/\\/g, '\\\\').
            replace(/\%/g, '\\%').
            replace(/\_/g, '\\_').
            replace(/\*/g, '%').
            replace(/\?/g, '_') + '%';
            searchQuery.EnableContains = !bIsDynamic;  //Enable/disable search in file content.

            // Get following additional properties from server in search results: snippet - text around search phrase.
            searchQuery.SelectProperties = [
                this.SnippetPropertyName
            ];

            this.CurrentFolder.SearchByQueryAsync(searchQuery, function (oResult) {
                /** @type {ITHit.WebDAV.Client.AsyncResult} oResult */

                /** @type {ITHit.WebDAV.Client.HierarchyItem[]} aItems */
                var aItems = oResult.Result;

                fCallback && fCallback(aItems);
            });
        },

        /**
         * Opens document for editing.
         * @param {string} sDocumentUrl Must be full path including domain name: https://webdavserver.com/path/file.ext
         */
        EditDoc: function (sDocumentUrl) {
            if (webDavSettings.EditDocAuth.Authentication.toLowerCase() === 'cookies') {
                ITHit.WebDAV.Client.DocManager.DavProtocolEditDocument(sDocumentUrl, this.GetMountUrl(), this._ProtocolInstallMessage.bind(this), null, webDavSettings.EditDocAuth.SearchIn,
                    webDavSettings.EditDocAuth.CookieNames, webDavSettings.EditDocAuth.LoginUrl);
            }
            else {
                ITHit.WebDAV.Client.DocManager.EditDocument(sDocumentUrl, this.GetMountUrl(), this._ProtocolInstallMessage.bind(this));
            }
        },

        /**
         * Opens document with.
         * @param {string} sDocumentUrl Must be full path including domain name: https://webdavserver.com/path/file.ext
         */
        OpenDocWith: function (sDocumentUrl) {
            ITHit.WebDAV.Client.DocManager.DavProtocolEditDocument(sDocumentUrl, this.GetMountUrl(), this._ProtocolInstallMessage.bind(this), null, webDavSettings.EditDocAuth.SearchIn,
                webDavSettings.EditDocAuth.CookieNames, webDavSettings.EditDocAuth.LoginUrl, 'OpenWith');
        },

        /**
         * Deletes document.
         * @param {string} sDocumentUrl Must be full path including domain name: https://webdavserver.com/path/file.ext
         */
        DeleteHierarchyItem: function (oItem) {
            oConfirmModal.Confirm('Are you sure want to delete ' + oItem.DisplayName + '?', function () {
                oItem.DeleteAsync(null);
            });
        },

        /**
         * Opens current folder in OS file manager.
         */
        OpenCurrentFolderInOsFileManager: function () {
            this.OpenFolderInOsFileManager(this.CurrentFolder.Href);
        },

        /**
         * Opens folder in OS file manager.
         * @param {string} sFolderUrl Must be full path including domain name: https://webdavserver.com/path/
         */
        OpenFolderInOsFileManager: function (sFolderUrl) {
            ITHit.WebDAV.Client.DocManager.OpenFolderInOsFileManager(sFolderUrl, this.GetMountUrl(), this._ProtocolInstallMessage.bind(this), null, webDavSettings.EditDocAuth.SearchIn,
                webDavSettings.EditDocAuth.CookieNames, webDavSettings.EditDocAuth.LoginUrl);
        },

        /**
         * @return {string}
         **/
        GetMountUrl: function () {
            // Web Folders on Windows XP require port, even if it is a default port 80 or 443.
            var port = window.location.port || (window.location.protocol == 'http:' ? 80 : 443);

            return window.location.protocol + '//' + window.location.hostname + ':' + port + webDavSettings.ApplicationPath;
        },

        /**
         * Creates new folder in current folder.
         */
        CreateFolder: function (sFolderName, fCallback) {
            this.CurrentFolder.CreateFolderAsync(sFolderName, null, null, function (oAsyncResult) {
                fCallback(oAsyncResult);
            });
        },

        /**
         * Function to be called when document or OS file manager failed to open.
         * @private
         */
        _ProtocolInstallMessage: function () {
            if (ITHit.WebDAV.Client.DocManager.IsDavProtocolSupported()) {
                oConfirmModal.Confirm('This action requires a protocol installation. <br/><br/>' +
                    'Make sure a web browser extension is enabled after protocol installation.<br/>' +
                    '<a href="https://www.webdavsystem.com/ajax/programming/open-doc-webpage/install/' +
                    (ITHit.DetectBrowser.Browser ? ('#' + ITHit.DetectBrowser.Browser.toLowerCase()) : '') +
                    '">How to check that the web browser extension is enabled.</a><br/><br/>' +
                    'Select OK to download the protocol installer.', function () {
                    // IT Hit WebDAV Ajax Library protocol installers path.
                    // Used to open non-MS Office documents or if MS Office is
                    // not installed as well as to open OS File Manager.

                    var installerFilePath = webDavSettings.ApplicationProtocolsPath + ITHit.WebDAV.Client.DocManager.GetInstallFileName();
                    window.open(installerFilePath);
                }, { size: 'lg' });
            }
        }

    };

    var oFolderGrid = new FolderGridView('.ithit-grid-container');
    var oSearchForm = new SearchFormView('.ithit-search-container');
    var oBreadcrumbs = new BreadcrumbsView('.ithit-breadcrumb-container');
    var oHistoryApi = new HistoryApiController('.ithit-grid-container, .ithit-breadcrumb-container');
    var oWebDAV = window.WebDAVController = new WebDAVController();
    var oConfirmModal = new ConfirmModal('#ConfirmModal');
    var oCreateFolderModal = new CreateFolderModal('#CreateFolderModal', '.btn-create-folder');
    // List files on a WebDAV server using WebDAV Ajax Library
    oWebDAV.NavigateFolder(location.href);

    // Set Ajax lib version
    $('.ithit-version-value').text('v' + ITHit.WebDAV.Client.WebDavSession.Version + ' (Protocol v' + ITHit.WebDAV.Client.WebDavSession.ProtocolVersion + ')');
    $('.ithit-current-folder-value').text(oWebDAV.GetMountUrl());

})();