/**
     * This class represents button that occurred on client.
     * @class
     * @param {string} sName - The name of button.
     * @param {string} cssClass - This cssClass will be inserted into html.
     * @property {string} Name
     * @property {string} CssClass
     */
function BaseButton(sName, cssClass) {
    this.Name = sName;
    this.CssClass = cssClass;
    this.InnerHtmlContent = "";

    this.Create = function ($toolbarContainer) {
        $toolbarContainer.append('<button class="' + this.CssClass + '" title="' + this.Name + '">' + this.InnerHtmlContent + '</button>');
        this.$Button = $('.' + this.CssClass);
    }

    this.Disable  = function () {
        this.$Button.attr('disabled', true);
    }

    this.Activate = function () {
        this.$Button.attr('disabled', false);
    }

    this.HideOnMobile = function () {
        this.$Button.addClass('d-none d-md-inline');
    }

    this.ShowOnMobile = function () {
        this.$Button.removeClass('d-none d-md-inline');
    }
}