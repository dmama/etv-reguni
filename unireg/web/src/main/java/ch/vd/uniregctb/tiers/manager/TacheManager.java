package ch.vd.uniregctb.tiers.manager;

import ch.vd.uniregctb.tiers.Contribuable;

public interface TacheManager {

	/**
	 * Construit le composant Ajax qui permet d'afficher les actions de synchronisation qui sont nécessaires sur un contribuable.
	 *
	 * @param ctb                   un contribuable
	 * @param titre                 le titre du composant affiché
	 * @param titreErreurValidation le titre du composant affiché en cas d'erreur de validation du tiers
	 * @return un compostant Ajax ou <b>null</b> si aucune action n'est nécessaire.
	 */
	SynchronizeActionsTable buildSynchronizeActionsTable(Contribuable ctb, String titre, String titreErreurValidation);
}
