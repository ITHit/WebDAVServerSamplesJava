
(function (WebdavCommon) {
    var sSearchErrorMessage = "Search is not supported.";
    var sSupportedFeaturesErrorMessage = "Supported Features error.";
    var sProfindErrorMessage = "PROPFIND request error.";


    ///////////////////
    // Folder Grid View
    var FolderGridView = function (selectorTableContainer, selectorTableToolbar) {
        var self = this;
        this.$el = $(selectorTableContainer);
        this.selectedItems = [];

        //Copied or cut items
        this.storedItems = [];
        this.isCopiedItems = false;
        this.selectedItem = null;
        this.activeSelectedTab = 'preview';
        this.isSearchMode = false;
        this._defaultEditor = 'OSEditor';

        this.$el.on({
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

        this.$el.find('th input[type="checkbox"]').change(function () {
            self.selectedItems = [];
            if ($(this).is(':checked')) {
                self.$el.find('td input[type="checkbox"]').prop('checked', true).change();
            }
            else {
                oToolbar.ResetToolbar();
                self.$el.find('td input[type="checkbox"]').prop('checked', false);
            }
        });

        // set timer for updating Modified field
        setInterval(function () {
            self.$el.find('td.modified-date').each(function () {
                $(this).text(WebdavCommon.Formatters.Date($(this).data('modified-date')));
            });
        }, 3000);
    };
    FolderGridView.prototype = {
        Render: function (aItems, bisSearchMode) {
            var self = this;
            this.isSearchMode = bisSearchMode || false;

            this.$el.find('tbody').html(
                aItems.map(function (oItem) {
                    var locked = oItem.ActiveLocks.length > 0
                        ? ('<span class="ithit-grid-icon-locked" title="' + self._RenderLokedIconTooltip(oItem) + '"></span>' +
                            (oItem.ActiveLocks[0].LockScope === 'Shared' ? ('<span class="badge badge-pill badge-dark">' + oItem.ActiveLocks.length + '</span>') : ''))
                        : '';
                    /** @type {ITHit.WebDAV.Client.HierarchyItem} oItem */
                    var $customCheckbox = $('<label class="custom-checkbox"><input type="checkbox" /><span class="checkmark"></span></label>');
                    $customCheckbox.find('input').on('change', function () {
                        if ($(this).is(':checked')) {
                            self._AddSelectedItem(oItem);
                        }
                        else {
                            self._RemoveSelectedItem(oItem);
                        }
                    }).attr('checked', this._IsSelectedItem(oItem));

                    return $('<div/>').html([
                        $('<tr />').html([
                            $('<td class="select-disabled"/>').html([
                                $customCheckbox
                            ]),
                            $('<td />').
                                html(oItem.IsFolder() ? ('<span class="icon-folder">' + locked + '</span>') : locked),
                            this._RenderDisplayName(oItem),
                            $('<td class="d-none d-xl-table-cell" />').text(oItem.IsFolder() ? 'Folder' : ('File ' + WebdavCommon.Formatters.GetFileExtension(oItem.DisplayName))),
                            $('<td />').
                                text(!oItem.IsFolder() ? WebdavCommon.Formatters.FileSize(oItem.ContentLength) : '').
                                css('text-align', 'right'),
                            $('<td class="d-none d-lg-table-cell modified-date" />').text(WebdavCommon.Formatters.Date(oItem.LastModified)).data('modified-date', oItem.LastModified),
                            $('<td class="text-right select-disabled"/>').html(this._RenderActions(oItem))
                        ]).on('click', function (e) {
                            // enable GSuite preview and edit only for files
                            if (!oItem.IsFolder() && !$(this).hasClass('active') &&
                                ((e.target.nodeName.toLowerCase() === 'td' && !$(e.target).hasClass('select-disabled')) ||
                                    (e.target.nodeName.toLowerCase() !== 'td' && !$(e.target).parents('td').hasClass('select-disabled')))) {
                                $(this).addClass('active').siblings().removeClass('active');
                                self.selectedItem = oItem;

                                // render GSuite Editor
                                WebdavCommon.GSuiteEditor.Render(oItem);
                            }
                        }).addClass(self.selectedItem != null && oItem.Href == self.selectedItem.Href ? 'active' : ''),
                        $('<tr class="tr-snippet-url"/>').html([
                            $('<td class="d-none d-xl-table-cell" />'),
                            $('<td class="d-none d-lg-table-cell" />'),
                            this._RenderSnippetAndUrl(oItem)])]).children();
                }.bind(this))
            );
        },

        /**
        * @param {ITHit.WebDAV.Client.HierarchyItem} oItem
        **/
        _RenderLokedIconTooltip(oItem) {
            var tooltipTitle = 'Exclusive lock: ' + oItem.ActiveLocks[0].Owner;
            if (oItem.ActiveLocks[0].LockScope === 'Shared') {
                var userNames = [];
                tooltipTitle = 'Shared lock' + (oItem.ActiveLocks.length > 1 ? '(s)':'') + ': ';
                for (var i = 0; i < oItem.ActiveLocks.length; i++) {
                    userNames.push(oItem.ActiveLocks[i].Owner);
                }
                tooltipTitle += userNames.join(', ');
            }
            return tooltipTitle;
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
            if (this.isSearchMode) {
                new BreadcrumbsView($('<ol />').addClass('breadcrumb').appendTo(oElement)).SetHierarchyItem(oItem);

                // Append snippet to name
                oElement.append(WebdavCommon.Formatters.Snippet(oItem.Properties.Find(oWebDAV.SnippetPropertyName)));
            }

            return oElement;
        },

        /**
         * @param {ITHit.WebDAV.Client.HierarchyItem} oItem
         * @returns string
         **/
        _RenderActions: function (oItem) {
            var self = this;
            var actions = [];
            var isDavProtocolSupported = ITHit.WebDAV.Client.DocManager.IsDavProtocolSupported();
            var isMicrosoftOfficeDocument = ITHit.WebDAV.Client.DocManager.IsMicrosoftOfficeDocument(oItem.Href);
            var isGSuiteDocument = ITHit.WebDAV.Client.DocManager.IsGSuiteDocument(oItem.Href);

            if (oItem.IsFolder()) {
                actions.push($('<button class="btn btn-primary btn-sm btn-labeled" type="button"/>').
                    html('<span class="btn-label"><i class="icon-open-folder"></i></span> <span class="d-none d-lg-inline-block">Browse</span>').
                    attr('title', 'Open this folder in Operating System file manager.').
                    on('click', function () {
                        oWebDAV.OpenFolderInOsFileManager(oItem.Href);
                    }).prop("disabled", !isDavProtocolSupported));
            } else {
                var $btnGroup = $('<div class="btn-group"></div>');
                var displayRadioBtns = (isMicrosoftOfficeDocument && isGSuiteDocument);
                var isExclusiveLocked = oItem.ActiveLocks.length > 0 && oItem.ActiveLocks[0].LockScope === 'Exclusive';
                $('<button type="button" class="btn btn-primary btn-sm btn-labeled btn-default-edit" title="' + (displayRadioBtns ? self._GetActionGroupBtnTooltipText() : 'Edit document with desktop associated application.') +
                    '"' + self._GetDisabledGroupBtnAttribute(isExclusiveLocked) + '><span class="btn-label"><i class="' +
                    (displayRadioBtns ? self._GetActionGroupBtnCssClass() : 'icon-edit') + '"></i></span><span class="d-none d-lg-inline-block btn-edit-label">Edit</span></button>')
                    .appendTo($btnGroup).on('click', function () {
                        var $radio = $(this).parent().find('input[type=radio]:checked');
                        if ($radio.length) {
                            $radio.parent().next().click();
                        }
                        else {
                            oWebDAV.EditDoc(oItem.Href);
                        }
                    }).prop("disabled", !isDavProtocolSupported && !isMicrosoftOfficeDocument);

                var $dropdownToggle = $('<button type="button" class="btn btn-primary dropdown-toggle dropdown-toggle-split btn-sm" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false"><span class="sr-only">Toggle Dropdown</span></button>')
                    .appendTo($btnGroup).prop("disabled", !isDavProtocolSupported && !isMicrosoftOfficeDocument);

                this._RenderContextMenu(oItem, $btnGroup, isMicrosoftOfficeDocument, isGSuiteDocument, isDavProtocolSupported, isExclusiveLocked);

                $btnGroup.on('shown.bs.dropdown', function () {
                    self.ContextMenuID = oItem.Href;
                });

                $btnGroup.on('hidden.bs.dropdown', function () {
                    self.ContextMenuID = null;
                });

                // open context menu if it was open before update
                if (self.ContextMenuID && self.ContextMenuID == oItem.Href) {
                    $dropdownToggle.dropdown('toggle');
                }

                actions.push($btnGroup);
            }

            return actions;
        },

        _GetActionGroupBtnTooltipText: function () {
            var tooltipText = 'Edit document with desktop associated application.';
            switch (this._defaultEditor) {
                case 'OSEditor':
                    tooltipText = 'Edit with Microsoft Office Desktop.';
                    break;
                case 'GSuiteEditor':
                    tooltipText = 'Edit document in G Suite Editor.';
                    break;
            }
            return tooltipText;
        },

        _GetDisabledGroupBtnAttribute: function (isExclusiveLocked) {
            var attribute = '';
            if (this._defaultEditor == 'GSuiteEditor' && isExclusiveLocked) {
                attribute = ' disabled="disabled"';
            }
            return attribute;
        },

        _GetActionGroupBtnCssClass: function () {
            var cssClassName = 'icon-edit';
            switch (this._defaultEditor) {
                case 'OSEditor':
                    cssClassName = 'icon-microsoft-edit';
                    break;
                case 'GSuiteEditor':
                    cssClassName = 'icon-gsuite-edit';
                    break;
            }

            return cssClassName;
        },

        _RenderContextMenu: function (oItem, $btnGroup, isMicrosoftOfficeDocument, isGSuiteDocument, isDavProtocolSupported, isExclusiveLocked) {
            var self = this;
            var supportGSuiteFeature = oWebDAV.OptionsInfo.Features & ITHit.WebDAV.Client.Features.GSuite;
            var displayRadioBtns = (isMicrosoftOfficeDocument && isGSuiteDocument);
            var $dropdownMenu = $('<div class="dropdown-menu dropdown-menu-right actions ' + (displayRadioBtns ? 'dropdown-menu-radio-btns' : '') + '"></div>').appendTo($btnGroup);
            if (isMicrosoftOfficeDocument) {
                if (displayRadioBtns) {
                    $('<label class="custom-radiobtn"><input type="radio" name="defaultEditor' + oItem.DisplayName +
                        '" value="OSEditor" ' + self._GetContextMenuRadioBtnCheckedProperty('OSEditor') +
                        ' /><span class="checkmark"></span></label>').appendTo($dropdownMenu).find('input[type=radio]').change(function () { self._ChangeContextMenuRadionBtnHandler($(this)); });
                }
                $('<a class="dropdown-item ' + (displayRadioBtns ? 'dropdown-radio' : '') + '" href="javascript:void()" title="Edit with Microsoft Office Desktop."><i class="icon-microsoft-edit"></i>Edit with Microsoft Office Desktop</a>')
                    .appendTo($dropdownMenu).on('click', function () {
                        oWebDAV.EditDoc(oItem.Href);
                    });
            }
            if (!isMicrosoftOfficeDocument) {
                $('<a class="dropdown-item' + (isDavProtocolSupported ? '' : ' disabled') + '" href="javascript:void()" title="Edit document with desktop associated application."><i class="icon-edit-associated"></i>Edit with Associated Desktop Application</a>')
                    .appendTo($dropdownMenu).on('click', function () {
                        oWebDAV.EditDoc(oItem.Href);
                    });
            }

            $('<div class="dropdown-divider"></div>').appendTo($dropdownMenu);
            $('<a class="dropdown-item desktop-app' + (isDavProtocolSupported ? '' : ' disabled') + '" href="javascript:void()" title="Select application to open this file with.">Select Desktop Application</a>')
                .appendTo($dropdownMenu).on('click', function () {
                    oWebDAV.OpenDocWith(oItem.Href);
                });
        },

        _GetContextMenuRadioBtnCheckedProperty: function (editorName) {
            return this._defaultEditor == editorName ? 'checked="checked"' : '';
        },

        _ChangeContextMenuRadionBtnHandler: function ($radioBtn) {
            var self = this;
            var iconClassName = $radioBtn.parent().next().find('i:first').attr('class');

            this._defaultEditor = $radioBtn.val();
            $('input[value="' + self._defaultEditor + '"]').prop('checked', true);

            // update button icon
            $('.btn-default-edit').each(function () {
                var $btn = $(this);
                if ($btn.parent().find('.actions input[type=radio]').length) {
                    $btn.find('i:first').attr('class', iconClassName);
                }
                $btn.attr('title', self._GetActionGroupBtnTooltipText());
            });
        },

        _AddSelectedItem: function (oItem) {
            this.selectedItems.push(oItem);
            oToolbar.UpdateToolbarButtons();
        },

        _RemoveSelectedItem: function (oItem) {
            var self = this;
            $.each(this.selectedItems, function (index) {
                if (self.selectedItems[index].Href === oItem.Href) {
                    self.selectedItems.splice(index, 1);
                    return false;
                }
            });

            oToolbar.UpdateToolbarButtons();
        },

        _IsSelectedItem: function (oItem) {
            var self = this;
            var isSelected = false;
            $.each(this.selectedItems, function (index) {
                if (self.selectedItems[index].Href === oItem.Href) {
                    isSelected = true;
                    return false;
                }
            });
            return isSelected;
        },

        UncheckTableCheckboxs: function () {
            this.selectedItems = [];
            this.$el.find('input[type="checkbox"]').prop('checked', false);
        },
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
            oWebDAV.NavigateSearch(sPhrase, false, 1, false, true, function (oResult) {
                if (oResult.IsSuccess) {
                    fCallback(oResult.Result.Page);
                } else {
                    WebdavCommon.ErrorModal.Show(sSearchErrorMessage, oResult.Error);
                }
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
            oWebDAV.NavigateSearch(oSearchForm.GetValue(), false, nPageNumber, true, true, function (oResult) {
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
            oElement.append(WebdavCommon.Formatters.Snippet(oItem.Properties.Find(oWebDAV.SnippetPropertyName)));

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
    var BreadcrumbsView = function (selector, upOneLevelBtn) {
        this.$el = $(selector);
        this.$upOneLevelBtn = $(upOneLevelBtn);
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
                var oLabel = i === 0 ? $('<span />').addClass('icon-home') : $('<span />').text(decodeURIComponent(sPart));
                return $('<li class="breadcrumb-item"/>').toggleClass('active', bIsLast).append(
                    bIsLast ?
                        $('<span />').html(oLabel) :
                        $('<a />').attr('href', location.protocol + '//' + aParts.slice(0, i + 1).join('/') + '/').html(oLabel)
                );
            }));

            if (this.$upOneLevelBtn) {
                var $lastLnk = this.$el.find('a').last();
                if ($lastLnk.length) {
                    this.$upOneLevelBtn.attr('href', $lastLnk.attr('href'));
                    this.$upOneLevelBtn.removeClass('disabled');
                } else {
                    this.$upOneLevelBtn.attr('href', 'javascript.void()');
                    this.$upOneLevelBtn.addClass('disabled');
                }

            }
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
        },

        Disable: function () {
            this.$headerCols.addClass('disabled');
        },

        Enable: function () {
            this.$headerCols.removeClass('disabled');
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
            if (oWebDAV.GetHashValue('search')) {
                oSearchForm.LoadFromHash();
            }
            else {
                var sUrl = oEvent.state && oEvent.state.Url || window.location.href.split("#")[0];
                oWebDAV.NavigateFolder(sUrl, null, null, null, true);
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
            oWebDAV.NavigateFolder(sUrl, null, null, null, true);
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
        this.$el.find('.btn-ok').click(function () {
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

    var WebDAVController = function () {
        this.PageSize = 10; // set size items of page
        this.CurrentFolder = null;
        this.AllowReloadGrid = true;
        this.WebDavSession = new ITHit.WebDAV.Client.WebDavSession();
        this.SnippetPropertyName = new ITHit.WebDAV.Client.PropertyName('snippet', 'ithit');
    };

    WebDAVController.prototype = {

        Reload: function () {
            if (this.CurrentFolder && this.AllowReloadGrid) {
                if (this.GetHashValue('search')) {
                    oSearchForm.LoadFromHash();
                }
                else {
                    this.NavigateFolder(this.CurrentFolder.Href);
                }
            }
        },

        NavigateFolder: function (sPath, pageNumber, sortColumn, sortAscending, resetSelectedItem, fCallback) {
            var pageSize = this.PageSize, currentPageNumber = 1;
            // add default sorting by file type
            var sortColumns = [new ITHit.WebDAV.Client.OrderProperty(new ITHit.WebDAV.Client.PropertyName('is-directory', ITHit.WebDAV.Client.DavConstants.NamespaceUri), this.CurrentSortColumnAscending)];
            if (!sPath && this.CurrentFolder) {
                sPath = this.CurrentFolder.Href;
            }

            //set upload url for uploader control
            if (typeof WebDAVUploaderGridView !== 'undefined') {
                WebDAVUploaderGridView.SetUploadUrl(sPath);
            }

            if (resetSelectedItem) {
                oToolbar.ResetToolbar();
            }

            //Enable sorting 
            oTableSorting.Enable();
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
                var self = this;
                if (oResponse.IsSuccess) {
                    self.CurrentFolder = oResponse.Result;
                    oBreadcrumbs.SetHierarchyItem(self.CurrentFolder);

                    // Detect search support. If search is not supported - disable search field.
                    this.CurrentFolder.GetSupportedFeaturesAsync(function (oResult) {
                        /** @typedef {ITHit.WebDAV.Client.OptionsInfo} oOptionsInfo */

                        if (oResult.IsSuccess) {
                            self.OptionsInfo = oResult.Result;
                            oSearchForm.SetDisabled(!(self.OptionsInfo.Features & ITHit.WebDAV.Client.Features.Dasl));
                        } else {
                            WebdavCommon.ErrorModal.Show(sSupportedFeaturesErrorMessage, oResult.Error);
                        }
                    });

                    this.CurrentFolder.GetPageAsync([], (currentPageNumber - 1) * pageSize, pageSize, sortColumns, function (oResult) {
                        /** @type {ITHit.WebDAV.Client.HierarchyItem[]} aItems */
                        if (oResult.IsSuccess) {
                            var aItems = oResult.Result.Page;
                            var aCountPages = Math.ceil(oResult.Result.TotalItems / pageSize);

                            oFolderGrid.Render(aItems, false);
                            oPagination.Render(currentPageNumber, aCountPages, function (pageNumber) {
                                oWebDAV.NavigateFolder(null, pageNumber, null, null, true);
                            });

                            if (aItems.length == 0 && pageNumber != 1) {
                                oWebDAV.NavigateFolder(null, 1, null, null, true);
                            }

                            if (fCallback)
                                fCallback(aItems);
                        } else {
                            WebdavCommon.ErrorModal.Show(sProfindErrorMessage, oResult.Error);
                        }
                    });
                } else {
                    WebdavCommon.ErrorModal.Show(sProfindErrorMessage, oResponse.Error);
                }
            }.bind(this));
        },

        NavigateSearch: function (sPhrase, bIsDynamic, pageNumber, updateUrlHash, resetSelectedItem, fCallback) {
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

            if (resetSelectedItem) {
                oToolbar.ResetToolbar();
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
            //Disable sorting 
            oTableSorting.Disable();

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

            function _getSearchPageByQuery() {
                oWebDAV.CurrentFolder.GetSearchPageByQueryAsync(searchQuery, (currentPageNumber - 1) * pageSize, pageSize, function (oResult) {
                    /** @type {ITHit.WebDAV.Client.AsyncResult} oResult */
                    /** @type {ITHit.WebDAV.Client.HierarchyItem[]} aItems */

                    if (oResult.IsSuccess) {
                        fCallback && fCallback(oResult);
                    } else {
                        WebdavCommon.ErrorModal.Show(sSearchErrorMessage, oResult.Error);
                    }
                });
            }

            if (window.location.href.split("#")[0] != this.CurrentFolder.Href) {
                this.WebDavSession.OpenFolderAsync(window.location.href.split("#")[0], [], function (oResponse) {
                    oWebDAV.CurrentFolder = oResponse.Result;
                    oBreadcrumbs.SetHierarchyItem(oWebDAV.CurrentFolder);
                    _getSearchPageByQuery();
                });
            }
            else {
                _getSearchPageByQuery();
            }


        },

        Sort: function (columnName, sortAscending) {
            this.NavigateFolder(null, null, columnName, sortAscending, true);
        },

        /**
         * Opens document for editing.
         * @param {string} sDocumentUrl Must be full path including domain name: https://webdavserver.com/path/file.ext
         */
        EditDoc: function (sDocumentUrl) {
            if (['cookies', 'ms-ofba'].indexOf(webDavSettings.EditDocAuth.Authentication.toLowerCase()) != -1) {
                if (webDavSettings.EditDocAuth.Authentication.toLowerCase() == 'ms-ofba' &&
                    ITHit.WebDAV.Client.DocManager.IsMicrosoftOfficeDocument(sDocumentUrl)) {
                    ITHit.WebDAV.Client.DocManager.EditDocument(sDocumentUrl, this.GetMountUrl(), this._ProtocolInstallMessage.bind(this));
                }
                else {
                    ITHit.WebDAV.Client.DocManager.DavProtocolEditDocument(sDocumentUrl, this.GetMountUrl(), this._ProtocolInstallMessage.bind(this), null, webDavSettings.EditDocAuth.SearchIn,
                        webDavSettings.EditDocAuth.CookieNames, webDavSettings.EditDocAuth.LoginUrl);
                }
            }
            else {
                ITHit.WebDAV.Client.DocManager.EditDocument(sDocumentUrl, this.GetMountUrl(), this._ProtocolInstallMessage.bind(this));
            }
        },

        /**		           
          * Opens document for editing in online G Suite editor.
          * @param {string} sDocumentUrl Must be full path including domain name: https://webdavserver.com/path/file.ext		
          * @param {DOM} gSuiteEditPanel html DOM element
          * @param {function} [errorCallback] Function to call if document opening failed.
        */
        GSuiteEditDoc: function (sDocumentUrl, gSuiteEditPanel, errorCallback) {
            ITHit.WebDAV.Client.DocManager.GSuiteEditDocument(sDocumentUrl, gSuiteEditPanel, errorCallback);
        },

        /**		           
          * Opens document for preview in online G Suite editor.
          * @param {string} sDocumentUrl Must be full path including domain name: https://webdavserver.com/path/file.ext		
          * @param {DOM} gSuitePreviewPanel html DOM element
          * @param {function} [errorCallback] Function to call if document opening failed.
        */
        GSuitePreviewDoc: function (sDocumentUrl, gSuitePreviewPanel, errorCallback) {
            ITHit.WebDAV.Client.DocManager.GSuitePreviewDocument(sDocumentUrl, gSuitePreviewPanel, errorCallback);
        },

        /**
          * Opens document with.
          * @param {string} sDocumentUrl Must be full path including domain name: https://webdavserver.com/path/file.ext
        */
        OpenDocWith: function (sDocumentUrl) {
            ITHit.WebDAV.Client.DocManager.DavProtocolEditDocument([sDocumentUrl], this.GetMountUrl(), this._ProtocolInstallMessage.bind(this), null, webDavSettings.EditDocAuth.SearchIn,
                webDavSettings.EditDocAuth.CookieNames, webDavSettings.EditDocAuth.LoginUrl, 'OpenWith');
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
         * Returns url of app installer
         */
        GetInstallerFileUrl: function () {
            return webDavSettings.ApplicationProtocolsPath + ITHit.WebDAV.Client.DocManager.GetProtocolInstallFileNames()[0];
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
                var $currentOS = $('#DownloadProtocolModal .current-os');
                var $currentBrowser = $('#DownloadProtocolModal .current-browser');

                // initialization browsers extension panel
                if ($currentBrowser.children().length === 0) {
                    let isChrome = !!window['chrome'] && (!!window['chrome']['webstore'] || !!window['chrome']['runtime']);

                    // Edge (based on chromium) detection
                    if (isChrome && (navigator.userAgent.indexOf('Edg') !== -1)) {
                        $('#DownloadProtocolModal .edge-chromium').appendTo($currentBrowser);
                    } else if (isChrome) {
                        $('#DownloadProtocolModal .goole-chrome').appendTo($currentBrowser);
                    } else if (typeof InstallTrigger !== 'undefined') {
                        $('#DownloadProtocolModal .mozilla-firefox').appendTo($currentBrowser);
                    }
                    else if (navigator.userAgent.indexOf("MSIE ") > 0 || !!navigator.userAgent.match(/Trident.*rv\:11\./)) {
                        $('#DownloadProtocolModal .not-required-internet-explorer').show();
                    }
                }

                // initialization custom protocol installers panel
                if ($currentOS.children().length === 0) {
                    if (ITHit.DetectOS.OS === 'Windows') {
                        $('#DownloadProtocolModal .window').appendTo($currentOS);
                    } else if (ITHit.DetectOS.OS === 'Linux') {
                        $('#DownloadProtocolModal .linux').appendTo($currentOS);
                    } else if (ITHit.DetectOS.OS === 'MacOS') {
                        $('#DownloadProtocolModal .mac-os').appendTo($currentOS);
                    }
                }

                $('#DownloadProtocolModal').modal('show');
                $('#DownloadProtocolModal .more-lnk').unbind().click(function () {
                    var $pnl = $(this).next();
                    if ($pnl.is(':visible')) {
                        $(this).find('span').text('+');
                        $pnl.hide();
                    } else {
                        $(this).find('span').text('-');
                        $pnl.show();
                    }
                });
            }
        }
    };
    var oWebDAV = window.WebDAVController = new WebDAVController();
    var oConfirmModal = new ConfirmModal('#ConfirmModal');
    var oFolderGrid = new FolderGridView('.ithit-grid-container', '.ithit-grid-toolbar');
    var oToolbar = new Toolbar('.ithit-grid-toolbar', oFolderGrid, oConfirmModal, oWebDAV);
    var oSearchForm = new SearchFormView('.ithit-search-container');
    var oBreadcrumbs = new BreadcrumbsView('.ithit-breadcrumb-container .breadcrumb', '.btn-up-one-level');
    var oPagination = new PaginationView('.ithit-pagination-container');
    var oTableSorting = new TableSortingView('.ithit-grid-container th.sort');
    var oHistoryApi = new HistoryApiController('.ithit-grid-container, .ithit-breadcrumb-container');


    // List files on a WebDAV server using WebDAV Ajax Library
    if (oWebDAV.GetHashValue('search')) {
        oWebDAV.NavigateFolder(window.location.href.split("#")[0], null, null, null, true, function () {
            oSearchForm.LoadFromHash();
        });
    }
    else {
        oWebDAV.NavigateFolder(window.location.href.split("#")[0], null, null, null, true);
    }

    // Set Ajax lib version
    if (ITHit.WebDAV.Client.DocManager.IsDavProtocolSupported()) {
        $('.ithit-version-value').html('v' + ITHit.WebDAV.Client.WebDavSession.Version + ' (<a href="' + oWebDAV.GetInstallerFileUrl() + '">Protocol v' + ITHit.WebDAV.Client.WebDavSession.ProtocolVersion + '</a>)');
    }
    else {
        $('.ithit-version-value').text('v' + ITHit.WebDAV.Client.WebDavSession.Version + ' (Protocol v' + ITHit.WebDAV.Client.WebDavSession.ProtocolVersion + ')');
    }
    $('.ithit-current-folder-value').text(oWebDAV.GetMountUrl());

})(WebdavCommon);