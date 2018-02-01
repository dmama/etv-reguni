package ch.vd.unireg.metier;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatCivil;

/**
 * Ensemble de services métiers de haut niveau, incluant toutes les régles fiscales nécessaires pour maintenir la cohérence des données.
 */
public interface MetierService {

	/**
	 * Détermine si les deux personnes physiques spécifiées sont en ménage (d'un point de vue fiscal) depuis la date exacte spécifiée.
	 *
	 * @param personneA une personne physique
	 * @param personneB une autre personne physique (optionnelle, dans ce cas la première personne est supposée mariée-seule)
	 * @param date      la date de début du ménage supposé
	 * @return <b>vrai</b> si les deux personnes sont en ménage à partir de la date exacte spécifiée; <b>faux</b> dans tous les autres cas.
	 */
	boolean isEnMenageDepuis(PersonnePhysique personneA, @Nullable PersonnePhysique personneB, RegDate date);

	/**
	 * Vérifie que deux personne physique peuvent bien se marier.
	 *
	 * @param dateMariage    la date effective du mariage
	 * @param principal      le tiers principal du ménage commun
	 * @param conjoint       le conjoint du ménage commun. Cette valeur peut-être laissée nulle si le conjoint n'est pas connu (cas du marié seul).
	 * @return le résultat de la validation.
	 */
	ValidationResults validateMariage(RegDate dateMariage, PersonnePhysique principal, PersonnePhysique conjoint);

	/**
	 * Marie deux personnes physiques à la date donnée. Un nouveau contribuable
	 * 'ménage commun' associé, et les for principaux concernés sont mis-à-jour.
	 * 
	 * @param dateMariage
	 *            la date effective du mariage
	 * @param principal
	 *            le tiers principal du ménage commun
	 * @param conjoint
	 *            le conjoint du ménage commun. Cette valeur peut-être laissée nulle si le conjoint n'est pas connu (cas du marié seul).
	 * @param remarque
	 *            sera ajoutée à la fin de la remarque de tous les contribuables mis à jour
	 * @param etatCivilFamille
	 *            l'état civil pour la nouvelle situation de famille si différent de celui dans le registre civil
	 * @param numeroEvenement
	 *            (optionnel) le numéro d'événement civil déclenchant le mariage
	 *
	 * @return le ménage commun marié
	 */
	MenageCommun marie(RegDate dateMariage, PersonnePhysique principal, PersonnePhysique conjoint, @Nullable String remarque, EtatCivil etatCivilFamille,
	                          @Nullable Long numeroEvenement) throws
			MetierServiceException;

	MenageCommun rattachToMenage(MenageCommun menage, PersonnePhysique principal, PersonnePhysique conjoint, RegDate date, @Nullable String remarque,
	                                    EtatCivil etatCivilFamille, @Nullable Long numeroEvenement) throws
			MetierServiceException;
	
	/**
	 * Vérifie que la réconstitution d'un ménage incomplet peut être faite.
	 * 
	 * @param menage
	 *            le ménage incomplet.
	 * @param pp
	 *            la personne manquant dans le ménage.
	 * @param date
	 *            la date effective du mariage.
	 * @return le résultat de la validation.
	 */
	ValidationResults validateReconstitution(MenageCommun menage, PersonnePhysique pp, RegDate date);

	/**
	 * Réconstitue un ménage incomplet.
	 * 
	 * @param menage
	 *            le ménage incomplet.
	 * @param pp
	 *            la personne manquant dans le ménage.
	 * @param date
	 *            la date effective du mariage.
	 * @param remarque
	 *            sera ajoutée à la fin de la remarque de tous les contribuables mis à jour
	 * @param etatCivilFamille
	 *            l'état civil à utiliser dans la situation de famille
	 * @return le ménage mis à jour.
	 */
	MenageCommun reconstitueMenage(MenageCommun menage, PersonnePhysique pp, RegDate date, @Nullable String remarque, EtatCivil etatCivilFamille);

	/**
	 * Vérifie que la fusion de deux ménages communs incomplets peut être effectuée.
	 * 
	 * @param menagePrincipal
	 *            le ménage commun du tiers principal du ménage.
	 * @param menageConjoint
	 *            le ménage commun du conjoint.
	 * @return le résultat de la validation.
	 */
	ValidationResults validateFusion(MenageCommun menagePrincipal, MenageCommun menageConjoint);

	/**
	 * Sélectionne le ménage sur lequel les deux membres vont être rattachés lors de la fusion.
	 * 
	 * @param menage1 un ménage
	 * @param menage2 un autre ménage
	 * @return le ménage considéré comme principal
	 */
	@NotNull
	MenageCommun getMenageForFusion(@NotNull MenageCommun menage1, @NotNull MenageCommun menage2);

	/**
	 * Fusionne deux ménage communs incomplets.
	 * 
	 * @param menagePrincipal
	 *            le ménage commun du tiers principal du ménage.
	 * @param menageConjoint
	 *            le ménage commun du conjoint.
	 * @param remarque
	 *            sera ajoutée à la fin de la remarque de tous les contribuables mis à jour.
	 * @param etatCivilFamille
	 *            l'état civil à utiliser dans la situation de famille
	 * @return le ménage mis à jour.
	 */
	MenageCommun fusionneMenages(MenageCommun menagePrincipal, MenageCommun menageConjoint, String remarque, EtatCivil etatCivilFamille) throws MetierServiceException;
	
	/**
	 * Annule un mariage.
	 *
	 * @param principal
	 *            le tiers principal du ménage commun
	 * @param conjoint
	 *            le conjoint du ménage commun. Cette valeur peut-être laissée nulle si le conjoint n'est pas connu (cas du marié seul).
	 * @param date
	 *            la date du mariage
	 * @param numeroEvenement
	 *            (optionnel) le numéro d'événement civil déclenchant le mariage
	 */
	void annuleMariage(PersonnePhysique principal, PersonnePhysique conjoint, RegDate date, Long numeroEvenement) throws MetierServiceException;

	/**
	 * Vérifie que deux personnes physiques sont mariés puis séparés et peuvent donc se reconcilier.
	 *
	 * @param principal le tiers principal du ménage commun
	 * @param conjoint le conjoint du ménage commun. Cette valeur peut-être laissée nulle si le conjoint n'est pas connu (cas du marié seul).
	 * @param date la date effective de la réconciliation.
	 * @param okCoupleValideFormeMemeDate si <code>true</code>, signifie que la présence d'un couple valide débutant à la date donnée ne sera pas constitutif d'une erreur
	 * @return le résultat de la validation.
	 */
	ValidationResults validateReconciliation(PersonnePhysique principal, PersonnePhysique conjoint, RegDate date, boolean okCoupleValideFormeMemeDate);

	/**
	 * Réconcilie deux personnes à la date donnée. Les fors associés au ménage
	 * sont réouverts, est ceux des individus fermés.
	 *
	 * @param principal
	 *            le tiers principal
	 * @param conjoint
	 *            le conjoint (optionnel si marié seul)
	 * @param date
	 *            la date de réconciliation
	 * @param remarque
	 *            sera ajoutée à la fin de la remarque de tous les contribuables mis à jour
	 * @param numeroEvenement
	 *            (optionnel) le numéro d'événement civil déclenchant la réconciliation
	 * @return le ménage commun.
	 */
	MenageCommun reconcilie(PersonnePhysique principal, PersonnePhysique conjoint, RegDate date, String remarque, @Nullable Long numeroEvenement) throws
			MetierServiceException;
	
	/**
	 * Annule la réconciliation d'un couple.
	 *
	 * @param principal
	 *            le tiers principal du ménage commun
	 * @param conjoint
	 *            le conjoint du ménage commun. Cette valeur peut-être laissée nulle si le conjoint n'est pas connu (cas du marié seul).
	 * @param date
	 *            la date de réconciliation
	 * @param numeroEvenement
	 *            (optionnel) le numéro d'événement civil déclenchant le mariage
	 */
	void annuleReconciliation(PersonnePhysique principal, PersonnePhysique conjoint, RegDate date, Long numeroEvenement) throws MetierServiceException;

	/**
	 * Test si la PP est majeur ou non à la date de référence spécifiée
	 *
	 * @param pp
	 *            la personne physique
	 * @param dateReference
	 *            la date à laquelle le test de la majorité est effectué
	 * @return vrai si la personne est majeure, faux autrement.
	 */
	boolean isMajeurAt(PersonnePhysique pp, RegDate dateReference);

	/**
	 * Ouvre les fors des contribuable nouvellement majeurs et qui sont domiciliés sur le canton de Vaud.
	 *
	 * @param dateReference
	 *            la date du jour
	 */
	OuvertureForsResults ouvertureForsContribuablesMajeurs(RegDate dateReference, StatusManager status);

	/**
	 * Met-à-jour les fors fiscaux de tous les contribuables suite à une fusion de communes (vaudoises ou hors-canton).
	 *
	 * @param anciensNoOfs la liste des numéros Ofs des commune ayant fusionné
	 * @param nouveauNoOfs le numéro Ofs de la commune résultant de la fusion
	 * @param dateFusion   la date effective de la fusion
	 * @param dateTraitement date de traitement du batch
	 *@param status       un status manager  @return les résultats détaillés du processus de fusion
	 */
	FusionDeCommunesResults fusionDeCommunes(Set<Integer> anciensNoOfs, int nouveauNoOfs, RegDate dateFusion, RegDate dateTraitement, StatusManager status);

	/**
	 * Vérifie que deux personnes physique peuvent bien se séparer.
	 *
	 * @param menage
	 *            le ménage
	 * @param date
	 *            la date effective du mariage
	 * @return le résultat de la validation.
	 */
	ValidationResults validateSeparation(MenageCommun menage, RegDate date);

	/**
	 * Sépare un ménage commun à une date donnée
	 *
	 * @param menage
	 *            le ménage à séparer
	 * @param date
	 *            la date de séparation
	 * @param remarque
 *            sera ajoutée à la fin de la remarque de tous les contribuables mis à jour
	 * @param etatCivilFamille
*            l'état civil pour la nouvelle situation de famille si différent de celui dans le registre civil
	 * @param numeroEvenement
	 */
	void separe(MenageCommun menage, RegDate date, @Nullable String remarque, EtatCivil etatCivilFamille, @Nullable Long numeroEvenement) throws
			MetierServiceException;

	/**
	 * Annule la séparation de(s) personne(s) appartenant au ménage commun.
	 *
	 * @param menage
	 *            le ménage
	 * @param date
	 *            la date de séparation
	 * @param numeroEvenement
	 *            (optionnel) le numéro d'événement civil déclenchant la séparation
	 */
	void annuleSeparation(MenageCommun menage, RegDate date, Long numeroEvenement) throws MetierServiceException;

	ValidationResults validateDeces(PersonnePhysique defunt, RegDate date);

	/**
	 * Traite un décès.
	 *
	 * @param defunt
	 *            le défunt
	 * @param date
	 *            la date de décès
	 * @param remarque
	 *            sera ajoutée à la fin de la remarque de tous les contribuables mis à jour
	 * @param numeroEvenement
	 *            (optionnel) le numéro d'événement civil déclenchant le décès
	 */
	void deces(PersonnePhysique defunt, RegDate date, @Nullable String remarque, @Nullable Long numeroEvenement) throws MetierServiceException;

	/**
	 * Annule un décès.
	 *
	 * @param tiers
	 *            l'ancient defunt
	 * @param date
	 *            la date de décès
	 */
	void annuleDeces(PersonnePhysique tiers, RegDate date) throws MetierServiceException;

	ValidationResults validateVeuvage(PersonnePhysique veuf, RegDate date);

	void validateForOfVeuvage(PersonnePhysique veuf, RegDate date, EnsembleTiersCouple couple, ValidationResults results);

	/**
	 * Traite un veuvage.
	 *
	 * @param veuf le veuf
	 * @param dateVeuvage la date de veuvage
	 * @param remarque sera ajoutée à la fin de la remarque de tous les contribuables mis à jour
	 * @param numeroEvenement (optionnel) le numéro d'événement civil déclenchant le veuvage
	 */
	void veuvage(PersonnePhysique veuf, RegDate dateVeuvage, @Nullable String remarque, Long numeroEvenement) throws MetierServiceException;

	/**
	 * Annule un veuvage.
	 *
	 * @param tiers l'ancient veuf
	 * @param date la date de veuvage annulé (= ancienne date de décès du conjoint)
	 * @param numeroEvenement (optionnel) le numéro d'événement civil déclenchant l'annulation
	 */
	void annuleVeuvage(PersonnePhysique tiers, RegDate date, Long numeroEvenement) throws MetierServiceException;

	/**Permet de sortir la liste des contribuables ayant un for sur une commune differente de celle de leur adresse.
	 *
	 * @param dateTraitement date de lancement du batch
	 * @param nbThreads  nombre de thread d'éxecution
	 * @param status le statut manager
	 * @return le résultat du batch
	 */
	ComparerForFiscalEtCommuneResults comparerForFiscalEtCommune(RegDate dateTraitement, int nbThreads, StatusManager status);

	/**
	 * Passe les nouveaux rentiers sourciers en mixte 1
	 *
	 * @param dateTraitement date de traitement effective du batch
	 * @param statusManager le status manager
	 * @return le résultat du batch
	 */
	PassageNouveauxRentiersSourciersEnMixteResults passageSourcierEnMixteNouveauxRentiers(RegDate dateTraitement, StatusManager statusManager);
}
