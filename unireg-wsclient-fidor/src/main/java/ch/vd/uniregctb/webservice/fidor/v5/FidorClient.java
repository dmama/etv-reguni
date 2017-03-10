package ch.vd.uniregctb.webservice.fidor.v5;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0007.v1.Country;
import ch.vd.evd0007.v1.ExtendedCanton;
import ch.vd.evd0012.v1.CommuneFiscale;
import ch.vd.evd0012.v1.DistrictFiscal;
import ch.vd.evd0012.v1.Logiciel;
import ch.vd.evd0012.v1.RegionFiscale;
import ch.vd.fidor.xml.categorieentreprise.v1.CategorieEntreprise;
import ch.vd.fidor.xml.impotspecial.v1.ImpotSpecial;
import ch.vd.fidor.xml.post.v1.PostalLocality;
import ch.vd.fidor.xml.post.v1.Street;
import ch.vd.fidor.xml.regimefiscal.v2.RegimeFiscal;
import ch.vd.registre.base.date.RegDate;

@SuppressWarnings({"UnusedDeclaration"})
public interface FidorClient {

	/**
	 * Simple appel vers FiDoR pour vérifier qu'il est bien là
	 * @throws ch.vd.uniregctb.webservice.fidor.v5.FidorClientException si ce n'est pas le cas
	 */
	void ping();

	/**
	 * Retourne une commune à partir de son numéro Ofs et d'une date de référence.
	 *
	 * @param ofsId un numéro Ofs de commune
	 * @param date  une date de référence (si <code>null</code>, la date du jour sera utilisée)
	 * @return la commune avec le numéro Ofs demandé valide à la date spécifiée; ou <b>null</b> si la commune est inconnue.
	 */
	CommuneFiscale getCommuneParNoOFS(int ofsId, RegDate date);

	/**
	 * Retourne toutes les communes qui possèdent le numéro Ofs spécifié. Dans l'immense majorité des cas, cette méthode ne retourne qu'une seule commune. Seuls quelques cas retourneront deux communes
	 * (lorsque l'OFS s'est trompé et a réattribué un numéro Ofs à une autre commune).
	 *
	 * @param ofsId un numéro Ofs de commune
	 * @return les communes avec le numéro Ofs spécifié.
	 */
	List<CommuneFiscale> getCommunesParNoOFS(int ofsId);

	/**
	 * Retourne toutes les communes d'un canton.
	 *
	 * @param ofsId le numéro Ofs d'un canton
	 * @param date  une date de référence (optionnelle)
	 * @return les communes du canton spécifié
	 */
	List<CommuneFiscale> getCommunesParCanton(int ofsId, RegDate date);

	/**
	 * @return toutes les communes existantes ou ayant existé.
	 */
	List<CommuneFiscale> getToutesLesCommunes();

	/**
	 * Recherche une commune par nom officiel.
	 *
	 * @param nomOfficiel le nom officiel de la commune
	 * @param date        la date de valeur de la commune (null = tout l'historique)
	 * @return la commune trouvée ou <b>null</b> si aucune commune n'a été trouvée.
	 */
	@NotNull
	List<CommuneFiscale> findCommuneByNomOfficiel(@NotNull String nomOfficiel, @Nullable RegDate date);

	/**
	 * @return tous les cantons suisses
	 */
	List<ExtendedCanton> getTousLesCantons();

	/**
	 * @param egid le numéro de bâtiment
	 * @param date une date de référence (si <code>null</code>, la date du jour sera utilisée)
	 * @return la commune sur laquelle est sis le bâtiment identifié par son numéro Ofs.
	 */
	CommuneFiscale getCommuneParBatiment(int egid, RegDate date);

	/**
	 * @param ofsId identifiant d'un pays
	 * @param date une date de référence (si <code>null</code>, la date du jour sera utilisée)
	 * @return les informations d'un pays
	 */
	Country getPaysDetail(long ofsId, RegDate date);

	/**
	 * @param iso2Id l'identifiant ISO sur deux positions d'un pays (e.g. 'ch')
	 * @param date une date de référence (si <code>null</code>, la date du jour sera utilisée)
	 * @return les informations d'un pays
	 */
	Country getPaysDetail(String iso2Id, RegDate date);

	/**
	 * @param ofsId identifiant d'un pays
	 * @return les différentes versions du pays associé à l'identifiant OFS
	 */
	List<Country> getPaysHisto(long ofsId);

	/**
	 * Retourne la liste de tous les pays
	 *
	 * @return collection contenant la liste des pays
	 */
	List<Country> getTousLesPays();

	/**
	 * Retourne les informations d'un logiciel
	 *
	 * @param logicielId l'identifiant du logiciel
	 * @return les details du logiciel
	 */
	Logiciel getLogicielDetail(long logicielId);

	/**
	 * Retourne la liste de tous les logiciels
	 *
	 * @return collection contenant tous les logiciels
	 */
	List<Logiciel> getTousLesLogiciels();

	/**
	 * @param code un code de district
	 * @return le district correspondant au code ou <b>null</b> si aucun district ne correspond
	 */
	DistrictFiscal getDistrict(int code);

	/**
	 * @param code un code de région
	 * @return la région correspondant au code ou <b>null</b> si aucune région ne correspond
	 */
	RegionFiscale getRegion(int code);

	/**
	 * retourne l'url d'accès pour une application et une cible passées en paramètre
	 *
	 * @param app        le code de l'application
	 * @param acces      le type d'accès
	 * @param targetType le type de target
	 * @param map        les paramètres à intégrer à l'url
	 * @return l'url
	 */
	String getUrl(String app, String acces, String targetType, Map<String, String> map);

	/**
	 * @param dateReference date de référence (si absente, on prendra la date du jour)
	 * @param npa (optionnel) NPA des localités recherchées
	 * @param noOrdrePostal (optionnel) numéro d'ordre postal des localités recherchées
	 * @param nom (optionnel) nom des localités recherchées
	 * @param cantonOfsId (optionnel) le numéro OFS du canton cible
	 * @return la liste des localités postales valides à la date de référence qui correspondent aux critères donnés
	 */
	List<PostalLocality> getLocalitesPostales(RegDate dateReference, Integer npa, Integer noOrdrePostal, String nom, Integer cantonOfsId);

	/**
	 * @param noOrdrePostal numéro d'ordre postal des localités recherchées
	 * @return la liste des localités postales ayant porté le numéro d'ordre postal donné au fil du temps
	 */
	List<PostalLocality> getLocalitesPostalesHisto(int noOrdrePostal);

	/**
	 * @param dateReference date de référence (si absente, on prendra la date du jour)
	 * @param noOrdrePostal numéro d'ordre postal de la localité désirée
	 * @return la localité postale trouvée, ou <code>null</code> si aucune localité ne correspond aux critères
	 */
	PostalLocality getLocalitePostale(RegDate dateReference, int noOrdrePostal);

	/**
	 * @param noOrdrePostal numéro d'ordre postal de la localité cible
	 * @param dateReference date de référence (si absente, on prendra la date du jour)
	 * @return la liste des rues de la localité indiquée à la date de référence
	 */
	List<Street> getRuesParNumeroOrdrePosteEtDate(int noOrdrePostal, RegDate dateReference);

	/**
	 * @param estrid identifiant fédéral de la rue recherchée
	 * @param dateReference date de référence (si absente, on prendra ira chercher l'historique de la rue en question)
	 * @return la liste des rues identifiées par les critères donnés
	 */
	List<Street> getRuesParEstrid(int estrid, RegDate dateReference);

	/**
	 * @param code le code d'un régime fiscal
	 * @return le régime fiscal correspondant (ou <code>null</code> si aucun régime fiscal ne correspond à ce code)
	 */
	RegimeFiscal getRegimeFiscalParCode(String code);

	/**
	 * @return la liste de tous les régimes fiscaux connus de FiDoR
	 */
	List<RegimeFiscal> getRegimesFiscaux();

	/**
	 * @param code le code d'une catégorie d'entreprise
	 * @return la catégorie d'entreprise correspondant (ou <code>null</code> si aucune ne correspond à ce code)
	 */
	CategorieEntreprise getCategorieEntrepriseParCode(String code);

	/**
	 * @return la liste de toutes les catégories d'entreprise connues de FiDoR
	 */
	List<CategorieEntreprise> getCategoriesEntreprise();

	/**
	 * @return la liste des types d'impôts spéciaux connus de FiDoR
	 */
	List<ImpotSpecial> getImpotsSpeciaux();
}
