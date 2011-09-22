package ch.vd.uniregctb.declaration.ordinaire;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.tiers.Contribuable;
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

	private DemandeDelaiCollectiveResults rapport;

	public DemandeDelaiCollectiveProcessor(PeriodeFiscaleDAO periodeFiscaleDAO, HibernateTemplate hibernateTemplate,
			PlatformTransactionManager transactionManager) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
	}

	/**
	 * For testing purpose only
	 */
	protected void setRapport(DemandeDelaiCollectiveResults rapport) {
		this.rapport = rapport;
	}

	public DemandeDelaiCollectiveResults run(final List<Long> ids, final int annee, final RegDate dateDelai, final RegDate dateTraitement,
			final StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final DemandeDelaiCollectiveResults rapportFinal = new DemandeDelaiCollectiveResults(annee, dateDelai, ids, dateTraitement);

		checkParams(annee);

		// Traite les contribuables par lots
		final BatchTransactionTemplate<Long, DemandeDelaiCollectiveResults> template = new BatchTransactionTemplate<Long, DemandeDelaiCollectiveResults>(ids, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE,
				transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchCallback<Long, DemandeDelaiCollectiveResults>() {

			@Override
			public DemandeDelaiCollectiveResults createSubRapport() {
				return new DemandeDelaiCollectiveResults(annee, dateDelai, ids, dateTraitement);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, DemandeDelaiCollectiveResults r) throws Exception {

				rapport = r;
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);
				
				traiterBatch(batch, annee, dateDelai, dateTraitement);
				return true;
			}
		});

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

	protected void traiterBatch(List<Long> batch, int annee, RegDate dateDelai, RegDate dateTraitement) {
		for (Long id : batch) {
			traiterContribuable(id, annee, dateDelai, dateTraitement);
		}
	}

	private void traiterContribuable(Long id, int annee, RegDate dateDelai, RegDate dateTraitement) {

		rapport.nbCtbsTotal++;

		final Contribuable tiers = hibernateTemplate.get(Contribuable.class, id);
		if (tiers == null) {
			rapport.addErrorCtbInconnu(id);
			return;
		}

		final DelaiDeclaration dd = newDelaiDeclaration(dateDelai, dateTraitement);
		accorderDelaiDeclaration(tiers, annee, dd);
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
	protected void accorderDelaiDeclaration(Contribuable ctb, int annee, DelaiDeclaration delai) {

		final RegDate nouveauDelai = delai.getDelaiAccordeAu();

		final List<Declaration> declarations = ctb.getDeclarationsForPeriode(annee, false);
		if (declarations == null || declarations.isEmpty()) {
			rapport.addErrorCtbSansDI(ctb);
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
					rapport.addIgnoreDIDelaiSuperieur(d);
				}
				else {
					d.addDelai(delai);
					rapport.addDeclarationTraitee(d);
				}
				break;
			}
			case RETOURNEE:
				rapport.addErrorDIRetournee(d);
				break;
			case ECHUE:
				rapport.addErrorDIEchue(d);
				break;
			case SOMMEE:
				rapport.addErrorDISommee(d);
				break;
			default:
				throw new IllegalArgumentException("Etat de DI invalide : " + etatDeclaration);
			}
		}
	}
}
