package ch.vd.unireg.coordfin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Tiers;

public interface CoordonneesFinancieresService {

	/**
	 * Ajoute de nouvelles coordonnées financières à un contribuable et ferme automatique les éventuelles coordonnées financières précédentes.
	 * @param tiers     le contribuable dont les coordonnées financières doivent être ajoutées
	 * @param dateDebut la date de début
	 * @param dateFin   la date de fin
	 * @param titulaire le nouveau titulaire à inscrire (optionel)
	 * @param iban      le nouvel iban à inscrire (optionel)
	 * @param bicSwift  le code bic swift (optionel)
	 */
	void addCoordonneesFinancieres(Tiers tiers, @NotNull RegDate dateDebut, @Nullable RegDate dateFin, @Nullable String titulaire, @Nullable String iban, @Nullable String bicSwift);

	/**
	 * Met-à-jour des coordonnées financières existantes. Si le seul changement est l'ajoute d'une date de fin, les coordonnées existantes sont simplement mises-à-jour ;
	 * dans les autres cas, les coordonnées existantes sont annulées et de nouvelles coordonnées existantes sont ajoutées.
	 *  @param id        l'id des coordonnées existantes à mettre-à-jour
	 * @param dateFin   la date de fin
	 * @param titulaire le nouveau titulaire à inscrire (optionel)
	 * @param iban      le nouvel iban à inscrire (optionel)
	 * @param bicSwift  le code bic swift (optionel)
	 */
	void updateCoordonneesFinancieres(long id, @Nullable RegDate dateFin, @Nullable String titulaire, @Nullable String iban, @Nullable String bicSwift);

	/**
	 * Annule les coordonnées financières spécifiées.
	 *
	 * @param id l'id des coordonnées existantes à annuler
	 */
	void cancelCoordonneesFinancieres(long id);

	interface UpdateNotifier {
		/**
		 * Méthode appelée lorsque le nouvel IBAN est invalide et que l'IBAN existant est bien valide.
		 * @param currentIban l'IBAN existant
		 * @param newIban le nouvel IBAN invalide
		 */
		void onInvalidNewIban(@NotNull String currentIban, @NotNull String newIban);
	}

	/**
	 * [SIFISC-20035] Met-à-jour les coordonnées financières si nécessaire (en gardant l'historique). Cette méthode est spécialisée
	 *                pour le traitement des données retournées par le contribuable sur sa déclaration d'impôt.
	 *
	 * @param ctb        le contribuable dont les coordonnées financières doivent être mises-à-jour
	 * @param titulaire  le nouveau titulaire à inscrire (optionel)
	 * @param iban       le nouvel iban à inscrire (optionel)
	 * @param dateValeur la date de valeur de la nouvelle inscription
	 */
	void detectAndUpdateCoordonneesFinancieres(@NotNull Contribuable ctb, @Nullable String titulaire, @Nullable String iban, @NotNull RegDate dateValeur, @NotNull UpdateNotifier notifier);

}
