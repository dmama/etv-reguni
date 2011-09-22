package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.acomptes.AcomptesResults;
import ch.vd.uniregctb.adresse.ResolutionAdresseResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.ListeNoteResults;
import ch.vd.uniregctb.declaration.ordinaire.DemandeDelaiCollectiveResults;
import ch.vd.uniregctb.declaration.ordinaire.DeterminationDIsResults;
import ch.vd.uniregctb.declaration.ordinaire.EchoirDIsResults;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiAnnexeImmeubleResults;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiDIsResults;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiSommationsDIsResults;
import ch.vd.uniregctb.declaration.ordinaire.ImportCodesSegmentResults;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionChemisesTOResults;
import ch.vd.uniregctb.declaration.ordinaire.ListeDIsNonEmises;
import ch.vd.uniregctb.declaration.ordinaire.StatistiquesCtbs;
import ch.vd.uniregctb.declaration.ordinaire.StatistiquesDIs;
import ch.vd.uniregctb.declaration.source.DeterminerLRsEchuesResults;
import ch.vd.uniregctb.declaration.source.EnvoiLRsResults;
import ch.vd.uniregctb.declaration.source.EnvoiSommationLRsResults;
import ch.vd.uniregctb.document.AcomptesRapport;
import ch.vd.uniregctb.document.ComparerForFiscalEtCommuneRapport;
import ch.vd.uniregctb.document.ComparerSituationFamilleRapport;
import ch.vd.uniregctb.document.CorrectionEtatDeclarationRapport;
import ch.vd.uniregctb.document.CorrectionFlagHabitantRapport;
import ch.vd.uniregctb.document.DemandeDelaiCollectiveRapport;
import ch.vd.uniregctb.document.DeterminationDIsRapport;
import ch.vd.uniregctb.document.DeterminerLRsEchuesRapport;
import ch.vd.uniregctb.document.DeterminerMouvementsDossiersEnMasseRapport;
import ch.vd.uniregctb.document.DocumentService;
import ch.vd.uniregctb.document.EchoirDIsRapport;
import ch.vd.uniregctb.document.EnvoiAnnexeImmeubleRapport;
import ch.vd.uniregctb.document.EnvoiDIsRapport;
import ch.vd.uniregctb.document.EnvoiLRsRapport;
import ch.vd.uniregctb.document.EnvoiSommationLRsRapport;
import ch.vd.uniregctb.document.EnvoiSommationsDIsRapport;
import ch.vd.uniregctb.document.ExclureContribuablesEnvoiRapport;
import ch.vd.uniregctb.document.ExtractionDonneesRptRapport;
import ch.vd.uniregctb.document.FusionDeCommunesRapport;
import ch.vd.uniregctb.document.IdentifierContribuableRapport;
import ch.vd.uniregctb.document.ImportCodesSegmentRapport;
import ch.vd.uniregctb.document.ImpressionChemisesTORapport;
import ch.vd.uniregctb.document.ListeAssujettisRapport;
import ch.vd.uniregctb.document.ListeContribuablesResidentsSansForVaudoisRapport;
import ch.vd.uniregctb.document.ListeDIsNonEmisesRapport;
import ch.vd.uniregctb.document.ListeNoteRapport;
import ch.vd.uniregctb.document.ListeTachesEnIsntanceParOIDRapport;
import ch.vd.uniregctb.document.ListesNominativesRapport;
import ch.vd.uniregctb.document.MajoriteRapport;
import ch.vd.uniregctb.document.MigrationCoquillesPMRapport;
import ch.vd.uniregctb.document.RapprocherCtbRapport;
import ch.vd.uniregctb.document.ReinitialiserBaremeDoubleGainRapport;
import ch.vd.uniregctb.document.ResolutionAdresseRapport;
import ch.vd.uniregctb.document.RolesCommunesRapport;
import ch.vd.uniregctb.document.RolesOIDsRapport;
import ch.vd.uniregctb.document.StatistiquesCtbsRapport;
import ch.vd.uniregctb.document.StatistiquesDIsRapport;
import ch.vd.uniregctb.document.StatistiquesEvenementsRapport;
import ch.vd.uniregctb.document.TraiterEvenementExterneRapport;
import ch.vd.uniregctb.document.ValidationJobRapport;
import ch.vd.uniregctb.evenement.externe.TraiterEvenementExterneResult;
import ch.vd.uniregctb.identification.contribuable.IdentifierContribuableResults;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.listes.afc.ExtractionDonneesRptResults;
import ch.vd.uniregctb.listes.assujettis.ListeAssujettisResults;
import ch.vd.uniregctb.listes.listesnominatives.ListesNominativesResults;
import ch.vd.uniregctb.listes.suisseoupermiscresident.ListeContribuablesResidentsSansForVaudoisResults;
import ch.vd.uniregctb.metier.ComparerForFiscalEtCommuneResults;
import ch.vd.uniregctb.metier.FusionDeCommunesResults;
import ch.vd.uniregctb.metier.OuvertureForsResults;
import ch.vd.uniregctb.mouvement.DeterminerMouvementsDossiersEnMasseResults;
import ch.vd.uniregctb.registrefoncier.RapprocherCtbResults;
import ch.vd.uniregctb.role.ProduireRolesCommunesResults;
import ch.vd.uniregctb.role.ProduireRolesOIDsResults;
import ch.vd.uniregctb.situationfamille.ComparerSituationFamilleResults;
import ch.vd.uniregctb.situationfamille.ReinitialiserBaremeDoubleGainResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsCivilsResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsExternesResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsIdentificationContribuableResults;
import ch.vd.uniregctb.tache.ListeTachesEnInstanceParOID;
import ch.vd.uniregctb.tiers.ExclureContribuablesEnvoiResults;
import ch.vd.uniregctb.tiers.rattrapage.etatdeclaration.CorrectionEtatDeclarationResults;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantSurMenagesResults;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantSurPersonnesPhysiquesResults;
import ch.vd.uniregctb.tiers.rattrapage.pm.MigrationCoquillesPM;
import ch.vd.uniregctb.validation.ValidationJobResults;

/**
 * {@inheritDoc}
 */
public class RapportServiceImpl implements RapportService {

	private static final Logger LOGGER = Logger.getLogger(RapportServiceImpl.class);

	private DocumentService docService;
	private ServiceInfrastructureService infraService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDocService(DocumentService docService) {
		this.docService = docService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DeterminationDIsRapport generateRapport(final DeterminationDIsResults results, StatusManager s) throws DeclarationException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportDetermDIs" + results.annee;
		final String description = String.format("Rapport du job de détermination des DIs à émettre pour l'année %d. Date de traitement = %s", results.annee, RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(DeterminationDIsRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<DeterminationDIsRapport>() {
				@Override
				public void writeDoc(DeterminationDIsRapport doc, OutputStream os) throws Exception {
					PdfDeterminationDIsRapport document = new PdfDeterminationDIsRapport();
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
	public EnvoiDIsRapport generateRapport(final EnvoiDIsResults results, StatusManager s) throws DeclarationException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportEnvoiDIs" + results.annee;
		final String description = String.format("Rapport d'exécution du job d'envoi des DIs en masse pour l'année %d. Date de traitement = %s. Type de contribuable = %s",
				                                 results.annee, RegDateHelper.dateToDisplayString(results.dateTraitement), results.categorie.name());
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(EnvoiDIsRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<EnvoiDIsRapport>() {
				@Override
				public void writeDoc(EnvoiDIsRapport doc, OutputStream os) throws Exception {
					PdfEnvoiDIsRapport document = new PdfEnvoiDIsRapport();
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
	public ListeDIsNonEmisesRapport generateRapport(final ListeDIsNonEmises results, final StatusManager status) {
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
		final String description = String.format("Rapport d'exécution du job d'ouverture des fors des contribuables majeurs.. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RolesCommunesRapport generateRapport(final ProduireRolesCommunesResults results, final StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RolesCommunes" + results.dateTraitement.index();
		final String description = String.format("Rapport des rôles pour les communes.. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(RolesCommunesRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<RolesCommunesRapport>() {
				@Override
				public void writeDoc(RolesCommunesRapport doc, OutputStream os) throws Exception {
					final PdfRolesCommunesRapport document = new PdfRolesCommunesRapport(infraService);
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
	public EnvoiSommationsDIsRapport generateRapport(final EnvoiSommationsDIsResults results, final StatusManager statusManager) {
		final String nom = "RapportSommationDI" + results.getDateTraitement().index();
		final String description = String.format("Rapport de l'envoi de sommation des DIs. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
		final Date dateGeneration = DateHelper.getCurrentDate();
		try {
			return docService.newDoc(EnvoiSommationsDIsRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<EnvoiSommationsDIsRapport>() {
				@Override
				public void writeDoc(EnvoiSommationsDIsRapport doc, OutputStream os) throws Exception {
					PdfEnvoiSommationsDIsRapport document = new PdfEnvoiSommationsDIsRapport();
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
	 * Genère le rapport (PDF) pour les impressions en masse des chemises de taxation d'office
	 *
	 * @param results le résultat de l'exécution du job
	 * @return le rapport
	 */
	@Override
	public ImpressionChemisesTORapport generateRapport(final ImpressionChemisesTOResults results, final StatusManager statusManager) {
		final String nom = "RapportChemisesTO" + results.getDateTraitement().index();
		final String description = String.format("Rapport de l'impression des chemises de taxation d'office au %s.", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
		final Date dateGeneration = DateHelper.getCurrentDate();
		try {
			return docService.newDoc(ImpressionChemisesTORapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ImpressionChemisesTORapport>() {
				@Override
				public void writeDoc(ImpressionChemisesTORapport doc, OutputStream os) throws Exception {
					final PdfImpressionChemisesTORapport document = new PdfImpressionChemisesTORapport();
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
	public EchoirDIsRapport generateRapport(final EchoirDIsResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportEchoirDIs" + results.dateTraitement.index();
		final String description = String.format("Rapport d'exécution du job de passage des DIs sommées à l'état échu. Date de traitement = %s.", RegDateHelper.dateToDisplayString(results.dateTraitement));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(EchoirDIsRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<EchoirDIsRapport>() {
				@Override
				public void writeDoc(EchoirDIsRapport doc, OutputStream os) throws Exception {
					PdfEchoirDIsRapport document = new PdfEchoirDIsRapport();
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
		final String description = String.format("Rapport d'exécution du job de réinitialisation des barèmes double-gain. Date de traitement = %s.", RegDateHelper.dateToDisplayString(results.dateTraitement));
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
	public CorrectionFlagHabitantRapport generateRapport(final CorrectionFlagHabitantSurPersonnesPhysiquesResults resultsPP, final CorrectionFlagHabitantSurMenagesResults resultsMC, StatusManager s) {

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
					document.write(resultsPP, resultsMC, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public StatistiquesEvenementsRapport generateRapport(final StatsEvenementsCivilsResults civils, final StatsEvenementsExternesResults externes,
	                                                     final StatsEvenementsIdentificationContribuableResults identCtb,
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
					document.write(civils, externes, identCtb, dateReference, nom, description, dateGeneration, os, status);
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
			final String description = String.format("Rapport d'exécution du job de relance de l'indentification des contribuables. Date de traitement = %s",
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
	public MigrationCoquillesPMRapport generateRapport(final MigrationCoquillesPM.MigrationResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "MigrationCoquillesPM";
		final String description = String.format("Rapport de la migration des coquilles des personnes morales. Date de traitement = %s", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(MigrationCoquillesPMRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<MigrationCoquillesPMRapport>() {
				@Override
				public void writeDoc(MigrationCoquillesPMRapport doc, OutputStream os) throws Exception {
					final PdfMigrationCoquillesPMRapport document = new PdfMigrationCoquillesPMRapport();
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
	 * @param status le status manager
	 * @return le rapport
	 */
	@Override
	public ImportCodesSegmentRapport generateRapport(final ImportCodesSegmentResults results, StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportImportCodesSegment";
		final String description = "Rapport d'exécution du job d'importation des codes de segmentation des déclarations d'impôt.";
		final Date dateGeneration = DateHelper.getCurrentDate();

		try {
			return docService.newDoc(ImportCodesSegmentRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<ImportCodesSegmentRapport>() {
				@Override
				public void writeDoc(ImportCodesSegmentRapport doc, OutputStream os) throws Exception {
					final PdfImportCodesSegmentRapport document = new PdfImportCodesSegmentRapport();
					document.write(results, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
