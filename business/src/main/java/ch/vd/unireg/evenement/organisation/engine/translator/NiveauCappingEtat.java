package ch.vd.unireg.evenement.organisation.engine.translator;

import ch.vd.unireg.type.EtatEvenementEntreprise;

/**
 * Niveaux disponibles pour le capping de l'état final des événements de RCEnt
 */
public enum NiveauCappingEtat {
	/**
	 * Tous les états finaux <strong>supérieurs</strong> (comme {@link EtatEvenementEntreprise#TRAITE TRAITE}, {@link EtatEvenementEntreprise#REDONDANT REDONDANT}) à
	 * {@link EtatEvenementEntreprise#A_VERIFIER A_VERIFIER} (et différents de {@link EtatEvenementEntreprise#FORCE FORCE}) seront finalement remplacés par {@link EtatEvenementEntreprise#A_VERIFIER A_VERIFIER}
	 */
	A_VERIFIER,

	/**
	 * Tous les états finaux <strong>supérieurs</strong> à l'état {@link EtatEvenementEntreprise#EN_ERREUR EN_ERREUR} (et différents de {@link EtatEvenementEntreprise#FORCE FORCE})
	 * seront finalement remplacés par {@link EtatEvenementEntreprise#EN_ERREUR EN_ERREUR} (et les modifications éventuelles abandonnées, évidemment)
	 */
	EN_ERREUR
}
