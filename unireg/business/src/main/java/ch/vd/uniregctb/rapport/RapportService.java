package ch.vd.uniregctb.rapport;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.acomptes.AcomptesResults;
import ch.vd.uniregctb.adresse.ResolutionAdresseResults;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.ordinaire.pm.DeterminationDIsPMResults;
import ch.vd.uniregctb.declaration.ordinaire.pm.EnvoiDIsPMResults;
import ch.vd.uniregctb.declaration.ordinaire.pm.EnvoiSommationsDIsPMResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.DemandeDelaiCollectiveResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.DeterminationDIsPPResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.EchoirDIsResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.EnvoiAnnexeImmeubleResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.EnvoiDIsPPResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.EnvoiSommationsDIsPPResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImportCodesSegmentResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.ListeDIsPPNonEmises;
import ch.vd.uniregctb.declaration.ordinaire.pp.ListeNoteResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.StatistiquesCtbs;
import ch.vd.uniregctb.declaration.ordinaire.pp.StatistiquesDIs;
import ch.vd.uniregctb.declaration.source.DeterminerLRsEchuesResults;
import ch.vd.uniregctb.declaration.source.EnvoiLRsResults;
import ch.vd.uniregctb.declaration.source.EnvoiSommationLRsResults;
import ch.vd.uniregctb.document.*;
import ch.vd.uniregctb.droits.ListeDroitsAccesResults;
import ch.vd.uniregctb.evenement.externe.TraiterEvenementExterneResult;
import ch.vd.uniregctb.identification.contribuable.IdentifierContribuableResults;
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
import ch.vd.uniregctb.registrefoncier.ImportImmeublesResults;
import ch.vd.uniregctb.registrefoncier.RapprocherCtbResults;
import ch.vd.uniregctb.role.ProduireRolesCommunesResults;
import ch.vd.uniregctb.role.ProduireRolesOIDsResults;
import ch.vd.uniregctb.situationfamille.ComparerSituationFamilleResults;
import ch.vd.uniregctb.situationfamille.ReinitialiserBaremeDoubleGainResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsCivilsEchResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsExternesResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsIdentificationContribuableResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsNotairesResults;
import ch.vd.uniregctb.tache.ListeTachesEnInstanceParOID;
import ch.vd.uniregctb.tache.TacheSyncResults;
import ch.vd.uniregctb.tiers.ExclureContribuablesEnvoiResults;
import ch.vd.uniregctb.tiers.rattrapage.ancienshabitants.RecuperationDonneesAnciensHabitantsResults;
import ch.vd.uniregctb.tiers.rattrapage.etatdeclaration.CorrectionEtatDeclarationResults;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantResults;
import ch.vd.uniregctb.tiers.rattrapage.origine.RecuperationOriginesNonHabitantsResults;
import ch.vd.uniregctb.validation.ValidationJobResults;

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
	 * Genère le rapport (PDF) des rôles des contribuables, décomposé par commune.
	 *
	 * @param results
	 *            le résultat de l'exécution du job de production des rôles pour les communes.
	 *
	 * @return le rapport
	 */
	RolesCommunesRapport generateRapport(final ProduireRolesCommunesResults results, StatusManager status);

	/**
	 * Genère le rapport (PDF) des rôles des contribuables pour un OID donné
	 * @param results le résultat de l'exécution du job de production des rôles pour un OID.
	 * @param dateTraitement date du traitement
	 * @return le rapport
	 */
	RolesOIDsRapport generateRapport(final ProduireRolesOIDsResults[] results, RegDate dateTraitement, StatusManager status);

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
	 * Genère le rapport (PDF) pour les déclarations ayant été passées à l'état échues.
	 *
	 * @param results
	 *            le résultat de l'exécution du job
	 * @return le rapport
	 */
	EchoirDIsRapport generateRapport(EchoirDIsResults results, StatusManager status);

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
	 * Génère le rapport pdf et le csv resultat du rapprochment entre les ctb et les propriétaires fonciers
	 * @param results
	 * 				   le résultat de l'exécution du job
	 *
	 * @return le rapport
	 */
	RapprocherCtbRapport generateRapport(RapprocherCtbResults results, StatusManager status);

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
	StatistiquesEvenementsRapport generateRapport(StatsEvenementsCivilsEchResults civilsEch,
	                                              StatsEvenementsExternesResults externes, StatsEvenementsIdentificationContribuableResults identCtb,
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
	 * Génère le rapport d'exécution du batch d'import des immeubles du registre foncier.
	 *
	 * @param results le résultat du batch
	 * @param status  le status manager
	 * @return le rapport
	 */
	ImportImmeublesRapport generateRapport(ImportImmeublesResults results, StatusManager status);

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
	 * Génère le rapport d'exécution du batch de récupération des noms des parents sur les anciens habitants
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	RecuperationDonneesAnciensHabitantsRapport generateRapport(RecuperationDonneesAnciensHabitantsResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch de récupération origines sur les non-habitants
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	RecuperationOriginesNonHabitantsRapport generateRapport(RecuperationOriginesNonHabitantsResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du batch qui liste les assujettissements par substitution
	 * @param results le résultat du batch
	 * @param status le status manager
	 * @return le rapport
	 */
	AssujettiParSubstitutionRapport generateRapport(AssujettisParSubstitutionResults results, StatusManager status);
}
