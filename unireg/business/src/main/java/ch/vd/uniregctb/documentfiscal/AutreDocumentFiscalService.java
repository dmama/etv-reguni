package ch.vd.uniregctb.documentfiscal;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Service qui gère les autres documents fiscaux
 */
public interface AutreDocumentFiscalService {

	/**
	 * Envoi des lettres de bienvenue en masse
	 * @param dateTraitement date de traitement (= date du jour, ou assimilée)
	 * @param delaiCarence délai (en jours) de décalage pour la prise en compte des assujettissements
	 * @param statusManager status manager (pour la gestion, par exemple, de l'interruption du job, ou de la mesure de progression...)
	 * @return les données du rapport d'exécution
	 */
	EnvoiLettresBienvenueResults envoyerLettresBienvenueEnMasse(RegDate dateTraitement, int delaiCarence, StatusManager statusManager);

	/**
	 * @param e entreprise pour laquelle on doit envoyer une lettre de bienvenue (en mode batch)
	 * @param dateTraitement date de traitement
	 * @return la lettre de bienvenue envoyée
	 */
	LettreBienvenue envoyerLettreBienvenueBatch(Entreprise e, RegDate dateTraitement) throws AutreDocumentFiscalException;
}
