package ch.vd.unireg.evenement.civil.interne.annulationpermis;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.engine.ech.EvenementCivilEchTranslationStrategy;
import ch.vd.unireg.evenement.civil.engine.regpp.EvenementCivilTranslationStrategy;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;

/**
 * Règles métiers permettant de traiter les événements suivants :
 * - annulation de permis
 * - annulation de la nationalité
 *
 * @author Pavel BLANCO
 */
public abstract class AnnulationPermisOuNationaliteTranslationStrategy implements EvenementCivilTranslationStrategy, EvenementCivilEchTranslationStrategy {

	/**
	 * Récupère l'état de l'individu <b>juste avant</b> le traitement de l'événement eCh spécifié.
	 * <p/>
	 * <b>Attention !</b> Cette méthode ne fonctionne qu'avec le service civil RcPers.
	 *
	 * @param event   événement civil d'annulation/correction
	 * @param context contexte de traitement de l'événement
	 * @return L'individu construit d'après les informations retournées par l'événement référencé par l'événement donné
	 * @throws EvenementCivilException en cas de souci grave (pas d'événement reférence trouvé, pas d'individu...)
	 */
	@NotNull
	protected static Individu getIndividuAvant(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
		if (event.getRefMessageId() == null) {
			throw new IllegalArgumentException("Evénement sans événement de référence");
		}
		return getIndividuAfterEvent(event.getRefMessageId(), context);
	}

	/**
	 * @param eventId identifiant de l'événement civil
	 * @param context contexte de traitement de l'événement
	 * @return L'individu construit d'après les informations retournées par l'événement donné
	 * @throws EvenementCivilException en cas de souci grave (pas d'événement trouvé, pas d'individu...)
	 */
	@NotNull
	protected static Individu getIndividuAfterEvent(long eventId, EvenementCivilContext context) throws EvenementCivilException {
		final IndividuApresEvenement data = context.getServiceCivil().getIndividuAfterEvent(eventId);
		if (data == null) {
			throw new EvenementCivilException(String.format("Pas de données fournies l'événement civil %d", eventId));
		}
		final Individu individu = data.getIndividu();
		if (individu == null) {
			throw new EvenementCivilException(String.format("Aucune donnée d'individu fournie avec l'événement civil %d", eventId));
		}
		return individu;
	}
	
}
