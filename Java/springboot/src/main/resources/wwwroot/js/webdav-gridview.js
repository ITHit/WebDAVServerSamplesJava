(function (WebdavCommon) {
    var sSearchErrorMessage = "Search is not supported.";
    var sSupportedFeaturesErrorMessage = "Supported Features error.";
    var sProfindErrorMessage = "Profind request error.";
    var sGSuitePreviewErrorMessage = "Preview document with G Suite Online Tool error.";
    var sGSuiteEditErrorMessage = "Edit document with G Suite Online Editor error.";
    var sCreateFolderErrorMessage = "Create folder error.";

    ///////////////////
    // Folder Grid View
    var FolderGridView = function (selectorTableContainer, selectorTableToolbar) {
        var self = this;
        this.$el = $(selectorTableContainer);
        this.$elToolbar = $(selectorTableToolbar);
        this.selectedItems = [];
        this.selectedItem = null;
        this.activeSelectedTab = 'preview';
        this.IsSearchMode = false;
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

        this.$elToolbar.find('.btn-print-items').on('click', function () {
            oConfirmModal.Confirm('Are you sure want to print selected items?', function () {
                var filesUrls = [];
                $.each(self.selectedItems, function () {
                    if (!this.IsFolder()) {
                        filesUrls.push(this.Href);
                    }
                });
                oWebDAV.PrintDocs(filesUrls);
            });
        });

        this.$elToolbar.find('.btn-delete-items').on('click', function () {
            oConfirmModal.Confirm('Are you sure want to delete selected items?', function () {
                $.each(self.selectedItems, function (index) {
                    self.selectedItems[index].DeleteAsync(null);
                });

                self.HideToolbar();
            });
        });

        this.$el.find('th input[type="checkbox"]').change(function () {
            self.selectedItems = [];
            if ($(this).is(':checked')) {
                self.$el.find('td input[type="checkbox"]').prop('checked', true).change();
            }
            else {
                self.HideToolbar();
                self.$el.find('td input[type="checkbox"]').prop('checked', false);
            }
        });

        // add spliter button
        // Split(['#leftPanel', '#rightPanel'], {
        //     elementStyle: function (dimension, size, gutterSize) {
        //         $(window).trigger('resize');
        //         if (size < 1) {
        //             return { 'flex-basis': '0px', 'height': '0px' };
        //         }
        //         else {
        //             return { 'flex-basis': 'calc(' + size + '% - ' + gutterSize + 'px)', 'height': '' };
        //         }
        //     },
        //     gutterStyle: function (dimension, gutterSize) {
        //         return { 'flex-basis': gutterSize + 'px' };
        //     },
        //     sizes: [60, 40],
        //     minSize: [400, 0],
        //     expandToMin: true,
        //     gutterSize: 20,
        //     cursor: 'col-resize',
        //     onDragEnd: function (sizes) {
        //         $('#rightPanel').removeClass('disable-iframe-events');
        //     },
        //     onDragStart: function (sizes) {
        //         $('#rightPanel').addClass('disable-iframe-events');
        //     }
        // });

        // handle resize window event
        $(window).resize(function () {
            // settimeout because resize event is triggered before applying 'flex-basis' css rule
            this.setTimeout(function () {
                var $pnl = $('#leftPanel');
                var classAttr = 'col';
                if ($pnl.width() <= 566) {
                    classAttr += ' medium-point';
                }
                else if ($pnl.width() <= 692) {
                    classAttr += ' large-point';
                }
                $pnl.attr('class', classAttr);
            }, 10);
        });

        // set timer for updating Modified field
        setInterval(function () {
            self.$el.find('td.modified-date').each(function () {
                $(this).text(WebdavCommon.Formatters.Date($(this).data('modified-date')));
            });
        }, 3000);
    };
    FolderGridView.prototype = {
        Render: function (aItems, bIsSearchMode) {
            var self = this;
            this.IsSearchMode = bIsSearchMode || false;

            this.$el.find('tbody').html(
                aItems.map(function (oItem) {
                    var locked = oItem.ActiveLocks.length > 0
                        ? ('<span class="ithit-grid-icon-locked"></span>' +
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
                                var $gSuitePreviewPanel = $('#gSuitePreview');
                                var $gSuitePreviewBackground = $('#gSuitePreviewBackground');
                                var $gSuiteEditPanel = $('#gSuiteEdit');
                                var $gSuiteEditBackground = $('#gSuiteEditBackground');
                                var $gSuiteTabs = $('#gSuiteTabs');
                                $gSuitePreviewPanel.empty();
                                $gSuiteEditPanel.empty();
                                self.selectedItem = oItem;
                                // display file name
                                $('#fileName').text(oItem.DisplayName);

                                var $gSuiteEditContainer = $('<div class="inner-container"/>').appendTo($gSuiteEditPanel);
                                var $gSuitePreviewContainer = $('<div class="inner-container"/>').appendTo($gSuitePreviewPanel);

                                if ((oWebDAV.OptionsInfo.Features & ITHit.WebDAV.Client.Features.GSuite)) {
                                    var isEditLoaded = false;
                                    var isPreviewLoaded = false;

                                    function _loadEditEditor() {
                                        self.activeSelectedTab = 'edit';
                                        if (ITHit.WebDAV.Client.DocManager.IsGSuiteDocument(oItem.Href)) {
                                            if (!isEditLoaded) {
                                                $gSuiteEditBackground.text('Loading ...');
                                                oWebDAV.GSuiteEditDoc(oItem.Href, $gSuiteEditContainer[0], function (e) {
                                                    $gSuiteEditPanel.empty();
                                                    $gSuiteEditBackground.text('Select a document to edit.');                                                  
                                                    if (e instanceof ITHit.WebDAV.Client.Exceptions.LockedException) {
                                                        WebdavCommon.ErrorModal.Show("The document is locked exclusively.<br/>" +
                                                            "You can not edit the document in G Suite in case of an exclusive lock.", e);
                                                    }
                                                    else {
                                                        WebdavCommon.ErrorModal.Show(sGSuiteEditErrorMessage, e);
                                                    }
                                                });
                                                isEditLoaded = true;
                                            }
                                        }
                                        else {
                                            $gSuiteEditBackground.text('GSuite editor for this type of document is not available.');
                                        }
                                    }

                                    function _loadPreviewEditor() {
                                        self.activeSelectedTab = 'preview';
                                        if (!isPreviewLoaded) {
                                            $gSuitePreviewBackground.text('Loading preview...');
                                            oWebDAV.GSuitePreviewDoc(oItem.Href, $gSuitePreviewContainer[0], function (e) {
                                                $gSuitePreviewPanel.empty();
                                                $gSuitePreviewBackground.text('Select a document to preview.');
                                                WebdavCommon.ErrorModal.Show(sGSuitePreviewErrorMessage, e);
                                            });
                                            isPreviewLoaded = true;
                                        }
                                    }

                                    if (self.activeSelectedTab == 'edit') {
                                        _loadEditEditor();
                                        $gSuiteTabs.find('#edit-tab').tab('show');
                                    }
                                    else {
                                        _loadPreviewEditor();
                                        $gSuiteTabs.find('#preview-tab').tab('show');
                                    }

                                    // add handler for preview tab
                                    $gSuiteTabs.find('#preview-tab').unbind().on('shown.bs.tab', function () {
                                        _loadPreviewEditor();
                                    });

                                    // add handler for edit tab
                                    $gSuiteTabs.find('#edit-tab').unbind().on('shown.bs.tab', function () {
                                        _loadEditEditor();
                                    });
                                }
                                else if (!(oWebDAV.OptionsInfo.Features & ITHit.WebDAV.Client.Features.GSuite)) {
                                    $gSuitePreviewBackground.text('GSuite preview and edit is not supported.');
                                }
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
            var supportGSuiteFeature = oWebDAV.OptionsInfo.Features & ITHit.WebDAV.Client.Features.GSuite;
            var isGSuiteDocument = ITHit.WebDAV.Client.DocManager.IsGSuiteDocument(oItem.Href);

            function _changeDefaultEditor() {
                var $radioBtn = $(this);
                var iconClassName = $radioBtn.parent().next().find('i:first').attr('class');

                self._defaultEditor = $radioBtn.val();
                $('input[value="' + self._defaultEditor + '"]').prop('checked', true);

                // update button icon
                $('.btn-default-edit').each(function () {
                    var $btn = $(this);
                    if ($btn.parent().find('.actions input[type=radio]').length) {
                        $btn.find('i:first').attr('class', iconClassName);
                    }
                    $btn.attr('title', _getDefaultToolText());
                });
            }

            function _isDefaultEditor(editorName) {
                return self._defaultEditor == editorName ? 'checked="checked"' : '';
            }

            function _getDefaultEditorIcon() {
                var iconClassName = 'icon-edit';
                if (self._defaultEditor) {
                    switch (self._defaultEditor) {
                        case 'OSEditor':
                            iconClassName = 'icon-microsoft-edit';
                            break;
                        case 'GSuiteEditor':
                            iconClassName = 'icon-gsuite-edit';
                            break;
                    }
                }
                return iconClassName;
            }

            function _getDefaultToolText() {
                var toolText = 'Edit document with desktop associated application.';
                if (self._defaultEditor) {
                    switch (self._defaultEditor) {
                        case 'OSEditor':
                            toolText = 'Edit with Microsoft Office Desktop.';
                            break;
                        case 'GSuiteEditor':
                            toolText = 'Edit document in G Suite Editor.';
                            break;
                    }
                }
                return toolText;
            }

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
                $('<button type="button" class="btn btn-primary btn-sm btn-labeled btn-default-edit" title="' + (displayRadioBtns ? _getDefaultToolText() : 'Edit document with desktop associated application.') + '"><span class="btn-label"><i class="' +
                    (displayRadioBtns ? _getDefaultEditorIcon() : 'icon-edit') + '"></i></span><span class="d-none d-lg-inline-block btn-edit-label">Edit</span></button>')
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

                $btnGroup.on('shown.bs.dropdown', function () {
                    self.ContextMenuID = oItem.Href;
                });
                $btnGroup.on('hidden.bs.dropdown', function () {
                    self.ContextMenuID = null;
                });
                $dropdownMenu = $('<div class="dropdown-menu dropdown-menu-right actions ' + (displayRadioBtns ? 'dropdown-menu-radio-btns' : '') + '"></div>').appendTo($btnGroup);
                if (isMicrosoftOfficeDocument) {
                    if (displayRadioBtns) {
                        $('<label class="custom-radiobtn"><input type="radio" name="defaultEditor' + oItem.DisplayName +
                            '" value="OSEditor" ' + _isDefaultEditor('OSEditor') +
                            ' /><span class="checkmark"></span></label>').appendTo($dropdownMenu).find('input[type=radio]').change(_changeDefaultEditor);
                    }
                    $('<a class="dropdown-item ' + (displayRadioBtns ? 'dropdown-radio' : '') + '" href="javascript:void()" title="Edit with Microsoft Office Desktop."><i class="icon-microsoft-edit"></i>Edit with Microsoft Office Desktop</a>')
                        .appendTo($dropdownMenu).on('click', function () {
                            oWebDAV.EditDoc(oItem.Href);
                        });
                }

                if (isGSuiteDocument) {
                    if (displayRadioBtns) {
                        $('<label class="custom-radiobtn"><input type="radio" name="defaultEditor' + oItem.DisplayName + '" value="GSuiteEditor" ' +
                            _isDefaultEditor('GSuiteEditor') + '/><span class="checkmark"></span></label>').appendTo($dropdownMenu).find('input[type=radio]')
                            .change(_changeDefaultEditor).attr('disabled', !supportGSuiteFeature);
                    }
                    $('<a class="dropdown-item ' + (displayRadioBtns ? 'dropdown-radio' : '') + (supportGSuiteFeature ? '' : ' disabled') + '" href="javascript:void()" title="Edit document in G Suite Editor."><i class="icon-gsuite-edit"></i>Edit with Google G Suite Online (Beta)</a>')
                        .appendTo($dropdownMenu).on('click', function () {
                            oWebDAV.GSuiteEditDoc(oItem.Href);
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

                // open context menu if it was open before update
                if (self.ContextMenuID && self.ContextMenuID == oItem.Href) {
                    $dropdownToggle.dropdown('toggle');
                }

                actions.push($btnGroup);
            }

            return actions;
        },

        _AddSelectedItem: function (oItem) {
            this.selectedItems.push(oItem);
            this._UpdateToolbarButtons();
        },

        _RemoveSelectedItem: function (oItem) {
            var self = this;
            $.each(this.selectedItems, function (index) {
                if (self.selectedItems[index].Href === oItem.Href) {
                    self.selectedItems.splice(index, 1);
                    return false;
                }
            });

            this._UpdateToolbarButtons();
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

        _UpdateToolbarButtons: function () {
            this.$elToolbar.find('.btn-delete-items').attr('disabled', this.selectedItems.length == 0);

            if (ITHit.Environment.OS == 'Windows') {
                this.$elToolbar.find('.btn-print-items').attr('disabled',
                    this.selectedItems.filter(function (item) { return !item.IsFolder(); }).length == 0);
            }
        },

        _UncheckTableCheckboxs: function () {
            this.selectedItems = [];
            this.$el.find('input[type="checkbox"]').prop('checked', false);
        },

        /**
         * Hide toolbar.
         **/
        HideToolbar: function () {
            this._UncheckTableCheckboxs();
            this._UpdateToolbarButtons();
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
                var oLabel = i === 0 ? $('<span />').addClass('icon-home') : $('<span />').text(decodeURIComponent(sPart));
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
                var oValidationMessage = WebdavCommon.Validators.ValidateName(self.$txt.val());
                if (oValidationMessage) {
                    self.$alert.removeClass('d-none').text(oValidationMessage);
                    return false;
                }

                self.$txt.blur();
                self.$submitButton.attr('disabled', 'disabled');
                oWebDAV.CreateFolder(self.$txt.val().trim(), function (oAsyncResult) {
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
                oFolderGrid.HideToolbar();
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
                oFolderGrid.HideToolbar();
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
            if (webDavSettings.EditDocAuth.Authentication.toLowerCase() == 'cookies') {
                ITHit.WebDAV.Client.DocManager.DavProtocolEditDocument(sDocumentUrl, this.GetMountUrl(), this._ProtocolInstallMessage.bind(this), null, webDavSettings.EditDocAuth.SearchIn,
                    webDavSettings.EditDocAuth.CookieNames, webDavSettings.EditDocAuth.LoginUrl);
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
           * Print documents.
           * @param {string} sDocumentUrls Array of document URLs
        */
        PrintDocs: function (sDocumentUrls) {
            ITHit.WebDAV.Client.DocManager.DavProtocolEditDocument(sDocumentUrls, this.GetMountUrl(), this._ProtocolInstallMessage.bind(this), null, webDavSettings.EditDocAuth.SearchIn,
                webDavSettings.EditDocAuth.CookieNames, webDavSettings.EditDocAuth.LoginUrl, 'Print');
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
         * Returns url of app installer
         */
        GetInstallerFileUrl: function () {
            return webDavSettings.ApplicationProtocolsPath + ITHit.WebDAV.Client.DocManager.GetInstallFileName();
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
            var installerFilePath = this.GetInstallerFileUrl();

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


                        window.open(installerFilePath);
                    }, { size: 'lg' });
            }
        }
    };

    var oFolderGrid = new FolderGridView('.ithit-grid-container', '.ithit-grid-toolbar');
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