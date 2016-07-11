package ch.vd.uniregctb.evenement.organisation.engine;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationBasicInfo;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationDAO;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;

public class ErrorPostProcessingMiseEnAttenteStrategy implements ErrorPostProcessingStrategy<Object> {

	private final EvenementOrganisationDAO evtOrganisationDAO;

	public ErrorPostProcessingMiseEnAttenteStrategy(EvenementOrganisationDAO dao) {
		evtOrganisationDAO = dao;
	}

	@Override
	public boolean needsTransactionOnCollectPhase() {
		return true;
	}

	@NotNull
	@Override
	public List<EvenementOrganisationBasicInfo> doCollectPhase(List<EvenementOrganisationBasicInfo> remainingEvents, Mutable<Object> customData) {
		final List<EvenementOrganisationBasicInfo> remaining = new ArrayList<>(remainingEvents.size());
		for (EvenementOrganisationBasicInfo info : remainingEvents) {
			final EvenementOrganisation evt = evtOrganisationDAO.get(info.getId());
			if (evt.getEtat() == EtatEvenementOrganisation.A_TRAITER) {
				setEnAttente(evt);
			}
			else {
				remaining.add(info);
			}
		}
		return remaining;
	}

	private static void setEnAttente(EvenementOrganisation evt) {
		evt.setEtat(EtatEvenementOrganisation.EN_ATTENTE);
		Audit.info(evt.getNoEvenement(), String.format("Mise en attente de %s", evt.toString()));
	}

	@Override
	public boolean needsTransactionOnFinalizePhase() {
		return false;
	}

	@Override
	public void doFinalizePhase(Object customData) {
		// tout a déjà été fait dans la phase de collecte
	}
}
