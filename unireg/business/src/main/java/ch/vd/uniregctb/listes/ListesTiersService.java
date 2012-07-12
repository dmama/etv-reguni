package ch.vd.uniregctb.listes;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.listes.listesnominatives.ListesNominativesResults;
import ch.vd.uniregctb.listes.listesnominatives.TypeAdresse;
import ch.vd.uniregctb.listes.suisseoupermiscresident.ListeContribuablesResidentsSansForVaudoisResults;

/**
 * Interface du service pour les listes de tiers
 */
public interface ListesTiersService {

	/**
	 * Retourne les données nécessaires à la génération des listes nominatives
	 *
	 * @param dateTraitement date de traitement - la date du jour
	 * @param nbThreads degré de parallélisation du traitement
	 * @param adressesIncluses le type d'adresse à inclure
	 * @param avecContribuablesPP si oui ou non les contribuables PP (personnes physiques et ménages communs) doivent être inclus
	 * @param avecContribuablesPM si oui ou non les contribuables PM (entreprises) doivent être inclus
	 * @param avecDebiteurs si oui ou non les débiteurs de prestations imposables (employeurs de sourciers) doivent être inclus
	 * @param statusManager un status manager
	 * @return les données pour la liste globale
	 */
	ListesNominativesResults produireListesNominatives(RegDate dateTraitement, int nbThreads, TypeAdresse adressesIncluses,
	                                                   boolean avecContribuablesPP, boolean avecContribuablesPM, boolean avecDebiteurs,
	                                                   StatusManager statusManager);

	/**
	 * Retourne les données de la liste des contribuables suisses ou titulaires d'un permis C ayant une
	 * adresse de domicile vaudoise mais sans for vaudois
	 * @param dateTraitement
	 * @param nbThreads
	 * @param statusManager
	 * @return
	 */
	ListeContribuablesResidentsSansForVaudoisResults produireListeContribuablesSuissesOuPermisCResidentsMaisSansForVd(RegDate dateTraitement, int nbThreads, StatusManager statusManager);
}
