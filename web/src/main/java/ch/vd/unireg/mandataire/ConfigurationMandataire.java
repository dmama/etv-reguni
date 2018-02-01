package ch.vd.unireg.mandataire;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.tiers.Tiers;

/**
 * Interface du bean qui permet de savoir si l'onglet "Mandataires" est visualisable, voire éditable (par principe et en fonction des droits plus fin de l'utilisateur)
 * pour un tiers donné, et si oui, quels sont les types de mandats concernés.
 * <br/>
 * <b>Note&nbsp;:</b> Cette interface ne tient pas compte des droits de l'utilisateur courant, qui peuvent encore limiter l'accès configuré ici.
 */
public interface ConfigurationMandataire {

	/**
	 * Types d'accès possibles (ordonnés du plus faible au plus puissant)
	 */
	enum Acces {
		/**
		 * Ni visualisation ni édition ne sont possibles
		 */
		AUCUN,

		/**
		 * Seule la visualisation est possible, pas d'édition (sous-entendu : même avec les droits ad'hoc)
		 */
		VISUALISATION_SEULE,

		/**
		 * Visualisation et édition sont possibles (sous-entendu, pour l'édition, seulement avec les droits ad'hoc)
		 */
		EDITION_POSSIBLE
	}

	/**
	 * @param tiers un tiers
	 * @return le type d'affichage maximal pour un mandat général prévisible pour ce tiers
	 */
	Acces getAffichageMandatGeneral(@NotNull Tiers tiers);

	/**
	 * @param tiers un tiers
	 * @return le type d'affichage maximal pour un mandat tiers (= perception) prévisible pour ce tiers
	 */
	Acces getAffichageMandatTiers(@NotNull Tiers tiers);

	/**
	 * @param tiers un tiers
	 * @param genreImpotMandataire un genre d'impôt qui caractèrise un mandat spécial
	 * @return le type d'affichage maximal pour un mandat spécial de ce genre d'impôt pour ce tiers
	 */
	Acces getAffichageMandatSpecial(@NotNull Tiers tiers, @NotNull GenreImpotMandataire genreImpotMandataire);

	/**
	 * @return <code>true</code> si l'établissement d'un nouveau rapport entre tiers pour gérer les mandats 'courrier' (types GENERAL et SPECIAL) est autorisé, <code>false</code>
	 * si seules les adresses mandataires peuvent être créées
	 */
	boolean isCreationRapportEntreTiersAutoriseePourMandatsCourrier();
}
