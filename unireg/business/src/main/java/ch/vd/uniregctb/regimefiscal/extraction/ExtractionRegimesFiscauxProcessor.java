package ch.vd.uniregctb.regimefiscal.extraction;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.hibernate.Query;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.FormeLegaleHisto;
import ch.vd.uniregctb.tiers.RaisonSocialeHisto;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.TiersService;

public class ExtractionRegimesFiscauxProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExtractionRegimesFiscauxProcessor.class);
	private static final int BATCH_SIZE = 20;

	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;
	private final ServiceInfrastructureService infraService;
	private final TiersService tiersService;

	public ExtractionRegimesFiscauxProcessor(HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager, ServiceInfrastructureService infraService, TiersService tiersService) {
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.infraService = infraService;
		this.tiersService = tiersService;
	}

	public ExtractionRegimesFiscauxResults run(boolean avecHistorique, int nbThreads, RegDate dateTraitement, StatusManager sm) {
		final StatusManager status = sm != null ? sm : new LoggingStatusManager(LOGGER);

		// un peu de suivi
		status.setMessage("Récupération des entreprises...");

		final ExtractionRegimesFiscauxResults resultatFinal = new ExtractionRegimesFiscauxResults(avecHistorique, nbThreads, dateTraitement);

		// allons chercher les entreprises
		final List<Long> idsEntreprises = getIdsEntreprises();

		// récupération des types de régimes fiscaux
		final Map<String, TypeRegimeFiscal> typesRegimes = infraService.getRegimesFiscaux().stream()
				.collect(Collectors.toMap(TypeRegimeFiscal::getCode, Function.identity()));

		// extracteur des régimes fiscaux à dumper
		final Predicate<RegimeFiscal> filtreRegimesFiscaux;
		if (avecHistorique) {
			// quand on demande l'historique, on veut tous les régimes fiscaux avant la date de traitement
			filtreRegimesFiscaux = rf -> RegDateHelper.isBeforeOrEqual(rf.getDateDebut(), dateTraitement, NullDateBehavior.EARLIEST);
		}
		else {
			// quand on ne demande pas l'historique, on veut seulement les régimes fiscaux valides à la date de traitement
			filtreRegimesFiscaux = rf -> rf.isValidAt(dateTraitement);
		}

		// et maintenant procédons à l'extraction
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, ExtractionRegimesFiscauxResults> template = new ParallelBatchTransactionTemplateWithResults<>(idsEntreprises,
		                                                                                                                                                      BATCH_SIZE,
		                                                                                                                                                      nbThreads,
		                                                                                                                                                      Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                                      transactionManager,
		                                                                                                                                                      status,
		                                                                                                                                                      AuthenticationInterface.INSTANCE);
		template.execute(resultatFinal, new BatchWithResultsCallback<Long, ExtractionRegimesFiscauxResults>() {
			@Override
			public boolean doInTransaction(List<Long> list, ExtractionRegimesFiscauxResults results) throws Exception {
				status.setMessage("Extraction en cours...", progressMonitor.getProgressInPercent());
				for (Long id : list) {
					final Entreprise entreprise = hibernateTemplate.get(Entreprise.class, id);
					if (entreprise == null) {
						results.addIdentifiantInvalide(id);
						continue;
					}

					// quelques informations sur l'entreprise, informations qui seront reprises dans les listes du rapport d'exécution
					final String ide = tiersService.getNumeroIDE(entreprise);
					final String raisonSociale = getLastBeforeDateOrMin(tiersService.getRaisonsSociales(entreprise, false), dateTraitement, RaisonSocialeHisto::getRaisonSociale);
					final FormeLegale formeLegale = getLastBeforeDateOrMin(tiersService.getFormesLegales(entreprise, false), dateTraitement, FormeLegaleHisto::getFormeLegale);

					final Map<RegimeFiscal.Portee, List<RegimeFiscal>> regimes = entreprise.getRegimesFiscauxNonAnnulesTries().stream()
							.filter(filtreRegimesFiscaux)
							.collect(Collectors.toMap(RegimeFiscal::getPortee,
							                          Collections::singletonList,
							                          ListUtils::union,
							                          () -> new EnumMap<>(RegimeFiscal.Portee.class)));
					for (RegimeFiscal.Portee portee : RegimeFiscal.Portee.values()) {
						final List<RegimeFiscal> regimesPourPortee = regimes.get(portee);
						if (regimesPourPortee == null || regimesPourPortee.isEmpty()) {
							results.addEntrepriseSansRegimeFiscal(id, portee, ide, raisonSociale, formeLegale);
						}
						else {
							for (RegimeFiscal regime : regimesPourPortee) {
								final TypeRegimeFiscal type = typesRegimes.get(regime.getCode());
								if (type == null) {
									results.addRegimeFiscalInconnu(id, regime, ide, raisonSociale, formeLegale);
								}
								else {
									results.addRegimeFiscal(id, regime, type, ide, raisonSociale, formeLegale);
								}
							}
						}
					}

					if (status.interrupted()) {
						break;
					}
				}
				return !status.interrupted();
			}

			@Override
			public ExtractionRegimesFiscauxResults createSubRapport() {
				return new ExtractionRegimesFiscauxResults(avecHistorique, nbThreads, dateTraitement);
			}

		}, progressMonitor);

		status.setMessage("Extraction terminée.");

		resultatFinal.end();
		return resultatFinal;
	}

	/**
	 * @return la liste des identifiants des entreprises présentes en base de données
	 */
	private List<Long> getIdsEntreprises() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> hibernateTemplate.execute(session -> {
			final String hql = "SELECT entreprise.numero FROM Entreprise entreprise ORDER BY entreprise.numero ASC";
			final Query query = session.createQuery(hql);
			//noinspection unchecked
			return query.list();
		}));
	}

	@Nullable
	private static <T extends DateRange, U> U getLastBeforeDateOrMin(List<T> data, RegDate dateReference, Function<? super T, U> mapper) {
		final Optional<T> lastBefore = data.stream()
				.filter(range -> RegDateHelper.isBeforeOrEqual(range.getDateDebut(), dateReference, NullDateBehavior.EARLIEST))
				.max(Comparator.comparing(DateRange::getDateDebut, NullDateBehavior.EARLIEST::compare));
		final Optional<T> min = lastBefore.isPresent()
				? Optional.empty()
				: data.stream().min(Comparator.comparing(DateRange::getDateDebut, NullDateBehavior.EARLIEST::compare));

		return Stream.of(lastBefore, min)
				.filter(Optional::isPresent)
				.findFirst()
				.map(Optional::get)
				.map(mapper)
				.orElse(null);
	}
}
