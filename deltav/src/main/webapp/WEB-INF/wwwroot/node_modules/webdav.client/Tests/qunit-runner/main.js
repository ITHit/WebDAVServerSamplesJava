(function() {

	var QUnitRunner = function() {

		/**
		 *
		 * @type {object}
		 * @protected
		 */
		this._config = {
			baseUrl: '',
			preload: [],
			tests: [],
			onInit: null,
			onEnd: null
		};

		this.tests = [];
		this._hashConfig = {};
		this._isInit = false;
		this._doStart = false;
		this._qUnitStart = null;
		this._qUnitLoad = null;
	};

	QUnitRunner.prototype = {

		/**
		 *
		 */
		init: function(config) {
			config = config || {};

			this._config.baseUrl = config.baseUrl || this._config.baseUrl;

			var that = this;
			// Load jQuery for render
			that.load('qunit-runner/qunit/jquery.js', function() {

				// Apply config
				$.extend(true, that._config, config);

				// Render QUnit elements
				that._render();

				// Load lib, scripts and tests
				that.load([
					'qunit-runner/qunit/qunit.js'
				], function() {

					QUnit.setWarning = function(/** QUnit.assert */ test) {
						var oDivAssert = $('#qunit-test-output' + test.test.testNumber + ' li')[test.test.assertions.length - 1];
						$(oDivAssert).addClass('warning');
					}
					// QUnit.stop = function(){};	// to backward compatibility

					that._freezeQUnit();
					that._preInitQUnit();

					that.load([
						'qunit-runner/qunit/qunit.css',
						'qunit-runner/highlight/github.css',
						'qunit-runner/highlight/highlight.js'
					].concat(that._config.preload), function() {
						that.load(that._config.tests, function() {

							// Configure QUnit library
							that._initQUnit();

							if (that._config.onInit) {
								that._config.onInit();
								that._config.onInit = null;
							}

							that._isInit = true;
						});
					});
				});
			});
		},

		/**
		 *
		 */
		start: function() {
			this._doStart = true;
			if (!this._isInit) {
				return;
			}

			QUnit.start = this._qUnitStart;
			QUnit.start();
		},

		/**
		 *
		 */
		stop: function() {
			this._doStart = false;
			if (!this._isInit) {
				return;
			}

			QUnit.stop();
		},

		async: function(callback) {
			QUnit.stop();
			return function() {
				QUnit.start();
				return callback.apply(null, arguments);
			};
		},

		test: function(testName, expected, callback, async) {
			this.tests.push(testName);
			QUnit.test.apply(QUnit.test, arguments);
		},

		/**
		 *
		 * @param {string|string[]} urls
		 * @param {function} [callback]
		 */
		load: function(urls, callback) {
			callback = callback || function() {};

			if (typeof urls === 'string') {
				urls = [urls];
			}

			if (urls.length === 0) {
				callback();
				return;
			}

			var counter = urls.length;
			var checkCallback = function() {
				counter--;

				if (counter === 0) {
					callback();
				}
			};

			for (var i = 0, l = urls.length; i < l; i++) {
				var aMatches = /\.([0-9a-z]+)(?:[\?#]|$)/i.exec(urls[i]);
				var fileExtension = aMatches !== null ? aMatches[1] : null;
				var url = this._config.baseUrl + urls[i];

				switch (fileExtension) {
					case 'js':
						this._loadScript(url, checkCallback);
						break;

					case 'css':
						this._loadCss(url, checkCallback);
						break;
				}
			}
		},

		setHash: function(key, value, forceRefresh) {
			forceRefresh = forceRefresh || false;

			this._hashConfig[key] = value;
			location.hash = this._buildUrlHash();

			if (forceRefresh) {
				window.location.reload();
			}
		},

		getHash: function(key) {
			return this._hashConfig.hasOwnProperty(key) ? this._hashConfig[key] : null;
		},

		_freezeQUnit: function() {
			// Fix QUnit autostart with global failure
			this._qUnitStart = QUnit.start;
			QUnit.start = function() {};
			this._qUnitLoad = QUnit.load;
			QUnit.load = function() {};
		},

		_parseUrlHash: function() {
			// Parse hash
			var hash = {};
			var hashParts = location.hash.substr(1).split('&');
			for (var i = 0, l = hashParts.length; i < l; i++) {
				var param = hashParts[i].split('=');
				if (param.length === 1) {
					param = ['url', param[0]];
				}
				hash[param[0]] = param[1];
			}

			return hash;
		},

		_buildUrlHash: function(customConfig) {
			var config = $.extend({}, this._hashConfig, customConfig);

			var params = [];
			for (var key in config) {
				if (config.hasOwnProperty(key)) {
					if (key === 'url') {
						continue;
					}
					if (config[key] === null) {
						continue;
					}

					params.push(key + '=' + config[key]);
				}
			}
			if (config.url) {
				params.push(config.url);
			}

			return params.length > 0 ? '#' + params.join('&') : '';
		},

		_preInitQUnit: function() {
			this._hashConfig = this._parseUrlHash();

			// Build url
			var that = this;
			QUnit.url = function(customConfig) {
				return location.href.replace(/#.*$/, '').replace(/\?.*$/, '') + that._buildUrlHash(customConfig);
			};

			if (this._hashConfig.testNumber) {
				QUnit.config.testNumber = this._hashConfig.testNumber.split(',');
				for (var i = 0, l = QUnit.config.testNumber.length; i < l; i++) {
					QUnit.config.testNumber[i] = parseInt(QUnit.config.testNumber[i]);
				}
			}
			if (this._hashConfig.module) {
				QUnit.config.module = this._hashConfig.module;
			}
			if (this._hashConfig.filter) {
				QUnit.config.filter = this._hashConfig.filter;
			}
		},

		_initQUnit: function() {
			QUnit.config.autostart = false;
			QUnit.config.reorder = false;
			QUnit.config.testTimeout = 15000;	// 15 seconds, then async tests will continue

			var that = this;
			QUnit.done(function() {
				if (that._doStart && that._config.onEnd) {
					that._config.onEnd();
					that._config.onEnd = null;
				}
			});

			QUnit.load = this._qUnitLoad;

			// Fix QUnit async load
			QUnit.config.autorun = false;
			if (document.readyState === "complete") {
				QUnit.load();
				QUnit.config.blocking = true;
			}
		},

		_render: function() {
			$('body').prepend('<div id="qunit"></div><div id="qunit-fixture"></div>');
		},

		/**
		 *
		 * @param {string} url
		 * @param {function} [callback]
		 */
		_loadScript: function(url, callback) {
			callback = callback || function() {};

			var script = document.createElement('script');

			script.async = false;
			script.src = this._appendCacheSuffix(url);
			script.onload = function () {
				callback(); callback = function() {};
			};
			// For IE<9
			script.onreadystatechange = function() {
				if (this.readyState == "complete" || this.readyState == "loaded") {
					setTimeout(function() {
						callback(); callback = function() {};
					});
				}
			};

			document.getElementsByTagName("head")[0].appendChild(script);
		},

		/**
		 *
		 * @param {string} url
		 * @param {function} [callback]
		 */
		_loadCss: function(url, callback) {
			callback = callback || function() {};

			var link = document.createElement('link');
			link.rel = 'stylesheet';
			link.type = 'text/css';
			link.href = this._appendCacheSuffix(url);
			document.getElementsByTagName("head")[0].appendChild(link);

			var sheet = 'sheet' in link ? 'sheet' : 'styleSheet';
			var cssRules = 'sheet' in link ? 'cssRules' : 'rules';

			var timeLeft = 10000; // 10 sec
			var timer = setInterval(function() {
				if ( link && link[sheet] ) { // success
					clearInterval( timer );
					callback();
				} else {
					timeLeft -= 10;
					if ( timeLeft < 0 ) {
						clearInterval( timer );
						callback();
					}
				}
			}, 10);
		},

		_appendCacheSuffix: function(url) {
			return url + (url.indexOf('?') === -1 ? '?' : '&') + 't=' + (new Date()).getTime();
		}
	};

	window.QUnitRunner = new QUnitRunner();

})();