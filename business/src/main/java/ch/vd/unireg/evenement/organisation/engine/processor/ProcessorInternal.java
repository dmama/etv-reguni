package ch.vd.unireg.evenement.organisation.engine.processor;

import java.util.List;

import ch.vd.unireg.evenement.organisation.EvenementEntrepriseBasicInfo;

/**
 * Volontairement laissée au niveau package pour un peu plus d'encapsulation
 */
interface ProcessorInternal {

	/**
	 * Traitement de l'événement donné
	 *
	 * @param evt descripteur de l'événement entreprise cible à traiter
	 * @param evts liste des descripteurs d'événements (en cas d'échec sur le traitement de l'événement cible, ceux qui sont après lui dans cette liste seront passés en attente, voir {@link #errorPostProcessing(List)})
	 * @param pointer indicateur de la position de l'événement entreprise cible dans la liste des événements
	 * @return <code>true</code> si tout s'est bien passé, <code>false</code> si l'un au moins des événements a terminé en erreur
	 */
	boolean processEventAndDoPostProcessingOnError(EvenementEntrepriseBasicInfo evt, List<EvenementEntrepriseBasicInfo> evts, int pointer);

	/**
	 * Forcer un événement en le traitement en n'exécutant que les actions sans impact Unireg (ex: envoi d'événements
	 * fiscaux). Sert à éviter de perdre les opérations non Unireg lorsqu'on force un événement. Bien noter:
	 * - Statut de l'événement soit FORCE, soit ERREUR
	 * - Les erreurs, warnings et suivis précédant sont conservés
	 * - Les nouveaux erreurs, warnings et suivis sont ajoutés aux précédant
	 * -
	 *
	 * @param evt descripteur de l'événement entreprise cible à traiter
	 * @return <code>true</code> si tout s'est bien passé, <code>false</code> si l'événement a terminé en erreur
	 */
	boolean forceEvent(EvenementEntrepriseBasicInfo evt);
}