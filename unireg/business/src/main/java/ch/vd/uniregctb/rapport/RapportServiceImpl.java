package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.acomptes.AcomptesResults;
import ch.vd.uniregctb.adresse.ResolutionAdresseResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.ordinaire.common.DemandeDelaiCollectiveResults;
import ch.vd.uniregctb.declaration.ordinaire.pm.DeterminationDIsPMResults;
import ch.vd.uniregctb.declaration.ordinaire.pm.EchoirDIsPMResults;
import ch.vd.uniregctb.declaration.ordinaire.pm.EnvoiDIsPMResults;
import ch.vd.uniregctb.declaration.ordinaire.pm.EnvoiSommationsDIsPMResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.DeterminationDIsPPResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.EchoirDIsPPResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.EnvoiAnnexeImmeubleResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.EnvoiDIsPPResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.EnvoiSommationsDIsPPResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImportCodesSegmentResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.ListeDIsPPNonEmises;
import ch.vd.uniregctb.declaration.ordinaire.pp.ListeNoteResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.StatistiquesCtbs;
import ch.vd.uniregctb.declaration.ordinaire.pp.StatistiquesDIs;
import ch.vd.uniregctb.declaration.snc.DeterminationQuestionnairesSNCResults;
import ch.vd.uniregctb.declaration.snc.EnvoiQuestionnairesSNCEnMasseResults;
import ch.vd.uniregctb.declaration.snc.EnvoiRappelsQuestionnairesSNCResults;
import ch.vd.uniregctb.declaration.source.DeterminerLRsEchuesResults;
import ch.vd.uniregctb.declaration.source.EnvoiLRsResults;
import ch.vd.uniregctb.declaration.source.EnvoiSommationLRsResults;
import ch.vd.uniregctb.document.AcomptesRapport;
import ch.vd.uniregctb.document.AnnoncesIDERapport;
import ch.vd.uniregctb.document.AppariementEtablissementsSecondairesRapport;
import ch.vd.uniregctb.document.AssujettiParSubstitutionRapport;
import ch.vd.uniregctb.document.CalculParentesRapport;
import ch.vd.uniregctb.document.ComparerForFiscalEtCommuneRapport;
import ch.vd.uniregctb.document.ComparerSituationFamilleRapport;
import ch.vd.uniregctb.document.CorrectionEtatDeclarationRapport;
import ch.vd.uniregctb.document.CorrectionFlagHabitantRapport;
import ch.vd.uniregctb.document.DemandeDelaiCollectiveRapport;
import ch.vd.uniregctb.document.DeterminationDIsPMRapport;
import ch.vd.uniregctb.document.DeterminationDIsPPRapport;
import ch.vd.uniregctb.document.DeterminationQuestionnairesSNCRapport;
import ch.vd.uniregctb.document.DeterminerLRsEchuesRapport;
import ch.vd.uniregctb.document.DeterminerMouvementsDossiersEnMasseRapport;
import ch.vd.uniregctb.document.DocumentService;
import ch.vd.uniregctb.document.DumpPeriodesImpositionImpotSourceRapport;
import ch.vd.uniregctb.document.EchoirDIsPMRapport;
import ch.vd.uniregctb.document.EchoirDIsPPRapport;
import ch.vd.uniregctb.document.EnvoiAnnexeImmeubleRapport;
import ch.vd.uniregctb.document.EnvoiDIsPMRapport;
import ch.vd.uniregctb.document.EnvoiDIsPPRapport;
import ch.vd.uniregctb.document.EnvoiFormulairesDemandeDegrevementICIRapport;
import ch.vd.uniregctb.document.EnvoiLRsRapport;
import ch.vd.uniregctb.document.EnvoiLettresBienvenueRapport;
import ch.vd.uniregctb.document.EnvoiQuestionnairesSNCRapport;
import ch.vd.uniregctb.document.EnvoiRappelsQuestionnairesSNCRapport;
import ch.vd.uniregctb.document.EnvoiSommationLRsRapport;
import ch.vd.uniregctb.document.EnvoiSommationsDIsPMRapport;
import ch.vd.uniregctb.document.EnvoiSommationsDIsPPRapport;
import ch.vd.uniregctb.document.ExclureContribuablesEnvoiRapport;
import ch.vd.uniregctb.document.ExtractionDonneesRptRapport;
import ch.vd.uniregctb.document.FusionDeCommunesRapport;
import ch.vd.uniregctb.document.IdentifierContribuableFromListeRapport;
import ch.vd.uniregctb.document.IdentifierContribuableRapport;
import ch.vd.uniregctb.document.ImportCodesSegmentRapport;
import ch.vd.uniregctb.document.ImportImmeublesRapport;
import ch.vd.uniregctb.document.ListeAssujettisRapport;
import ch.vd.uniregctb.document.ListeContribuablesResidentsSansForVaudoisRapport;
import ch.vd.uniregctb.document.ListeDIsNonEmisesRapport;
import ch.vd.uniregctb.document.ListeDroitsAccesRapport;
import ch.vd.uniregctb.document.ListeNoteRapport;
import ch.vd.uniregctb.document.ListeTachesEnIsntanceParOIDRapport;
import ch.vd.uniregctb.document.ListesNominativesRapport;
import ch.vd.uniregctb.document.MajoriteRapport;
import ch.vd.uniregctb.document.MigrationDDCsvLoaderRapport;
import ch.vd.uniregctb.document.MutationsRFDetectorRapport;
import ch.vd.uniregctb.document.MutationsRFProcessorRapport;
import ch.vd.uniregctb.document.PassageNouveauxRentiersSourciersEnMixteRapport;
import ch.vd.uniregctb.document.RappelLettresBienvenueRapport;
import ch.vd.uniregctb.document.RapprochementTiersRFRapport;
import ch.vd.uniregctb.document.RapprocherCtbRapport;
import ch.vd.uniregctb.document.RecalculTachesRapport;
import ch.vd.uniregctb.document.RecuperationDonneesAnciensHabitantsRapport;
import ch.vd.uniregctb.document.RecuperationOriginesNonHabitantsRapport;
import ch.vd.uniregctb.document.ReinitialiserBaremeDoubleGainRapport;
import ch.vd.uniregctb.document.ResolutionAdresseRapport;
import ch.vd.uniregctb.document.RolePMCommunesRapport;
import ch.vd.uniregctb.document.RolePMOfficeRapport;
import ch.vd.uniregctb.document.RolePPCommunesRapport;
import ch.vd.uniregctb.document.RolePPOfficesRapport;
import ch.vd.uniregctb.document.RolesCommunesPMRapport;
import ch.vd.uniregctb.document.RolesCommunesPPRapport;
import ch.vd.uniregctb.document.RolesOIDsRapport;
import ch.vd.uniregctb.document.RolesOIPMRapport;
import ch.vd.uniregctb.document.StatistiquesCtbsRapport;
import ch.vd.uniregctb.document.StatistiquesDIsRapport;
import ch.vd.uniregctb.document.StatistiquesEvenementsRapport;
import ch.vd.uniregctb.document.SuppressionOIDRapport;
import ch.vd.uniregctb.document.TraiterEvenementExterneRapport;
import ch.vd.uniregctb.document.ValidationJobRapport;
import ch.vd.uniregctb.documentfiscal.EnvoiLettresBienvenueResults;
import ch.vd.uniregctb.documentfiscal.RappelLettresBienvenueResults;
import ch.vd.uniregctb.droits.ListeDroitsAccesResults;
import ch.vd.uniregctb.evenement.externe.TraiterEvenementExterneResult;
import ch.vd.uniregctb.evenement.ide.AnnonceIDEJobResults;
import ch.vd.uniregctb.foncier.EnvoiFormulairesDemandeDegrevementICIResults;
import ch.vd.uniregctb.foncier.migration.MigrationDDImporterResults;
import ch.vd.uniregctb.identification.contribuable.IdentifierContribuableFromListeResults;
import ch.vd.uniregctb.identification.contribuable.IdentifierContribuableResults;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.listes.afc.ExtractionDonneesRptResults;
import ch.vd.uniregctb.listes.assujettis.AssujettisParSubstitutionResults;
import ch.vd.uniregctb.listes.assujettis.ListeAssujettisResults;
import ch.vd.uniregctb.listes.listesnominatives.ListesNominativesResults;
import ch.vd.uniregctb.listes.suisseoupermiscresident.ListeContribuablesResidentsSansForVaudoisResults;
import ch.vd.uniregctb.metier.ComparerForFiscalEtCommuneResults;
import ch.vd.uniregctb.metier.FusionDeCommunesResults;
import ch.vd.uniregctb.metier.OuvertureForsResults;
import ch.vd.uniregctb.metier.PassageNouveauxRentiersSourciersEnMixteResults;
import ch.vd.uniregctb.metier.piis.DumpPeriodesImpositionImpotSourceResults;
import ch.vd.uniregctb.mouvement.DeterminerMouvementsDossiersEnMasseResults;
import ch.vd.uniregctb.oid.SuppressionOIDResults;
import ch.vd.uniregctb.parentes.CalculParentesResults;
import ch.vd.uniregctb.registrefoncier.dataimport.MutationsRFDetectorResults;
import ch.vd.uniregctb.registrefoncier.dataimport.MutationsRFProcessorResults;
import ch.vd.uniregctb.registrefoncier.processor.RapprochementTiersRFResults;
import ch.vd.uniregctb.rf.ImportImmeublesResults;
import ch.vd.uniregctb.rf.RapprocherCtbResults;
import ch.vd.uniregctb.role.RolePMCommunesResults;
import ch.vd.uniregctb.role.RolePMOfficeResults;
import ch.vd.uniregctb.role.RolePPCommunesResults;
import ch.vd.uniregctb.role.RolePPOfficesResults;
import ch.vd.uniregctb.role.before2016.ProduireRolesOIDsResults;
import ch.vd.uniregctb.role.before2016.ProduireRolesOIPMResults;
import ch.vd.uniregctb.role.before2016.ProduireRolesPMCommunesResults;
import ch.vd.uniregctb.role.before2016.ProduireRolesPPCommunesResults;
import ch.vd.uniregctb.situationfamille.ComparerSituationFamilleResults;
import ch.vd.uniregctb.situationfamille.ReinitialiserBaremeDoubleGainResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsCivilsOrganisationsResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsCivilsPersonnesResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsExternesResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsIdentificationContribuableResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsNotairesResults;
import ch.vd.uniregctb.tache.ListeTachesEnInstanceParOID;
import ch.vd.uniregctb.tache.TacheSyncResults;
import ch.vd.uniregctb.tiers.ExclureContribuablesEnvoiResults;
import ch.vd.uniregctb.tiers.rattrapage.ancienshabitants.RecuperationDonneesAnciensHabitantsResults;
import ch.vd.uniregctb.tiers.rattrapage.appariement.AppariementEtablissementsSecondairesResults;
import ch.vd.uniregctb.tiers.rattrapage.etatdeclaration.CorrectionEtatDeclarationResults;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantResults;
import ch.vd.uniregctb.tiers.rattrapage.origine.RecuperationOriginesNonHabitantsResults;
import ch.vd.uniregctb.validation.ValidationJobResults;

/**
 * {@inheritDoc}
 */
public class RapportServiceImpl implements RapportService, ApplicationContextAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(RapportServiceImpl.class);

	private DocumentService docService;
	private ServiceInfrastructureService infraService;
	private ApplicationContext applicationContext;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDocService(DocumentService docService) {
		this.docService = docService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DeterminationDIsPPRapport generateRapport(final DeterminationDIsPPResults results, StatusManager s) throws DeclarationException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportDetermDIs" + results.annee;
		final String description = String.format("Rapport du job de détermination des DIs PP à émettre pour l'année %d. Date de traitement = %s",
		                                         results.annee,
		                                         RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(DeterminationDIsPPRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<DeterminationDIsPPRapport>() {
				@Override
				public void writeDoc(DeterminationDIsPPRapport doc, OutputStream os) throws Exception {
					final PdfDeterminationDIsPPRapport document = new PdfDeterminationDIsPPRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(DeterminationDIsPMRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<DeterminationDIsPMRapport>() {
				@Override
				public void writeDoc(DeterminationDIsPMRapport doc, OutputStream os) throws Exception {
					final PdfDeterminationDIsPMRapport document = new PdfDeterminationDIsPMRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new DeclarationException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EnvoiDIsPPRapport generateRapport(final EnvoiDIsPPResults results, StatusManager s) throws DeclarationException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportEnvoiDIsPP" + results.annee;
		final String description = String.format("Rapport d'exécution du job d'envoi des DIs PP en masse pour l'année %d. Date de traitement = %s. Type de contribuable = %s",
				                                 results.annee, RegDateHelper.dateToDisplayString(results.dateTraitement), results.categorie.name());
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(EnvoiDIsPPRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<EnvoiDIsPPRapport>() {
				@Override
				public void writeDoc(EnvoiDIsPPRapport doc, OutputStream os) throws Exception {
					final PdfEnvoiDIsPPRapport document = new PdfEnvoiDIsPPRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(EnvoiDIsPMRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<EnvoiDIsPMRapport>() {
				@Override
				public void writeDoc(EnvoiDIsPMRapport doc, OutputStream os) throws Exception {
					final PdfEnvoiDIsPMRapport document = new PdfEnvoiDIsPMRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(EnvoiAnnexeImmeubleRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<EnvoiAnnexeImmeubleRapport>() {
				@Override
				public void writeDoc(EnvoiAnnexeImmeubleRapport doc, OutputStream os) throws Exception {
					PdfEnvoiAnnexeImmeubleRapport document = new PdfEnvoiAnnexeImmeubleRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(ListeDIsNonEmisesRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ListeDIsNonEmisesRapport>() {
				@Override
				public void writeDoc(ListeDIsNonEmisesRapport doc, OutputStream os) throws Exception {
					PdfListeDIsNonEmisesRapport document = new PdfListeDIsNonEmisesRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MajoriteRapport generateRapport(final OuvertureForsResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportMajorite" + results.dateTraitement.index();
		final String description = String.format("Rapport d'exécution du job d'ouverture des fors des contribuables majeurs.. Date de traitement = %s",
		                                         RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(MajoriteRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<MajoriteRapport>() {
				@Override
				public void writeDoc(MajoriteRapport doc, OutputStream os) throws Exception {
					PdfMajoriteRapport document = new PdfMajoriteRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FusionDeCommunesRapport generateRapport(final FusionDeCommunesResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "FusionDeCommunes" + results.dateTraitement.index();
		final String description = String.format("Rapport d'exécution du job de fusion de communes.. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(FusionDeCommunesRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<FusionDeCommunesRapport>() {
				@Override
				public void writeDoc(FusionDeCommunesRapport doc, OutputStream os) throws Exception {
					PdfFusionDeCommunesRapport document = new PdfFusionDeCommunesRapport(infraService);
					document.write(results, nom, description, dateGeneration, os, status);
				}
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RolesCommunesPPRapport generateRapport(final ProduireRolesPPCommunesResults results, final StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RolesCommunesPP" + results.dateTraitement.index();
		final String description = String.format("Rapport des rôles PP pour les communes.. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RolesCommunesPPRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<RolesCommunesPPRapport>() {
				@Override
				public void writeDoc(RolesCommunesPPRapport doc, OutputStream os) throws Exception {
					final PdfRolesPPCommunesRapport document = new PdfRolesPPCommunesRapport(infraService);
					document.write(results, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RolesCommunesPMRapport generateRapport(final ProduireRolesPMCommunesResults results, final StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RolesCommunesPM" + results.dateTraitement.index();
		final String description = String.format("Rapport des rôles PM pour les communes.. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RolesCommunesPMRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<RolesCommunesPMRapport>() {
				@Override
				public void writeDoc(RolesCommunesPMRapport doc, OutputStream os) throws Exception {
					final PdfRolesPMCommunesRapport document = new PdfRolesPMCommunesRapport(infraService);
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(RolesOIDsRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<RolesOIDsRapport>() {
				@Override
				public void writeDoc(RolesOIDsRapport doc, OutputStream os) throws Exception {
					final PdfRolesOIDsRapport document = new PdfRolesOIDsRapport(infraService);
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(RolesOIPMRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<RolesOIPMRapport>() {
				@Override
				public void writeDoc(RolesOIPMRapport doc, OutputStream os) throws Exception {
					final PdfRolesOIPMRapport document = new PdfRolesOIPMRapport(infraService);
					document.write(results, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public StatistiquesDIsRapport generateRapport(final StatistiquesDIs results, final StatusManager status) {
		final String nom = "RapportStatsDIs" + results.dateTraitement.index();
		final String description = String.format("Rapport des statistiques des déclarations d'impôt ordinaires.. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(StatistiquesDIsRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<StatistiquesDIsRapport>() {
				@Override
				public void writeDoc(StatistiquesDIsRapport doc, OutputStream os) throws Exception {
					PdfStatsDIsRapport document = new PdfStatsDIsRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public StatistiquesCtbsRapport generateRapport(final StatistiquesCtbs results, final StatusManager status) {
		final String nom = "RapportStatsCtbs" + results.dateTraitement.index();
		final String description = String.format("Rapport des statistiques des contribuables assujettis.. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(StatistiquesCtbsRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<StatistiquesCtbsRapport>() {
				@Override
				public void writeDoc(StatistiquesCtbsRapport doc, OutputStream os) throws Exception {
					PdfStatsCtbsRapport document = new PdfStatsCtbsRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(EnvoiSommationsDIsPPRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<EnvoiSommationsDIsPPRapport>() {
				@Override
				public void writeDoc(EnvoiSommationsDIsPPRapport doc, OutputStream os) throws Exception {
					PdfEnvoiSommationsDIsPPRapport document = new PdfEnvoiSommationsDIsPPRapport();
					document.write(results, nom, description, dateGeneration, os, statusManager);

				}
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
			return docService.newDoc(EnvoiSommationsDIsPMRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<EnvoiSommationsDIsPMRapport>() {
				@Override
				public void writeDoc(EnvoiSommationsDIsPMRapport doc, OutputStream os) throws Exception {
					final PdfEnvoiSommationsDIsPMRapport document = new PdfEnvoiSommationsDIsPMRapport();
					document.write(results, nom, description, dateGeneration, os, statusManager);

				}
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
			return docService.newDoc(ValidationJobRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ValidationJobRapport>() {
				@Override
				public void writeDoc(ValidationJobRapport doc, OutputStream os) throws Exception {
					PdfValidationRapport document = new PdfValidationRapport();
					document.write(results, nom, description, dateGeneration, os, statusManager);
				}
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
			return docService.newDoc(EnvoiLRsRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<EnvoiLRsRapport>() {
				@Override
				public void writeDoc(EnvoiLRsRapport doc, OutputStream os) throws Exception {
					PdfEnvoiLRsRapport document = new PdfEnvoiLRsRapport();
					document.write(results, nom, description, dateGeneration, os, statusManager);
				}
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
			return docService.newDoc(EnvoiSommationLRsRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<EnvoiSommationLRsRapport>() {
				@Override
				public void writeDoc(EnvoiSommationLRsRapport doc, OutputStream os) throws Exception {
					PdfEnvoiSommationsLRsRapport document = new PdfEnvoiSommationsLRsRapport();
					document.write(results, nom, description, dateGeneration, os, statusManager);
				}
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
			return docService.newDoc(ListesNominativesRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ListesNominativesRapport>() {
				@Override
				public void writeDoc(ListesNominativesRapport doc, OutputStream os) throws Exception {
					final PdfListesNominativesRapport document = new PdfListesNominativesRapport();
					document.write(results, nom, description, dateGeneration, os, statusManager);
				}
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
			return docService.newDoc(AcomptesRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<AcomptesRapport>() {
				@Override
				public void writeDoc(AcomptesRapport doc, OutputStream os) throws Exception {
					final PdfAcomptesRapport document = new PdfAcomptesRapport();
					document.write(results, nom, description, dateGeneration, os, statusManager);
				}
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
			return docService.newDoc(ExtractionDonneesRptRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ExtractionDonneesRptRapport>() {
				@Override
				public void writeDoc(ExtractionDonneesRptRapport doc, OutputStream os) throws Exception {
					final PdfExtractionDonneesRptRapport document = new PdfExtractionDonneesRptRapport();
					document.write(results, nom, description, dateGeneration, os, statusManager);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EchoirDIsPPRapport generateRapport(final EchoirDIsPPResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportEchoirDIsPP" + results.dateTraitement.index();
		final String description = String.format("Rapport d'exécution du job de passage des DIs PP sommées à l'état échu. Date de traitement = %s.",
		                                         RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(EchoirDIsPPRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<EchoirDIsPPRapport>() {
				@Override
				public void writeDoc(EchoirDIsPPRapport doc, OutputStream os) throws Exception {
					final PdfEchoirDIsPPRapport document = new PdfEchoirDIsPPRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(EchoirDIsPMRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<EchoirDIsPMRapport>() {
				@Override
				public void writeDoc(EchoirDIsPMRapport doc, OutputStream os) throws Exception {
					final PdfEchoirDIsPMRapport document = new PdfEchoirDIsPMRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ReinitialiserBaremeDoubleGainRapport generateRapport(final ReinitialiserBaremeDoubleGainResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "ReinitDoubleGain" + results.dateTraitement.index();
		final String description = String.format("Rapport d'exécution du job de réinitialisation des barèmes double-gain. Date de traitement = %s.",
				RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(ReinitialiserBaremeDoubleGainRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ReinitialiserBaremeDoubleGainRapport>() {
				@Override
				public void writeDoc(ReinitialiserBaremeDoubleGainRapport doc, OutputStream os) throws Exception {
					PdfReinitDoubleGainRapport document = new PdfReinitDoubleGainRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(ListeTachesEnIsntanceParOIDRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ListeTachesEnIsntanceParOIDRapport>() {
				@Override
				public void writeDoc(ListeTachesEnIsntanceParOIDRapport doc, OutputStream os) throws Exception {
					PdfListeTacheEnInstanceParOIDRapport document = new PdfListeTacheEnInstanceParOIDRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(ExclureContribuablesEnvoiRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ExclureContribuablesEnvoiRapport>() {
				@Override
				public void writeDoc(ExclureContribuablesEnvoiRapport doc, OutputStream os) throws Exception {
					PdfExclureContribuablesEnvoiRapport document = new PdfExclureContribuablesEnvoiRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DemandeDelaiCollectiveRapport generateRapport(final DemandeDelaiCollectiveResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final RegDate dateTraitement = RegDate.get();
		final String nom = "RapportDemDelaiColl" + dateTraitement.index();
		final String description = String.format("Rapport d'exécution du traitement d'une demande de délais collective. Date de traitement = %s.", RegDateHelper.dateToDisplayString(dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(DemandeDelaiCollectiveRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<DemandeDelaiCollectiveRapport>() {
				@Override
				public void writeDoc(DemandeDelaiCollectiveRapport doc, OutputStream os) throws Exception {
					PdfDemandeDelaiCollectiveRapport document = new PdfDemandeDelaiCollectiveRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RapprocherCtbRapport generateRapport(final RapprocherCtbResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportRapprochementCtbs";
		final String description = String.format("Rapport d'exécution du job qui fait le rappochement entre les contribuables et les propriétaires fonciers. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RapprocherCtbRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<RapprocherCtbRapport>() {
				@Override
				public void writeDoc(RapprocherCtbRapport doc, OutputStream os) throws Exception {
					PdfRapprochementCtbRapport document = new PdfRapprochementCtbRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
	           return docService.newDoc(ListeContribuablesResidentsSansForVaudoisRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ListeContribuablesResidentsSansForVaudoisRapport>() {
				   @Override
				   public void writeDoc(ListeContribuablesResidentsSansForVaudoisRapport doc, OutputStream os) throws Exception {
					   final PdfListeContribuablesResidentsSansForVaudoisRapport document = new PdfListeContribuablesResidentsSansForVaudoisRapport();
					   document.write(results, nom, description, dateGeneration, os, status);
				   }
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
			return docService.newDoc(CorrectionFlagHabitantRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<CorrectionFlagHabitantRapport>() {
				@Override
				public void writeDoc(CorrectionFlagHabitantRapport doc, OutputStream os) throws Exception {
					final PdfCorrectionFlagHabitantRapport document = new PdfCorrectionFlagHabitantRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public StatistiquesEvenementsRapport generateRapport(final StatsEvenementsCivilsPersonnesResults civilsPersonnes,
	                                                     final StatsEvenementsCivilsOrganisationsResults civilsOrganisations,
	                                                     final StatsEvenementsExternesResults externes,
	                                                     final StatsEvenementsIdentificationContribuableResults identCtb,
	                                                     final StatsEvenementsNotairesResults notaires,
	                                                     final RegDate dateReference, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportStatsEvenements";
		final String description = String.format("Statistiques des événements reçus par Unireg. Date de traitement = %s", RegDateHelper.dateToDisplayString(RegDate.get()));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(StatistiquesEvenementsRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<StatistiquesEvenementsRapport>() {
				@Override
				public void writeDoc(StatistiquesEvenementsRapport doc, OutputStream os) throws Exception {
					final PdfStatistiquesEvenementsRapport document = new PdfStatistiquesEvenementsRapport();
					document.write(civilsPersonnes, civilsOrganisations, externes, identCtb, notaires, dateReference, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(DeterminerMouvementsDossiersEnMasseRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<DeterminerMouvementsDossiersEnMasseRapport>() {
				@Override
				public void writeDoc(DeterminerMouvementsDossiersEnMasseRapport doc, OutputStream os) throws Exception {
					final PdfDeterminerMouvementsDossiersEnMasseRapport document = new PdfDeterminerMouvementsDossiersEnMasseRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(DeterminerLRsEchuesRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<DeterminerLRsEchuesRapport>() {
				@Override
				public void writeDoc(DeterminerLRsEchuesRapport doc, OutputStream os) throws Exception {
					final PdfDeterminerLRsEchuesRapport document = new PdfDeterminerLRsEchuesRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
				return docService.newDoc(IdentifierContribuableRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<IdentifierContribuableRapport>() {
					@Override
					public void writeDoc(IdentifierContribuableRapport doc, OutputStream os) throws Exception {
						final PdfIdentifierContribuableRapport document = new PdfIdentifierContribuableRapport();
						document.write(results, nom, description, dateGeneration, os, status);
					}
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
				return docService.newDoc(TraiterEvenementExterneRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<TraiterEvenementExterneRapport>() {
					@Override
					public void writeDoc(TraiterEvenementExterneRapport doc, OutputStream os) throws Exception {
						final PdfTraiterEvenementExterneRapport document = new PdfTraiterEvenementExterneRapport();
						document.write(results, nom, description, dateGeneration, os, status);
					}
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
			return docService.newDoc(ResolutionAdresseRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ResolutionAdresseRapport>() {
				@Override
				public void writeDoc(ResolutionAdresseRapport doc, OutputStream os) throws Exception {
					final PdfResolutionAdresseRapport document = new PdfResolutionAdresseRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(ComparerSituationFamilleRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ComparerSituationFamilleRapport>() {
				@Override
				public void writeDoc(ComparerSituationFamilleRapport doc, OutputStream os) throws Exception {
					final PdfComparerSituationFamilleRapport document = new PdfComparerSituationFamilleRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(ListeNoteRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ListeNoteRapport>() {
				@Override
				public void writeDoc(ListeNoteRapport doc, OutputStream os) throws Exception {
					final PdfListeNoteRapport document = new PdfListeNoteRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(ComparerForFiscalEtCommuneRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ComparerForFiscalEtCommuneRapport>() {
				@Override
				public void writeDoc(ComparerForFiscalEtCommuneRapport doc, OutputStream os) throws Exception {
					final PdfComparerForFiscalEtCommuneRapport document = new PdfComparerForFiscalEtCommuneRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(CorrectionEtatDeclarationRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<CorrectionEtatDeclarationRapport>() {
				@Override
				public void writeDoc(CorrectionEtatDeclarationRapport doc, OutputStream os) throws Exception {
					final PdfCorrectionEtatDeclarationRapport document = new PdfCorrectionEtatDeclarationRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(ListeAssujettisRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ListeAssujettisRapport>() {
				@Override
				public void writeDoc(ListeAssujettisRapport doc, OutputStream os) throws Exception {
					final PdfListeAssujettisRapport document = new PdfListeAssujettisRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(ImportCodesSegmentRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ImportCodesSegmentRapport>() {
				@Override
				public void writeDoc(ImportCodesSegmentRapport doc, OutputStream os) throws Exception {
					final PdfImportCodesSegmentRapport document = new PdfImportCodesSegmentRapport();
					document.write(results, nbLignesLuesFichierEntree, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ImportImmeublesRapport generateRapport(final ImportImmeublesResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportImportImmeubles";
		final String description = "Rapport d'exécution du job d'importation des immeubles du registre foncier.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(ImportImmeublesRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ImportImmeublesRapport>() {
				@Override
				public void writeDoc(ImportImmeublesRapport doc, OutputStream os) throws Exception {
					final PdfImportImmeublesRapport document = new PdfImportImmeublesRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(ListeDroitsAccesRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ListeDroitsAccesRapport>() {
				@Override
				public void writeDoc(ListeDroitsAccesRapport doc, OutputStream os) throws Exception {
					final PdfListeDroitsAccesRapport document = new PdfListeDroitsAccesRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(SuppressionOIDRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<SuppressionOIDRapport>() {
				@Override
				public void writeDoc(SuppressionOIDRapport doc, OutputStream os) throws Exception {
					final PdfSuppressionOIDRapport document = new PdfSuppressionOIDRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(PassageNouveauxRentiersSourciersEnMixteRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<PassageNouveauxRentiersSourciersEnMixteRapport>() {
				@Override
				public void writeDoc(PassageNouveauxRentiersSourciersEnMixteRapport doc, OutputStream os) throws Exception {
					final PdfPassageNouveauxRentiersSourciersEnMixteRapport document = new PdfPassageNouveauxRentiersSourciersEnMixteRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(RecalculTachesRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<RecalculTachesRapport>() {
				@Override
				public void writeDoc(RecalculTachesRapport doc, OutputStream os) throws Exception {
					final PdfRecalculTachesRapport document = new PdfRecalculTachesRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(AnnoncesIDERapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<AnnoncesIDERapport>() {
				@Override
				public void writeDoc(AnnoncesIDERapport doc, OutputStream os) throws Exception {
					final PdfAnnoncesIDERapport document = new PdfAnnoncesIDERapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(CalculParentesRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<CalculParentesRapport>() {
				@Override
				public void writeDoc(CalculParentesRapport doc, OutputStream os) throws Exception {
					final PdfCalculParentesRapport document = new PdfCalculParentesRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(DumpPeriodesImpositionImpotSourceRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<DumpPeriodesImpositionImpotSourceRapport>() {
				@Override
				public void writeDoc(DumpPeriodesImpositionImpotSourceRapport doc, OutputStream os) throws Exception {
					final PdfDumpPeriodesImpositionImpotSourceRapport document = new PdfDumpPeriodesImpositionImpotSourceRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RecuperationDonneesAnciensHabitantsRapport generateRapport(final RecuperationDonneesAnciensHabitantsResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportRecuperationNomsParentsAnciensHabitants";
		final String description = "Rapport d'exécution du job de récupération des noms/prénoms des anciens habitants depuis les données civiles";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RecuperationDonneesAnciensHabitantsRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<RecuperationDonneesAnciensHabitantsRapport>() {
				@Override
				public void writeDoc(RecuperationDonneesAnciensHabitantsRapport doc, OutputStream os) throws Exception {
					final PdfRecuperationDonneesAnciensHabitantsRapport document = new PdfRecuperationDonneesAnciensHabitantsRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RecuperationOriginesNonHabitantsRapport generateRapport(final RecuperationOriginesNonHabitantsResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportRecuperationOriginesNonHabitants";
		final String description = "Rapport d'exécution du job de récupération des origines des non-habitants";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RecuperationOriginesNonHabitantsRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<RecuperationOriginesNonHabitantsRapport>() {
				@Override
				public void writeDoc(RecuperationOriginesNonHabitantsRapport doc, OutputStream os) throws Exception {
					final PdfRecuperationOriginesNonHabitantsRapport document = new PdfRecuperationOriginesNonHabitantsRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(AssujettiParSubstitutionRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<AssujettiParSubstitutionRapport>() {
				@Override
				public void writeDoc(AssujettiParSubstitutionRapport doc, OutputStream os) throws Exception {
					final PdfAssujettistParSubstitutionRapport document = new PdfAssujettistParSubstitutionRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(EnvoiLettresBienvenueRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<EnvoiLettresBienvenueRapport>() {
				@Override
				public void writeDoc(EnvoiLettresBienvenueRapport doc, OutputStream os) throws Exception {
					final PdfEnvoiLettresBienvenueRapport document = new PdfEnvoiLettresBienvenueRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(RappelLettresBienvenueRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<RappelLettresBienvenueRapport>() {
				@Override
				public void writeDoc(RappelLettresBienvenueRapport doc, OutputStream os) throws Exception {
					final PdfRappelLettresBienvenueRapport document = new PdfRappelLettresBienvenueRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(DeterminationQuestionnairesSNCRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<DeterminationQuestionnairesSNCRapport>() {
				@Override
				public void writeDoc(DeterminationQuestionnairesSNCRapport doc, OutputStream os) throws Exception {
					final PdfDeterminationQuestionnairesSNCRapport document = new PdfDeterminationQuestionnairesSNCRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(EnvoiQuestionnairesSNCRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<EnvoiQuestionnairesSNCRapport>() {
				@Override
				public void writeDoc(EnvoiQuestionnairesSNCRapport doc, OutputStream os) throws Exception {
					final PdfEnvoiQuestionnairesSNCRapport document = new PdfEnvoiQuestionnairesSNCRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(EnvoiRappelsQuestionnairesSNCRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<EnvoiRappelsQuestionnairesSNCRapport>() {
				@Override
				public void writeDoc(EnvoiRappelsQuestionnairesSNCRapport doc, OutputStream os) throws Exception {
					final PdfEnvoiRappelsQuestionnairesSNCRapport document = new PdfEnvoiRappelsQuestionnairesSNCRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(AppariementEtablissementsSecondairesRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<AppariementEtablissementsSecondairesRapport>() {
				@Override
				public void writeDoc(AppariementEtablissementsSecondairesRapport doc, OutputStream os) throws Exception {
					final PdfAppariementEtablissementsSecondairesRapport document = new PdfAppariementEtablissementsSecondairesRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(IdentifierContribuableFromListeRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<IdentifierContribuableFromListeRapport>() {
				@Override
				public void writeDoc(IdentifierContribuableFromListeRapport doc, OutputStream os) throws Exception {
					final PdfIdentifierContribuableFromListeRapport document = new PdfIdentifierContribuableFromListeRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
			return docService.newDoc(RapprochementTiersRFRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<RapprochementTiersRFRapport>() {
				@Override
				public void writeDoc(RapprochementTiersRFRapport doc, OutputStream os) throws Exception {
					final PdfRapprochementTiersRFRapport document = new PdfRapprochementTiersRFRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
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
	public MigrationDDCsvLoaderRapport generateRapport(MigrationDDImporterResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportMigrationDemandesDegrevement";
		final String description = "Rapport d'exécution du job de migration des demandes de dégrèvement de SIMPA-PM.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(MigrationDDCsvLoaderRapport.class, nom, description, "pdf", (doc, os) -> {
				final PdfMigrationDemandesDegrevementRapport document = new PdfMigrationDemandesDegrevementRapport();
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
}
