package ch.vd.unireg.declaration.ordinaire.common;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.TypeDelaiDeclaration;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

/**
 * Processeur qui traite une demande de délais collective.
 *
 * @author Frédéric Noguier
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 *
 */
public class DemandeDelaiCollectiveProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(DemandeDelaiCollectiveProcessor.class);

	private static final int BATCH_SIZE = 100;

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

	public DemandeDelaiCollectiveResults run(final List<Long> ids, final int annee, final RegDate dateDelai, final RegDate dateTraitement, final StatusManager s) {

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

		if (status.isInterrupted()) {
			status.setMessage("Le traitement de la demande de délais collective a été interrompu."
					+ " Nombre de contribuables traités au moment de l'interruption = " + count);
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("Le traitement de la demande de délais collective est terminé." + " Nombre de contribuables traités = "
					+ count + ". Nombre d'erreurs = " + rapportFinal.errors.size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	private void checkParams(final int annee) {
		final TransactionTemplate t = new TransactionTemplate(transactionManager);
		final PeriodeFiscale periodeFiscale = t.execute(status -> periodeFiscaleDAO.getPeriodeFiscaleByYear(annee));
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

		accorderDelaiDeclaration(tiers, annee, dateDelai, dateTraitement, r);
	}

	/**
	 * @return un nouveau délai <b>explicite</b> avec la date spécifiée
	 */
	private static DelaiDeclaration newDelaiDeclaration(RegDate delai, RegDate dateTraitement) {
		final DelaiDeclaration dd = new DelaiDeclaration();
		dd.setCleArchivageCourrier(null);
		dd.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		dd.setDateDemande(dateTraitement);
		dd.setDateTraitement(dateTraitement);
		dd.setDelaiAccordeAu(delai);
		dd.setAnnule(false);
		dd.setTypeDelai(TypeDelaiDeclaration.EXPLICITE);
		return dd;
	}

	/**
	 * Accorde le délai spécifié au contribuable.
	 */
	protected void accorderDelaiDeclaration(Contribuable ctb, int annee,
	                                        RegDate nouveauDelai, RegDate dateTraitement,
	                                        DemandeDelaiCollectiveResults r) {

		final List<DeclarationImpotOrdinaire> declarations = ctb.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, annee, false);
		if (declarations.isEmpty()) {
			r.addErrorCtbSansDI(ctb);
			return;
		}

		for (DeclarationImpotOrdinaire d : declarations) {
			if (d.isAnnule()) {
				throw new IllegalArgumentException();
			}
			final TypeEtatDocumentFiscal etatDeclaration = d.getDernierEtatDeclaration().getEtat();
			switch (etatDeclaration) {
			case EMIS: {
				final RegDate delaiExistant = d.getDelaiAccordeAu();
				if (delaiExistant != null && delaiExistant.isAfterOrEqual(nouveauDelai)) {
					// Le délai accordé est égal ou au delà du délai souhaité
					r.addIgnoreDelaiSuperieur(d);
				}
				else {
					accorderDelaiDeclaration(d, newDelaiDeclaration(nouveauDelai, dateTraitement));
					r.addDeclarationTraitee(d);
				}
				break;
			}
			case RETOURNE:
				r.addErrorDeclarationRetournee(d);
				break;
			case ECHU:
				r.addErrorDeclarationEchue(d);
				break;
			case RAPPELE:
				r.addErrorDeclarationRappelee(d);
				break;
			case SUSPENDU:
				r.addErrorDeclarationSuspendue(d);
				break;
			case SOMME:
				r.addErrorDeclarationSommee(d);
				break;
			default:
				throw new IllegalArgumentException("Etat de déclaration non-supporté : " + etatDeclaration);
			}
		}
	}

	private void accorderDelaiDeclaration(DeclarationImpotOrdinaire declaration, DelaiDeclaration delai) {
		declaration.addDelai(delai);

		// pour les Déclarations PM, il faut envoyer un courrier...
		// TODO [SIPM] à faire une fois les maquettes éditiques connue...
	}
}
