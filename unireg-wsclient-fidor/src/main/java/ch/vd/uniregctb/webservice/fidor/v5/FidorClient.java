package ch.vd.uniregctb.webservice.fidor.v5;

import java.util.List;
import java.util.Map;

import ch.vd.evd0007.v1.Country;
import ch.vd.evd0012.v1.CommuneFiscale;
import ch.vd.evd0012.v1.DistrictFiscal;
import ch.vd.evd0012.v1.Logiciel;
import ch.vd.evd0012.v1.RegionFiscale;
import ch.vd.registre.base.date.RegDate;

@SuppressWarnings({"UnusedDeclaration"})
public interface FidorClient {

	/**
	 * Retourne une commune à partir de son numéro Ofs et d'une date de référence.
	 *
	 * @param ofsId un numéro Ofs de commune
	 * @param date  une date de référence
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
	 * @param date une date ou <b>null</b> pour obtenir l'historique complet.
	 * @return toutes les communes valides à la date demandée, ou l'historique complet des communes si la date est nulle.
	 */
//	List<CommuneFiscale> getCommunesValides(RegDate date);

	/**
	 * @return toutes les communes existantes ou ayant existé.
	 */
	List<CommuneFiscale> getToutesLesCommunes();

	/**
	 * @param egid le numéro de bâtiment
	 * @param date une date de référence
	 * @return la commune sur laquelle est sis le bâtiment identifié par son numéro Ofs.
	 */
	CommuneFiscale getCommuneParBatiment(int egid, RegDate date);

	/**
	 * @param ofsId identifiant d'un pays
	 * @param date une date de référence
	 * @return les informations d'un pays
	 */
	Country getPaysDetail(long ofsId, RegDate date);

	/**
	 * @param iso2Id l'identifiant ISO sur deux positions d'un pays (e.g. 'ch')
	 * @param date une date de référence
	 * @return les informations d'un pays
	 */
	Country getPaysDetail(String iso2Id, RegDate date);

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
}
