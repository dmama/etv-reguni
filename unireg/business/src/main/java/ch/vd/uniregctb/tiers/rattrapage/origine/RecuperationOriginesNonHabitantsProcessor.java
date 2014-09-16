package ch.vd.uniregctb.tiers.rattrapage.origine;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.StringComparator;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.IndividuNotFoundException;
import ch.vd.uniregctb.tiers.OriginePersonnePhysique;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class RecuperationOriginesNonHabitantsProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RecuperationOriginesNonHabitantsProcessor.class);

	private static final int TAILLE_LOT = 10;

	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;
	private final ServiceCivilService serviceCivil;
	private final ServiceInfrastructureService serviceInfra;

	public RecuperationOriginesNonHabitantsProcessor(HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager,
	                                                 ServiceCivilService serviceCivil, ServiceInfrastructureService serviceInfra) {
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.serviceCivil = serviceCivil;
		this.serviceInfra = serviceInfra;
	}

	public RecuperationOriginesNonHabitantsResults run(final int nbThreads, final boolean dryRun, StatusManager s) {
		final StatusManager statusManager = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final RecuperationOriginesNonHabitantsResults rapportFinal = new RecuperationOriginesNonHabitantsResults(nbThreads, dryRun);

		// on commence par aller chercher les numéros des personnes physiques non-habitantes
		statusManager.setMessage("Recherche des non-habitants.");
		final List<Long> ids = getIdsNonHabitants();
		if (ids != null && !ids.isEmpty()) {

			statusManager.setMessage("Récupération des communes de Suisse.");
			final Map<String, Commune> communes = buildCommuneMap();

			final String messageTraitement = String.format("Traitement des %d non-habitants trouvés.", ids.size());

			final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
			final ParallelBatchTransactionTemplateWithResults<Long, RecuperationOriginesNonHabitantsResults> template =
					new ParallelBatchTransactionTemplateWithResults<>(ids, TAILLE_LOT, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager,
					                                                  statusManager, AuthenticationInterface.INSTANCE);

			// et on y va !
			template.execute(rapportFinal, new BatchWithResultsCallback<Long, RecuperationOriginesNonHabitantsResults>() {
				@Override
				public boolean doInTransaction(List<Long> batch, RecuperationOriginesNonHabitantsResults rapport) throws Exception {
					statusManager.setMessage(messageTraitement, progressMonitor.getProgressInPercent());
					traiterBatch(batch, dryRun, rapport, communes);
					return true;
				}

				@Override
				public RecuperationOriginesNonHabitantsResults createSubRapport() {
					return new RecuperationOriginesNonHabitantsResults(nbThreads, dryRun);
				}

			}, progressMonitor);
		}

		if (statusManager.interrupted()) {
			rapportFinal.setInterrupted(true);
			statusManager.setMessage("Traitement interrompu.");
		}
		else {
			statusManager.setMessage("Traitement terminé.");
		}
		rapportFinal.end();
		return rapportFinal;
	}

	private List<Long> getIdsNonHabitants() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				final String hql = "select nh.id from PersonnePhysique nh where nh.habitant=false";
				return hibernateTemplate.find(hql, null);
			}
		});
	}

	private void traiterBatch(List<Long> batch, boolean dryRun, RecuperationOriginesNonHabitantsResults rapport, Map<String, Commune> communes) {
		for (Long id : batch) {
			final Tiers tiers = hibernateTemplate.get(Tiers.class, id);
			if (tiers instanceof PersonnePhysique) {
				final PersonnePhysique pp = (PersonnePhysique) tiers;
				if (pp.isHabitantVD()) {
					// la donnée doit venir directement du civil, pas de sauvegarde fiscale
					rapport.addIgnore(id, RecuperationOriginesNonHabitantsResults.RaisonIgnorement.HABITANT_VD);
				}
				else if (pp.isConnuAuCivil()) {
					// c'est un ancien habitant -> on va demander au registre civil les données
					final Individu individu = serviceCivil.getIndividu(pp.getNumeroIndividu(), null, AttributeIndividu.ORIGINE);
					if (individu == null) {
						throw new IndividuNotFoundException(pp.getNumeroIndividu());
					}
					final Collection<Origine> origines = individu.getOrigines();
					if (origines == null || origines.isEmpty()) {
						// le civil ne connait aucune origine pour cet individu (étranger ?)
						rapport.addIgnore(id, RecuperationOriginesNonHabitantsResults.RaisonIgnorement.AUCUNE_ORIGINE_CIVILE_CONNUE);
					}
					else {
						final Origine first = origines.iterator().next();
						final OriginePersonnePhysique newOrigine = new OriginePersonnePhysique(first.getNomLieu(), first.getSigleCanton());
						final OriginePersonnePhysique oldOrigine = pp.getOrigine();
						if (!isSameOrigin(newOrigine, oldOrigine)) {
							if (!dryRun) {
								pp.setOrigine(newOrigine);
							}
							rapport.addTraite(id, newOrigine);
						}
						else {
							rapport.addIgnore(id, RecuperationOriginesNonHabitantsResults.RaisonIgnorement.VALEUR_DEJA_PRESENTE);
						}
					}
				}
				else if (StringUtils.isBlank(pp.getLibelleCommuneOrigine())) {
					// jamais été habitant, mais pas de donnée de toute façon -> on oublie
					rapport.addIgnore(id, RecuperationOriginesNonHabitantsResults.RaisonIgnorement.NON_HABITANT_SANS_LIBELLE_ORIGINE);
				}
				else {
					// jamais été habitant mais il y a un libellé de lieu d'origine, que l'on va donc essayer de mapper sur une commune existante
					// (et ainsi, accessoirement, récupérer le canton)
					final String libelle = pp.getLibelleCommuneOrigine();
					final Commune communeMappee = communes.get(normalizeCommuneName(libelle));
					if (communeMappee == null) {
						rapport.addErreurCommuneInconnue(id, libelle);
					}
					else {
						final OriginePersonnePhysique newOrigine = new OriginePersonnePhysique(communeMappee.getNomOfficiel(), communeMappee.getSigleCanton());
						final OriginePersonnePhysique oldOrigine = pp.getOrigine();
						if (!isSameOrigin(newOrigine, oldOrigine)) {
							if (!dryRun) {
								pp.setOrigine(newOrigine);
							}
							rapport.addTraite(id, newOrigine);
						}
						else {
							rapport.addIgnore(id, RecuperationOriginesNonHabitantsResults.RaisonIgnorement.VALEUR_DEJA_PRESENTE);
						}
					}
				}
			}
			else {
				rapport.addIgnore(id, RecuperationOriginesNonHabitantsResults.RaisonIgnorement.PAS_PERSONNE_PHYSIQUE);
			}
		}
	}

	private static boolean isSameOrigin(OriginePersonnePhysique o1, OriginePersonnePhysique o2) {
		return o1 == o2 || (o1 != null && o2 != null && o1.equals(o2));
	}

	private static String normalizeCommuneName(String name) {
		return StringComparator.toLowerCaseWithoutAccent(name);
	}

	private Map<String, Commune> buildCommuneMap() {
		final List<Commune> allCommunes = serviceInfra.getCommunes();
		final Map<String, Commune> map = new HashMap<>(allCommunes.size() * 2);
		for (Commune commune : allCommunes) {
			map.put(normalizeCommuneName(commune.getNomOfficiel()), commune);
			map.put(normalizeCommuneName(commune.getNomCourt()), commune);
		}
		return Collections.unmodifiableMap(map);
	}
}
