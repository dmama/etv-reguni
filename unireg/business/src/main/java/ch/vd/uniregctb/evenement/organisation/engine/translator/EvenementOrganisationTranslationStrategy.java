package ch.vd.uniregctb.evenement.organisation.engine.translator;


import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;

public interface EvenementOrganisationTranslationStrategy {

	/**
	 * Crée un événement organisation interne à partir d'un événement organisation externe.
	 *
	 * Cette méthode lève une exception en cas d'impossibilité de créer une une instance, mais
	 * ne renvoie jamais null.
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 * @return L'événement interne qui correspond à l'événement externe reçu
	 * @throws EvenementOrganisationException en cas de problème
	 */
	@NotNull
	EvenementOrganisationInterne create(EvenementOrganisation event, Organisation organisation, EvenementOrganisationContext context, EvenementOrganisationOptions options) throws EvenementOrganisationException;

	/**
	 * Crée un événement organisation interne à partir d'un événement organisation externe.
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 * @return l'événement interne correspondant à l'événement externe reçu, ou null si pas applicable
	 * @throws EvenementOrganisationException en cas de problème
	 */
	EvenementOrganisationInterne match(EvenementOrganisation event, Organisation organisation, EvenementOrganisationContext context, EvenementOrganisationOptions options) throws EvenementOrganisationException;
}
