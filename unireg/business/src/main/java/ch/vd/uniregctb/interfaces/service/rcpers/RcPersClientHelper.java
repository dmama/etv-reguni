package ch.vd.uniregctb.interfaces.service.rcpers;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;

/**
 * TODO (msi) supprimer ce bean et utiliser le service civil à la place
 */
public interface RcPersClientHelper {

	/**
	 * Renvoie un individu correspondant à l'événement donné
	 * <p/>
	 * <b>Attention : </b> les relations ne sont pas renseignées sur cet individu
	 * @param eventId identifiant de l'événement
	 * @return l'individu correspondant à l'état juste après le traitement civil de l'événement, ou <code>null</code> si l'id ne correspond à rien
	 */
	IndividuApresEvenement getIndividuFromEvent(long eventId);
}
