package ch.vd.uniregctb.rapport;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.acomptes.AcomptesResults;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.ordinaire.DemandeDelaiCollectiveResults;
import ch.vd.uniregctb.declaration.ordinaire.DeterminationDIsResults;
import ch.vd.uniregctb.declaration.ordinaire.EchoirDIsResults;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiDIsResults;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiSommationsDIsResults;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionChemisesTOResults;
import ch.vd.uniregctb.declaration.ordinaire.ListeDIsNonEmises;
import ch.vd.uniregctb.declaration.ordinaire.StatistiquesCtbs;
import ch.vd.uniregctb.declaration.ordinaire.StatistiquesDIs;
import ch.vd.uniregctb.declaration.source.DeterminerLRsEchuesResults;
import ch.vd.uniregctb.declaration.source.EnvoiLRsResults;
import ch.vd.uniregctb.declaration.source.EnvoiSommationLRsResults;
import ch.vd.uniregctb.document.*;
import ch.vd.uniregctb.document.ListeDIsNonEmisesRapport;
import ch.vd.uniregctb.listes.listesnominatives.ListesNominativesResults;
import ch.vd.uniregctb.listes.suisseoupermiscresident.ListeContribuablesResidentsSansForVaudoisResults;
import ch.vd.uniregctb.metier.FusionDeCommunesResults;
import ch.vd.uniregctb.metier.OuvertureForsResults;
import ch.vd.uniregctb.mouvement.DeterminerMouvementsDossiersEnMasseResults;
import ch.vd.uniregctb.registrefoncier.RapprocherCtbResults;
import ch.vd.uniregctb.role.ProduireRolesResults;
import ch.vd.uniregctb.situationfamille.ReinitialiserBaremeDoubleGainResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsCivilsResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsExternesResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsIdentificationContribuableResults;
import ch.vd.uniregctb.tache.ListeTachesEnIsntanceParOID;
import ch.vd.uniregctb.tiers.ExclureContribuablesEnvoiResults;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantSurMenagesResults;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantSurPersonnesPhysiquesResults;
import ch.vd.uniregctb.validation.ValidationJobResults;

/**
 * Service de génération des rapports d'exécution de job à partir des résultats d'exécution de ces jobs.
 */
public interface RapportService {

	/**
	 * Génère le rapport (PDF) d'exécution du job de déterminatin des DIs à émettre.
	 *
	 * @param results
	 *            le résultat d'exécution du job de déterminatin des DIs à émettre.
	 * @return un document de rapport
	 */
	DeterminationDIsRapport generateRapport(DeterminationDIsResults results, StatusManager status) throws DeclarationException;

	/**
	 * Génère un document le rapport (PDF) d'exécution du job d'envoi des DIs en masse.
	 *
	 * @param results
	 *            le résultat de l'exécution du job d'envoi des DIs en masse
	 * @return un document de rapport
	 */
	EnvoiDIsRapport generateRapport(EnvoiDIsResults results, StatusManager s) throws DeclarationException;

	/**
	 * Génère le rapport (PDF) de l'ouverture des fors des habitants majeurs.
	 *
	 * @param results
	 *            le résultat de l'exécution du job d'ouverture des fors des habitants majeurs
	 * @return le rapport
	 */
	MajoriteRapport generateRapport(final OuvertureForsResults results, StatusManager s);

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
	RolesCommunesRapport generateRapport(final ProduireRolesResults results, StatusManager status);

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
	ListeDIsNonEmisesRapport generateRapport(final ListeDIsNonEmises results, StatusManager status);

	/**
	 * Genère le rapport (PDF) pour l'envoi des sommations de DI
	 *
	 * @param results
	 *            le résultat de l'exécution du job de sommation des DIs.
	 *
	 * @return le rapport
	 */
	EnvoiSommationsDIsRapport generateRapport(EnvoiSommationsDIsResults results, StatusManager statusManager);

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
	 * Genère le rapport (PDF) pour les déclarations ayant été passées à l'état échues.
	 *
	 * @param results
	 *            le résultat de l'exécution du job
	 * @return le rapport
	 */
	EchoirDIsRapport generateRapport(EchoirDIsResults results, StatusManager status);

	/**
	 * Genère le rapport (PDF) pour les impressions en masse des chemises de taxation d'office
	 *
	 * @param results
	 *            le résultat de l'exécution du job
	 *
	 * @return le rapport
	 */
	ImpressionChemisesTORapport generateRapport(ImpressionChemisesTOResults results, StatusManager statusManager);

	/**
	 * Genère le rapport (PDF) pour la réinitialisation des barêmes double-gain
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
	ListeTachesEnIsntanceParOIDRapport generateRapport(ListeTachesEnIsntanceParOID results,StatusManager statusManager);

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
	 * @param results
	 * @param status
	 * @return le rapport
	 */
	ListeContribuablesResidentsSansForVaudoisRapport generateRapport(ListeContribuablesResidentsSansForVaudoisResults result, StatusManager status);

	/**
	 * Génère le rapport d'exécution du job de correction des flags "habitant"
	 * @param resultsPP
	 * @param resultsMC
	 * @param status
	 * @return le rapport
	 */
	CorrectionFlagHabitantRapport generateRapport(CorrectionFlagHabitantSurPersonnesPhysiquesResults resultsPP, CorrectionFlagHabitantSurMenagesResults resultsMC, StatusManager status);

	/**
	 * Génère le rapport des statistiques sur les événements unireg
	 * @param civils
	 * @param externes
	 * @param identCtb
	 * @param dateReference date que les requêtes du type "évolutions depuis le..." utilisent
	 * @param statusManager
	 * @return le rapport
	 */
	StatistiquesEvenementsRapport generateRapport(StatsEvenementsCivilsResults civils, StatsEvenementsExternesResults externes,
	                                              StatsEvenementsIdentificationContribuableResults identCtb,
	                                              RegDate dateReference, StatusManager statusManager);

	/**
	 * Génère le rapport d'exécution du job de détermination des mouvements de masse
	 * @param results
	 * @param status
	 * @return le rapport
	 */
	DeterminerMouvementsDossiersEnMasseRapport generateRapport(DeterminerMouvementsDossiersEnMasseResults results, StatusManager status);

	/**
	 * Génère le rapport d'exécution du job d'échéance des LR
	 * @param results
	 * @param status
	 * @return le rapport
	 */
	DeterminerLRsEchuesRapport generateRapport(DeterminerLRsEchuesResults results, StatusManager status);
}
