package ch.vd.uniregctb.editique;

/**
 * Interface implémentée par les trigger déclenchés à la réception
 * d'un retour d'impression
 */
public interface RetourImpressionTrigger {

	/**
	 * méthode appelée à la réception du retour d'impression pour lequel
	 * le trigger a été enregistré
	 * @param resultat document reçu
	 * @throws Exception en cas d'impossibilité d'effectuer la tâche demandée
	 */
	void trigger(EditiqueResultatRecu resultat) throws Exception;

}
