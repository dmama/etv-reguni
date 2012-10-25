package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.DataHolder;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.type.EtatEvenementCivil;

public class ErrorPostProcessingMiseEnAttenteStrategy implements ErrorPostProcessingStrategy<Object> {

	private final EvenementCivilEchDAO evtCivilDAO;

	public ErrorPostProcessingMiseEnAttenteStrategy(EvenementCivilEchDAO dao) {
		evtCivilDAO = dao;
	}

	@Override
	public boolean needsTransactionOnCollectPhase() {
		return true;
	}

	@NotNull
	@Override
	public List<EvenementCivilEchBasicInfo> doCollectPhase(List<EvenementCivilEchBasicInfo> remainingEvents, DataHolder<Object> customData) {
		final List<EvenementCivilEchBasicInfo> remaining = new ArrayList<EvenementCivilEchBasicInfo>(remainingEvents.size());
		for (EvenementCivilEchBasicInfo info : remainingEvents) {
			if (info.getEtat() == EtatEvenementCivil.A_TRAITER) {
				final EvenementCivilEch evt = evtCivilDAO.get(info.getId());
				if (evt.getEtat() == EtatEvenementCivil.A_TRAITER) {        // re-test pour vérifier que l'information dans le descripteur est toujours à jour
					evt.setEtat(EtatEvenementCivil.EN_ATTENTE);
					Audit.info(evt.getId(), String.format("Mise en attente de l'événement %d", evt.getId()));
				}
				else {
					remaining.add(info);
				}
			}
		}
		return remaining;
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
