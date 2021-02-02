﻿<!--!<#@ template language="C#" #>!-->
<!--!<#@ samplevars Processor="DirectiveProcessor" #>!-->
<!--!<# if (!Params.AddCustomGetHandler || Params.CalDav || Params.CardDav)  return string.Empty; #>!-->
function ToolbarReloadButton(name, cssClass, toolbar) {
    BaseButton.call(this, name, cssClass);

    this.Render = function () {
        this.$Button.on('click', function () {
            toolbar.WebDAV.Reload();
        })
    }
}