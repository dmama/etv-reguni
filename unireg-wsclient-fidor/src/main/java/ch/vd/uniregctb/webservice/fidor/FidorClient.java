package ch.vd.uniregctb.webservice.fidor;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collection;
import java.util.List;

import ch.vd.fidor.ws.v2.Acces;
import ch.vd.fidor.ws.v2.CommuneFiscale;
import ch.vd.fidor.ws.v2.FidorBusinessException_Exception;
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
	CommuneFiscale getCommuneParNoOFS(int ofsId, XMLGregorianCalendar date) throws FidorBusinessException_Exception;

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
	 * Retourne l'historique complet (notamment en cas de fusion, changement de nom, ..) d'une commune à partir de son numéro Ofs. Les communes retournées peuvent donc avoir des numéros Ofs différents de
	 * celui spécifié, dans la mesure où elles représentent d'anciennes communes fusionnées.
	 *
	 * @param ofsId un numéro Ofs de commune
	 * @return l'historique de la commune avec le numéro Ofs spécifié.
	 * @throws ch.vd.fidor.ws.v2.FidorBusinessException_Exception
	 *          en cas d'erreur métier
	 */
	List<CommuneFiscale> getCommunesHistoParNoOFS(int ofsId) throws FidorBusinessException_Exception;

	/**
	 * Retourne l'historique complet (notamment en cas de fusion, changement de nom, ..) d'une commune à partir de son numéro technique.
	 *
	 * @param noTechnique un numéro Ofs de commune
	 * @return l'historique de la commune avec le numéro Ofs spécifié.
	 * @throws ch.vd.fidor.ws.v2.FidorBusinessException_Exception
	 *          en cas d'erreur métier
	 */
	List<CommuneFiscale> getCommunesHistoParNoTechnique(int noTechnique) throws FidorBusinessException_Exception;

	/**
	 * @param date une date ou <b>null</b> pour obtenir l'historique complet.
	 * @return toutes les communes valides à la date demandée, ou l'historique complet des communes si la date est nulle.
	 * @throws ch.vd.fidor.ws.v2.FidorBusinessException_Exception
	 *          en cas d'erreur métier
	 */
	List<CommuneFiscale> getCommunes(XMLGregorianCalendar date) throws FidorBusinessException_Exception;

	/**
	 * @param ofsCommune le numéro Ofs de la commune d'annonce qui a associé la bâtiment avec une adresse.
	 * @param egid       le numéro de bâtiment
	 * @param date       une date de référence
	 * @return la commune sur laquelle est sis le bâtiment identifié par son numéro Ofs.
	 */
	CommuneFiscale getCommuneParBatiment(int ofsCommune, int egid, XMLGregorianCalendar date);

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
