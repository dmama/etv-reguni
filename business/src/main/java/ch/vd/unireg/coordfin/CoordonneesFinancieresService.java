package ch.vd.unireg.coordfin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Contribuable;

public interface CoordonneesFinancieresService {

	interface UpdateNotifier {
		/**
		 * Méthode appelée lorsque le nouvel IBAN est invalide et que l'IBAN existant est bien valide.
		 * @param currentIban l'IBAN existant
		 * @param newIban le nouvel IBAN invalide
		 */
		void onInvalidNewIban(@NotNull String currentIban, @NotNull String newIban);
	}

	/**
	 * [SIFISC-20035] Met-à-jour les coordonnées financières si nécessaire (en gardant l'historique)
	 *
	 * @param ctb        le contribuable dont les coordonnées financières doivent être mises-à-jour
	 * @param titulaire  le nouveau titulaire à inscrire (optionel)
	 * @param iban       le nouvel iban à inscrire (optionel)
	 * @param dateValeur la date de valeur de la nouvelle inscription
	 */
	void updateCoordonneesFinancieres(@NotNull Contribuable ctb, @Nullable String titulaire, @Nullable String iban, @NotNull RegDate dateValeur, @NotNull UpdateNotifier notifier);

}
