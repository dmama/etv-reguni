package ch.vd.unireg.wsclient.rcpers;

import java.util.Collection;

import ch.vd.evd0001.v3.ListOfFoundPersons;
import ch.vd.evd0001.v3.ListOfPersons;
import ch.vd.evd0001.v3.ListOfRelations;
import ch.vd.evd0006.v1.Event;
import ch.vd.registre.base.date.RegDate;

@SuppressWarnings("UnusedDeclaration")
public interface RcPersClient {

	/**
	 * Récupère une ou plusieurs <i>personnes</i> par leurs ids (c'est-à-dire des individus, dans l'ancienne terminologie) dans le registre cantonal des personnes (RCPers).
	 *
	 * @param ids         les ids des personnes à retourner
	 * @param date        une date de validité (peut être nulle)
	 * @param withHistory <b>vrai</b> si les collections historisées doivent être renseignée; <b>faux</b> autrement.
	 * @return une liste de personnes
	 */
	ListOfPersons getPersons(Collection<Long> ids, RegDate date, boolean withHistory);

	/**
	 * Récupère une ou plusieurs <i>personnes</i> par leurs numéros AVS 13 (c'est-à-dire des individus, dans l'ancienne terminologie) dans le registre cantonal des personnes (RCPers).
	 *
	 * @param numbers     les numéros AVS 13 des personnes à retourner
	 * @param date        une date de validité (peut être nulle)
	 * @param withHistory <b>vrai</b> si les collections historisées doivent être renseignée; <b>faux</b> autrement.
	 * @return une liste de personnes
	 */
	ListOfPersons getPersonsBySocialsNumbers(Collection<String> numbers, RegDate date, boolean withHistory);

	/**
	 * Récupère les relations vers d'autres personnes (parents, enfants, conjoints, ...) d'une ou plusieurs <i>personnes</i>.
	 *
	 * @param ids         les ids des personnes à retourner
	 * @param date        une date de validité (peut être nulle)
	 * @param withHistory <b>vrai</b> si les collections historisées doivent être renseignée; <b>faux</b> autrement.
	 * @return une liste de relations entre personnes
	 */
	ListOfRelations getRelations(Collection<Long> ids, RegDate date, boolean withHistory);

	/**
	 * Récupère l'événement civil (+ l'état de la personne juste après l'événement) dont l'identifiant est donné
	 *
	 * @param eventId l'identifiant de l'événement civil
	 * @return l'événement civil demandé
	 */
	Event getEvent(long eventId);

	/**
	 * Recherche de personne. Voir la spécification https://portail.etat-de-vaud.ch/outils/dsiwiki/download/attachments/103940131/TEC-ServicesEchangesDonnees-3-0.doc.
	 *
	 * @param sex                     Sexe
	 * @param firstNames              Prénoms
	 * @param officialName            Nom officiel
	 * @param swissZipCode            Localité postale
	 * @param municipalityId          Commune de résidence
	 * @param dataSource              Option de choix de la source des données d’identification
	 * @param contains                Option de recherche « contient »
	 * @param history                 Option de recherche dans l’historique
	 * @param originalName            Nom de naissance
	 * @param alliancePartnershipName Nom d’alliance
	 * @param aliasName               Nom d’alias
	 * @param nationalityStatus       Statut de nationalité
	 * @param nationalityCountryId    Pays de nationalité
	 * @param town                    Désignation de la localité
	 * @param passportName            Nom du passeport
	 * @param otherNames              Autres noms
	 * @param birthDateFrom           Date de naissance (limite inférieure ou date exacte si birthDateTo n'est pas spécifié)
	 * @param birthDateTo             Date de naissance (limite supérieur)
	 * @return les personnes trouvées
	 */
	ListOfFoundPersons findPersons(String sex, String firstNames, String officialName, String swissZipCode, String municipalityId, String dataSource, String contains, Boolean history,
	                               String originalName, String alliancePartnershipName, String aliasName, Integer nationalityStatus, Integer nationalityCountryId, String town,
	                               String passportName, String otherNames, RegDate birthDateFrom, RegDate birthDateTo);
}
