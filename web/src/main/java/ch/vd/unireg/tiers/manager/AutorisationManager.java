package ch.vd.unireg.tiers.manager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public interface AutorisationManager {

	/**
	 * Vérifie si l'utilisateur est autorisé à voir le tiers spécifié.
	 *
	 * @param tiers un tiers
	 * @return <b>true</b> si la visualisation du tiers est autorisée; <b>false</b> autrement.
	 */
	boolean isVisuAllowed(@NotNull Tiers tiers);

	/**
	 * Vérifie si l'utilisateur est autorisé à éditer le tiers spécifié.
	 *
	 * @param tiers un tiers
	 * @return <b>true</b> si l'édition du tiers est autorisée; <b>false</b> autrement.
	 */
	boolean isEditAllowed(Tiers tiers);

	/**
	 * Vérifie si l'utilisateur est autorisé à éditer la personne physique ou le ménage-commun spécifié.
	 *
	 * @param tiers (uniquement PP ou ménage)
	 * @return <b>true</b> si l'édition du tiers est autorisée; <b>false</b> autrement.
	 */
	boolean isEditAllowedPP(ContribuableImpositionPersonnesPhysiques tiers);

	/**
	 * Vérifie si l'utilisateur est autorisé à éditer la collectivité administrative spécifiée.
	 *
	 * @param tiers (uniquement PP ou ménage)
	 * @return <b>true</b> si l'édition du tiers est autorisée; <b>false</b> autrement.
	 */
	boolean isEditAllowedCA(CollectiviteAdministrative tiers);

	boolean isEditAllowedEntreprise(Entreprise tiers);

	boolean isEditAllowedEtablissement(Etablissement tiers);

	/**
	 * Détermine et retourne les autorisations d'un utilisateur sur le tiers spécifié.
	 *
	 * @param tiers un tiers; ou <b>null</b> si on s'intéresse aux autorisations pour créé un nouveau tiers
	 * @param visa  le visa de l'utilisateur
	 * @param oid   l'office d'impot de l'utilisateur
	 * @return les autorisations détaillées.
	 */
	@NotNull
	Autorisations getAutorisations(@Nullable Tiers tiers, String visa, int oid);

	/**
	 * Détermine si le mode d'imposition spécifié est autorisé sur le tiers donné.
	 *
	 *
	 *
	 * @param tiers               un tiers
	 * @param modeImposition      le mode d'imposition du for fiscal principal que l'on veut ajouter sur le tiers
	 * @param typeAutoriteFiscale le type d'autorité fiscal du for fiscal principal
	 * @param motifRattachement   le motif de rattachement du fiscal du for fiscal principal
	 * @param date                la date de validité du mode d'imposition
	 * @param visa                le visa de l'utilisateur
	 * @param oid                 l'oid de l'utilisateur
	 * @return Une valeur de l'enum RetourModeImpositionAllowed: OK, ou le type d'erreur rencontrée
	 */
	RetourModeImpositionAllowed isModeImpositionAllowed(@NotNull Tiers tiers, @NotNull ModeImposition modeImposition, @NotNull TypeAutoriteFiscale typeAutoriteFiscale, MotifRattachement motifRattachement, RegDate date,
	                                                    String visa, int oid);
}
