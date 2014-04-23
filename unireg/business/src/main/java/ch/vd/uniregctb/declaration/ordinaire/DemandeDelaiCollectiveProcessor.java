package ch.vd.uniregctb.declaration.ordinaire;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

/**
 * Processeur qui traite une demande de délais collective.
 *
 * @author Frédéric Noguier
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 *
 */
public class DemandeDelaiCollectiveProcessor {

	private static final int BATCH_SIZE = 100;

	final Logger LOGGER = Logger.getLogger(DemandeDelaiCollectiveProcessor.class);

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final PeriodeFiscaleDAO periodeFiscaleDAO;
	private final TiersService tiersService;
	private final AdresseService adresseService;

	public DemandeDelaiCollectiveProcessor(PeriodeFiscaleDAO periodeFiscaleDAO, HibernateTemplate hibernateTemplate,
	                                       PlatformTransactionManager transactionManager, TiersService tiersService, AdresseService adresseService) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
	}

	public DemandeDelaiCollectiveResults run(final List<Long> ids, final int annee, final RegDate dateDelai, final RegDate dateTraitement,
			final StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final DemandeDelaiCollectiveResults rapportFinal = new DemandeDelaiCollectiveResults(annee, dateDelai, ids, dateTraitement, tiersService, adresseService);

		checkParams(annee);

		// Traite les contribuables par lots
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Long, DemandeDelaiCollectiveResults> template = new BatchTransactionTemplateWithResults<>(ids, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, DemandeDelaiCollectiveResults>() {

			@Override
			public DemandeDelaiCollectiveResults createSubRapport() {
				return new DemandeDelaiCollectiveResults(annee, dateDelai, ids, dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, DemandeDelaiCollectiveResults r) throws Exception {
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());
				traiterBatch(batch, annee, dateDelai, dateTraitement, r);
				return true;
			}
		}, progressMonitor);

		final int count = rapportFinal.traites.size();

		if (status.interrupted()) {
			status.setMessage("Le traitement de la demande de délai collective a été interrompu."
					+ " Nombre de contribuables traités au moment de l'interruption = " + count);
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("Le traitement de la demande de délai collective est terminé." + " Nombre de contribuables traités = "
					+ count + ". Nombre d'erreurs = " + rapportFinal.errors.size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	private void checkParams(final int annee) {
		final TransactionTemplate t = new TransactionTemplate(transactionManager);
		final PeriodeFiscale periodeFiscale = t.execute(new TransactionCallback<PeriodeFiscale>() {
			@Override
			public PeriodeFiscale doInTransaction(TransactionStatus status) {
				return periodeFiscaleDAO.getPeriodeFiscaleByYear(annee);
			}
		});
		if (periodeFiscale == null) {
			throw new ObjectNotFoundException("Impossible de retrouver la période fiscale pour l'année " + annee);
		}
	}

	protected void traiterBatch(List<Long> batch, int annee, RegDate dateDelai, RegDate dateTraitement, DemandeDelaiCollectiveResults r) {
		for (Long id : batch) {
			traiterContribuable(id, annee, dateDelai, dateTraitement, r);
		}
	}

	private void traiterContribuable(Long id, int annee, RegDate dateDelai, RegDate dateTraitement, DemandeDelaiCollectiveResults r) {

		r.nbCtbsTotal++;

		final Contribuable tiers = hibernateTemplate.get(Contribuable.class, id);
		if (tiers == null) {
			r.addErrorCtbInconnu(id);
			return;
		}

		final DelaiDeclaration dd = newDelaiDeclaration(dateDelai, dateTraitement);
		accorderDelaiDeclaration(tiers, annee, dd, r);
	}

	/**
	 * @return un nouveau délai avec la date spécifiée
	 */
	protected static DelaiDeclaration newDelaiDeclaration(RegDate delai, RegDate dateTraitement) {
		DelaiDeclaration dd = new DelaiDeclaration();
		dd.setConfirmationEcrite(false);
		dd.setDateDemande(dateTraitement);
		dd.setDateTraitement(dateTraitement);
		dd.setDelaiAccordeAu(delai);
		dd.setAnnule(false);
		return dd;
	}

	/**
	 * Accorde le délai spécifié au contribuable.
	 */
	protected void accorderDelaiDeclaration(Contribuable ctb, int annee, DelaiDeclaration delai, DemandeDelaiCollectiveResults r) {

		final RegDate nouveauDelai = delai.getDelaiAccordeAu();

		final List<Declaration> declarations = ctb.getDeclarationsForPeriode(annee, false);
		if (declarations == null || declarations.isEmpty()) {
			r.addErrorCtbSansDI(ctb);
			return;
		}

		for (Declaration d : declarations) {
			Assert.isFalse(d.isAnnule());
			final TypeEtatDeclaration etatDeclaration = d.getDernierEtat().getEtat();
			switch (etatDeclaration) {
			case EMISE: {
				final RegDate delaiExistant = d.getDelaiAccordeAu();
				if (delaiExistant != null && delaiExistant.isAfterOrEqual(nouveauDelai)) {
					// Le délai accordé est égal ou au delà du délai souhaité
					r.addIgnoreDIDelaiSuperieur(d);
				}
				else {
					d.addDelai(delai);
					r.addDeclarationTraitee(d);
				}
				break;
			}
			case RETOURNEE:
				r.addErrorDIRetournee(d);
				break;
			case ECHUE:
				r.addErrorDIEchue(d);
				break;
			case SOMMEE:
				r.addErrorDISommee(d);
				break;
			default:
				throw new IllegalArgumentException("Etat de DI invalide : " + etatDeclaration);
			}
		}
	}
}
