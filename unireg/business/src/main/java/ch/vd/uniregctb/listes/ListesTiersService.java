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
	 * @param dateTraitement
	 * @param nbThreads
	 * @param adressesIncluses le type d'adresse à inclure
	 * @param avecContribuables
	 * @param avecDebiteurs
	 * @param statusManager
	 * @return les données pour la liste globale
	 */
	ListesNominativesResults produireListesNominatives(RegDate dateTraitement, int nbThreads, TypeAdresse adressesIncluses, boolean avecContribuables, boolean avecDebiteurs,
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
