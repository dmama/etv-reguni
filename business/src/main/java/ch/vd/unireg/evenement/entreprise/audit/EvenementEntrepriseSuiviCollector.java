package ch.vd.unireg.evenement.entreprise.audit;

public interface EvenementEntrepriseSuiviCollector {

	/**
	 * Ajoute un suivi à partir du message donné
	 * @param msg le message
	 */
	void addSuivi(String msg);

	/**
	 * @return <code>true</code> si au moins un suivi a été introduit, <code>false</code> sinon
	 */
	boolean hasSuivis();
}
