
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
    return ns;
})();