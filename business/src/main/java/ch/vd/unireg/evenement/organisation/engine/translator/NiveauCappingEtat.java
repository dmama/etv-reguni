package ch.vd.unireg.evenement.organisation.engine.translator;

import ch.vd.unireg.type.EtatEvenementOrganisation;

/**
 * Niveaux disponibles pour le capping de l'état final des événements de RCEnt
 */
public enum NiveauCappingEtat {
	/**
	 * Tous les états finaux <strong>supérieurs</strong> (comme {@link EtatEvenementOrganisation#TRAITE TRAITE}, {@link EtatEvenementOrganisation#REDONDANT REDONDANT}) à
	 * {@link EtatEvenementOrganisation#A_VERIFIER A_VERIFIER} (et différents de {@link EtatEvenementOrganisation#FORCE FORCE}) seront finalement remplacés par {@link EtatEvenementOrganisation#A_VERIFIER A_VERIFIER}
	 */
	A_VERIFIER,

	/**
	 * Tous les états finaux <strong>supérieurs</strong> à l'état {@link EtatEvenementOrganisation#EN_ERREUR EN_ERREUR} (et différents de {@link EtatEvenementOrganisation#FORCE FORCE})
	 * seront finalement remplacés par {@link EtatEvenementOrganisation#EN_ERREUR EN_ERREUR} (et les modifications éventuelles abandonnées, évidemment)
	 */
	EN_ERREUR
}
