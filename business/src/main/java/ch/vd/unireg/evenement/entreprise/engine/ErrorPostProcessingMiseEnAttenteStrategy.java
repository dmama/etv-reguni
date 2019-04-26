package ch.vd.unireg.evenement.entreprise.engine;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseBasicInfo;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseDAO;
import ch.vd.unireg.type.EtatEvenementEntreprise;

public class ErrorPostProcessingMiseEnAttenteStrategy implements ErrorPostProcessingStrategy<Object> {

	private final EvenementEntrepriseDAO evtEntrepriseDAO;
	private final AuditManager audit;


	public ErrorPostProcessingMiseEnAttenteStrategy(EvenementEntrepriseDAO dao, AuditManager audit) {
		evtEntrepriseDAO = dao;
		this.audit = audit;
	}

	@Override
	public boolean needsTransactionOnCollectPhase() {
		return true;
	}

	@NotNull
	@Override
	public List<EvenementEntrepriseBasicInfo> doCollectPhase(List<EvenementEntrepriseBasicInfo> remainingEvents, Mutable<Object> customData) {
		final List<EvenementEntrepriseBasicInfo> remaining = new ArrayList<>(remainingEvents.size());
		for (EvenementEntrepriseBasicInfo info : remainingEvents) {
			final EvenementEntreprise evt = evtEntrepriseDAO.get(info.getId());
			if (evt.getEtat() == EtatEvenementEntreprise.A_TRAITER) {
				setEnAttente(evt);
			}
			else {
				remaining.add(info);
			}
		}
		return remaining;
	}

	private void setEnAttente(EvenementEntreprise evt) {
		evt.setEtat(EtatEvenementEntreprise.EN_ATTENTE);
		audit.info(evt.getNoEvenement(), String.format("Mise en attente de %s", evt.toString()));
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
