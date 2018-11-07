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
        },

        /**
         *
         * @param {string} fileName
         * @returns {string}
         */
        GetFileExtension: function (fileName) {
            var index = fileName.lastIndexOf('.');
            return index !== -1 ? fileName.substr(index + 1).toLowerCase() : '';
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
                            $('<td class="d-none d-sm-table-cell" />').text(oItem.IsFolder() ? 'Folder' : ('File ' + Formatters.GetFileExtension(oItem.DisplayName))),
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

        LoadFromHash: function () {
            this.$el.find('input.tt-input').val(oWebDAV.GetHashValue('search'));
            this._RenderFolderGrid(oWebDAV.GetHashValue('search'), oWebDAV.GetHashValue('page'));
        },

        _Source: function (sPhrase, c, fCallback) {
            oWebDAV.NavigateSearch(sPhrase, false, 1, false, function (oResult) {
                fCallback(oResult.Result.Page);
            });
        },

        _OnKeyUp: function (oEvent) {
            if (oEvent.keyCode === 13) {
                this._RenderFolderGrid(oSearchForm.GetValue(), 1);
                this.$el.find('input').typeahead('close');
                this._HideKeyboard(this.$el.find('input'));
            }
        },

        _OnSelect: function (oEvent, oItem) {
            oFolderGrid.Render([oItem], true);
            oPagination.Hide();
        },

        _OnSubmit: function () {
            this._RenderFolderGrid(oSearchForm.GetValue(), 1);
        },

        _RenderFolderGrid: function (oSearchQuery, nPageNumber) {
            var oSearchFormView = this;
            oWebDAV.NavigateSearch(oSearchForm.GetValue(), false, nPageNumber, true, function (oResult) {
                oFolderGrid.Render(oResult.Result.Page, true);
                oPagination.Render(nPageNumber, Math.ceil(oResult.Result.TotalItems / oWebDAV.PageSize), function (pageNumber) {
                    oSearchFormView._RenderFolderGrid(oSearchQuery, pageNumber);
                });

                if (oResult.Result.Page.length == 0 && nPageNumber != 1) {
                    oSearchFormView._RenderFolderGrid(oSearchQuery, 1);
                }
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

    ///////////////////
    // Pagination View
    var PaginationView = function (selector) {
        this.$el = $(selector);
        this.maxItems = 5;
    };
    PaginationView.prototype = {
        Render: function (pageNumber, countPages, changePageCallback) {
            this.$el.empty();

            if (countPages && countPages > 1) {
                // render Previous link
                $('<a />').addClass('page-link').appendTo($('<li/>').addClass('page-item ' + (pageNumber == 1 ? 'disabled' : '')).appendTo(this.$el)).text('<<').click(function () {
                    if (pageNumber != 1)
                        changePageCallback(pageNumber - 1);
                    return false;
                });

                // render pages
                var firstPage = countPages > this.maxItems && (pageNumber - Math.floor(this.maxItems / 2)) > 0 ? (pageNumber - Math.floor(this.maxItems / 2)) : 1;
                var lastPage = (firstPage + this.maxItems - 1) <= countPages ? (firstPage + this.maxItems - 1) : countPages;

                if (countPages > this.maxItems && lastPage - firstPage < this.maxItems) {
                    firstPage = lastPage - this.maxItems + 1;
                }

                if (firstPage > 1 && countPages > this.maxItems) {
                    $('<a />').addClass('page-link').data('page-number', 1).appendTo($('<li/>').addClass('page-item ' + (1 == pageNumber ? 'active' : '')).appendTo(this.$el)).text(1).click(function () {
                        if (pageNumber != $(this).data('page-number')) {
                            changePageCallback($(this).data('page-number'));
                        }
                        return false;
                    });
                    if (firstPage - 1 > 1) {
                        $('<a />').addClass('page-link').data('page-number', i).appendTo($('<li/>').addClass('page-item disabled').appendTo(this.$el)).text('...');
                    }
                }

                for (var i = firstPage; i <= lastPage; i++) {
                    $('<a />').addClass('page-link').data('page-number', i).appendTo($('<li/>').addClass('page-item ' + (i == pageNumber ? 'active' : '')).appendTo(this.$el)).text(i).click(function () {
                        if (pageNumber != $(this).data('page-number')) {
                            changePageCallback($(this).data('page-number'));
                        }
                        return false;
                    });
                }

                if (lastPage != countPages && countPages > this.maxItems) {
                    if (lastPage != countPages - 1) {
                        $('<a />').addClass('page-link').data('page-number', i).appendTo($('<li/>').addClass('page-item disabled').appendTo(this.$el)).text('...');
                    }
                    $('<a />').addClass('page-link').data('page-number', countPages).appendTo($('<li/>').addClass('page-item ' + (countPages == pageNumber ? 'active' : '')).appendTo(this.$el)).text(countPages).click(function () {
                        if (pageNumber != $(this).data('page-number'))
                            changePageCallback($(this).data('page-number'));
                        return false;
                    });
                }

                // render Next link
                $('<a />').addClass('page-link').appendTo($('<li/>').addClass('page-item ' + (countPages == pageNumber ? 'disabled' : '')).appendTo(this.$el)).text('>>').click(function () {
                    if (pageNumber != countPages)
                        changePageCallback(pageNumber + 1);
                    return false;
                });
            }
        },

        Hide: function () {
            this.$el.empty();
        }
    }

    ///////////////////
    // Table sorting View
    var TableSortingView = function (selector) {
        this.$headerCols = $(selector);
        this.Init();
    };
    TableSortingView.prototype = {
        Init: function () {
            var $cols = this.$headerCols;
            $cols.click(function () {
                var className = 'ascending'
                if ($(this).hasClass('ascending')) {
                    className = 'descending';
                }

                oWebDAV.Sort($(this).data('sort-column'), className == 'ascending');
            })
        },

        Set: function (sortColumn, sortAscending) {
            var $col = this.$headerCols.filter('[data-sort-column="' + sortColumn + '"]');
            this.$headerCols.removeClass('ascending descending');
            if (sortAscending) {
                $col.removeClass('descending').addClass('ascending');
            } else {
                $col.removeClass('ascending').addClass('descending');
            }
        }
    }

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

        PushState: function () {
            if (this._IsBrowserSupport()) {
                history.pushState('', document.title, window.location.pathname + window.location.search);
            }
        },

        _OnPopState: function (oEvent) {
            if (!oWebDAV.GetHashValue('search')) {
                var sUrl = oEvent.state && oEvent.state.Url || window.location.href.split("#")[0];
                oWebDAV.NavigateFolder(sUrl);
            }
        },

        _OnLinkClick: function (oEvent) {
            var sUrl = $(oEvent.target).closest('a').attr('href');
            if (!sUrl) {
                return;
            }

            if (sUrl.indexOf((location.origin || window.location.href.split("#")[0].replace(location.pathname, ''))) !== 0) {
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
                $modalDialog.removeClass('modal-sm').addClass('modal-lg');
            else
                $modalDialog.removeClass('modal-lg').addClass('modal-sm');

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
        this.PageSize = 10; // set size items of page
        this.CurrentFolder = null;
        this.WebDavSession = new ITHit.WebDAV.Client.WebDavSession();
        this.SnippetPropertyName = new ITHit.WebDAV.Client.PropertyName('snippet', 'ithit');
    };

    WebDAVController.prototype = {

        Reload: function () {
            if (this.CurrentFolder) {
                if (this.GetHashValue('search')) {
                    oSearchForm.LoadFromHash();
                }
                else {
                    this.NavigateFolder(this.CurrentFolder.Href);
                }
            }
        },

        NavigateFolder: function (sPath, pageNumber, sortColumn, sortAscending, fCallback) {
            var pageSize = this.PageSize, currentPageNumber = 1;
            // add default sorting by file type
            var sortColumns = [new ITHit.WebDAV.Client.OrderProperty(new ITHit.WebDAV.Client.PropertyName('is-directory', ITHit.WebDAV.Client.DavConstants.NamespaceUri), this.CurrentSortColumnAscending)];
            if (!sPath && this.CurrentFolder) {
                sPath = this.CurrentFolder.Href;
            }

            if (sortColumn) {
                this.CurrentSortColumn = sortColumn;
                this.CurrentSortAscending = sortAscending;
                this.SetHashValues([{ Name: 'sortcolumn', Value: sortColumn }, { Name: 'sortascending', Value: sortAscending.toString() }]);
            } else if (this.GetHashValue('sortcolumn')) {
                this.CurrentSortColumn = this.GetHashValue('sortcolumn');
                this.CurrentSortAscending = this.GetHashValue('sortascending') == 'true';
                oTableSorting.Set(this.CurrentSortColumn, this.CurrentSortAscending);
            } else {
                this.CurrentSortColumn = 'displayname';
                this.CurrentSortAscending = true;
                oTableSorting.Set(this.CurrentSortColumn, this.CurrentSortAscending);
            }

            // apply sorting by table column
            if (this.CurrentSortColumn) {
                sortColumns.push(new ITHit.WebDAV.Client.OrderProperty(new ITHit.WebDAV.Client.PropertyName(this.CurrentSortColumn, ITHit.WebDAV.Client.DavConstants.NamespaceUri), this.CurrentSortAscending));
            }

            // update page number
            if (pageNumber) {
                currentPageNumber = pageNumber;
            } else if (this.GetHashValue('page')) {
                currentPageNumber = parseInt(this.GetHashValue('page'));
            }

            if (currentPageNumber != 1) {
                this.SetHashValue('page', currentPageNumber);
            } else {
                this.SetHashValue('page', '');
            }

            this.WebDavSession.OpenFolderAsync(sPath, [], function (oResponse) {
                this.CurrentFolder = oResponse.Result;
                oBreadcrumbs.SetHierarchyItem(this.CurrentFolder);

                // Detect search support. If search is not supported - disable search field.
                this.CurrentFolder.GetSupportedFeaturesAsync(function (oResult) {
                    /** @typedef {ITHit.WebDAV.Client.OptionsInfo} oOptionsInfo */
                    var oOptionsInfo = oResult.Result;

                    oSearchForm.SetDisabled(!(oOptionsInfo.Features & ITHit.WebDAV.Client.Features.Dasl));
                });

                this.CurrentFolder.GetPageAsync([], (currentPageNumber - 1) * pageSize, pageSize, sortColumns, function (oResult) {
                    /** @type {ITHit.WebDAV.Client.HierarchyItem[]} aItems */
                    var aItems = oResult.Result.Page;
                    var aCountPages = Math.ceil(oResult.Result.TotalItems / pageSize);

                    oFolderGrid.Render(aItems, false);
                    oPagination.Render(currentPageNumber, aCountPages, function (pageNumber) {
                        oWebDAV.NavigateFolder(null, pageNumber);
                    });

                    if (aItems.length == 0 && pageNumber != 1) {
                        oWebDAV.NavigateFolder(null, 1);
                    }

                    if (fCallback)
                        fCallback(aItems);
                });
            }.bind(this));
        },

        NavigateSearch: function (sPhrase, bIsDynamic, pageNumber, updateUrlHash, fCallback) {
            var pageSize = this.PageSize, currentPageNumber = 1;

            if (!this.CurrentFolder) {
                fCallback && fCallback({ Items: [], TotalItems: 0 });
                return;
            }

            if (updateUrlHash) {
                this.SetHashValue('search', sPhrase);
            }

            if (sPhrase === '') {
                this.Reload();
                return;
            }

            // update page number
            if (pageNumber) {
                currentPageNumber = pageNumber;
            } else if (this.GetHashValue('page')) {
                currentPageNumber = parseInt(this.GetHashValue('page'));
            }

            if (updateUrlHash && currentPageNumber != 1) {
                this.SetHashValue('page', currentPageNumber);
            } else {
                this.SetHashValue('page', '');
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

            this.CurrentFolder.GetSearchPageByQueryAsync(searchQuery, (currentPageNumber - 1) * pageSize, pageSize, function (oResult) {
                /** @type {ITHit.WebDAV.Client.AsyncResult} oResult */

                /** @type {ITHit.WebDAV.Client.HierarchyItem[]} aItems */

                fCallback && fCallback(oResult);
            });
        },

        Sort: function (columnName, sortAscending) {
            this.NavigateFolder(null, null, columnName, sortAscending)
        },

        /**
         * Opens document for editing.
         * @param {string} sDocumentUrl Must be full path including domain name: https://webdavserver.com/path/file.ext
         */
        EditDoc: function (sDocumentUrl) {
            if (webDavSettings.EditDocAuth.Authentication.toLowerCase() == 'cookies') {
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
         * Returns value from hash
         * @return {string}
         */
        GetHashValue: function (key) {
            var hashConfig = this._parseUrlHash();

            return hashConfig.hasOwnProperty(key) ? hashConfig[key] : null;
        },

        /**
         * Sets values to hash
         */
        SetHashValues: function (arrayValues) {
            var hashValue = '';
            var params = [];
            var hashConfig = this._parseUrlHash();

            for (var i = 0; i < arrayValues.length; i++) {
                hashConfig = this._addParameterToArray(arrayValues[i].Name, arrayValues[i].Value, hashConfig)
            }

            for (var key in hashConfig) {
                params.push(key + '=' + hashConfig[key]);
            }

            hashValue = params.length > 0 ? ('#' + params.join('&')) : '';

            if (hashValue != location.hash) {
                location.hash = hashValue;
            }

            if (location.href[location.href.length - 1] == '#') {
                oHistoryApi.PushState();
            }
        },

        /**
         * Sets value to hash
         */
        SetHashValue: function (name, value) {
            this.SetHashValues([{ Name: name, Value: value }]);
        },

        /**
         * Adds name and value to array
         * @return {Array}
         */
        _addParameterToArray: function (name, value, arrayParams) {
            var nameExist = false;

            for (var key in arrayParams) {
                if (arrayParams.hasOwnProperty(key)) {
                    if (key == name) {
                        nameExist = true;
                        arrayParams[key] = value;
                    }

                    if (!arrayParams[key]) {
                        continue;
                    }
                }
            }

            if (!nameExist && value) {
                arrayParams[name] = value;
            }

            return arrayParams;
        },

        /**
         * Parses hash
         * @return {string}
         */
        _parseUrlHash: function () {
            // Parse hash
            var hash = {};
            if (location.hash.length > 0) {
                var hashParts = location.hash.substr(1).split('&');
                for (var i = 0, l = hashParts.length; i < l; i++) {
                    var param = hashParts[i].split('=');
                    hash[param[0]] = param[1];
                }
            }

            return hash;
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
    var oPagination = new PaginationView('.ithit-pagination-container');
    var oTableSorting = new TableSortingView('.ithit-grid-container th.sort');
    var oHistoryApi = new HistoryApiController('.ithit-grid-container, .ithit-breadcrumb-container');
    var oWebDAV = window.WebDAVController = new WebDAVController();
    var oConfirmModal = new ConfirmModal('#ConfirmModal');
    var oCreateFolderModal = new CreateFolderModal('#CreateFolderModal', '.btn-create-folder');
    // List files on a WebDAV server using WebDAV Ajax Library
    if (oWebDAV.GetHashValue('search')) {
        oWebDAV.NavigateFolder(window.location.href.split("#")[0], null, null, null, function () {
            oSearchForm.LoadFromHash();
        });
    }
    else {
        oWebDAV.NavigateFolder(window.location.href.split("#")[0]);
    }

    // Set Ajax lib version
    $('.ithit-version-value').text('v' + ITHit.WebDAV.Client.WebDavSession.Version + ' (Protocol v' + ITHit.WebDAV.Client.WebDavSession.ProtocolVersion + ')');
    $('.ithit-current-folder-value').text(oWebDAV.GetMountUrl());

})();