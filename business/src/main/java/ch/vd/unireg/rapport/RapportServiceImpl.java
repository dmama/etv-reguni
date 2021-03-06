package ch.vd.unireg.rapport;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.acomptes.AcomptesResults;
import ch.vd.unireg.adresse.ResolutionAdresseResults;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.ordinaire.StatistiquesCtbs;
import ch.vd.unireg.declaration.ordinaire.StatistiquesDIs;
import ch.vd.unireg.declaration.ordinaire.common.AjouterDelaiPourMandataireResults;
import ch.vd.unireg.declaration.ordinaire.common.DemandeDelaiCollectiveResults;
import ch.vd.unireg.declaration.ordinaire.common.RattraperEmissionDIPourCyberContexteResults;
import ch.vd.unireg.declaration.ordinaire.pm.DeterminationDIsPMResults;
import ch.vd.unireg.declaration.ordinaire.pm.EchoirDIsPMResults;
import ch.vd.unireg.declaration.ordinaire.pm.EnvoiDIsPMResults;
import ch.vd.unireg.declaration.ordinaire.pm.EnvoiSommationsDIsPMResults;
import ch.vd.unireg.declaration.ordinaire.pp.DeterminationDIsPPResults;
import ch.vd.unireg.declaration.ordinaire.pp.EchoirDIsPPResults;
import ch.vd.unireg.declaration.ordinaire.pp.EnvoiAnnexeImmeubleResults;
import ch.vd.unireg.declaration.ordinaire.pp.EnvoiDIsPPResults;
import ch.vd.unireg.declaration.ordinaire.pp.EnvoiSommationsDIsPPResults;
import ch.vd.unireg.declaration.ordinaire.pp.ImportCodesSegmentResults;
import ch.vd.unireg.declaration.ordinaire.pp.ListeDIsPPNonEmises;
import ch.vd.unireg.declaration.ordinaire.pp.ListeNoteResults;
import ch.vd.unireg.declaration.snc.DeterminationQuestionnairesSNCResults;
import ch.vd.unireg.declaration.snc.EchoirQuestionnairesSNCResults;
import ch.vd.unireg.declaration.snc.EnvoiQuestionnairesSNCEnMasseResults;
import ch.vd.unireg.declaration.snc.EnvoiRappelsQuestionnairesSNCResults;
import ch.vd.unireg.declaration.snc.liens.associes.LienAssociesSNCEnMasseImporterResults;
import ch.vd.unireg.declaration.source.DeterminerLRsEchuesResults;
import ch.vd.unireg.declaration.source.EnvoiLRsResults;
import ch.vd.unireg.declaration.source.EnvoiSommationLRsResults;
import ch.vd.unireg.document.AcomptesRapport;
import ch.vd.unireg.document.AjouterDelaiPourMandataireRapport;
import ch.vd.unireg.document.AnnoncesIDERapport;
import ch.vd.unireg.document.AppariementEtablissementsSecondairesRapport;
import ch.vd.unireg.document.AssujettiParSubstitutionRapport;
import ch.vd.unireg.document.CalculParentesRapport;
import ch.vd.unireg.document.ChangementRegimesFiscauxRapport;
import ch.vd.unireg.document.CleanupRFProcessorRapport;
import ch.vd.unireg.document.ComparerForFiscalEtCommuneRapport;
import ch.vd.unireg.document.ComparerSituationFamilleRapport;
import ch.vd.unireg.document.CorrectionEtatDeclarationRapport;
import ch.vd.unireg.document.CorrectionFlagHabitantRapport;
import ch.vd.unireg.document.DatabaseIndexationRapport;
import ch.vd.unireg.document.DemandeDelaiCollectiveRapport;
import ch.vd.unireg.document.DeterminationDIsPMRapport;
import ch.vd.unireg.document.DeterminationDIsPPRapport;
import ch.vd.unireg.document.DeterminationQuestionnairesSNCRapport;
import ch.vd.unireg.document.DeterminerLRsEchuesRapport;
import ch.vd.unireg.document.DeterminerMouvementsDossiersEnMasseRapport;
import ch.vd.unireg.document.DocumentService;
import ch.vd.unireg.document.DumpPeriodesImpositionImpotSourceRapport;
import ch.vd.unireg.document.EchoirDIsPMRapport;
import ch.vd.unireg.document.EchoirDIsPPRapport;
import ch.vd.unireg.document.EchoirQSNCRapport;
import ch.vd.unireg.document.EnvoiAnnexeImmeubleRapport;
import ch.vd.unireg.document.EnvoiDIsPMRapport;
import ch.vd.unireg.document.EnvoiDIsPPRapport;
import ch.vd.unireg.document.EnvoiFormulairesDemandeDegrevementICIRapport;
import ch.vd.unireg.document.EnvoiLRsRapport;
import ch.vd.unireg.document.EnvoiLettresBienvenueRapport;
import ch.vd.unireg.document.EnvoiQuestionnairesSNCRapport;
import ch.vd.unireg.document.EnvoiRappelsQuestionnairesSNCRapport;
import ch.vd.unireg.document.EnvoiSommationLRsRapport;
import ch.vd.unireg.document.EnvoiSommationsDIsPMRapport;
import ch.vd.unireg.document.EnvoiSommationsDIsPPRapport;
import ch.vd.unireg.document.ExclureContribuablesEnvoiRapport;
import ch.vd.unireg.document.ExtractionDonneesRptRapport;
import ch.vd.unireg.document.ExtractionRegimesFiscauxRapport;
import ch.vd.unireg.document.FusionDeCommunesRapport;
import ch.vd.unireg.document.IdentifierContribuableFromListeRapport;
import ch.vd.unireg.document.IdentifierContribuableRapport;
import ch.vd.unireg.document.ImportCodesSegmentRapport;
import ch.vd.unireg.document.InitialisationIFoncRapport;
import ch.vd.unireg.document.LienAssociesSNCEnMasseImporterRapport;
import ch.vd.unireg.document.ListeAssujettisRapport;
import ch.vd.unireg.document.ListeContribuablesResidentsSansForVaudoisRapport;
import ch.vd.unireg.document.ListeDIsNonEmisesRapport;
import ch.vd.unireg.document.ListeDroitsAccesRapport;
import ch.vd.unireg.document.ListeEchangeRenseignementsRapport;
import ch.vd.unireg.document.ListeNoteRapport;
import ch.vd.unireg.document.ListeTachesEnIsntanceParOIDRapport;
import ch.vd.unireg.document.ListesNominativesRapport;
import ch.vd.unireg.document.MajoriteRapport;
import ch.vd.unireg.document.MigrationMandatairesSpeciauxRapport;
import ch.vd.unireg.document.MutationsRFDetectorRapport;
import ch.vd.unireg.document.MutationsRFProcessorRapport;
import ch.vd.unireg.document.PassageNouveauxRentiersSourciersEnMixteRapport;
import ch.vd.unireg.document.RappelFormulairesDemandeDegrevementICIRapport;
import ch.vd.unireg.document.RappelLettresBienvenueRapport;
import ch.vd.unireg.document.RapprochementTiersRFRapport;
import ch.vd.unireg.document.RattrapageModelesCommunautesRFProcessorRapport;
import ch.vd.unireg.document.RattrapageRegimesFiscauxRapport;
import ch.vd.unireg.document.RattraperDatesMetierDroitProcessorRapport;
import ch.vd.unireg.document.RattraperEmissionDIPourCyberContexteRapport;
import ch.vd.unireg.document.RecalculTachesRapport;
import ch.vd.unireg.document.ReinitialiserBaremeDoubleGainRapport;
import ch.vd.unireg.document.ResolutionAdresseRapport;
import ch.vd.unireg.document.RolePMCommunesRapport;
import ch.vd.unireg.document.RolePMOfficeRapport;
import ch.vd.unireg.document.RolePPCommunesRapport;
import ch.vd.unireg.document.RolePPOfficesRapport;
import ch.vd.unireg.document.RoleSNCRapport;
import ch.vd.unireg.document.RolesCommunesPMRapport;
import ch.vd.unireg.document.RolesCommunesPPRapport;
import ch.vd.unireg.document.RolesOIDsRapport;
import ch.vd.unireg.document.RolesOIPMRapport;
import ch.vd.unireg.document.StatistiquesCtbsRapport;
import ch.vd.unireg.document.StatistiquesDIsRapport;
import ch.vd.unireg.document.StatistiquesEvenementsRapport;
import ch.vd.unireg.document.SuppressionOIDRapport;
import ch.vd.unireg.document.TraiterEvenementExterneRapport;
import ch.vd.unireg.document.ValidationJobRapport;
import ch.vd.unireg.documentfiscal.EnvoiLettresBienvenueResults;
import ch.vd.unireg.documentfiscal.RappelLettresBienvenueResults;
import ch.vd.unireg.droits.ListeDroitsAccesResults;
import ch.vd.unireg.evenement.externe.TraiterEvenementExterneResult;
import ch.vd.unireg.evenement.ide.AnnonceIDEJobResults;
import ch.vd.unireg.foncier.EnvoiFormulairesDemandeDegrevementICIResults;
import ch.vd.unireg.foncier.InitialisationIFoncResults;
import ch.vd.unireg.foncier.RappelFormulairesDemandeDegrevementICIResults;
import ch.vd.unireg.foncier.migration.mandataire.MigrationMandatImporterResults;
import ch.vd.unireg.identification.contribuable.IdentifierContribuableFromListeResults;
import ch.vd.unireg.identification.contribuable.IdentifierContribuableResults;
import ch.vd.unireg.indexer.jobs.DatabaseIndexationResults;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.listes.afc.ExtractionDonneesRptResults;
import ch.vd.unireg.listes.afc.pm.ExtractionDonneesRptPMResults;
import ch.vd.unireg.listes.assujettis.AssujettisParSubstitutionResults;
import ch.vd.unireg.listes.assujettis.ListeAssujettisResults;
import ch.vd.unireg.listes.ear.ListeEchangeRenseignementsResults;
import ch.vd.unireg.listes.listesnominatives.ListesNominativesResults;
import ch.vd.unireg.listes.suisseoupermiscresident.ListeContribuablesResidentsSansForVaudoisResults;
import ch.vd.unireg.metier.ComparerForFiscalEtCommuneResults;
import ch.vd.unireg.metier.FusionDeCommunesResults;
import ch.vd.unireg.metier.OuvertureForsResults;
import ch.vd.unireg.metier.PassageNouveauxRentiersSourciersEnMixteResults;
import ch.vd.unireg.metier.piis.DumpPeriodesImpositionImpotSourceResults;
import ch.vd.unireg.mouvement.DeterminerMouvementsDossiersEnMasseResults;
import ch.vd.unireg.oid.SuppressionOIDResults;
import ch.vd.unireg.parentes.CalculParentesResults;
import ch.vd.unireg.regimefiscal.changement.ChangementRegimesFiscauxJobResults;
import ch.vd.unireg.regimefiscal.extraction.ExtractionRegimesFiscauxResults;
import ch.vd.unireg.regimefiscal.rattrapage.RattrapageRegimesFiscauxJobResults;
import ch.vd.unireg.registrefoncier.dataimport.MutationsRFDetectorResults;
import ch.vd.unireg.registrefoncier.dataimport.MutationsRFProcessorResults;
import ch.vd.unireg.registrefoncier.importcleanup.CleanupRFProcessorResults;
import ch.vd.unireg.registrefoncier.processor.RapprochementTiersRFResults;
import ch.vd.unireg.registrefoncier.rattrapage.RattrapageModelesCommunautesRFProcessorResults;
import ch.vd.unireg.registrefoncier.rattrapage.RattraperDatesMetierDroitRFProcessorResults;
import ch.vd.unireg.role.RolePMCommunesResults;
import ch.vd.unireg.role.RolePMOfficeResults;
import ch.vd.unireg.role.RolePPCommunesResults;
import ch.vd.unireg.role.RolePPOfficesResults;
import ch.vd.unireg.role.RoleSNCResults;
import ch.vd.unireg.role.before2016.ProduireRolesOIDsResults;
import ch.vd.unireg.role.before2016.ProduireRolesOIPMResults;
import ch.vd.unireg.role.before2016.ProduireRolesPMCommunesResults;
import ch.vd.unireg.role.before2016.ProduireRolesPPCommunesResults;
import ch.vd.unireg.situationfamille.ComparerSituationFamilleResults;
import ch.vd.unireg.situationfamille.ReinitialiserBaremeDoubleGainResults;
import ch.vd.unireg.stats.evenements.StatsEvenementsCivilsEntreprisesResults;
import ch.vd.unireg.stats.evenements.StatsEvenementsCivilsPersonnesResults;
import ch.vd.unireg.stats.evenements.StatsEvenementsExternesResults;
import ch.vd.unireg.stats.evenements.StatsEvenementsIdentificationContribuableResults;
import ch.vd.unireg.stats.evenements.StatsEvenementsNotairesResults;
import ch.vd.unireg.tache.ListeTachesEnInstanceParOID;
import ch.vd.unireg.tache.TacheSyncResults;
import ch.vd.unireg.tiers.ExclureContribuablesEnvoiResults;
import ch.vd.unireg.tiers.rattrapage.appariement.AppariementEtablissementsSecondairesResults;
import ch.vd.unireg.tiers.rattrapage.etatdeclaration.CorrectionEtatDeclarationResults;
import ch.vd.unireg.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantResults;
import ch.vd.unireg.validation.ValidationJobResults;

public class RapportServiceImpl implements RapportService, ApplicationContextAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(RapportServiceImpl.class);

	private DocumentService docService;
	private ServiceInfrastructureService infraService;
	private ApplicationContext applicationContext;
	private AuditManager audit;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDocService(DocumentService docService) {
		this.docService = docService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setAudit(AuditManager audit) {
		this.audit = audit;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public DeterminationDIsPPRapport generateRapport(final DeterminationDIsPPResults results, StatusManager s) throws DeclarationException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportDetermDIs" + results.annee;
		final String description = String.format("Rapport du job de détermination des DIs PP à émettre pour l'année %d. Date de traitement = %s",
		                                         results.annee,
		                                         RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(DeterminationDIsPPRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfDeterminationDIsPPRapport document = new PdfDeterminationDIsPPRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public DeterminationDIsPMRapport generateRapport(final DeterminationDIsPMResults results, StatusManager s) throws DeclarationException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportDetermDIsPM" + results.annee;
		final String description = String.format("Rapport du job de détermination des DIs PM à émettre pour l'année %d. Date de traitement = %s",
		                                         results.annee,
		                                         RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(DeterminationDIsPMRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfDeterminationDIsPMRapport document = new PdfDeterminationDIsPMRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public EnvoiDIsPPRapport generateRapport(final EnvoiDIsPPResults results, StatusManager s) throws DeclarationException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportEnvoiDIsPP" + results.annee;
		final String description = String.format("Rapport d'exécution du job d'envoi des DIs PP en masse pour l'année %d. Date de traitement = %s. Type de contribuable = %s",
				                                 results.annee, RegDateHelper.dateToDisplayString(results.dateTraitement), results.categorie.name());
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(EnvoiDIsPPRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfEnvoiDIsPPRapport document = new PdfEnvoiDIsPPRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public EnvoiDIsPMRapport generateRapport(final EnvoiDIsPMResults results, StatusManager s) throws DeclarationException {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportEnvoiDIsPM" + results.getPeriodeFiscale();
		final String description = String.format("Rapport d'exécution du job d'envoi des DIs PM en masse pour la période fiscale %d. Date de traitement = %s. Type de document = %s",
		                                         results.getPeriodeFiscale(), RegDateHelper.dateToDisplayString(results.getDateTraitement()), results.getCategorieEnvoi().name());
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(EnvoiDIsPMRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfEnvoiDIsPMRapport document = new PdfEnvoiDIsPMRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public EnvoiAnnexeImmeubleRapport generateRapport(final EnvoiAnnexeImmeubleResults results, StatusManager s) throws DeclarationException {
			final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportEnvoiAnnexeImmeuble" + results.annee;
		final String description = String.format("Rapport d'exécution du job d'envoi des annexes immeubles en masse pour l'année %d. Date de traitement = %s.",
				results.annee, RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(EnvoiAnnexeImmeubleRapport.class, nom, description, "pdf", (doc, os) -> {
				PdfEnvoiAnnexeImmeubleRapport document = new PdfEnvoiAnnexeImmeubleRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public ListeDIsNonEmisesRapport generateRapport(final ListeDIsPPNonEmises results, final StatusManager status) {
		final String nom = "RapportListeDIsNonEmises" + results.dateTraitement.index();
		final String description = String.format("Rapport de la liste des DIs non émises.. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();
		try {
			return docService.newDoc(ListeDIsNonEmisesRapport.class, nom, description, "pdf", (doc, os) -> {
				PdfListeDIsNonEmisesRapport document = new PdfListeDIsNonEmisesRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public MajoriteRapport generateRapport(final OuvertureForsResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportMajorite" + results.dateTraitement.index();
		final String description = String.format("Rapport d'exécution du job d'ouverture des fors des contribuables majeurs.. Date de traitement = %s",
		                                         RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(MajoriteRapport.class, nom, description, "pdf", (doc, os) -> {
				PdfMajoriteRapport document = new PdfMajoriteRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public FusionDeCommunesRapport generateRapport(final FusionDeCommunesResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "FusionDeCommunes" + results.dateTraitement.index();
		final String description = String.format("Rapport d'exécution du job de fusion de communes.. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(FusionDeCommunesRapport.class, nom, description, "pdf", (doc, os) -> {
				PdfFusionDeCommunesRapport document = new PdfFusionDeCommunesRapport(infraService);
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RolePPCommunesRapport generateRapport(final RolePPCommunesResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final String nom = "RolePPCommunes" + results.annee;
		final String description = String.format("Rapport d'exécution du job d'extraction du rôle PP %d des communes", results.annee);
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RolePPCommunesRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRolePPCommunesRapport document = new PdfRolePPCommunesRapport(infraService);
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RolePPOfficesRapport generateRapport(final RolePPOfficesResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final String nom = "RolePPOffices" + results.annee;
		final String description = String.format("Rapport d'exécution du job d'extraction du rôle PP %d des OID", results.annee);
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RolePPOfficesRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRolePPOfficesRapport document = new PdfRolePPOfficesRapport(infraService);
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RolePMCommunesRapport generateRapport(final RolePMCommunesResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final String nom = "RolePMCommunes" + results.annee;
		final String description = String.format("Rapport d'exécution du job d'extraction du rôle PM %d des communes", results.annee);
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RolePMCommunesRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRolePMCommunesRapport document = new PdfRolePMCommunesRapport(infraService);
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RolePMOfficeRapport generateRapport(final RolePMOfficeResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final String nom = "RolePMOffice" + results.annee;
		final String description = String.format("Rapport d'exécution du job d'extraction du rôle PM %d de l'OIPM", results.annee);
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RolePMOfficeRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRolePMOfficeRapport document = new PdfRolePMOfficeRapport(infraService);
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RolesCommunesPPRapport generateRapport(final ProduireRolesPPCommunesResults results, final StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RolesCommunesPP" + results.dateTraitement.index();
		final String description = String.format("Rapport des rôles PP pour les communes.. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RolesCommunesPPRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRolesPPCommunesRapport document = new PdfRolesPPCommunesRapport(infraService, audit);
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RolesCommunesPMRapport generateRapport(final ProduireRolesPMCommunesResults results, final StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RolesCommunesPM" + results.dateTraitement.index();
		final String description = String.format("Rapport des rôles PM pour les communes.. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RolesCommunesPMRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRolesPMCommunesRapport document = new PdfRolesPMCommunesRapport(infraService, audit);
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Genère le rapport (PDF) des rôles des contribuables pour un ou plusieurs OID donné(s)
	 * @param results le résultat de l'exécution du job de production des rôles pour un ou plusieurs OID.
	 * @return le rapport
	 */
	@Override
	public RolesOIDsRapport generateRapport(final ProduireRolesOIDsResults[] results, RegDate dateTraitement, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RolesOIDs" + dateTraitement.index();
		final String description = String.format("Rapport des rôles pour les OID.. Date de traitement = %s", RegDateHelper.dateToDisplayString(dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RolesOIDsRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRolesOIDsRapport document = new PdfRolesOIDsRapport(infraService, audit);
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Genère le rapport (PDF) des rôles des contribuables PM du canton (pour l'OIPM, donc)
	 * @param results le résultat de l'exécution du job de production des rôles pour l'OIPM
	 * @param dateTraitement date du traitement
	 * @param s status manager
	 * @return le rapport
	 */
	@Override
	public RolesOIPMRapport generateRapport(final ProduireRolesOIPMResults results, RegDate dateTraitement, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RolesOIPM" + dateTraitement.index();
		final String description = String.format("Rapport des rôles pour l'OIPM.. Date de traitement = %s", RegDateHelper.dateToDisplayString(dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RolesOIPMRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRolesOIPMRapport document = new PdfRolesOIPMRapport(infraService, audit);
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public StatistiquesDIsRapport generateRapport(final StatistiquesDIs results, final StatusManager status) {
		final String nom = "RapportStatsDIs" + results.dateTraitement.index();
		final String description = String.format("Rapport des statistiques des déclarations d'impôt ordinaires.. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(StatistiquesDIsRapport.class, nom, description, "pdf", (doc, os) -> {
				PdfStatsDIsRapport document = new PdfStatsDIsRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public StatistiquesCtbsRapport generateRapport(final StatistiquesCtbs results, final StatusManager status) {
		final String nom = "RapportStatsCtbs" + results.dateTraitement.index();
		final String description = String.format("Rapport des statistiques des contribuables assujettis.. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(StatistiquesCtbsRapport.class, nom, description, "pdf", (doc, os) -> {
				PdfStatsCtbsRapport document = new PdfStatsCtbsRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public EnvoiSommationsDIsPPRapport generateRapport(final EnvoiSommationsDIsPPResults results, final StatusManager statusManager) {
		final String nom = "RapportSommationDIPP" + results.getDateTraitement().index();
		final String description = String.format("Rapport de l'envoi de sommation des DIs PP. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
		final Date dateGeneration = DateHelper.getCurrentDate();
		try {
			return docService.newDoc(EnvoiSommationsDIsPPRapport.class, nom, description, "pdf", (doc, os) -> {
				PdfEnvoiSommationsDIsPPRapport document = new PdfEnvoiSommationsDIsPPRapport();
				document.write(results, nom, description, dateGeneration, os, statusManager);

			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public EnvoiSommationsDIsPMRapport generateRapport(final EnvoiSommationsDIsPMResults results, final StatusManager statusManager) {
		final String nom = "RapportSommationDIPM" + results.getDateTraitement().index();
		final String description = String.format("Rapport de l'envoi de sommation des DIs PM. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
		final Date dateGeneration = DateHelper.getCurrentDate();
		try {
			return docService.newDoc(EnvoiSommationsDIsPMRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfEnvoiSommationsDIsPMRapport document = new PdfEnvoiSommationsDIsPMRapport();
				document.write(results, nom, description, dateGeneration, os, statusManager);

			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ValidationJobRapport generateRapport(final ValidationJobResults results, final StatusManager statusManager) {
		final String nom = "RapportValidationTiers" + results.dateTraitement.index();
		final String description = String.format("Rapport de la validation de tous les tiers. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();
		try {
			return docService.newDoc(ValidationJobRapport.class, nom, description, "pdf", (doc, os) -> {
				PdfValidationRapport document = new PdfValidationRapport();
				document.write(results, nom, description, dateGeneration, os, statusManager);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public EnvoiLRsRapport generateRapport(final EnvoiLRsResults results, final StatusManager statusManager) {
		final String nom = "RapportEnvoiLR" + results.dateTraitement.index();
		final RegDate month = RegDate.get(results.dateFinPeriode.year(), results.dateFinPeriode.month());
		final String description = String.format("Rapport de l'envoi de LR pour le mois de %s. Date de traitement = %s", RegDateHelper.dateToDisplayString(month), RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();
		try {
			return docService.newDoc(EnvoiLRsRapport.class, nom, description, "pdf", (doc, os) -> {
				PdfEnvoiLRsRapport document = new PdfEnvoiLRsRapport();
				document.write(results, nom, description, dateGeneration, os, statusManager);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public EnvoiSommationLRsRapport generateRapport(final EnvoiSommationLRsResults results, final StatusManager statusManager) {
		final String nom = "RapportSommationLR" + results.dateTraitement.index();
		final String description = String.format("Rapport de l'envoi de sommation de LR. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();
		try {
			return docService.newDoc(EnvoiSommationLRsRapport.class, nom, description, "pdf", (doc, os) -> {
				PdfEnvoiSommationsLRsRapport document = new PdfEnvoiSommationsLRsRapport();
				document.write(results, nom, description, dateGeneration, os, statusManager);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Genère le rapport (PDF) pour l'envoi des listes nominatives
	 *
	 * @param results le résultat de l'exécution du job
	 * @return le rapport
	 */
	@Override
	public ListesNominativesRapport generateRapport(final ListesNominativesResults results, final StatusManager statusManager) {
		final String nom = "RapportListesNominatives" + results.getDateTraitement().index();
		final String description = String.format("Rapport de la génération des listes nominatives au %s.", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
		final Date dateGeneration = DateHelper.getCurrentDate();
		try {
			return docService.newDoc(ListesNominativesRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfListesNominativesRapport document = new PdfListesNominativesRapport();
				document.write(results, nom, description, dateGeneration, os, statusManager);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public AcomptesRapport generateRapport(final AcomptesResults results, final StatusManager statusManager) {
		final String nom = "RapportAcomptes" + results.getDateTraitement().index();
		final String description = String.format("Rapport de la génération des populations pour les bases acomptes au %s.", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
		final Date dateGeneration = DateHelper.getCurrentDate();
		try {
			return docService.newDoc(AcomptesRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfAcomptesRapport document = new PdfAcomptesRapport();
				document.write(results, nom, description, dateGeneration, os, statusManager);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Genère le rapport (PDF) pour les extractions des données de référence RPT
	 * @param results le résultat de l'exécution du job
	 * @return le rapport
	 */
	@Override
	public ExtractionDonneesRptRapport generateRapport(final ExtractionDonneesRptResults results, final StatusManager statusManager) {
		final String nom = "RapportExtractionDonneesRpt" + results.getDateTraitement().index();
		final String description = String.format("Rapport de l'extraction des données de référence RPT au %s.", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
		final Date dateGeneration = DateHelper.getCurrentDate();
		try {
			return docService.newDoc(ExtractionDonneesRptRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfExtractionDonneesRptRapport document = new PdfExtractionDonneesRptRapport();
				document.write(results, nom, description, dateGeneration, os, statusManager);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
	@Override
	public ExtractionDonneesRptRapport generateRapport(ExtractionDonneesRptPMResults results, StatusManager statusManager) {
		final String nom = "RapportExtractionDonneesRptPM" + results.getDateTraitement().index();
		final String description = String.format("Rapport de l'extraction des données de référence RPT au %s.", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
		final Date dateGeneration = DateHelper.getCurrentDate();
		try {
			return docService.newDoc(ExtractionDonneesRptRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfExtractionDonneesRptPMRapport document = new PdfExtractionDonneesRptPMRapport();
				document.write(results, nom, description, dateGeneration, os, statusManager);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public EchoirDIsPPRapport generateRapport(final EchoirDIsPPResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportEchoirDIsPP" + results.dateTraitement.index();
		final String description = String.format("Rapport d'exécution du job de passage des DIs PP sommées à l'état échu. Date de traitement = %s.",
		                                         RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(EchoirDIsPPRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfEchoirDIsPPRapport document = new PdfEchoirDIsPPRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public EchoirDIsPMRapport generateRapport(final EchoirDIsPMResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportEchoirDIsPM" + results.dateTraitement.index();
		final String description = String.format("Rapport d'exécution du job de passage des DIs PM sommées à l'état échu. Date de traitement = %s.",
		                                         RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(EchoirDIsPMRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfEchoirDIsPMRapport document = new PdfEchoirDIsPMRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ReinitialiserBaremeDoubleGainRapport generateRapport(final ReinitialiserBaremeDoubleGainResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "ReinitDoubleGain" + results.dateTraitement.index();
		final String description = String.format("Rapport d'exécution du job de réinitialisation des barèmes double-gain. Date de traitement = %s.",
				RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(ReinitialiserBaremeDoubleGainRapport.class, nom, description, "pdf", (doc, os) -> {
				PdfReinitDoubleGainRapport document = new PdfReinitDoubleGainRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ListeTachesEnIsntanceParOIDRapport generateRapport(final ListeTachesEnInstanceParOID results, final StatusManager status) {
		final String nom = "RapportListeTacheEnInstanceParOID" + results.dateTraitement.index();
		final String description = String.format("Rapport de la liste des Taches en instance par OID.. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();
		try {
			return docService.newDoc(ListeTachesEnIsntanceParOIDRapport.class, nom, description, "pdf", (doc, os) -> {
				PdfListeTacheEnInstanceParOIDRapport document = new PdfListeTacheEnInstanceParOIDRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ExclureContribuablesEnvoiRapport generateRapport(final ExclureContribuablesEnvoiResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final RegDate dateTraitement = RegDate.get();
		final String nom = "RapportExclCtbsEnvoi" + dateTraitement.index();
		final String description = String.format("Rapport d'exécution du job d'exclusion de contribuables de l'envoi automatique de DIs. Date de traitement = %s.", RegDateHelper.dateToDisplayString(dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(ExclureContribuablesEnvoiRapport.class, nom, description, "pdf", (doc, os) -> {
				PdfExclureContribuablesEnvoiRapport document = new PdfExclureContribuablesEnvoiRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public DemandeDelaiCollectiveRapport generateRapport(final DemandeDelaiCollectiveResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final RegDate dateTraitement = RegDate.get();
		final String nom = "RapportDemDelaiColl" + dateTraitement.index();
		final String description = String.format("Rapport d'exécution du traitement d'une demande de délais collective. Date de traitement = %s.", RegDateHelper.dateToDisplayString(dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(DemandeDelaiCollectiveRapport.class, nom, description, "pdf", (doc, os) -> {
				PdfDemandeDelaiCollectiveRapport document = new PdfDemandeDelaiCollectiveRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ListeContribuablesResidentsSansForVaudoisRapport generateRapport(final ListeContribuablesResidentsSansForVaudoisResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

	       final String nom = "RapportResSansForVD";
	       final String description = String.format("Rapport d'exécution du job qui liste les contribuables résidents suisses ou titulaires d'un permis C sans for vaudois. Date de traitement = %s",
	                                                RegDateHelper.dateToDisplayString(results.getDateTraitement()));
	       final Date dateGeneration = DateHelper.getCurrentDate();

	       try {
	           return docService.newDoc(ListeContribuablesResidentsSansForVaudoisRapport.class, nom, description, "pdf", (doc, os) -> {
		           final PdfListeContribuablesResidentsSansForVaudoisRapport document = new PdfListeContribuablesResidentsSansForVaudoisRapport();
		           document.write(results, nom, description, dateGeneration, os, status);
	           });
	       }
	       catch (Exception e) {
	           throw new RuntimeException(e);
	       }
	}

	@Override
	public CorrectionFlagHabitantRapport generateRapport(final CorrectionFlagHabitantResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportCorrectionFlagHabitant";
		final String description = String.format("Rapport d'exécution du job qui corrige les flags 'habitant' sur les personnes physiques en fonction de leur for principal actif. Date de traitement = %s",
				                                 RegDateHelper.dateToDisplayString(RegDate.get()));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(CorrectionFlagHabitantRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfCorrectionFlagHabitantRapport document = new PdfCorrectionFlagHabitantRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public StatistiquesEvenementsRapport generateRapport(final StatsEvenementsCivilsPersonnesResults civilsPersonnes,
	                                                     final StatsEvenementsCivilsEntreprisesResults civilsEntreprises,
	                                                     final StatsEvenementsExternesResults externes,
	                                                     final StatsEvenementsIdentificationContribuableResults identCtb,
	                                                     final StatsEvenementsNotairesResults notaires,
	                                                     final RegDate dateReference, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportStatsEvenements";
		final String description = String.format("Statistiques des événements reçus par Unireg. Date de traitement = %s", RegDateHelper.dateToDisplayString(RegDate.get()));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(StatistiquesEvenementsRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfStatistiquesEvenementsRapport document = new PdfStatistiquesEvenementsRapport();
				document.write(civilsPersonnes, civilsEntreprises, externes, identCtb, notaires, dateReference, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public DeterminerMouvementsDossiersEnMasseRapport generateRapport(final DeterminerMouvementsDossiersEnMasseResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportMouvementsDossiersMasse";
		final String description = String.format("Rapport d'exécution du job de détermination des mouvements de dossiers en masse. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(DeterminerMouvementsDossiersEnMasseRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfDeterminerMouvementsDossiersEnMasseRapport document = new PdfDeterminerMouvementsDossiersEnMasseRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public DeterminerLRsEchuesRapport generateRapport(final DeterminerLRsEchuesResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportLrEchues";
		final String description = String.format("Rapport d'exécution du job de détermination LR échues. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(DeterminerLRsEchuesRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfDeterminerLRsEchuesRapport document = new PdfDeterminerLRsEchuesRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public IdentifierContribuableRapport generateRapport(final IdentifierContribuableResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

			final String nom = "RapportRelanceIdentification";
			final String description = String.format("Rapport d'exécution du job de relance de l'identification des contribuables. Date de traitement = %s",
			                                         RegDateHelper.dateToDisplayString(results.getDateTraitement()));
			final Date dateGeneration = DateHelper.getCurrentDate();

			try {
				return docService.newDoc(IdentifierContribuableRapport.class, nom, description, "pdf", (doc, os) -> {
					final PdfIdentifierContribuableRapport document = new PdfIdentifierContribuableRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				});
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}

	}

	@Override
	public TraiterEvenementExterneRapport generateRapport(final TraiterEvenementExterneResult results, StatusManager s) {
			final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

			final String nom = "RapportRelanceEvenementExterne";
			final String description = String.format("Rapport d'exécution du job de relance des evenements externes. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
			final Date dateGeneration = DateHelper.getCurrentDate();

			try {
				return docService.newDoc(TraiterEvenementExterneRapport.class, nom, description, "pdf", (doc, os) -> {
					final PdfTraiterEvenementExterneRapport document = new PdfTraiterEvenementExterneRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				});
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
	}

	@Override
	public ResolutionAdresseRapport generateRapport(final ResolutionAdresseResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportResolutionAdresse";
		final String description = String.format("Rapport d'exécution du job de résolution des adresses. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(ResolutionAdresseRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfResolutionAdresseRapport document = new PdfResolutionAdresseRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public ComparerSituationFamilleRapport generateRapport(final ComparerSituationFamilleResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportComparerSituationFamille";
		final String description = String.format("Rapport d'exécution du job de comparaison des situations de famille. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(ComparerSituationFamilleRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfComparerSituationFamilleRapport document = new PdfComparerSituationFamilleRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ListeNoteRapport generateRapport(final ListeNoteResults results, StatusManager s) {
	final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportListeNote";
		final String description = String.format("Rapport d'exécution du job qui produit la liste des contribuable ayant reçu une note. Date de traitement = %s",
		                                         RegDateHelper.dateToDisplayString(results.getDateTraitement()));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(ListeNoteRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfListeNoteRapport document = new PdfListeNoteRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ComparerForFiscalEtCommuneRapport generateRapport(final ComparerForFiscalEtCommuneResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportComparerForFiscalEtCommune";
		final String description = String.format("Rapport d'exécution du job de comparaison du dernier For fiscal et de la commune de résidence. Date de traitement = %s",
		                                         RegDateHelper.dateToDisplayString(results.getDateTraitement()));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(ComparerForFiscalEtCommuneRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfComparerForFiscalEtCommuneRapport document = new PdfComparerForFiscalEtCommuneRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public CorrectionEtatDeclarationRapport generateRapport(final CorrectionEtatDeclarationResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportCorrectionEtatDeclaration";
		final String description = String.format("Rapport d'exécution du job de suppression des doublons des états des déclarations. Date de traitement = %s",
		                                         RegDateHelper.dateToDisplayString(results.getDateTraitement()));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(CorrectionEtatDeclarationRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfCorrectionEtatDeclarationRapport document = new PdfCorrectionEtatDeclarationRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ListeAssujettisRapport generateRapport(final ListeAssujettisResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportListeAssujettis";
		final String description = "Rapport d'exécution du job d'extraction de la liste des assujettis d'une période fiscale.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(ListeAssujettisRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfListeAssujettisRapport document = new PdfListeAssujettisRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Génère le rapport d'exécution du batch d'import des codes de segmentation fournis par TAO
	 * @param results les résultats du batch
	 * @param nbLignesLuesFichierEntree le nombre de lignes lues dans le fichier d'entrée (indication du nombre de doublons)
	 * @return le rapport
	 */
	@Override
	public ImportCodesSegmentRapport generateRapport(final ImportCodesSegmentResults results, final int nbLignesLuesFichierEntree, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportImportCodesSegment";
		final String description = "Rapport d'exécution du job d'importation des codes de segmentation des déclarations d'impôt.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(ImportCodesSegmentRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfImportCodesSegmentRapport document = new PdfImportCodesSegmentRapport();
				document.write(results, nbLignesLuesFichierEntree, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ListeDroitsAccesRapport generateRapport(final ListeDroitsAccesResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportListeDroitsAcces";
		final String description = "Rapport d'exécution du job de listings des dossiers protégés.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(ListeDroitsAccesRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfListeDroitsAccesRapport document = new PdfListeDroitsAccesRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public SuppressionOIDRapport generateRapport(final SuppressionOIDResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "SuppressionOID";
		final String description = "Rapport d'exécution du job de suppression d'un office d'impôt.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(SuppressionOIDRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfSuppressionOIDRapport document = new PdfSuppressionOIDRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public PassageNouveauxRentiersSourciersEnMixteRapport generateRapport(final PassageNouveauxRentiersSourciersEnMixteResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportPassageSourciersRentiersEnMixte" + results.dateTraitement.index();
		final String description = String.format("Rapport d'exécution du job de passage des nouveaux sourciers rentiers en mixte 1. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(PassageNouveauxRentiersSourciersEnMixteRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfPassageNouveauxRentiersSourciersEnMixteRapport document = new PdfPassageNouveauxRentiersSourciersEnMixteRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public RecalculTachesRapport generateRapport(final TacheSyncResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportRecalculTaches";
		final String description = "Rapport d'exécution du job de recalcul des tâches d'envoi et d'annulation de déclaration d'impôt";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RecalculTachesRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRecalculTachesRapport document = new PdfRecalculTachesRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public AnnoncesIDERapport generateRapport(final AnnonceIDEJobResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportAnnoncesIDE";
		final String description = "Rapport d'exécution du job d'annonces à l'IDE des entreprises sous responsabilité d'Unireg.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(AnnoncesIDERapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfAnnoncesIDERapport document = new PdfAnnoncesIDERapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public CalculParentesRapport generateRapport(final CalculParentesResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportInitialisationParentes";
		final String description = "Rapport d'exécution du job de calcul des relations de parenté";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(CalculParentesRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfCalculParentesRapport document = new PdfCalculParentesRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public DumpPeriodesImpositionImpotSourceRapport generateRapport(final DumpPeriodesImpositionImpotSourceResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportDumpPeriodesImpositionImpotSource";
		final String description = "Rapport d'exécution du job de calcul des périodes d'imposition IS";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(DumpPeriodesImpositionImpotSourceRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfDumpPeriodesImpositionImpotSourceRapport document = new PdfDumpPeriodesImpositionImpotSourceRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public AssujettiParSubstitutionRapport generateRapport(final AssujettisParSubstitutionResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapporAssujettisParSubstituttion";
		final String description = "Rapport d'exécution du job qui liste les liens d'assujettissement par substitution.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(AssujettiParSubstitutionRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfAssujettistParSubstitutionRapport document = new PdfAssujettistParSubstitutionRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public EnvoiLettresBienvenueRapport generateRapport(final EnvoiLettresBienvenueResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "EnvoiLettresBienvenue";
		final String description = "Rapport d'exécution du job qui envoie les lettres de bienvenue.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(EnvoiLettresBienvenueRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfEnvoiLettresBienvenueRapport document = new PdfEnvoiLettresBienvenueRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RappelLettresBienvenueRapport generateRapport(final RappelLettresBienvenueResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RappelLettresBienvenue";
		final String description = "Rapport d'exécution du job qui envoie les rappels des lettres de bienvenue.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RappelLettresBienvenueRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRappelLettresBienvenueRapport document = new PdfRappelLettresBienvenueRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public DeterminationQuestionnairesSNCRapport generateRapport(final DeterminationQuestionnairesSNCResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportDetermSQNC";
		final String description = "Rapport d'exécution du job qui génère les tâches d'envoi des questionnaires SNC.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(DeterminationQuestionnairesSNCRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfDeterminationQuestionnairesSNCRapport document = new PdfDeterminationQuestionnairesSNCRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public EnvoiQuestionnairesSNCRapport generateRapport(final EnvoiQuestionnairesSNCEnMasseResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportEnvoiQSNC";
		final String description = "Rapport d'exécution du job d'envoi en masse des questionnaires SNC.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(EnvoiQuestionnairesSNCRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfEnvoiQuestionnairesSNCRapport document = new PdfEnvoiQuestionnairesSNCRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public EnvoiRappelsQuestionnairesSNCRapport generateRapport(final EnvoiRappelsQuestionnairesSNCResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportRappelsQSNC";
		final String description = "Rapport d'exécution du job d'envoi en masse des rappels des questionnaires SNC.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(EnvoiRappelsQuestionnairesSNCRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfEnvoiRappelsQuestionnairesSNCRapport document = new PdfEnvoiRappelsQuestionnairesSNCRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public AppariementEtablissementsSecondairesRapport generateRapport(final AppariementEtablissementsSecondairesResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportAppariementEtbsSecondaires";
		final String description = "Rapport d'exécution du job d'appariement des établissements secondaires.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(AppariementEtablissementsSecondairesRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfAppariementEtablissementsSecondairesRapport document = new PdfAppariementEtablissementsSecondairesRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public IdentifierContribuableFromListeRapport generateRapport(IdentifierContribuableFromListeResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportIdentifierContribuableFromListe";
		final String description = String.format("Rapport d'exécution du job d'identification à partir d'une liste. Date de traitement = %s",
				RegDateHelper.dateToDisplayString(results.getDateTraitement()));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(IdentifierContribuableFromListeRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfIdentifierContribuableFromListeRapport document = new PdfIdentifierContribuableFromListeRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public RapprochementTiersRFRapport generateRapport(final RapprochementTiersRFResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportRapprochementTiersRF";
		final String description = "Rapport d'exécution du job de rapprochement des tiers RF.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RapprochementTiersRFRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRapprochementTiersRFRapport document = new PdfRapprochementTiersRFRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public MutationsRFDetectorRapport generateRapport(MutationsRFDetectorResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportTraiterImportRF";
		final String description = "Rapport d'exécution du job d'import (détection des mutations) du RF.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(MutationsRFDetectorRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfMutationsRFDetectorRapport document = new PdfMutationsRFDetectorRapport();
				document.write(results, nom, description, dateGeneration, os, status, applicationContext);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public MutationsRFProcessorRapport generateRapport(MutationsRFProcessorResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportTraiterMutationRF";
		final String description = "Rapport d'exécution du job d'import (traitement des mutations) du RF.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(MutationsRFProcessorRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfMutationsRFProcessorRapport document = new PdfMutationsRFProcessorRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public EnvoiFormulairesDemandeDegrevementICIRapport generateRapport(EnvoiFormulairesDemandeDegrevementICIResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportEnvoiDemandesDegrevementICI";
		final String description = "Rapport d'exécution du job d'envoi en masse des formulaires de demande de dégrèvement ICI.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(EnvoiFormulairesDemandeDegrevementICIRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfEnvoiFormulairesDemandeDegrevementICIRapport document = new PdfEnvoiFormulairesDemandeDegrevementICIRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RappelFormulairesDemandeDegrevementICIRapport generateRapport(RappelFormulairesDemandeDegrevementICIResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportRappelDemandesDegrevementICI";
		final String description = "Rapport d'exécution du job d'envoi en masse des rappels de formulaires de demande de dégrèvement ICI.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RappelFormulairesDemandeDegrevementICIRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRappelFormulairesDemandeDegrevementICIRapport document = new PdfRappelFormulairesDemandeDegrevementICIRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public CleanupRFProcessorRapport generateRapport(CleanupRFProcessorResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportCleanupDonneesRF";
		final String description = "Rapport d'exécution du batch de nettoyage des données d'import du RF.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(CleanupRFProcessorRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfCleanupRFProcessorRapport document = new PdfCleanupRFProcessorRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public InitialisationIFoncRapport generateRapport(InitialisationIFoncResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportInitialisationIFonc";
		final String description = "Rapport d'exécution du batch d'extraction des données nécessaires à l'initialisation de la taxation IFONC.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(InitialisationIFoncRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfInitialisationIFoncRapport document = new PdfInitialisationIFoncRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RattrapageRegimesFiscauxRapport generateRapport(final RattrapageRegimesFiscauxJobResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportRattrapageRegimesFiscaux";
		final String description = "Rapport d'exécution du job de rattrapage des régimes fiscaux.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RattrapageRegimesFiscauxRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRattrapageRegimesFiscauxRapport document = new PdfRattrapageRegimesFiscauxRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ExtractionRegimesFiscauxRapport generateRapport(ExtractionRegimesFiscauxResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportExtractionRegimesFiscaux";
		final String description = "Rapport d'exécution du job d'extraction des régimes fiscaux.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(ExtractionRegimesFiscauxRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfExtractionRegimesFiscauxRapport document = new PdfExtractionRegimesFiscauxRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public MigrationMandatairesSpeciauxRapport generateRapport(MigrationMandatImporterResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportMigrationMandatairesSpeciaux";
		final String description = "Rapport d'exécution du job de migration des mandataires spéciaux.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(MigrationMandatairesSpeciauxRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfMigrationMandatairesSpeciauxRapport document = new PdfMigrationMandatairesSpeciauxRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RattraperDatesMetierDroitProcessorRapport generateRapport(RattraperDatesMetierDroitRFProcessorResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportRattrapageDatesMetierDroitRF";
		final String description = "Rapport d'exécution du job de rattrapage des dates métier des droits RF.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RattraperDatesMetierDroitProcessorRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRattrapageDatesMetierDroitRFRapport document = new PdfRattrapageDatesMetierDroitRFRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RattrapageModelesCommunautesRFProcessorRapport generateRapport(RattrapageModelesCommunautesRFProcessorResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportRattrapageModelesCommunautesRF";
		final String description = "Rapport d'exécution du job de rattrapage des regroupement des communautés sur les modèles de communauté RF.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RattrapageModelesCommunautesRFProcessorRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRattrapageModelesCommunautesRFRapport document = new PdfRattrapageModelesCommunautesRFRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public LienAssociesSNCEnMasseImporterRapport generateRapport(LienAssociesSNCEnMasseImporterResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportImportLiensAssociesEtSNC";
		final String description = "Rapport d'exécution du job d'import des rapports entre associés et les SNC.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(LienAssociesSNCEnMasseImporterRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfLienAssociesSNCEnMasseImporterRapport document = new PdfLienAssociesSNCEnMasseImporterRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RoleSNCRapport generateRapport(RoleSNCResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final String nom = "RoleSNC" + results.annee;
		final String description = String.format("Rapport d'exécution du job d'extraction du rôle SNC %d de l'OIPM", results.annee);
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RoleSNCRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRoleSNCRapport document = new PdfRoleSNCRapport(infraService);
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public EchoirQSNCRapport generateRapport(EchoirQuestionnairesSNCResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportEchoirQSNC" + results.dateTraitement.index();
		final String description = "Rapport d'exécution du job de passage des questionnaires SNC à l'état échu. Date de traitement = " + RegDateHelper.dateToDisplayString(results.dateTraitement) + ".";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(EchoirQSNCRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfEchoirQSNCRapport document = new PdfEchoirQSNCRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ChangementRegimesFiscauxRapport generateRapport(ChangementRegimesFiscauxJobResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportChangementRegimesFiscaux" + results.getAncienType().getCode() + "_" + results.getNouveauType().getCode() + "_" + results.getDateChangement().index();
		final String description = "Rapport d'exécution du job de changement des régimes fiscaux. Date de changement = " + RegDateHelper.dateToDisplayString(results.getDateChangement()) + ".";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(ChangementRegimesFiscauxRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfChangementRegimesFiscauxRapport document = new PdfChangementRegimesFiscauxRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ListeEchangeRenseignementsRapport generateRapport(ListeEchangeRenseignementsResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RappportListeEchangeRenseignements";
		final String description = "Rapport d'exécution du job d'extraction de la liste pour l'EAR d'une période fiscale.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(ListeEchangeRenseignementsRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfListeEchangeRenseignementsRapport document = new PdfListeEchangeRenseignementsRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RattraperEmissionDIPourCyberContexteRapport generateRapport(RattraperEmissionDIPourCyberContexteResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapporRattraperEmissionDIPourCyberContexte" + results.periodeDebut;
		final String description = "Rapport d'exécution du job de réémission des événements de mise-à-disposition des DIs dans le contexte de la cyberfiscalité.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RattraperEmissionDIPourCyberContexteRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfRattraperEmissionDIPourCyberContexteRapport document = new PdfRattraperEmissionDIPourCyberContexteRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public DatabaseIndexationRapport generateRapport(DatabaseIndexationResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapporDatabaseIndexation" + results.getDateTraitement().index();
		final String description = "Rapport d'exécution du job d'indexation des tiers. Date de traitement = " + RegDateHelper.dateToDisplayString(results.getDateTraitement()) + ".";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(DatabaseIndexationRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfDatabaseIndexationRapport document = new PdfDatabaseIndexationRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public AjouterDelaiPourMandataireRapport generateRapport(AjouterDelaiPourMandataireResults results, StatusManager status) {

		final RegDate dateTraitement = RegDate.get();
		final String nom = "RapportAjouterDelaiPourMandataire" + dateTraitement.index();
		final String description = String.format("Rapport d'exécution du traitement d'ajout de délai sur demande des mandataires. Date de traitement = %s.", RegDateHelper.dateToDisplayString(dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(AjouterDelaiPourMandataireRapport.class, nom, description, "pdf", (doc, os) -> {
				PdfAjouterDelaiPourMandataireRapport document = new PdfAjouterDelaiPourMandataireRapport();
				document.write(results, nom, description, dateGeneration, os, status);
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
