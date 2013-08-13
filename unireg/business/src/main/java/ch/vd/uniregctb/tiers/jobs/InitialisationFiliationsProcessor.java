package ch.vd.uniregctb.tiers.jobs;

import java.util.List;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallback;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.Filiation;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class InitialisationFiliationsProcessor {

	private static final Logger LOGGER = Logger.getLogger(InitialisationFiliationsProcessor.class);
	private static final int BATCH_SIZE = 20;

	private final RapportEntreTiersDAO rapportDAO;
	private final TiersDAO tiersDAO;
	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final ServiceCivilService serviceCivil;
	private final TiersService tiersService;

	public InitialisationFiliationsProcessor(RapportEntreTiersDAO rapportDAO, TiersDAO tiersDAO, PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate,
	                                         ServiceCivilService serviceCivil, TiersService tiersService) {
		this.rapportDAO = rapportDAO;
		this.tiersDAO = tiersDAO;
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.serviceCivil = serviceCivil;
		this.tiersService = tiersService;
	}

	public InitialisationFiliationsResults run(int nbThreads, @Nullable StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		// dabord, on vide la base de tous les rapports filiation existants
		final int nbRemoved = removeFiliations(status);
		LOGGER.info("Nombre d'anciens rapports de filiation effacés : " + nbRemoved);

		// ensuite, il faut charger les nouveaux...
		return initFiliations(nbThreads, status);
	}

	private int removeFiliations(StatusManager status) {
		status.setMessage("Effacement des filiations existantes...");
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		return template.execute(new TxCallback<Integer>() {
			@Override
			public Integer execute(TransactionStatus status) throws Exception {
				return rapportDAO.removeAllOfKind(TypeRapportEntreTiers.FILIATION);
			}
		});
	}

	private InitialisationFiliationsResults initFiliations(final int nbThreads, final StatusManager status) {
		final List<Long> ids = getNumerosPersonnesPhysiquesConnuesDuCivil(status);
		LOGGER.info("Nombre de personnes physiques connues dans le registre civil trouvées : " + ids.size());

		final String msg = "Génération des relations de filiation...";
		status.setMessage(msg, 0);
		final InitialisationFiliationsResults rapportFinal = new InitialisationFiliationsResults(nbThreads);

		final ParallelBatchTransactionTemplate<Long, InitialisationFiliationsResults> template = new ParallelBatchTransactionTemplate<>(ids, BATCH_SIZE, nbThreads,
		                                                                                                                                BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, InitialisationFiliationsResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, InitialisationFiliationsResults rapport) throws Exception {
				// TODO jde préchauffer le cache (avec part PARENT) civil ?

				status.setMessage(msg, percent);
				for (Long idTiers : batch) {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idTiers);
					final long noIndividu = pp.getNumeroIndividu();
					final Individu individu = serviceCivil.getIndividu(noIndividu, null, AttributeIndividu.PARENTS);
					final List<RelationVersIndividu> parentRel = individu.getParents();
					if (parentRel != null && !parentRel.isEmpty()) {
						final RegDate dateDebut = individu.getDateNaissance();
						final RegDate dateDecesEnfant = tiersService.getDateDeces(pp);

						for (RelationVersIndividu rel : parentRel) {
							final PersonnePhysique parent = tiersService.getPersonnePhysiqueByNumeroIndividu(rel.getNumeroAutreIndividu());
							if (parent != null) {
								final RegDate dateDecesParent = tiersService.getDateDeces(parent);
								final RegDate dateFin = RegDateHelper.minimum(dateDecesEnfant, dateDecesParent, NullDateBehavior.LATEST);

								final Filiation filiation = new Filiation(dateDebut, dateFin, parent, pp);
								rapport.addFiliation(filiation);
								hibernateTemplate.merge(filiation);
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
			public InitialisationFiliationsResults createSubRapport() {
				return new InitialisationFiliationsResults(nbThreads);
			}
		});

		if (status.interrupted()) {
			status.setMessage("Génération des filiations interrompue.");
			rapportFinal.interrupted = true;
		}
		else {
			status.setMessage("Génération des filiations terminée.");
		}
		rapportFinal.end();

		return rapportFinal;
	}

	private List<Long> getNumerosPersonnesPhysiquesConnuesDuCivil(StatusManager status) {
		status.setMessage("Récupération des identifiants des personnes physiques connues du civil...");
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TxCallback<List<Long>>() {
			@Override
			public List<Long> execute(TransactionStatus status) throws Exception {
				return tiersDAO.getIdsConnusDuCivil();
			}
		});
	}
}
