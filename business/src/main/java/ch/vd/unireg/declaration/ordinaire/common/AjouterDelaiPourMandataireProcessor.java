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
import ch.vd.unireg.declaration.AjoutDelaiDeclarationException;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.mandataire.DemandeDelaisMandataire;
import ch.vd.unireg.mandataire.DemandeDelaisMandataireDAO;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

/**
 * Processeur qui traite une demande de délais mandataire.
 *
 * @author Baba Ngom
 *
 */
public class AjouterDelaiPourMandataireProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(AjouterDelaiPourMandataireProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final PeriodeFiscaleDAO periodeFiscaleDAO;
	private final TiersService tiersService;
	private final AdresseService adresseService;
	private final DemandeDelaisMandataireDAO demandeDelaisMandataireDAO;
	private final DeclarationImpotService declarationImpotService;

	public AjouterDelaiPourMandataireProcessor(PeriodeFiscaleDAO periodeFiscaleDAO, HibernateTemplate hibernateTemplate,
	                                           PlatformTransactionManager transactionManager, TiersService tiersService, AdresseService adresseService,
	                                           DemandeDelaisMandataireDAO demandeDelaisMandataireDAO, DeclarationImpotService declarationImpotService) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
		this.demandeDelaisMandataireDAO = demandeDelaisMandataireDAO;
		this.declarationImpotService = declarationImpotService;
	}

	public AjouterDelaiPourMandataireResults run(final List<InfosDelaisMandataire> infos,  final RegDate dateDelai, final RegDate dateTraitement, final StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final AjouterDelaiPourMandataireResults rapportFinal = new AjouterDelaiPourMandataireResults( dateDelai, infos, dateTraitement, tiersService, adresseService);



		// Traite les contribuables par lots
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<InfosDelaisMandataire, AjouterDelaiPourMandataireResults> template = new BatchTransactionTemplateWithResults<>(infos, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<InfosDelaisMandataire, AjouterDelaiPourMandataireResults>() {

			@Override
			public AjouterDelaiPourMandataireResults createSubRapport() {
				return new AjouterDelaiPourMandataireResults( dateDelai,infos, dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<InfosDelaisMandataire> batch, AjouterDelaiPourMandataireResults r) throws Exception {
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());
				traiterBatch(batch,  dateDelai, dateTraitement, r);
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

	protected void traiterBatch(List<InfosDelaisMandataire> batch, RegDate dateDelai, RegDate dateTraitement, AjouterDelaiPourMandataireResults r) {
		for (InfosDelaisMandataire info : batch) {
			traiterContribuable(info, dateDelai, dateTraitement, r);
		}
	}

	private void traiterContribuable(InfosDelaisMandataire info,  RegDate dateDelai, RegDate dateTraitement, AjouterDelaiPourMandataireResults r) {

		r.nbCtbsTotal++;

		final Contribuable tiers = hibernateTemplate.get(Contribuable.class, info.getNumeroTiers());
		if (tiers == null) {
			r.addErrorCtbInconnu(info.getNumeroTiers());
			return;
		}

		ajouterDelaiDeclarationPourMandataire(tiers, info, dateDelai, dateTraitement, r);
	}



	private static EtatDelaiDocumentFiscal getEtatDelais(InfosDelaisMandataire infos) {
		switch(infos.getStatut()){
		case ACCEPTE:
			return EtatDelaiDocumentFiscal.ACCORDE;
		case REFUSE:
			return EtatDelaiDocumentFiscal.REFUSE;
			default:
				throw new IllegalArgumentException("Etat du Délai "+infos.getStatut()+" non reconnue pour la ligne "+infos.toString());
		}
	}

	/**
	 * Accorde le délai spécifié au contribuable.
	 */
	protected void ajouterDelaiDeclarationPourMandataire(Contribuable ctb,
	                                                     InfosDelaisMandataire infos, RegDate nouveauDelai, RegDate dateTraitement,
	                                                     AjouterDelaiPourMandataireResults r) {



		checkParams(infos.getPeriodeFiscale());

		final List<DeclarationImpotOrdinaire> declarations = ctb.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, infos.getPeriodeFiscale(), true);
		if (declarations.isEmpty()) {
			r.addErrorCtbSansDI(ctb);
			return;
		}

		for (DeclarationImpotOrdinaire d : declarations) {
			DemandeDelaisMandataire demande = new DemandeDelaisMandataire();
			demande.setNumeroIDE(infos.getIdeMandataire());
			demande.setRaisonSociale(infos.getRaisonSocialeMandataire());
			demande.setReferenceId(infos.getIdentifiantDemandeMandataire());
			demande.setBusinessId("VIA_BATCH");
			demande = demandeDelaisMandataireDAO.save(demande);
			try {
				declarationImpotService.ajouterDelaiDI(d, infos.getDateSoumission(), nouveauDelai, getEtatDelais(infos), demande);
				r.addDeclarationTraitee(d);
			}
			catch (AjoutDelaiDeclarationException e) {
				switch (e.getRaison()){
				case DATE_DELAI_INVALIDE:
					r.addErrorDateDelaiInvalide(d,e);
					break;
				case DELAI_DEJA_EXISTANT:
					r.addErrorDelaiDejaExistant(d,e);
					break;
				case DECLARATION_ANNULEE:
					r.addErrorDeclarationAnnulee(d);
					break;
				case DATE_OBTENTION_INVALIDE:
					r.addErrorDateObtentionInvalide(d,e);
					break;
				case MAUVAIS_ETAT_DECLARATION:
					r.addErrorDeclarationMauvaisEtat(d,e);
					break;
				default:
					r.addErrorException(infos,e);
				}
			}

		}
	}

}
