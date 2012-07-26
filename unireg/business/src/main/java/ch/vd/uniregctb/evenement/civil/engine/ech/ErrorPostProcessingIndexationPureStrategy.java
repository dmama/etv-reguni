package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.type.EtatEvenementCivil;

public class ErrorPostProcessingIndexationPureStrategy implements ErrorPostProcessingStrategy<List<EvenementCivilEchBasicInfo>> {

	private static final Logger LOGGER = Logger.getLogger(ErrorPostProcessingIndexationPureStrategy.class);

	private final EvenementCivilEchDAO evtCivilDAO;
	private final EvenementCivilEchTranslator translator;
	private final EvenementCivilEchInternalProcessor processor;

	public ErrorPostProcessingIndexationPureStrategy(EvenementCivilEchDAO evtCivilDAO, EvenementCivilEchTranslator translator, EvenementCivilEchInternalProcessor processor) {
		this.evtCivilDAO = evtCivilDAO;
		this.translator = translator;
		this.processor = processor;
	}

	@Override
	public boolean needsTransactionOnCollectPhase() {
		return true;
	}

	@NotNull
	@Override
	public List<EvenementCivilEchBasicInfo> doCollectPhase(List<EvenementCivilEchBasicInfo> remainingEvents,
	                                                       CustomDataHolder<List<EvenementCivilEchBasicInfo>> customData) {
		final List<EvenementCivilEchBasicInfo> traites = new ArrayList<EvenementCivilEchBasicInfo>(remainingEvents.size());
		final List<EvenementCivilEchBasicInfo> nonTraites = new ArrayList<EvenementCivilEchBasicInfo>(remainingEvents.size());
		for (EvenementCivilEchBasicInfo info : remainingEvents) {
			if (info.getEtat() == EtatEvenementCivil.A_TRAITER) {
				final EvenementCivilEch evt = evtCivilDAO.get(info.getId());
				if (evt.getEtat() == EtatEvenementCivil.A_TRAITER && translator.isIndexationOnly(evt)) {
					traites.add(info);
				}
				else {
					nonTraites.add(info);
				}
			}
		}

		customData.member = traites;
		return nonTraites;
	}

	@Override
	public boolean needsTransactionOnFinalizePhase() {
		return false;
	}

	@Override
	public void doFinalizePhase(List<EvenementCivilEchBasicInfo> pourIndexation) {
		if (pourIndexation.size() > 0) {
			int pointer = 0;
			try {
				LOGGER.info("Lancement du traitement des événements d'indexation pure restants");
				for (EvenementCivilEchBasicInfo info : pourIndexation) {
					if (!processor.processEventAndDoPostProcessingOnError(info, pourIndexation, pointer)) {
						// si on reviens avec <code>false</code>, c'est qu'on a essayé de re-traiter les suivants aussi
						break;
					}
					++ pointer;
				}
			}
			catch (Exception e) {
				LOGGER.error(String.format("Erreur lors du traitement de l'événements civil %d", pourIndexation.get(pointer).getId()), e);
			}
		}
	}
}
