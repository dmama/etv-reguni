package ch.vd.unireg.identification.contribuable;

import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.cxf.common.i18n.Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;

/**
 * Processor qui rapproche les contribuables des propriétaires fonciers
 *
 * @author baba
 */
public class IdentifierContribuableFromListeProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdentifierContribuableFromListeProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final IdentificationContribuableService identService;
	private final AdresseService adresseService;
	private final TiersService tiersService;
	private final TiersDAO tiersDAO;


	public IdentifierContribuableFromListeProcessor(IdentificationContribuableService identificationContribuableService,PlatformTransactionManager transactionManager,
	                                                TiersService tiersService, AdresseService adresseService,
	                                                 TiersDAO tiersDAO ) {
		this.transactionManager = transactionManager;
		this.identService = identificationContribuableService;
		this.adresseService = adresseService;
		this.tiersService = tiersService;
		this.tiersDAO = tiersDAO;
	}

	public IdentifierContribuableFromListeResults run(List<CriteresPersonne> listeCriteresPersonnes, StatusManager s, final RegDate dateTraitement, int nbThreads) {
		final StatusManager status = (s != null ? s : new LoggingStatusManager(LOGGER));

		status.setMessage("Début de l'identification ...");

		final IdentifierContribuableFromListeResults rapportFinal = new IdentifierContribuableFromListeResults(dateTraitement, tiersService, adresseService);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<CriteresPersonne, IdentifierContribuableFromListeResults> template =
				new ParallelBatchTransactionTemplateWithResults<>(listeCriteresPersonnes, BATCH_SIZE,
																 nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status,
																 AuthenticationInterface.INSTANCE);
		template.setReadonly(true);
		template.execute(rapportFinal, new BatchWithResultsCallback<CriteresPersonne, IdentifierContribuableFromListeResults>() {

			@Override
			public IdentifierContribuableFromListeResults createSubRapport() {
				return new IdentifierContribuableFromListeResults(dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<CriteresPersonne> batch, IdentifierContribuableFromListeResults r) throws Exception {
				status.setMessage("Traitement des lignes à identifier ...", progressMonitor.getProgressInPercent());
				traiterBatch(batch, r);
				return !status.isInterrupted();
			}
		}, progressMonitor);

		rapportFinal.interrompu = status.isInterrupted();
		rapportFinal.end();
		return rapportFinal;
	}

	private void traiterBatch(List<CriteresPersonne> batch, IdentifierContribuableFromListeResults rapport) {

		// maintenant, le boulot d'identification proprement dit...
		for (CriteresPersonne criteresPersonne : batch) {

			if (criteresPersonne != null) {
				final Mutable<String> avsUpi = new MutableObject<>();
				List<Long> found;
				IdentificationContribuableServiceImpl.IdentificationResultKind resultKind;
				try {
					found = identService.identifiePersonnePhysique(criteresPersonne, avsUpi);
					switch (found.size()) {
					case 0:
						resultKind = IdentificationContribuableServiceImpl.IdentificationResultKind.FOUND_NONE;
						break;
					case 1:
						resultKind = IdentificationContribuableServiceImpl.IdentificationResultKind.FOUND_ONE;
						break;
					default:
						resultKind = IdentificationContribuableServiceImpl.IdentificationResultKind.FOUND_SEVERAL;
						break;
					}
				}
				catch (TooManyIdentificationPossibilitiesException e) {
					found = e.getExamplesFound();
					resultKind = IdentificationContribuableServiceImpl.IdentificationResultKind.FOUND_MANY;
				}

				// un résultat trouvé -> on a réussi !
				if (resultKind == IdentificationContribuableServiceImpl.IdentificationResultKind.FOUND_ONE) {
					// on a trouvé un et un seul contribuable:
					final Long numeroCtbTrouve = found.get(0);
					final PersonnePhysique personne = (PersonnePhysique) tiersDAO.get(numeroCtbTrouve);
					Long noContribuable = null;
					Long noMenageCommun = null;
					noContribuable = numeroCtbTrouve;
					final EnsembleTiersCouple ensembleTiersCouple =personne==null?null: tiersService.getEnsembleTiersCouple(personne, RegDate.get());
					if (ensembleTiersCouple != null) {
						MenageCommun mc = ensembleTiersCouple.getMenage();
						noMenageCommun = mc.getNumero();
					}

					rapport.addIdentifies(criteresPersonne, noContribuable, noMenageCommun);
				}
				else if (resultKind == IdentificationContribuableServiceImpl.IdentificationResultKind.FOUND_MANY ||
						resultKind == IdentificationContribuableServiceImpl.IdentificationResultKind.FOUND_SEVERAL) {
					rapport.addPlusieursTrouves(criteresPersonne, found);
				}
				else if (resultKind == IdentificationContribuableServiceImpl.IdentificationResultKind.FOUND_NONE) {
					rapport.addNonIdentifies(criteresPersonne);
				}
				else {
					rapport.addErrorException(criteresPersonne, new RuntimeException("Erreur inattendue"));
				}
			}

		}
	}



}
