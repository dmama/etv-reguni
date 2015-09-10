package ch.vd.uniregctb.interfaces.service;

public interface ServiceCivilLogger {

	/**
	 * Active ou non le dump des individus récupérés dans le registre civil <b>pour le thread courant</b>.
	 *
	 * @param value <b>vrai</b> si le dump est activé; <b>faux</b> s'il n'est pas activé.
	 */
	public void setIndividuLogging(boolean value);
}
