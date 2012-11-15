package ch.vd.uniregctb.tiers.manager;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Tiers;

public interface AutorisationManager {

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
	boolean isEditAllowedPP(Tiers tiers);

	/**
	 * Vérifie si l'utilisateur est autorisé à éditer la collectivité administrative spécifiée.
	 *
	 * @param tiers (uniquement PP ou ménage)
	 * @return <b>true</b> si l'édition du tiers est autorisée; <b>false</b> autrement.
	 */
	boolean isEditAllowedCA(CollectiviteAdministrative tiers);

	/**
	 * Détermine et retourne les autorisations de l'utilisateur courant sur le tiers spécifié.
	 *
	 * @param tiers un tiers
	 * @return les autorisations détaillées.
	 */
	@NotNull
	Autorisations getAutorisations(Tiers tiers);
}
