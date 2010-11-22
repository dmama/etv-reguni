package ch.vd.uniregctb.webservices.tiers2.stats;

abstract class Analyze {

	abstract void addCall(Call call);

	/**
	 * Construit et retourne l'url d'un graphique Google.
	 *
	 * @param method le nom d'une méthode du web-service
	 * @return l'url d'un graphique Google; ou <b>null</b> si aucune donnée n'existe pour la méthode spécifiée.
	 */
	abstract Chart buildGoogleChart(String method);

	abstract void print();

	abstract String name();
}
