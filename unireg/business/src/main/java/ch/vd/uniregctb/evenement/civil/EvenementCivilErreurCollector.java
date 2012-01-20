package ch.vd.uniregctb.evenement.civil;

public interface EvenementCivilErreurCollector {

	/**
	 * Ajoute une erreur à partir de l'exception donnée en paramètre
	 * @param e exception
	 */
	void addErreur(Exception e);

	/**
	 * Ajoute une erreur à partir du message donné en paramètre
	 * @param msg le message
	 */
	void addErreur(String msg);

	/**
	 * Ajoute une erreur à partir du message et de l'exception donnée en paramètre
	 * @param msg le message
	 * @param e exception
	 */
	void addErreur(String msg, Exception e);

	/**
	 * @return <code>true</code> si au moins une erreur a été introduite, <code>false</code> sinon
	 */
	boolean hasErreurs();
}
