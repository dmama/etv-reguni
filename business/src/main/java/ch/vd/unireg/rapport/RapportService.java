package ch.vd.unireg.rapport;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.acomptes.AcomptesResults;
import ch.vd.unireg.adresse.ResolutionAdresseResults;
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
import ch.vd.unireg.document.*;
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

/**
 * Service de génération des rapports d'exécution de job à partir des résultats d'exécution de ces jobs.
 */
public interface RapportService {

	/**
	 * Génère le rapport (PDF) d'exécution du job de déterminatin des DIs PP à émettre.
	 * @param results le résultat d'exécution du job de détermination des DIs PP à émettre.
	 * @return un document de rapport
	 */
	DeterminationDIsPPRapport generateRapport(DeterminationDIsPPResults results, StatusManager status) throws DeclarationException;

	/**
	 * Génère le rapport (PDF) d'exécution du job de déterminatin des DIs PM à émettre.
	 * @param results le résultat d'exécution du job de détermination des DIs PM à émettre.
	 * @return un document de rapport
	 */
	DeterminationDIsPMRapport generateRapport(DeterminationDIsPMResults results, StatusManager status) throws DeclarationException;

	/**
	 * Génère un document le rapport (PDF) d'exécution du job d'envoi des DIs PP en masse.
	 *
	 * @param results
	 *            le résultat de l'exécution du job d'envoi des DIs en masse
	 * @return un document de rapport
	 */
	EnvoiDIsPPRapport generateRapport(EnvoiDIsPPResults results, StatusManager s) throws DeclarationException;

	/**
	 * Génère le rapport PDF d'exécution du job d'envoi des DI PM en masse
	 * @param results résultat de l'exécution du job
	 * @param s le status manager
	 * @return un document de rapport
	 * @throws DeclarationException en cas de souci
	 */
	EnvoiDIsPMRapport generateRapport(EnvoiDIsPMResults results, StatusManager s) throws DeclarationException;

		/**
	 * Génère un document le rapport (PDF) d'exécution du job d'envoi des DIs en masse.
	 *
	 * @param results
	 *            le résultat de l'exécution du job d'envoi des DIs en masse
	 * @return un document de rapport
	 */
	EnvoiAnnexeImmeubleRapport generateRapport(EnvoiAnnexeImmeubleResults results, StatusManager s) throws DeclarationException;

	/**
	 * Génère le rapport (PDF) de l'ouverture des fors des habitants majeurs.
	 *
	 * @param results
	 *            le résultat de l'exécution du job d'ouverture des fors des habitants majeurs
	 * @return le rapport
	 */
	MajoriteRapport generateRapport(final OuvertureForsResults results, StatusManager s);

	/**
	 * Génère le rapport (PDF) du passage des nouveaux rentiers sourciers en mixte 1.
	 *
	 * @param results
	 *            le résultat de l'exécution du job d'ouverture des fors des habitants majeurs
	 * @return le rapport
	 */
	PassageNouveauxRentiersSourciersEnMixteRapport generateRapport(final PassageNouveauxRentiersSourciersEnMixteResults results, StatusManager s);

	/**
	 * Génère le rapport (PDF) du traitement de la fusion de communes.
	 *
	 * @param results
	 *            le résultat de l'exécution du job.
	 * @return le rapport
	 */
	FusionDeCommunesRapport generateRapport(final FusionDeCommunesResults results, StatusManager s);

	/**
	 * Génération du rapport d'exécution du rôle PP des communes
	 * @param results output du job
	 * @param status status manager
	 * @return le rapport
	 */
	RolePPCommunesRapport generateRapport(RolePPCommunesResults results, StatusManager status);

	/**
	 * Génération du rapport d'exécution du rôle PP des OID
	 * @param results output du job
	 * @param status status manager
	 * @return le rapport
	 */
	RolePPOfficesRapport generateRapport(RolePPOfficesResults results, StatusManager status);

	/**
	 * Génération du rapport d'exécution du rôle PM des communes
	 * @param results output du job
	 * @param status status manager
	 * @return le rapport
	 */
	RolePMCommunesRapport generateRapport(RolePMCommunesResults results, StatusManager status);

	/**
	 * Génération du rapport d'exécution du rôle PM complet de l'OIPM
	 * @param results output du job
	 * @param status status manager
	 * @return le rapport
	 */
	RolePMOfficeRapport generateRapport(RolePMOfficeResults results, StatusManager status);

	/**
	 * Genère le rapport (PDF) des rôles des contribuables PP, décomposé par commune.
	 *
	 * @param results
	 *            le résultat de l'exécution du job de production des rôles PP pour les communes.
	 *
	 * @return le rapport
	 */
	RolesCommunesPPRapport generateRapport(final ProduireRolesPPCommunesResults results, StatusManager status);

	/**
	 * Genère le rapport (PDF) des rôles des contribuables PM, décomposé par commune.
	 *
	 * @param results
	 *            le résultat de l'exécution du job de production des rôles PM pour les communes.
	 *
	 * @return le rapport
	 */
	RolesCommunesPMRapport generateRapport(final ProduireRolesPMCommunesResults results, StatusManager status);

	/**
	 * Genère le rapport (PDF) des rôles des contribuables pour un OID donné
	 * @param results le résultat de l'exécution du job de production des rôles pour un OID.
	 * @param dateTraitement date du traitement
	 * @return le rapport
	 */
	RolesOIDsRapport generateRapport(final ProduireRolesOIDsResults[] results, RegDate dateTraitement, StatusManager status);

	/**
	 * Genère le rapport (PDF) des rôles des contribuables PM du canton (pour l'OIPM, donc)
	 * @param results le résultat de l'exécution du job de production des rôles pour l'OIPM
	 * @param dateTraitement date du traitement
	 * @param status status manager
	 * @return le rapport
	 */
	RolesOIPMRapport generateRapport(final ProduireRolesOIPMResults results, RegDate dateTraitement, StatusManager status);

	/**
	 * Genère le rapport (PDF) des statistiques sur les déclaration d'impôt ordinaires.
	 *
	 * @param results
	 *            le résultat de l'exécution du job de production des statistiques des déclaration d'impôt ordinaires.
	 *
	 * @return le rapport
	 */
	StatistiquesDIsRapport generateRapport(final StatistiquesDIs results, StatusManager status);

	/**
	 * Genère le rapport (PDF) des statistiques sur les contribuables assujettis.
	 *
	 * @param results
	 *            le résultat de l'exécution du job de production des statistiques sur les contribuables assujettis.
	 *
	 * @return le rapport
	 */
	StatistiquesCtbsRapport generateRapport(final StatistiquesCtbs results, StatusManager status);

	/**
	 * Genère le rapport (PDF) de la liste des DI non émises
	 *
	 * @param results
	 *            le résultat de l'exécution du job de production de la liste des DIs non émises.
	 *
	 * @return le rapport
	 */
	ListeDIsNonEmisesRapport generateRapport(final ListeDIsPPNonEmises results, StatusManager status);

	/**
	 * Genère le rapport (PDF) pour l'envoi des sommations de DI PP
	 *
	 * @param results le résultat de l'exécution du job de sommation des DIs.
	 * @return le rapport
	 */
	EnvoiSommationsDIsPPRapport generateRapport(EnvoiSommationsDIsPPResults results, StatusManager statusManager);

	/**
	 * Genère le rapport (PDF) pour l'envoi des sommations de DI PP
	 *
	 * @param results le résultat de l'exécution du job de sommation des DIs.
	 * @return le rapport
	 */
	EnvoiSommationsDIsPMRapport generateRapport(EnvoiSommationsDIsPMResults results, StatusManager statusManager);

	/**
	 * Genère le rapport (PDF) du job de vérification de la validation des tiers.
	 *
	 * @param results
	 *            le résultat de l'exécution du job
	 *
	 * @return le rapport
	 */
	ValidationJobRapport generateRapport(ValidationJobResults results, StatusManager statusManager);

	/**
	 * Genère le rapport (PDF) pour l'envoi des listes récapitulatives.
	 *
	 * @param results
	 *            le résultat de l'exécution du job
	 *
	 * @return le rapport
	 */
	EnvoiLRsRapport generateRapport(EnvoiLRsResults results, StatusManager statusManager);

	/**
	 * Genère le rapport (PDF) pour l'envoi des sommations de listes récapitulatives.
	 *
	 * @param results
	 *            le résultat de l'exécution du job
	 *
	 * @return le rapport
	 */
	EnvoiSommationLRsRapport generateRapport(EnvoiSommationLRsResults results, StatusManager statusManager);

	/**
	 * Genère le rapport (PDF) pour l'envoi des listes nominatives
	 *
	 * @param results
	 *            le résultat de l'exécution du job
	 * @return le rapport
	 */
	ListesNominativesRapport generateRapport(ListesNominativesResults results, StatusManager statusManager);

	/**
	 * Genère le rapport (PDF) pour les populations pour les bases acomptes
	 *
	 * @param results
	 *            le résultat de l'exécution du job
	 * @return le rapport
	 */
	AcomptesRapport generateRapport(AcomptesResults results, StatusManager statusManager);

	/**
	 * Genère le rapport (PDF) pour les extractions des données de référence RPT
	 * @param results le résultat de l'exécution du job
	 * @return le rapport
	 */
	ExtractionDonneesRptRapport generateRapport(ExtractionDonneesRptResults results, StatusManager statusManager);

	/**
	 * Genère le rapport (PDF) pour les extractions des données de référence RPT PM
	 * @param results le résultat de l'exécution du job
	 * @return le rapport
	 */
	ExtractionDonneesRptRapport generateRapport(ExtractionDonneesRptPMResults results, StatusManager statusManager);

	/**
	 * Genère le rapport (PDF) pour les déclarations PP ayant été passées à l'état échues.
	 *
	 * @param results
	 *            le résultat de l'exécution du job
	 * @return le rapport
	 */
	EchoirDIsPPRapport generateRapport(EchoirDIsPPResults results, StatusManager status);

	/**
	 * Genère le rapport (PDF) pour les déclarations PM ayant été passées à l'état échues.
	 *
	 * @param results
	 *            le résultat de l'exécution du job
	 * @return le rapport
	 */
	EchoirDIsPMRapport generateRapport(EchoirDIsPMResults results, StatusManager status);

	/**
	 * Genère le rapport (PDF) pour la réinitialisation des barèmes double-gain
	 *
	 * @param results
	 *            le résultat de l'exécution du job
	 *
	 * @return le rapport
	 */
	ReinitialiserBaremeDoubleGainRapport generateRapport(ReinitialiserBaremeDoubleGainResults results, StatusManager statusManager);

	/**
	 * Genère le rapport (PDF) de la liste des taches en instance par OID, avec le nombre de tache et le nombre de tiers concerné
	 *
	 * @param results
	 *            le résultat de l'exécution du job
	 *
	 * @return le rapport
	 */
	ListeTachesEnIsntanceParOIDRapport generateRapport(ListeTachesEnInstanceParOID results,StatusManager statusManager);

	ExclureContribuablesEnvoiRapport generateRapport(ExclureContribuablesEnvoiResults results, StatusManager status);

	DemandeDelaiCollectiveRapport generateRapport(DemandeDelaiCollectiveResults results, StatusManager status);

	/**
	 * Génère le rapport (PDF) de la liste des contribuables Suisses ou titulaires d'un permis C, résidents sur sol vaudois
	 * d'après leur adresse de domicile, mais sans for vaudois
	 *
	 * @return le rapport
	 */
	ListeContribuablesResidentsSansForVaudoisRapport generateRapport(ListeContribuablesResidentsSansForVaudoisResults result, StatusManager status);

	/**
	 * Génère le rapport d'exécution du job de correction des flags "habitant"
	 *
	 * @return le rapport
	 */
	CorrectionFlagHabitantRapport generateRapport(CorrectionFlagHabitantResults results, StatusManager status);

	/**
	 * Génère le rapport des statistiques sur les événements unireg
	 * @param dateReference date que les requêtes du type "évolutions depuis le..." utilisent
	 * @return le rapport
	 */
	StatistiquesEvenementsRapport generateRapport(StatsEvenementsCivilsPersonnesResults civilsPersonnes,
	                                              StatsEvenementsCivilsEntreprisesResults civilsEntreprises,
	                                              StatsEvenementsExternesResults externes,
	                                              StatsEvenementsIdentificationContribuableResults identCtb,
	                                              StatsEvenementsNotairesResults notaires,
	                                              RegDate dateReference, StatusManager statusManager);

	/**
	 * Génère le rapport d'exécution du job de détermination des mouvements de masse
	 *
	 * @return le rapport
	 */
	DeterminerMouvementsDossiersEnMasseRapport generateRapport(DeterminerMouvementsDossiersEnMasseResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du job d'échéance des LR
	 *
	 * @return le rapport
	 */
	DeterminerLRsEchuesRapport generateRapport(DeterminerLRsEchuesResults results, StatusManager status);

	/**
	 * Génère le rapport suite à l'execution du job de relance de l'identifcation
	 *
	 */
	IdentifierContribuableRapport generateRapport(IdentifierContribuableResults results,StatusManager status);

	/**
	 * Génère le rapport suite à l'execution du job de relance des evenement externe
	 *
	 */
	TraiterEvenementExterneRapport generateRapport(TraiterEvenementExterneResult results, StatusManager status);

	/**
	 * Génère le rapport suite à l'execution du job de résolution des adresses
	 */
	ResolutionAdresseRapport generateRapport(ResolutionAdresseResults results, StatusManager status);

	/**
	 * Génère le rapport suite à l'execution du job de comparaison de situation de famille
	 */
	ComparerSituationFamilleRapport generateRapport(ComparerSituationFamilleResults results, StatusManager status);

	/**
	 * Génère le rapport suite à l'execution du job de production de la liste des contribuables avec note
	 * @param results  resultat du batch
	 * @param statusManager  le status manager des jobs
	 * @return   le rapport généré
	 */
	ListeNoteRapport generateRapport(ListeNoteResults results, StatusManager statusManager);

	/**
	 * Génère le rapport d'exécution de la comparaison de la commune du for et de la commune de la résidence
	 * d'un contribuable
	 * @param results  résultat du batch
	 * @param status   status manager
	 * @return  le rapport
	 */
	ComparerForFiscalEtCommuneRapport generateRapport(ComparerForFiscalEtCommuneResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du job de suppression des doublons des états des déclarations
	 *
	 * @param results résultat du batch
	 * @param status  status manager
	 * @return le rapport
	 */
	CorrectionEtatDeclarationRapport generateRapport(CorrectionEtatDeclarationResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du job d'extraction de la liste des assujettis d'une période fiscale
	 *
	 * @param results les résultats du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	ListeAssujettisRapport generateRapport(ListeAssujettisResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch d'import des codes de segmentation fournis par TAO
	 * @param results les résultats du batch
	 * @param nbLignesLuesFichierEntree le nombre de lignes lues dans le fichier d'entrée (indication du nombre de doublons)
	 * @param status le status manager
	 * @return le rapport
	 */
	ImportCodesSegmentRapport generateRapport(ImportCodesSegmentResults results, int nbLignesLuesFichierEntree, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch de listing des dossiers protégés
	 *
	 * @param results le résultat du batch
	 * @param status  le status manager
	 * @return le rapport
	 */
	ListeDroitsAccesRapport generateRapport(ListeDroitsAccesResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch de suppression d'un OID.
	 *
	 * @param results le résultat du batch
	 * @param status  le status manager
	 * @return le rapport
	 */
	SuppressionOIDRapport generateRapport(SuppressionOIDResults results, StatusManager status);

	AnnoncesIDERapport generateRapport(AnnonceIDEJobResults results, StatusManager s);

	/**
	 * Génère le rapport d'exécution du batch de recalcul des tâches
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	RecalculTachesRapport generateRapport(TacheSyncResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch de génération des relations de parenté
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	CalculParentesRapport generateRapport(CalculParentesResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch de calcul des périodes d'imposition IS
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	DumpPeriodesImpositionImpotSourceRapport generateRapport(DumpPeriodesImpositionImpotSourceResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch qui liste les assujettissements par substitution
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	AssujettiParSubstitutionRapport generateRapport(AssujettisParSubstitutionResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch qui envoie les lettres de bienvenue
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	EnvoiLettresBienvenueRapport generateRapport(EnvoiLettresBienvenueResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch qui envoie les rappels des lettres de bienvenue
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	RappelLettresBienvenueRapport generateRapport(RappelLettresBienvenueResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch qui génére les tâches d'envoi des questionnaires SNC pour une PF donnée
	 * @param results le résultat du batch
	 * @param status  le status manager
	 * @return le rapport
	 */
	DeterminationQuestionnairesSNCRapport generateRapport(DeterminationQuestionnairesSNCResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch qui envoie les questionnaires SNC en masse pour une PF donnée
	 * @param results le résultat du batch
	 * @param status  le status manager
	 * @return le rapport
	 */
	EnvoiQuestionnairesSNCRapport generateRapport(EnvoiQuestionnairesSNCEnMasseResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch qui envoie les rappels des questionnaires SNC en masse
	 * @param results le résultat du batch
	 * @param status  le status manager
	 * @return le rapport
	 */
	EnvoiRappelsQuestionnairesSNCRapport generateRapport(EnvoiRappelsQuestionnairesSNCResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch qui tente des appariements sur les établissements secondaires
	 * @param results le résultat du batch
	 * @param status  le status manager
	 * @return le rapport
	 */
	AppariementEtablissementsSecondairesRapport generateRapport(AppariementEtablissementsSecondairesResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch qui tente des rapprochements sur les tiers RF
	 * @param results le résultat du batch
	 * @param status  le status manager
	 * @return le rapport
	 */
	RapprochementTiersRFRapport generateRapport(RapprochementTiersRFResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch qui traite l'import des immeubles du RF
	 *
	 * @param results le résultat du batch
	 * @param status  le status manager
	 * @return le rapport
	 */
	MutationsRFDetectorRapport generateRapport(MutationsRFDetectorResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch qui tente d'identifier des contribuables à partir d'une liste de critères
	 * @param results le résultat du batch
	 * @param status  le status manager
	 * @return le rapport
	 */
	IdentifierContribuableFromListeRapport generateRapport(IdentifierContribuableFromListeResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch qui traite les mutations des immeubles du RF
	 *
	 * @param results le résultat du batch
	 * @param status  le status manager
	 * @return le rapport
	 */
	MutationsRFProcessorRapport generateRapport(MutationsRFProcessorResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch qui envoie les formulaires de demande de dégrèvement ICI en masse
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	EnvoiFormulairesDemandeDegrevementICIRapport generateRapport(EnvoiFormulairesDemandeDegrevementICIResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch qui envoie les rappels des formulaires de demande de dégrèvement ICI en masse
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	RappelFormulairesDemandeDegrevementICIRapport generateRapport(RappelFormulairesDemandeDegrevementICIResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch de cleanup des données du RF.
	 *
	 * @param results le résultat du batch
	 * @param status  le status manager
	 * @return le rapport
	 */
	CleanupRFProcessorRapport generateRapport(CleanupRFProcessorResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch d'extraction de la population nécessaire à l'initialisation de la taxation IFonc
	 *
	 * @param results le résultat du batch
	 * @param status  le status manager
	 * @return le rapport
	 */
	InitialisationIFoncRapport generateRapport(InitialisationIFoncResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch de rattrapage des régimes fiscaux.
	 *
	 * @param results le résultat du batch
	 * @param status  le status manager
	 * @return le rapport
	 */
	RattrapageRegimesFiscauxRapport generateRapport(RattrapageRegimesFiscauxJobResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch d'extraction des régimes fiscaux.
	 *
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	ExtractionRegimesFiscauxRapport generateRapport(ExtractionRegimesFiscauxResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch de migration des mandataires spéciaux en provenance de TAO-PP ou SIMPA
	 *
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	MigrationMandatairesSpeciauxRapport generateRapport(MigrationMandatImporterResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch de rattrapage des dates métier des droits RF.
	 *
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	RattraperDatesMetierDroitProcessorRapport generateRapport(RattraperDatesMetierDroitRFProcessorResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch de rattrapage des regroupement des communautés sur les modèles de communauté RF.
	 *
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	RattrapageModelesCommunautesRFProcessorRapport generateRapport(RattrapageModelesCommunautesRFProcessorResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch d'import em masse des SNC et leurs associés.
	 *
	 * @param results le résultat du batch
	 * @param s       le status manager
	 * @return le rapport
	 */
	LienAssociesSNCEnMasseImporterRapport generateRapport(LienAssociesSNCEnMasseImporterResults results, StatusManager s);
	/**
	 * Génération du rapport d'exécution du rôle SNC complet de l'OIPM.
	 * @param results le résultat du batch
	 * @param s       le status manager
	 * @return le rapport
	 */
	RoleSNCRapport generateRapport(RoleSNCResults results, StatusManager s);

	/**
	 * Génère le rapport d'exécution du batch d'échéance des questionnaires SNC.
	 *
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	EchoirQSNCRapport generateRapport(EchoirQuestionnairesSNCResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch de changement des régimes fiscaux.
	 *
	 * @param results le résultat du batch
	 * @param status  le status manager
	 * @return le rapport
	 */
	ChangementRegimesFiscauxRapport generateRapport(ChangementRegimesFiscauxJobResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du job d'extraction de la liste EAR d'une période fiscale
	 *
	 * @param results les résultats du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	ListeEchangeRenseignementsRapport generateRapport(ListeEchangeRenseignementsResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch de réémission des événements de mise-à-disposition des DIs dans le contexte de la cyberfiscalité.
	 *
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	RattraperEmissionDIPourCyberContexteRapport generateRapport(RattraperEmissionDIPourCyberContexteResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch d'indexation des tiers.
	 *
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	DatabaseIndexationRapport generateRapport(DatabaseIndexationResults results, StatusManager status);

	/**
	 * Génére le rapport d'éxecution de l'ajout des délais demandés par les mandataires pour leurs clients ctbs
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	AjouterDelaiPourMandataireRapport generateRapport(AjouterDelaiPourMandataireResults results, StatusManager status);
}
