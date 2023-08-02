function ToolbarReloadButton(name, cssClass, toolbar) {
    BaseButton.call(this, name, cssClass);

    this.Render = function () {
        this.$Button.on('click', function () {
            toolbar.WebDAV.Reload();
        })
    }
}