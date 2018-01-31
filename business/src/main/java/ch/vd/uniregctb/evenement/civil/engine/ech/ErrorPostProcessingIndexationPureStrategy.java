package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.type.EtatEvenementCivil;

public class ErrorPostProcessingIndexationPureStrategy implements ErrorPostProcessingStrategy<List<EvenementCivilEchBasicInfo>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ErrorPostProcessingIndexationPureStrategy.class);

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
	                                                       Mutable<List<EvenementCivilEchBasicInfo>> customData) {
		final List<EvenementCivilEchBasicInfo> traites = new ArrayList<>(remainingEvents.size());
		final List<EvenementCivilEchBasicInfo> nonTraites = new ArrayList<>(remainingEvents.size());
		for (EvenementCivilEchBasicInfo info : remainingEvents) {
			if (info.getEtat() == EtatEvenementCivil.A_TRAITER) {
				final EvenementCivilEch evt = evtCivilDAO.get(info.getId());
				if (!evt.getEtat().isTraite() && translator.isIndexationOnly(evt)) {
					traites.add(info);
				}
				else {
					nonTraites.add(info);
				}
			}
			else {
				nonTraites.add(info);
			}
		}

		customData.setValue(traites);
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
				LOGGER.error(String.format("Erreur lors du traitement de l'événement civil %d", pourIndexation.get(pointer).getId()), e);
			}
		}
	}
}
