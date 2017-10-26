
(function() {

	window.Helper = {
		_TestFolder: 'qtest_' + (new Date().getTime()) + '/',
		_Folders: {},
		_Files: {},

		/**
		 *
		 * @param sRelativePath
		 * @returns {string}
		 */
		GetPath: function(sRelativePath) {
			return '/' + this._TestFolder + sRelativePath;
		},

		/**
		 *
		 * @param sRelativePath
		 * @returns {string}
		 */
		GetAbsolutePath: function(sRelativePath) {
			return this._GetHost() + this.GetPath(sRelativePath).substr(1);
		},

		GetSession: function() {
			return window.webDavSession;
		},

		GetFolder: function(/** {string} ['/folder1/folder11/' | '/'] */ sPath, fCallback) {
			var that = this;

			if (this._Folders[sPath]) {
				fCallback(that._Folders[sPath]);
			} else {
				sPath = sPath.replace(/^\//, "");

				var sAbsolutePath = this._GetHost() + sPath;	// .substr(1);

				var oSession = this.GetSession();
				oSession.OpenFolderAsync(sAbsolutePath, null, function (oAsyncResult) {
					if (oAsyncResult.Result === null) {
						console.debug('ERROR :: Helper :: Can not get folder `' + sAbsolutePath + '` :: oAsyncResult.Error :: ', oAsyncResult.Error);
						throw Error('Helper :: Can not get folder `' + sAbsolutePath + '` :: oAsyncResult.Error :: ' + oAsyncResult.Error);
					}
					that._Folders[sPath] = oAsyncResult.Result;
					fCallback(oAsyncResult.Result);
				});
			}
		},

		/**
		 * @param aPaths {string|string[]} paths to create. When all pathes have been created call fCallback
		 * @param fCallback
		 */
		Create: function(aPaths, fCallback) {
			aPaths = [].concat(aPaths);

			var that = this;
			this._CreateNext(aPaths, 0, function() {
				var aItems = [];

				for (var i = 0, l = aPaths.length; i < l; i++) {
					if (aPaths[i] === '/') {
						aItems.push(that._Folders['']);
					} else if (aPaths[i].substr(-1) === '/') {
						aItems.push(that._Folders[that._TestFolder + aPaths[i]]);
					} else {
						aItems.push(that._Files['/' + that._TestFolder + aPaths[i]]);
					}
				}

				fCallback.apply(null, aItems);
			});
		},

		/**
		 * creates aPaths[i]. if "i" greater then aPaths.length - 1 fCallback will be called
		 * @param aPaths
		 * @param i {number}
		 * @param fCallback
		 */
		_CreateNext: function(aPaths, i, fCallback) {
			var sPath = aPaths[i];

			if (!sPath) {
				fCallback();
				return;
			}

			var that = this;
			if (/\/$/.test(sPath)) {
				this._CreateFolder(sPath, function() {
					that._CreateNext(aPaths, i + 1, fCallback);
				});
			} else {
				this._CreateFile(sPath, function() {
					that._CreateNext(aPaths, i + 1, fCallback);
				});
			}
		},

		CheckVersionsSupported: function(oFile, fCallback) {
			oFile.GetSupportedFeaturesAsync(function(oAsyncResult) {

				/** @typedef {ITHit.WebDAV.Client.OptionsInfo} oOptionsInfo */
				var oOptionsInfo = oAsyncResult.Result;

				if ((oOptionsInfo.VersionControl & ITHit.WebDAV.Client.Features.VersionControl) === 0) {
					fCallback(false);
					return;
				}

				if (oFile.VersionControlled) {
					fCallback(true);
					return;
				}

				oFile.PutUnderVersionControlAsync(true, null, function(oAsyncResult) {
					fCallback(oAsyncResult.IsSuccess);
				});
			});
		},

		_GetHost: function() {
			return window.ITHitTestsConfig.Url.replace(/\/?$/, '/');
		},

		/**
		 *
		 * @param sPath
		 * @param fCallback
		 * @private
		 */
		_CreateFolder: function(sPath, fCallback) {
			var folders = this.GetPath(sPath).split('/');
			folders.shift();

			var that = this;
			that._CreateFolderNext(folders, '', 0, fCallback);
		},

		_CreateFolderNext: function(folders, sParentFolder, i, fCallback) {
			if (!folders[i]) {
				fCallback();
				return;
			}

			var sFolder = sParentFolder + folders[i] + '/';

			if (this._Folders[sFolder]) {
				this._CreateFolderNext(folders, sFolder, i + 1, fCallback);
			} else {
				var that = this;
				this.GetFolder(sParentFolder || '/', function(oFolder) {
					var sFolderName = folders[i] + '/';
					oFolder.CreateFolderAsync(sFolderName, null, null, function (oAsyncResult) {
						that._Folders[sFolder] = oAsyncResult.Result;
						that._CreateFolderNext(folders, sFolder, i + 1, fCallback);
					});
				});
			}
		},

		/**
		 *
		 * @param sPath
		 * @param fCallback
		 * @private
		 */
		_CreateFile: function(sPath, fCallback) {
			var matches = /(.+\/)?([^/]+)$/.exec(sPath);
			var folderRelative = matches[1] || '';
			var file = matches[2];
			var folder = this.GetPath(folderRelative);

			if (folderRelative) {
				var that = this;
				this._CreateFolder(folderRelative, function() {
					that._CreateFileItem(folder, file, fCallback);
				});
			} else {
				this._CreateFileItem(folder, file, fCallback);
			}
		},

		_CreateFileItem: function(folder, file, fCallback) {
			if (this._Files[folder + file]) {
				setTimeout(function() {
					fCallback();
				});
				return;
			}

			var that = this;
			this.GetFolder(folder, function(oFolder) {
				oFolder.CreateFileAsync(file, null, 'test..', null, function(oAsyncResult) {
					that._Files[folder + file] = oAsyncResult.Result;
					fCallback(oAsyncResult.Result);
				});
			});
		},

		Destroy: function() {
			this.GetFolder(this._TestFolder, function(oFolder) {
				oFolder.DeleteAsync(null, function() {
					// ok
				});
			});
		}
	};

})();