package ch.vd.uniregctb.foncier;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalException;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalServiceImpl;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

public class RappelFormulairesDemandeDegrevementICIProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RappelFormulairesDemandeDegrevementICIProcessor.class);
	private static final int BATCH_SIZE = 10;

	private final ParametreAppService parametreAppService;
	private final PlatformTransactionManager transactionManager;
	private final AutreDocumentFiscalServiceImpl autreDocumentFiscalService;
	private final HibernateTemplate hibernateTemplate;
	private final RegistreFoncierService registreFoncierService;
	private final DelaisService delaisService;

	public RappelFormulairesDemandeDegrevementICIProcessor(ParametreAppService parametreAppService, PlatformTransactionManager transactionManager, AutreDocumentFiscalServiceImpl autreDocumentFiscalService, HibernateTemplate hibernateTemplate,
	                                                       RegistreFoncierService registreFoncierService, DelaisService delaisService) {
		this.parametreAppService = parametreAppService;
		this.transactionManager = transactionManager;
		this.autreDocumentFiscalService = autreDocumentFiscalService;
		this.hibernateTemplate = hibernateTemplate;
		this.registreFoncierService = registreFoncierService;
		this.delaisService = delaisService;
	}

	public RappelFormulairesDemandeDegrevementICIResults run(RegDate dateTraitement, StatusManager statusManager) {

		final StatusManager status = statusManager != null ? statusManager : new LoggingStatusManager(LOGGER);

		// récupération des identifiants formulaires de demande de dégrèvement dont le délai est passé et qui n'ont pas encore été rappelés
		status.setMessage("Récupération des cas à traiter...");
		final List<Long> idsLettres = fetchIdsFormulaires(dateTraitement);

		final RappelFormulairesDemandeDegrevementICIResults rapportFinal = new RappelFormulairesDemandeDegrevementICIResults(dateTraitement);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Long, RappelFormulairesDemandeDegrevementICIResults> template = new BatchTransactionTemplateWithResults<>(idsLettres, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, RappelFormulairesDemandeDegrevementICIResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, RappelFormulairesDemandeDegrevementICIResults rapport) throws Exception {
				status.setMessage("Envoi des rappels...", progressMonitor.getProgressInPercent());
				traiterBatch(batch, rapport, status);
				return !status.isInterrupted();
			}

			@Override
			public RappelFormulairesDemandeDegrevementICIResults createSubRapport() {
				return new RappelFormulairesDemandeDegrevementICIResults(dateTraitement);
			}
		}, progressMonitor);

		rapportFinal.setInterrompu(status.isInterrupted());
		status.setMessage("Envoi des rappels terminé.");

		rapportFinal.end();
		return rapportFinal;
	}

	private void traiterBatch(List<Long> idsDemandes, RappelFormulairesDemandeDegrevementICIResults rapport, StatusManager status) throws AutreDocumentFiscalException {
		for (Long idDemande : idsDemandes) {
			final DemandeDegrevementICI demande = hibernateTemplate.get(DemandeDegrevementICI.class, idDemande);
			final Entreprise entreprise = demande.getEntreprise();
			final ImmeubleRF immeuble = demande.getImmeuble();
			final SituationRF situation = registreFoncierService.getSituation(immeuble, rapport.dateTraitement);
			final Commune commune = registreFoncierService.getCommune(immeuble, rapport.dateTraitement);

			// 1. vérification de l'état du formulaire
			if (demande.getEtat() == TypeEtatDocumentFiscal.RAPPELE) {
				// cas normalement assez rare : le rappel a eu lieu (à la main, par exemple, si tant est que cela soit possible) entre le début du job et le traitement de ce formulaire
				rapport.addFormulaireIgnore(entreprise.getNumero(), demande.getPeriodeFiscale(), situation, commune, demande.getId(), demande.getDateEnvoi(), RappelFormulairesDemandeDegrevementICIResults.RaisonIgnorement.FORMULAIRE_DEJA_RAPPELE);
			}
			else if (demande.getEtat() == TypeEtatDocumentFiscal.RETOURNE) {
				// cas normalement assez rare, mais tout-à-fait commun : le quittancement a eu lieu entre le début du job et le traitement de ce formulaire
				// (réception de données en provenance de e-dégrèvement au fil de l'eau...)
				rapport.addFormulaireIgnore(entreprise.getNumero(), demande.getPeriodeFiscale(), situation, commune, demande.getId(), demande.getDateEnvoi(), RappelFormulairesDemandeDegrevementICIResults.RaisonIgnorement.FORMULAIRE_DEJA_RETOURNE);
			}
			else if (demande.getEtat() != TypeEtatDocumentFiscal.EMIS) {
				// cas bizarre qui sent le bug...
				rapport.addRappelErreur(entreprise.getNumero(), demande.getPeriodeFiscale(), situation, commune, demande.getId(), "Etat de lettre inconnu : " + demande.getEtat());
			}
			else {
				// [SIFISC-25066] la demande est émise... s'il n'existe aucun droit de propriété valide (selon ses dates métier) au 1er janvier de la PF du formulaire,
				// alors on n'envoie pas de rappel, car en fait la demande n'aurait pas dû être envoyée
				final boolean aucunDroit = registreFoncierService.getDroitsForCtb(entreprise, false).stream()
						.filter(DroitProprieteRF.class::isInstance)
						.map(DroitProprieteRF.class::cast)
						.filter(droit -> droit.getImmeuble() == immeuble)
						.noneMatch(droit -> droit.getRangeMetier().isValidAt(RegDate.get(demande.getPeriodeFiscale(), 1, 1)));
				if (aucunDroit) {
					rapport.addFormulaireIgnore(entreprise.getNumero(), demande.getPeriodeFiscale(), situation, commune, demande.getId(), demande.getDateEnvoi(), RappelFormulairesDemandeDegrevementICIResults.RaisonIgnorement.DROIT_CLOTURE);
				}
				else {
					// vérfication du délai administratif
					final RegDate delaiEffectif = delaisService.getFinDelai(demande.getDelaiRetour(), parametreAppService.getDelaiEnvoiRappelDemandeDegrevementICI());
					if (rapport.dateTraitement.isBeforeOrEqual(delaiEffectif)) {
						rapport.addFormulaireIgnore(entreprise.getNumero(), demande.getPeriodeFiscale(), situation, commune, demande.getId(), demande.getDateEnvoi(),
						                            RappelFormulairesDemandeDegrevementICIResults.RaisonIgnorement.DELAI_ADMINISTRATIF_NON_ECHU);
					}
					else {
						// tout est bon, on peut envoyer la sauce
						final RegDate dateEnvoiRappel = delaisService.getDateFinDelaiCadevImpressionDemandeDegrevementICI(rapport.dateTraitement);
						demande.setDateRappel(dateEnvoiRappel);
						autreDocumentFiscalService.envoyerRappelFormulaireDemandeDegrevementICIBatch(demande, rapport.dateTraitement);
						rapport.addRappelEnvoye(entreprise.getNumero(), demande.getPeriodeFiscale(), situation, commune, demande.getId(), demande.getDateEnvoi());
					}
				}
			}

			// on interrompt le programme...
			if (status.isInterrupted()) {
				break;
			}
		}
	}

	List<Long> fetchIdsFormulaires(RegDate dateTraitement) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> hibernateTemplate.executeWithNewSession(new HibernateCallback<List<Long>>() {
			@Override
			public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
				final String hql = "select distinct dd.id from DemandeDegrevementICI as dd where dd.annulationDate is null and dd.dateRetour is null and dd.dateRappel is null and dd.delaiRetour < :dateTraitement order by dd.id";
				final Query query = session.createQuery(hql);
				query.setParameter("dateTraitement", dateTraitement);
				//noinspection unchecked
				return query.list();
			}
		}));
	}
}
