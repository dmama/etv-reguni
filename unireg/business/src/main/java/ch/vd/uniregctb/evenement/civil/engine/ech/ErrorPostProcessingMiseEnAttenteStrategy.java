package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.audit.Audit;
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
	public List<EvenementCivilEchBasicInfo> doCollectPhase(List<EvenementCivilEchBasicInfo> remainingEvents, Mutable<Object> customData) {
		final List<EvenementCivilEchBasicInfo> remaining = new ArrayList<>(remainingEvents.size());
		for (EvenementCivilEchBasicInfo info : remainingEvents) {
			if (info.getEtat() == EtatEvenementCivil.A_TRAITER) {
				final EvenementCivilEch evt = evtCivilDAO.get(info.getId());
				if (evt.getEtat() == EtatEvenementCivil.A_TRAITER) {        // re-test pour vérifier que l'information dans le descripteur est toujours à jour
					setEnAttente(evt, info.getNoIndividu());
				}
				else {
					remaining.add(info);
				}
			}

			// pour la complétude de la chose, on met également les événements <i>referrers</i> en attente s'ils sont encore "à traiter"
			for (EvenementCivilEchBasicInfo ref : info.getSortedReferrers()) {
				if (ref.getEtat() == EtatEvenementCivil.A_TRAITER) {
					final EvenementCivilEch evt = evtCivilDAO.get(ref.getId());
					if (evt.getEtat() == EtatEvenementCivil.A_TRAITER) {        // re-test pour vérifier que l'information dans le descripteur est toujours à jour
						setEnAttente(evt, ref.getNoIndividu());
					}
				}
			}
		}
		return remaining;
	}

	private static void setEnAttente(EvenementCivilEch evt, long noIndividu) {
		// [SIFISC-9031] Rattrapage du numéro d'individu si traitement mis en branle par le biais des relations entre événements
		if (evt.getNumeroIndividu() == null) {
			evt.setNumeroIndividu(noIndividu);
		}
		evt.setEtat(EtatEvenementCivil.EN_ATTENTE);
		Audit.info(evt.getId(), String.format("Mise en attente de l'événement %d", evt.getId()));
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
