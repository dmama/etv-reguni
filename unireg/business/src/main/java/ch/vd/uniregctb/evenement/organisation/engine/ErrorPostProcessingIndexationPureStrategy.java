package ch.vd.uniregctb.evenement.organisation.engine;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationBasicInfo;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationDAO;
import ch.vd.uniregctb.evenement.organisation.engine.processor.EvenementOrganisationProcessorInternal;
import ch.vd.uniregctb.evenement.organisation.engine.translator.EvenementOrganisationTranslator;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;

public class ErrorPostProcessingIndexationPureStrategy implements ErrorPostProcessingStrategy<List<EvenementOrganisationBasicInfo>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ErrorPostProcessingIndexationPureStrategy.class);

	private final EvenementOrganisationDAO evtOrganisationDAO;
	private final EvenementOrganisationTranslator translator;
	private final EvenementOrganisationProcessorInternal processor;

	public ErrorPostProcessingIndexationPureStrategy(EvenementOrganisationDAO evtOrganisationDAO, EvenementOrganisationTranslator translator, EvenementOrganisationProcessorInternal processor) {
		this.evtOrganisationDAO = evtOrganisationDAO;
		this.translator = translator;
		this.processor = processor;
	}

	@Override
	public boolean needsTransactionOnCollectPhase() {
		return true;
	}

	@NotNull
	@Override
	public List<EvenementOrganisationBasicInfo> doCollectPhase(List<EvenementOrganisationBasicInfo> remainingEvents,
	                                                       Mutable<List<EvenementOrganisationBasicInfo>> customData) {
		final List<EvenementOrganisationBasicInfo> traites = new ArrayList<>(remainingEvents.size());
		final List<EvenementOrganisationBasicInfo> nonTraites = new ArrayList<>(remainingEvents.size());
		for (EvenementOrganisationBasicInfo info : remainingEvents) {
			if (info.getEtat() == EtatEvenementOrganisation.A_TRAITER) {
				final EvenementOrganisation evt = evtOrganisationDAO.get(info.getId());
				if (!evt.getEtat().isTraite()) {// && translator.isIndexationOnly(evt)) { // FIXME: a réviser
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
	public void doFinalizePhase(List<EvenementOrganisationBasicInfo> pourIndexation) {
		if (pourIndexation.size() > 0) {
			int pointer = 0;
			try {
				LOGGER.info("Lancement du traitement des événements d'indexation pure restants");
				for (EvenementOrganisationBasicInfo info : pourIndexation) {
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
