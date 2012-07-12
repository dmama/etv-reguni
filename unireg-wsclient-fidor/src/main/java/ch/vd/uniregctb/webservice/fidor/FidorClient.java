package ch.vd.uniregctb.webservice.fidor;

import java.util.Collection;
import java.util.List;

import ch.vd.fidor.ws.v2.Acces;
import ch.vd.fidor.ws.v2.CommuneFiscale;
import ch.vd.fidor.ws.v2.FidorBusinessException_Exception;
import ch.vd.fidor.ws.v2.FidorDate;
import ch.vd.fidor.ws.v2.FusionCommune;
import ch.vd.fidor.ws.v2.Logiciel;
import ch.vd.fidor.ws.v2.ParameterMap;
import ch.vd.fidor.ws.v2.Pays;

@SuppressWarnings({"UnusedDeclaration"})
public interface FidorClient {

	/**
	 * Retourne une commune à partir de son numéro Ofs et d'une date de référence.
	 *
	 * @param ofsId un numéro Ofs de commune
	 * @param date  une date de référence
	 * @return la commune avec le numéro Ofs demandé valide à la date spécifiée; ou <b>null</b> si la commune est inconnue.
	 * @throws ch.vd.fidor.ws.v2.FidorBusinessException_Exception
	 *          en cas d'erreur métier
	 */
	CommuneFiscale getCommuneParNoOFS(int ofsId, FidorDate date) throws FidorBusinessException_Exception;

	/**
	 * Retourne une commune à partir de son numéro technique.
	 *
	 * @param noTechnique le numéro technique (interne à l'ACV) de la commune
	 * @return la commune avec le numéro technique demandé; ou <b>null</b> si la commune est inconnue.
	 * @throws ch.vd.fidor.ws.v2.FidorBusinessException_Exception
	 *          en cas d'erreur métier
	 */
	CommuneFiscale getCommuneParNoTechnique(int noTechnique) throws FidorBusinessException_Exception;

	/**
	 * Retourne toutes les communes qui possèdent le numéro Ofs spécifié. Dans l'immense majorité des cas, cette méthode ne retourne qu'une seule commune. Seuls quelques cas retourneront deux communes
	 * (lorsque l'OFS s'est trompé et a réattribué un numéro Ofs à une autre commune).
	 *
	 * @param ofsId un numéro Ofs de commune
	 * @return les communes avec le numéro Ofs spécifié.
	 * @throws ch.vd.fidor.ws.v2.FidorBusinessException_Exception
	 *          en cas d'erreur métier
	 */
	List<CommuneFiscale> getCommunesParNoOFS(int ofsId) throws FidorBusinessException_Exception;

	/**
	 * Retourne la liste des communes participant à une fusion.
	 *
	 *
	 * @param ofsId      le numéro Ofs d'une commune participant à une fusion (en tant qu'ancienne commune ou nouvelle commune fusionnée)
	 * @param dateFusion la date précise de la fusion (= début de validité de la nouvelle commune résultant de la fusion)
	 * @return la liste des communes ayant participé à la fusion (incluant les anciennes communes et la nouvelle commune).
	 * @throws ch.vd.fidor.ws.v2.FidorBusinessException_Exception
	 *          en cas d'erreur métier
	 */
	FusionCommune getCommunesParFusion(int ofsId, FidorDate dateFusion) throws FidorBusinessException_Exception;

	/**
	 * @param date une date ou <b>null</b> pour obtenir l'historique complet.
	 * @return toutes les communes valides à la date demandée, ou l'historique complet des communes si la date est nulle.
	 * @throws ch.vd.fidor.ws.v2.FidorBusinessException_Exception
	 *          en cas d'erreur métier
	 */
	List<CommuneFiscale> getCommunesValides(FidorDate date) throws FidorBusinessException_Exception;

	/**
	 * @return toutes les communes existantes ou ayant existé.
	 * @throws ch.vd.fidor.ws.v2.FidorBusinessException_Exception
	 *          en cas d'erreur métier
	 */
	List<CommuneFiscale> getToutesLesCommunes() throws FidorBusinessException_Exception;

	/**
	 *
	 * @param egid       le numéro de bâtiment
	 * @param date       une date de référence
	 * @return la commune sur laquelle est sis le bâtiment identifié par son numéro Ofs.
	 */
	CommuneFiscale getCommuneParBatiment(int egid, FidorDate date);

	/**
	 * @param ofsId identifiant d'un pays
	 * @return les information d'un pays
	 */
	Pays getPaysDetail(long ofsId);

	/**
	 * Retourne la liste de tous les pays
	 *
	 * @return collection contenant la liste des pays
	 */
	Collection<Pays> getTousLesPays();

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
	Collection<Logiciel> getTousLesLogiciels();

	/**
	 * retourne l'url d'accès pour une application et une cible passées en paramètre
	 *
	 * @param app        le code de l'application
	 * @param acces      le type d'accès
	 * @param targetType le type de target
	 * @param map        les paramètres à intégrer à l'url
	 * @return l'url
	 */
	String getUrl(String app, Acces acces, String targetType, ParameterMap map);
}
