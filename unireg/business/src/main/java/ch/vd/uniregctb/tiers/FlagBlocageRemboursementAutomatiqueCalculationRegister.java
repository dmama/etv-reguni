package ch.vd.uniregctb.tiers;

/**
 * Interface implémentée par la mécanique que calcul différée du flag de blocage de remboursement automatique
 * selon les fors du contribuable
 */
public interface FlagBlocageRemboursementAutomatiqueCalculationRegister {

	/**
	 * Enregistre une demande de recalcul du flag pour le tiers dont le numéro est donné
	 * @param tiersId numéro du tiers pour lequel on veut faire un recalcul différé du flag de blocaqe de remboursement automatique
	 */
	void enregistrerDemandeRecalcul(long tiersId);
}
