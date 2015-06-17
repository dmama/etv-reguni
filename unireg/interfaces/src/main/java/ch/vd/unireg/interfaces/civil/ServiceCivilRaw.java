package ch.vd.unireg.interfaces.civil;

import java.util.Collection;
import java.util.List;

import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;

public interface ServiceCivilRaw {

	String SERVICE_NAME = "ServiceCivil";

	/**
	 * Retourne l'individu identifié par le numéro en paramètre avec son historique complet.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués ainsi que les attributs muti-valués suivants :
	 * <ul>
	 * <li>La liste des historiques de l'individu.</li>
	 * <li>La liste des états civils de l'individu.</li>
	 * <li>La liste des conjoints l'individu.</li>
	 * </ul>
	 * <p/>
	 * L'objet retourné par ce service peut être <code>null</code>, signifiant l'absence de données d'un point de vue métier pour les paramêtres donnés.
	 *
	 * @param noIndividu le numéro technique de l'individu.
	 * @param parties    les parties optionnelles devant être renseignées
	 * @return l'individu populé avec les données valides jusqu'à l'année spécifiée; ou <b>null</b> si l'individu n'existe pas.
	 * @throws ServiceCivilException en cas d'erreur lors de la récupération de l'individu.
	 */
	Individu getIndividu(long noIndividu, AttributeIndividu... parties) throws ServiceCivilException;

	/**
	 * Retourne l'individu concerné par l'événement civil dont l'identifiant est donné en paramètre avec son historique complet.
	 * <p/>
	 * Ce service renseigne, pour chaque objet du graphe retourné, l'ensemble des attributs mono-valués ainsi que les attributs muti-valués suivants :
	 * <ul>
	 * <li>La liste des historiques de l'individu.</li>
	 * <li>La liste des états civils de l'individu.</li>
	 * <li>La liste des conjoints l'individu.</li>
	 * </ul>
	 * <p/>
	 * L'objet retourné par ce service peut être <code>null</code>, signifiant l'absence de données d'un point de vue métier pour les paramêtres donnés.
	 *
	 * @param evtId le numéro technique de l'individu.
	 * @param parties    les parties optionnelles devant être renseignées
	 * @return l'individu populé avec les données valides jusqu'à l'année spécifiée; ou <b>null</b> si l'individu n'existe pas.
	 * @throws ServiceCivilException en cas d'erreur lors de la récupération de l'individu.
	 */
	Individu getIndividuByEvent(long evtId, AttributeIndividu... parties) throws ServiceCivilException;

	/**
	 * Retourne un lot d'individu avec les parties spécifiées.
	 * <p/>
	 * <b>Attention !</b> L'ordre des individus retourné ne correspond pas forcément à celui des numéros d'individu spécifiés.
	 *
	 * @param nosIndividus les numéros d'individus demandés
	 * @param parties      les parties optionnelles devant être renseignées
	 * @return la liste des individus trouvés, ou <b>null</b> si le service n'est pas capable de charger les individus par lots.
	 * @throws ServiceCivilException en cas d'erreur lors de la récupération d'un ou plusieurs individus.
	 */
	List<Individu> getIndividus(Collection<Long> nosIndividus, AttributeIndividu... parties) throws ServiceCivilException;

	/**
	 * Renvoie un individu correspondant à l'événement donné
	 * <p/>
	 * <b>Attention : </b> les relations ne sont pas renseignées sur cet individu
	 * @param eventId identifiant de l'événement
	 * @return l'individu correspondant à l'état juste après le traitement civil de l'événement, ou <code>null</code> si l'id ne correspond à rien
	 */
	IndividuApresEvenement getIndividuAfterEvent(long eventId);

	/**
	 * Méthode qui permet de tester que le service civil répond bien. Cette méthode est insensible aux caches.
	 *
	 * @throws ServiceCivilException en cas de non-fonctionnement du service civil
	 */
	void ping() throws ServiceCivilException;

	/**
	 * @return <b>vrai</b> si l'implémentation courante du service civil possède un cache et que ce cache est susceptible d'être chauffé avec un appel à getIndividus().
	 */
	boolean isWarmable();
}
