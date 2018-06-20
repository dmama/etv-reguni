package ch.vd.unireg.evenement.entreprise.audit;

public interface EvenementEntrepriseWarningCollector {

	/**
	 * Ajoute un warning à partir du message donné
	 * @param msg le message
	 */
	void addWarning(String msg);

	/**
	 * @return <code>true</code> si au moins un warning a été introduit, <code>false</code> sinon
	 */
	boolean hasWarnings();
}
