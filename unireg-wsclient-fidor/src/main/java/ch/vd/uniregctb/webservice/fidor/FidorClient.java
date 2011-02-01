package ch.vd.uniregctb.webservice.fidor;

import java.util.Collection;

import ch.vd.fidor.ws.v2.Acces;
import ch.vd.fidor.ws.v2.Logiciel;
import ch.vd.fidor.ws.v2.ParameterMap;
import ch.vd.fidor.ws.v2.Pays;

public interface FidorClient {
	/**Retourne les informations d'un logiciel
	 *
	 * @param logicielId l'identifiant du logiciel
	 * @return  les details du logiciel
	 */
	public Logiciel getLogicielDetail(long logicielId);

	/**Retourne les information d'un pays
	 *
	 * @param ofsId identifiant d'un pays
	 * @return
	 */
	public Pays getPaysDetail(long ofsId);


	/** Retourne la liste de tous les logiciels
	 *
	 * @return collection contenant tous les logiciels
	 */
	public Collection<Logiciel> getTousLesLogiciels();


	/**Retourne la liste de tous les pays
	 *
	 *
	 * @return collection contenant la liste des pays
	 */
	public Collection<Pays> getTousLesPays();

	/**retourne l'url d'accès pour une application et une cible passées en paramètre
	 *
	 * @param app
	 * @param acces
	 * @param targetType
	 * @param map
	 * @return l'url
	 */
	public String getUrl(String app, Acces acces, String targetType, ParameterMap map);
}
