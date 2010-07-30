package ch.vd.uniregctb.metier;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatCivil;

/**
 * Ensemble de services métiers de haut niveau, incluant toutes les régles fiscales nécessaires pour maintenir la cohérence des données.
 */
public interface MetierService {

	/**
	 * Vérifie que deux personne physique peuvent bien se marier.
	 *
	 * @param dateMariage
	 *            la date effective du mariage
	 * @param principal
	 *            le tiers principal du ménage commun
	 * @param conjoint
	 *            le conjoint du ménage commun. Cette valeur peut-être laissée nulle si le conjoint n'est pas connu (cas du marié seul).
	 * @return le résultat de la validation.
	 */
	public ValidationResults validateMariage(RegDate dateMariage, PersonnePhysique principal, PersonnePhysique conjoint);

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
	 * @return
	 */
	public MenageCommun marie(RegDate dateMariage, PersonnePhysique principal, PersonnePhysique conjoint, String remarque, ch.vd.uniregctb.type.EtatCivil etatCivilFamille, boolean changeHabitantFlag, Long numeroEvenement);

	public MenageCommun rattachToMenage(MenageCommun menage, PersonnePhysique principal, PersonnePhysique conjoint, RegDate date, String remarque, ch.vd.uniregctb.type.EtatCivil etatCivilFamille, boolean changeHabitantFlag, Long numeroEvenement);
	
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
	public ValidationResults validateReconstitution(MenageCommun menage, PersonnePhysique pp, RegDate date);

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
	public MenageCommun reconstitueMenage(MenageCommun menage, PersonnePhysique pp, RegDate date, String remarque, EtatCivil etatCivilFamille);

	/**
	 * Vérifie que la fusion de deux ménages communs incomplets peut être effectuée.
	 * 
	 * @param menagePrincipal
	 *            le ménage commun du tiers principal du ménage.
	 * @param menageConjoint
	 *            le ménage commun du conjoint.
	 * @return le résultat de la validation.
	 */
	public ValidationResults validateFusion(MenageCommun menagePrincipal, MenageCommun menageConjoint);

	/**
	 * Sélectionne le ménage sur lequel les deux membres vont être rattachés lors de la fusion.
	 * 
	 * @param menage1
	 * @param menage2
	 * @return
	 */
	public MenageCommun getMenageForFusion(MenageCommun menage1, MenageCommun menage2);

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
	public MenageCommun fusionneMenages(MenageCommun menagePrincipal, MenageCommun menageConjoint, String remarque, EtatCivil etatCivilFamille);
	
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
	public void annuleMariage(PersonnePhysique principal, PersonnePhysique conjoint, RegDate date, Long numeroEvenement);

	/**
	 * Vérifie que deux personnes physiques sont mariés puis séparés et peuvent donc se reconcilier.
	 *
	 * @param principal
	 *            le tiers principal du ménage commun
	 * @param conjoint
	 *            le conjoint du ménage commun. Cette valeur peut-être laissée nulle si le conjoint n'est pas connu (cas du marié seul).
	 * @param date
	 *            la date effective de la réconciliation.
	 * @return le résultat de la validation.
	 */
	public ValidationResults validateReconciliation(PersonnePhysique principal, PersonnePhysique conjoint, RegDate date);

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
	public MenageCommun reconcilie(PersonnePhysique principal, PersonnePhysique conjoint, RegDate date, String remarque, boolean changeHabitantFlag, Long numeroEvenement);
	
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
	public void annuleReconciliation(PersonnePhysique principal, PersonnePhysique conjoint, RegDate date, Long numeroEvenement);

	/**
	 * Test si la PP est majeur ou non à la date de référence spécifiée
	 *
	 * @param pp
	 *            la personne physique
	 * @param dateReference
	 *            la date à laquelle le test de la majorité est effectué
	 * @return vrai si la personne est majeure, faux autrement.
	 */
	public boolean isMajeurAt(PersonnePhysique pp, RegDate dateReference);

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
	 * @param dateTraitement
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
	public ValidationResults validateSeparation(MenageCommun menage, RegDate date);

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
	 *            (optionnel) le numéro d'événement civil déclenchant la séparation
	 */
	public void separe(MenageCommun menage, RegDate date, String remarque, ch.vd.uniregctb.type.EtatCivil etatCivilFamille, boolean changeHabitantFlag, Long numeroEvenement);

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
	public void annuleSeparation(MenageCommun menage, RegDate date, Long numeroEvenement);

	public ValidationResults validateDeces(PersonnePhysique defunt, RegDate date);

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
	public void deces(PersonnePhysique defunt, RegDate date, String remarque, Long numeroEvenement);

	/**
	 * Annule un décès.
	 *
	 * @param tiers
	 *            l'ancient defunt
	 * @param date
	 *            la date de décès
	 */
	public void annuleDeces(PersonnePhysique tiers, RegDate date);

	public ValidationResults validateVeuvage(PersonnePhysique veuf, RegDate date);

	/**
	 * Traite un veuvage.
	 *
	 * @param veuf
	 *            le veuf
	 * @param dateVeuvage
	 *            la date de veuvage
	 * @param remarque
	 *            sera ajoutée à la fin de la remarque de tous les contribuables mis à jour
	 * @param numeroEvenement
	 *            (optionnel) le numéro d'événement civil déclenchant le veuvage
	 */
	public void veuvage(PersonnePhysique veuf, RegDate dateVeuvage, String remarque, Long numeroEvenement);

	/**
	 * Annule un veuvage.
	 *
	 * @param tiers
	 *            l'ancient veuf
	 * @param date
	 *            la date de veuvage annulé
	 * @param numeroEvenement
	 *            (optionnel) le numéro d'événement civil déclenchant l'annulation
	 */
	public void annuleVeuvage(PersonnePhysique tiers, RegDate date, Long numeroEvenement);
}
