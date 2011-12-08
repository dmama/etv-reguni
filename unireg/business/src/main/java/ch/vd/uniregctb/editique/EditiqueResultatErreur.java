package ch.vd.uniregctb.editique;

/**
 * Interface implémentée par un résultat d'impression en erreur
 */
public interface EditiqueResultatErreur extends EditiqueResultatRecu {

	/**
	 * Obtient le message de l'erreur survenue lors de la création du document.
	 *
	 * @return le message en cas d'erreur, sinon <codeb>null</code>.
	 */
	String getError();
}
