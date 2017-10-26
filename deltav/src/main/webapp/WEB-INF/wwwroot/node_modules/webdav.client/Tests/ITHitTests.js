document.title = 'IT Hit WebDAV AJAX Library Integration Tests';

window.ITHitTestsConfig = {
	Url: '',
	TestsHelperMethodsNamespace: 'ITHit.WebDAV.Client.Tests',
	Tests: [
		'DocManager/MsOfficeEditExtensions.js',
		'HierarchyItems/MicrosoftOfficeDiscovery.js',
		'HierarchyItems/CreateFolder.js',
		'HierarchyItems/CreateFile.js',
		'HierarchyItems/GetItemBySession.js',
		'HierarchyItems/GetItemByFolder.js',
		'HierarchyItems/GetFolderItems.js',
		'HierarchyItems/GetParent.js',
		'HierarchyItems/CopyMove.js',
		'HierarchyItems/Delete.js',
		'HierarchyItems/Refresh.js',
		'HierarchyItems/ItemExists.js',
		'HierarchyItems/SupportedFeatures.js',
		'HierarchyItems/Quota.js',
		'HierarchyItems/Progress.js',
		'HierarchyItems/Search.js',
		'HierarchyItems/NameCheck.js',
		'HierarchyProperties/GetProperties.js',
		'HierarchyProperties/UpdateProperties.js',
		'Locks/CheckSupport.js',
		'Locks/GetLocks.js',
		'Locks/Lock.js',
		'Locks/RefreshLock.js',
		'Versions/GetVersions.js',
		'Versions/ManageVersions.js',
		'Versions/PutUnderVersion.js',
		'Versions/ReadContent.js',
		'Upload/GetBytesUploaded.js',
		'Upload/CancelUpload.js',
		'WebDavSession/Events.js'

		// @todo Add tests for methods:
		// GetSource, GetSourceAsync
		// CreateLockNull
	]
};

// IE<=9
if (typeof console === "undefined" || typeof console.log === "undefined") {
	console = {};
	console.log = function() {};
	console.debug = function () { };
}
if (!Array.prototype.forEach) {
	Array.prototype.forEach = function(fn, scope) {
		for(var i = 0, len = this.length; i < len; ++i) {
			fn.call(scope, this[i], i, this);
		}
	}
}

;
// setTimeout(function() {

	function findFileDirectory(currentFileName) {
		var scripts = document.getElementsByTagName('script');
		for (var i = 0, l = scripts.length; i < l; i++) {
			var fileNameMatch = /(.+)(\\|\/)([^\/\\?]+)(\?.*)?$/.exec(scripts[i].src);
			if (fileNameMatch && fileNameMatch[3] === currentFileName) {
				return fileNameMatch[1] + fileNameMatch[2];
			}
		}
		return '';
	}

	// Find base url
	var baseUrl = findFileDirectory('ITHitTests.js') || findFileDirectory('TestsUI.js');

	// Load QUnit Runner
	(function(url, callback) {
		var script = document.createElement('script');
		script.src = url + '?t=' + (new Date()).getTime();
		script.onload = function () {
			callback();
			callback = function () { };
		};
		// For IE<9
		script.onreadystatechange = function () {
			if (this.readyState == "complete" || this.readyState == "loaded") {
				setTimeout(function() {
					callback();
					callback = function () { };
				});
			}
		};
		document.getElementsByTagName("head")[0].appendChild(script);
	})(baseUrl + 'qunit-runner/main.js', function() {

		// Initialize QUnit Runner
		QUnitRunner.init({
			baseUrl: baseUrl,
			preload: [
				'Helper.js',
				'ITHitTests.css'
			],
			tests: window.ITHitTestsConfig.Tests,
			onInit: function() {
				$(function() {
					window.ITHitTests = new ITHitTests(window.ITHitTestsConfig);
				});
			},
			onEnd: function() {
				Helper.Destroy();
			}
		});

		function ITHitTests(config) {
			this.config = config;
			this.isRun = false;
			this.isDone = false;
			this.$el = null;

			this.testInstances = {};
			this.testRequestsLog = {};

			// Store test instances
			QUnit.testStart($.proxy(function() {
				var test = QUnit.config.current;
				this.testInstances[test.testNumber] = test;
				this.testRequestsLog[test.testNumber] = [];

				this._renderTestButtons(test.testNumber);
				this._refreshProgress();
			}, this));
			QUnit.done($.proxy(function() {
				this.isRun = false;
				this.isDone = true;
				this.$el.find('.button-startToggle').val('Run');

				// Disable stop button
				this.$el.find('.button-stop').prop('disabled', true);
				this._refreshProgress();
			}, this));

			this._render($.proxy(function () {
				this._initUrl();

				// Auto start
				if (QUnitRunner.getHash('autostart') !== '0') {
					this.start();
				} else {
					QUnitRunner.setHash('autostart', null);
				}
			}, this));
		}

		ITHitTests.prototype = {

			/**
			 * Start tests
			 */
			start: function() {
				if (this.isRun) {
					return;
				}
				this.isRun = true;
				this.$el.find('.button-startToggle').val('Pause');

				// Enable stop button
				this.$el.find('.button-stop').prop('disabled', false);

				// Initialize
				window.webDavSession = new ITHit.WebDAV.Client.WebDavSession();
				ITHit.Config.PreventCaching = true; // Force disable cache for browsers. With cache ContentRange test is failed.
				this._subscribeOnRequests();

				// Create tests dir
				Helper.Create('/', function() {
					QUnitRunner.start();
				});
			},

			startAll: function() {
				setTimeout(function() {
					QUnitRunner.setHash('module', null);
					QUnitRunner.setHash('testNumber', null, true);
				});
			},

			/**
			 * Pause tests
			 */
			toggleStart: function() {
				if (this.isDone) {
					setTimeout(function() {
						window.location.reload();
					});
				} else if (this.isRun) {
					this.pause();
				} else {
					this.start();
				}
			},

			/**
			 * Pause tests
			 */
			pause: function() {
				if (!this.isRun) {
					return;
				}
				this.isRun = false;
				this.$el.find('.button-startToggle').val('Continue');

				QUnitRunner.stop();
			},

			stop: function() {
				this.pause();
				this.$el.find('.button-startToggle').val('Start');
				this.$el.find('.button-stop').prop('disabled', true);

				QUnitRunner.setHash('autostart', '0', true);
			},

			skip: function(assert, message) {
				assert.ok(true, message);

				var li = $('#qunit-test-output' + assert.test.testNumber);

				li.css({
					backgroundColor: '#eee',
					color: '#aaa'
				});
				li.find('.test-name').css({
					color: '#aaa'
				});
				li.find('.module-name').prepend('SKIPPED! ');

				setTimeout(function() {
					li.find('.counts').css({
						display: 'none'
					});
				}, 14);
			},

			/**
			 *
			 * @param url
			 */
			setUrl: function(url) {
				this.config.Url = url;
				this.$el.find('input[name="Url"]').val(url);

				QUnitRunner.setHash('url', url);
			},

			/**
			 *
			 * @private
			 */
			_render: function(fCallback) {
				if (jQuery('#qunit-testrunner-toolbar').length < 1) {
					setTimeout($.proxy(function () {
						this._render(fCallback);
					}, this), 100);
				} else {
					this.$el = $('<div />').appendTo('#qunit-testrunner-toolbar');
					$('<meta />')
					.appendTo('head')
					.attr({
						charset: 'utf-8'
					});

					this.$el.css({
						paddingTop: 5
					});

					$('<label />')
						.appendTo(this.$el)
						.text('Server Url:');

					$('<input />')
						.appendTo(this.$el.find('label'))
						.attr({
							name: 'Url'
						})
						.css({
							width: 200,
							marginLeft: 5
						})
						.on('keyup', $.proxy(function (e) {
							if (e.keyCode === 13) {
								this.setUrl(this.$el.find('input[name="Url"]').val());
								this.toggleStart();
							}
						}, this));

					$('<input type="button" />')
						.appendTo(this.$el)
						.addClass('button-startToggle')
						.attr({
							value: 'Start'
						})
						.on('click', $.proxy(function () {
							this.setUrl(this.$el.find('input[name="Url"]').val());
							this.toggleStart();
						}, this));

					if (QUnit.config.testNumber.length > 0) {
						$('<input type="button" />')
							.appendTo(this.$el)
							.css('margin-left', '5px')
							.attr({
								value: 'Run all'
							})
							.on('click', $.proxy(function () {
								this.setUrl(this.$el.find('input[name="Url"]').val());
								this.startAll();
							}, this));
					}

					$('<input type="button" />')
						.appendTo(this.$el)
						.addClass('button-stop')
						.css('margin-left', '5px')
						.attr({
							value: 'Stop'
						})
						.prop({
							disabled: true
						})
						.on('click', $.proxy(function () {
							this.stop();
						}, this));

					// Hide QUnit `Running...`
					$('#qunit-testresult').html('');

					this._refreshProgress();

					fCallback();
				}
			},

			_renderTestButtons: function(testNumber) {
				var li = $('#qunit-test-output' + testNumber);

				var container = $('<div class="actions" />')
					.insertAfter(li.find('strong'));

				// Fix reload page
				li.find('a:contains(Rerun)')
					.appendTo(container)
					.on('click', function() {
						// Force reload
						setTimeout(function() {
							location.reload();
						});
					});

				hljs.configure({
					useBR: true
				});

				$('<a href="javascript:void(0)" />')
					.appendTo(container)
					.text('Show code')
					.on('click', $.proxy(function() {
						var code = this.testInstances[testNumber].callback.toString() + '\n';

						// Append helper methods from tests namespace
						var sNamespace = this.config.TestsHelperMethodsNamespace;
						if (sNamespace) {
							var oRegExp = new RegExp('(' + sNamespace.replace(/\./, '\\.') + '\\.[^(]+)\\(');
							var oMethods = {};

							function findHelperMethods(sSource) {
								var i = 0;
								var oMatch = null;

								while (true) {
									oMatch = oRegExp.exec(sSource.substr(i))
									if (!oMatch || oMatch.index < i) {
										break;
									}
									i = oMatch.index + 1;

									if (!oMethods[oMatch[1]]) {
										oMethods[oMatch[1]] = ITHit.Namespace(oMatch[1]).toString();
										findHelperMethods(oMethods[oMatch[1]]);
									}
								}
							}
							findHelperMethods(code);

							for (var sName in oMethods) {
								if (oMethods.hasOwnProperty(sName)) {
									code += '\n' + oMethods[sName].replace(/\n(    |\t)/g, '\n').replace(/(^function )/, '$1' + sName);
								}
							}

							// Remove namespace
							for (var sName2 in oMethods) {
								if (oMethods.hasOwnProperty(sName2)) {
									var aParts = sName2.split('.');
									aParts.pop();

									code = code.replace(new RegExp(aParts.join('\\.') + '\\.', 'g'), '');
								}
							}
						}

						if (!ITHit.DetectBrowser.IE || ITHit.DetectBrowser.IE > 9) {
							code = hljs.highlight('javascript', code).value;
						} else {
							code = code.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
						}

						this._showPopup('<pre>' + code + '</pre>');
					}, this));

				$('<a href="javascript:void(0)" />')
					.appendTo(container)
					.text('Show logs')
					.on('click', $.proxy(function() {
						var code = this.testRequestsLog[testNumber].join('\n');
						if (!ITHit.DetectBrowser.IE || ITHit.DetectBrowser.IE > 9) {
							code = hljs.highlight('xml', code).value;
						} else {
							code = code.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
						}

						this._showPopup('<pre>' + code + '</pre>');
					}, this));
			},

			_refreshProgress: function() {
				var elBanner = $('#qunit-banner');
				var elProgressText = $('#qunit-header').find('.ITHitTests-progress-text');

				// Lazy render
				if (elProgressText.length === 0) {
					elProgressText = $('<span />')
						.appendTo('#qunit-header')
						.addClass('ITHitTests-progress-text');

					elBanner.addClass('qunit-pass');
				}

				var totalCount = QUnit.config.testNumber.length > 0 ? QUnit.config.testNumber.length : QUnitRunner.tests.length;
				var currentCount = this.isDone ? totalCount : (QUnit.config.current ? QUnit.config.current.testNumber - 1 : 0);
				elProgressText.text('(' + currentCount + '/' + totalCount + ')');

				elBanner.css('width', Math.round(100 * currentCount / totalCount) + '%');
			},

			_showPopup: function(html) {
				var dialog = $('<div />')
					.appendTo('body')
					.addClass('ITHitTests-popup');

				// Content
				$('<div>')
					.appendTo(dialog)
					.addClass('ITHitTests-popup-content')
					.html(html);

				// Close button
				$('<a href="javascript:void(0)" />')
					.appendTo(dialog)
					.addClass('ITHitTests-popup-closeButton')
					.text('x')
					.on('click', function() {
						dialog.remove();
					});

				// Close on esc
				$(document).one('keyup', function(e) {
					if (e.keyCode == 27) {
						dialog.remove();
					}
				});

				return dialog;
			},

			/**
			 *
			 * @private
			 */
			_initUrl: function() {
				if (this.config.Url) {
					return;
				}

				var url = '';
				if (!url && QUnitRunner.getHash('url')) {
					url = QUnitRunner.getHash('url');
				}
				if (!url && window.opener && window.opener.location) {
					url = window.opener.location.href;
				}
				if (!url) {
					url = location.href.replace(/[^/]*(\?[^?]+)?(#[^#]+)?$/, '');
				}
				this.setUrl(url);
			},

			_subscribeOnRequests: function() {
				var that = this;

				webDavSession.AddListener('OnBeforeRequestSend', function(oEvent) {
					var log = '';

					var oDateNow = new Date();
					log += '----------------- Started: ' + oDateNow.toUTCString() + ' [' + oDateNow.getTime() + '] -----------------\n';

					// Show request info
					log += oEvent.Method + ' ' + oEvent.Href + '\n';
					for (var sKey in oEvent.Headers) {
						if (oEvent.Headers.hasOwnProperty(sKey)) {
							log += sKey + ': ' + oEvent.Headers[sKey] + '\n';
						}
					}

					// Show request body
					log += oEvent.Body + '\n';

					var testNumber = QUnit.config.current && QUnit.config.current.testNumber;
					if (testNumber) {
						that.testRequestsLog[testNumber].push(log);
					}
				});

				webDavSession.AddListener('OnResponse', function(oEvent) {
					var log = '';

					// Show HTTP status and description
					log += oEvent.Status + ' ' + oEvent.StatusDescription + '\n';

					// Show headers
					for (var sKey in oEvent.Headers) {
						if (oEvent.Headers.hasOwnProperty(sKey)) {
							log += sKey + ': ' + oEvent.Headers[sKey] + '\n';
						}
					}

					// Show response body
					log += oEvent.BodyText + '\n';

					var oDateNow = new Date();
					log += '----------------- Finished: ' + oDateNow.toUTCString() + ' [' + oDateNow.getTime() + '] -----------------\n';

					var testNumber = QUnit.config.current && QUnit.config.current.testNumber;
					if (testNumber) {
						that.testRequestsLog[testNumber].push(log);
					}
				});
			}
		};
	});

// }, 10);