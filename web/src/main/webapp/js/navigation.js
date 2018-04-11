"use strict";

/**
 * Méthodes pour gérer la navigation dans l'historique des pages consultées par un utilisateur dans sa session courante.
 */
var Navigation = {

	contextPath : null,

	/**
	 * Initialise le context de déploiement de l'application. Cette méthode doit être appelée une seule fois par page.
	 * @param cp le context path de déploiement de l'application (e.g. http://localhost:8080/fiscalite/unireg/web)
	 */
	init: function(cp) {
		cp = cp.replace(/;jsessionid.*$/, ''); // supprime le jsession id qui apparaît de temps en temps dans IE...
		this.contextPath = cp;
	},

	/**
	 * Mémorise l'affichage de la page courante.
	 *
	 * @param url l'url de la page courante
	 */
	onShow: function (url) {
		this.__addUrl(this.__getRelativeUrl(url));
	},

	/**
	 * Retourne à la page précédante. Cette méthode permet de simuler l'utilisation du bouton <i>back</i> tout en assurant
	 * d'atterrir sur une page valide si l'historique est vide.
	 *
	 * @param defaultPageUrl       l'URL de la page sur laquelle on veut revenir (e.g. '/tiers/visu.do')
	 * @param defaultParams les paramètres par défaut à utiliser si la page n'est pas trouvée dans l'historique (e.g. 'id=12345')
	 */
	back: function (defaultPageUrl, defaultParams) {

		// valeur par défaut
		var targetUrl = defaultPageUrl + (Navigation.__isBlankString(defaultParams) ? '' : '?' + defaultParams);

		// on recherche dans l'historique la dernière url visitée sur la page spécifiée
		this.__doInHistory(function (histo) {
			histo.pop();    // on ignore l'url courante de la page
			var u = histo.pop();
			if (u) {
				// il y a une page dans l'historique, on l'utilise
				targetUrl = u;
			}
			return histo;
		});

		// on navigue vers l'url de destination
		window.location.href = Navigation.__curl(targetUrl);
	},

	/**
	 * Construit l'URL pour retourner à une page précédemment consultée par l'utilisateur. Cette méthode permet de simuler l'utilisation
	 * répétée du bouton <i>back</i> en remontant sélectivement dans l'historique des pages consultées par l'utilisateur.
	 * Si la page spécifiée n'existe pas dans l'historique de navigation, l'utilisateur est renvoyé vers la page en utilisant des paramètres par défaut.
	 *
	 * @param pageUrls       les URLs des pages vers lesquelles on veut revenir, le plus récente des pages visitée sera choisie (e.g. '/tiers/visu.do')
	 * @param defaultPageUrl l'URL de la page par défaut (e.g. '/tiers/visu.do')
	 * @param defaultParams  les paramètres par défaut à utiliser si la page n'est pas trouvée dans l'historique (e.g. 'id=12345')
	 */
	backTo: function (pageUrls, defaultPageUrl, defaultParams) {

		// valeur par défaut
		var targetUrl = defaultPageUrl + (Navigation.__isBlankString(defaultParams) ? '' : '?' + defaultParams);

		// on recherche dans l'historique la dernière url visitée parmi les pages spécifiées
		this.__doInHistory(function (histo) {
			var u;
			while (u = histo.pop()) {
				if (Navigation.__inList(u, pageUrls)) {
					// on a trouvé une page correspondante dans l'historique, on l'utilise
					targetUrl = u;
					break;
				}
			}
			return histo;
		});

		// on navigue vers l'url de destination
		window.location.href = Navigation.__curl(targetUrl);
	},

	__curl: function(url) {
		if (!url) {
			return '';
		}
		while (url.indexOf('/') === 0) {
			url = url.substring(1);
		}
		return this.contextPath + url;
	},


	__isBlankString: function(str) {
		return (!str || /^\s*$/.test(str));
	},

	__inList: function (string, list) {
		for (var i = 0; i < list.length; ++i) {
			if (string.indexOf(list[i]) >= 0) {
				return true;
			}
		}
		return false;
	},

	__getRelativeUrl: function (url) {
		var pos = url.indexOf(Navigation.contextPath);
		if (pos >= 0) {
			url = url.slice(pos + Navigation.contextPath.length - 1); // -1 : on veut garder le / final : http://localhost:8079/unireg/web/tiers/visu.do => /tiers/visu.do
		}
		return url;
	},

	__doInHistory: function (action) {

		// on récupère l'historique dans le session storage
		var histoAsString = sessionStorage.getItem('unireg-url-history');
		var histo = histoAsString ? JSON.parse(histoAsString) : null;
		if (!histo || !Array.isArray(histo)) {
			histo = [];
		}

		// on appelle l'action
		histo = action(histo);

		// on met-à-jour l'historique dans le session storage
		sessionStorage.setItem('unireg-url-history', JSON.stringify(histo));
	},

	__addUrl: function (url) {
		this.__doInHistory(function (histo) {
			if (!histo.length || histo[histo.length - 1] !== url) { // inutile de mémoriser plusieurs fois de suite la même url
				histo.push(url);
			}
			// on limite l'historique à 20 positions, ça devrait suffire
			while (histo.length > 20) {
				histo.shift();
			}
			return histo;
		});
	}
};