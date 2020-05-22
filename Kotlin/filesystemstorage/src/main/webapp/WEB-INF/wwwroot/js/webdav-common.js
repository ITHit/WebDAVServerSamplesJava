
/**
 * @namespace WebdavCommon
 */
window.WebdavCommon = (function () {
    var sGSuitePreviewErrorMessage = "Preview document with G Suite Online Tool error.";
    var sGSuiteEditErrorMessage = "Edit document with G Suite Online Editor error.";
    var sFileNameSpecialCharactersRestrictionFormat = "The name cannot contain any of the following characters: {0}";
    var sForbiddenNameChars = '\/:*?"<>|';

    var ns = {};

    /**@class Formatters
     * @memberof! WebdavCommon
     */
    var Formatters = ns.Formatters = {

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
            return moment(oDate).fromNow();
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
        },

        /**
         *
         * @param {string} fileName
         * @returns {string}
         */
        GetFileNameWithoutExtension: function (fileName) {
            var index = fileName.lastIndexOf('.');
            return index !== -1 ? fileName.slice(0, index) : '';
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
        },
        /**
         * Converts a string to an HTML-encoded string.
         * @param {string} sText - The string to encode.
         * @return {string} - An encoded string.
         */
        HtmlEscape: function(sText) {
            return String(sText)
                .replace(/&/g, '&amp;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;');
        }
    };

    /**
     * This class represents error that occured on client.
     * @class ClientError
     * @memberof! WebdavCommon
     * @param {string} sMessage - The message will be displayed as error's short description.
     * @param {string} sUri - This url will be displayed as item's URL caused error.
     * @property {string} Message
     * @property {string} Uri
     */
    var ClientError = ns.ClientError = function ClientError(sMessage, sUri) {
        this.Message = sMessage;
        this.Uri = sUri;
    };

    /**@class Validators
     * @memberof! WebdavCommon
     */
    ns.Validators = /** @leads Validators */ {

        /**
         * @param {string} sName - The name to check.
         * @memberof Validators.
         * @returns {undefined | string} - Undefined if item valid or error string.
         */
        ValidateName: function(sName) {
            var oRegExp = new RegExp('[' + sForbiddenNameChars + ']', 'g');
            if(oRegExp.test(sName)) {
                var sMessage = WebdavCommon.PasteFormat(sFileNameSpecialCharactersRestrictionFormat,
                    sForbiddenNameChars.replace(/\\?(.)/g, '$1 '));
                return sMessage;
            }
        }
    };

    ns.PasteFormat = function pasteFormat(sPhrase) {
        var callbackReplace = function(oArguments) {
            this._arguments = oArguments;
        };

        callbackReplace.prototype.Replace = function(sPlaceholder) {

            var iIndex = sPlaceholder.substr(1, sPlaceholder.length - 2);
            return ('undefined' !== typeof this._arguments[iIndex]) ? this._arguments[iIndex] : sPlaceholder;
        };

        if(/\{\d+?\}/.test(sPhrase)) {
            var oReplace = new callbackReplace(Array.prototype.slice.call(arguments, 1));
            sPhrase = sPhrase.replace(/\{(\d+?)\}/g, function(args) { return oReplace.Replace(args); });
        }

        return sPhrase;
    };

    /**
     * This class provides method for display error modal window.
     * @param {string} selector - The selector of root element of modal window markup.
     * @class ErrorModal
     * @memberof! WebdavCommon
     */
    function ErrorModal(selector) {
        this.$el = $(selector);
        this.$el.on('hidden.bs.modal', this._onModalHideHandler.bind(this));
    };

    /**@lends ErrorModal.prototype */
    ErrorModal.prototype = {

        /**
         * Shows modal window with message and error details.
         * @method
         * @param {string} sMessage - The error message.
         * @param {ITHit.WebDAV.Client.Exceptions.WebDavHttpException | ClientError} oError - The error object to display.
         * @param {function()} [fCallback] - The callback to be called on close.
         */
        Show: function (sMessage, oError, fCallback) {
            this._closeCallback = fCallback || $.noop;
            this._SetErrorMessage(sMessage);
            this._SetUrl(oError.Uri);
            this._SetMessage(oError.Message);

            if (oError.Error) {
                this._SetBody(oError.Error.Description || oError.Error.BodyText);
            } else if (oError.InnerException) {
                this._SetBody(oError.InnerException.toString());
            }

            this.$el.modal('show');
        },

        _SetErrorMessage: function (sMessage) {
            this.$el.find('.error-message').html(sMessage);
        },

        _SetUrl: function (sUrl) {
            this.$el.find('.error-details-url').html(Formatters.HtmlEscape(sUrl));
        },

        _SetMessage: function (sMessage) {
            sMessage = Formatters.HtmlEscape(sMessage);
            sMessage = String(sMessage).replace(/\n/g, '<br />\n').replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;');
            this.$el.find('.error-details-message').html(sMessage);
        },

        _SetBody: function (sMessage) {
            var iframe = this.$el.find('iframe')[0];
            var doc = iframe.contentDocument || iframe.contentWindow.document;

            // FireFox fix, trigger a page `load`
            doc.open();
            doc.close();

            doc.body.innerHTML = sMessage;
        },
        _onModalHideHandler: function () {
            this._closeCallback();
        }
    };

    ns.ErrorModal = new ErrorModal('#ErrorModal');
    // function Spliter(selectorLeftPanel, selectorRightPanel) {
    //     // add spliter button
    //     Split([selectorLeftPanel, selectorRightPanel], {
    //         elementStyle: function (dimension, size, gutterSize) {
    //             $(window).trigger('resize');
    //             if (size < 1) {
    //                 return { 'flex-basis': '0px', 'height': '0px' };
    //             }
    //             else {
    //                 return { 'flex-basis': 'calc(' + size + '% - ' + gutterSize + 'px)', 'height': '' };
    //             }
    //         },
    //         gutterStyle: function (dimension, gutterSize) {
    //             return { 'flex-basis': gutterSize + 'px' };
    //         },
    //         sizes: [60, 40],
    //         minSize: [400, 0],
    //         expandToMin: true,
    //         gutterSize: 30,
    //         cursor: 'col-resize',
    //         onDragEnd: function (sizes) {
    //             $(selectorRightPanel).removeClass('disable-iframe-events');
    //         },
    //         onDragStart: function (sizes) {
    //             $(selectorRightPanel).addClass('disable-iframe-events');
    //         }
    //     });
    //
    //     // handle resize window event
    //     $(window).resize(function () {
    //         // settimeout because resize event is triggered before applying 'flex-basis' css rule
    //         this.setTimeout(function () {
    //             var $pnl = $('#leftPanel');
    //             var classAttr = 'col';
    //             if ($pnl.width() <= 566) {
    //                 classAttr += ' medium-point';
    //             }
    //             else if ($pnl.width() <= 692) {
    //                 classAttr += ' large-point';
    //             }
    //             $pnl.attr('class', classAttr);
    //         }, 10);
    //     });
    // };

    // ns.Spliter = new Spliter('#leftPanel', '#rightPanel');

    function GSuiteEditor(selectorGSuite) {
        this.$gSuiteTabs = $(selectorGSuite + ' #gSuiteTabs');
        this.$gSuitePreviewPanel = $(selectorGSuite + ' #gSuitePreview');
        this.$gSuitePreviewBackground = $(selectorGSuite + ' #gSuitePreviewBackground');
        this.$gSuiteEditPanel = $(selectorGSuite + ' #gSuiteEdit');
        this.$gSuiteEditBackground = $(selectorGSuite + ' #gSuiteEditBackground');
        this.$fileName = $(selectorGSuite + ' #fileName');
    }

    GSuiteEditor.prototype = {
        Render: function (oItem) {
            var self = this;
            this.isEditorLoaded = false;
            this.isPreviewLoaded = false;
            this.$gSuitePreviewPanel.empty();
            this.$gSuiteEditPanel.empty();
            this.$gSuiteEditContainer = $('<div class="inner-container"/>').appendTo(this.$gSuiteEditPanel);
            this.$gSuitePreviewContainer = $('<div class="inner-container"/>').appendTo(this.$gSuitePreviewPanel);

            // display file name
            this.$fileName.text(oItem.DisplayName);

            if (WebDAVController.OptionsInfo.Features & ITHit.WebDAV.Client.Features.GSuite) {
                if (self.activeSelectedTab == 'edit') {
                    this._RenderEditor(oItem);
                    this.$gSuiteTabs.find('#edit-tab').tab('show');
                }
                else {
                    this._RenderPreview(oItem);
                    this.$gSuiteTabs.find('#preview-tab').tab('show');
                }

                // add handler for preview tab
                this.$gSuiteTabs.find('#preview-tab').unbind().on('shown.bs.tab', function () {
                    self._RenderPreview(oItem);
                });

                // add handler for edit tab
                this.$gSuiteTabs.find('#edit-tab').unbind().on('shown.bs.tab', function () {
                    self._RenderEditor(oItem);
                });
            }
            else if (!(WebDAVController.OptionsInfo.Features & ITHit.WebDAV.Client.Features.GSuite)) {
                this.$gSuitePreviewBackground.text('GSuite preview and edit is not supported.');
            }
        },

        _RenderEditor: function (oItem) {
            var self = this;
            this.activeSelectedTab = 'edit';

            if (ITHit.WebDAV.Client.DocManager.IsGSuiteDocument(oItem.Href)) {
                if (!this.isEditorLoaded) {
                    this.$gSuiteEditBackground.text('Loading ...');
                    WebDAVController.GSuiteEditDoc(oItem.Href, this.$gSuiteEditContainer[0], function (e) {
                        self.$gSuiteEditPanel.empty();
                        self.$gSuiteEditBackground.text('Select a document to edit.');
                        if (e instanceof ITHit.WebDAV.Client.Exceptions.LockedException) {
                            WebdavCommon.ErrorModal.Show("The document is locked exclusively.<br/>" +
                                "You can not edit the document in G Suite in case of an exclusive lock.", e);
                        }
                        else {
                            WebdavCommon.ErrorModal.Show(sGSuiteEditErrorMessage, e);
                        }

                    });
                    this.isEditorLoaded = true;
                }
            }
            else {
                this.$gSuiteEditBackground.text('GSuite editor for this type of document is not available.');
            }
        },

        _RenderPreview: function (oItem) {
            var self = this;
            this.activeSelectedTab = 'preview';

            if (!this.isPreviewLoaded) {
                this.$gSuitePreviewBackground.text('Loading preview...');
                WebDAVController.GSuitePreviewDoc(oItem.Href, this.$gSuitePreviewContainer[0], function (e) {
                    self.$gSuitePreviewPanel.empty();
                    self.$gSuitePreviewBackground.text('Select a document to preview.');
                    WebdavCommon.ErrorModal.Show(sGSuitePreviewErrorMessage, e);
                });
                isPreviewLoaded = true;
            }
        }
    };

    ns.GSuiteEditor = new GSuiteEditor('.gsuite-container');
    return ns;
})();