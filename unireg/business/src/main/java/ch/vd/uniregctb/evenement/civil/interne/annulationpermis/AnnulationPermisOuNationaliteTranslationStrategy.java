package ch.vd.uniregctb.evenement.civil.interne.annulationpermis;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchContext;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilEchTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.engine.regpp.EvenementCivilTranslationStrategy;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.rcpers.IndividuApresEvenement;

/**
 * Règles métiers permettant de traiter les événements suivants :
 * - annulation de permis
 * - annulation de la nationalité
 *
 * @author Pavel BLANCO
 */
public abstract class AnnulationPermisOuNationaliteTranslationStrategy implements EvenementCivilTranslationStrategy, EvenementCivilEchTranslationStrategy {

	/**
	 * @param event événement civil d'annulation/correction
	 * @param context contexte de traitement de l'événement
	 * @return L'individu construit d'après les informations retournées par l'événement référencé par l'événement donné
	 * @throws EvenementCivilException en cas de souci grave (pas d'événement reférence trouvé, pas d'individu...)
	 */
	@NotNull
	protected static Individu getIndividuAvant(EvenementCivilEch event, EvenementCivilEchContext context) throws EvenementCivilException {
		Assert.notNull(event.getRefMessageId(), "Evénement sans événement de référence");
		return getIndividuFromEvent(event.getRefMessageId(), context);
	}

	/**
	 * @param eventId identifiant de l'événement civil
	 * @param context contexte de traitement de l'événement
	 * @return L'individu construit d'après les informations retournées par l'événement donné
	 * @throws EvenementCivilException en cas de souci grave (pas d'événement trouvé, pas d'individu...)
	 */
	@NotNull
	protected static Individu getIndividuFromEvent(long eventId, EvenementCivilEchContext context) throws EvenementCivilException {
		final IndividuApresEvenement data = context.getRcPersClientHelper().getIndividuFromEvent(eventId);
		if (data == null) {
			throw new EvenementCivilException(String.format("Pas de données fournies pour l'individu de l'événement %d", eventId));
		}
		return data.getIndividu();
	}
	
}
