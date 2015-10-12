package ch.vd.uniregctb.evenement.organisation.interne.creation;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.evenement.organisation.interne.helper.CategorieEntreprise;
import ch.vd.uniregctb.evenement.organisation.interne.helper.CategorieEntrepriseHelper;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateOrganisationStrategy extends AbstractOrganisationStrategy {

	private static final String MSG_CREATION_AUTOMATIQUE_IMPOSSIBLE = "Création automatique non prise en charge.";

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne {@link CreateEntrepriseBase} est
	 * pertinente.
	 *
	 * Spécifications:
	 *  - Ti01SE03-Identifier et traiter les mutations entreprise.doc - Version 1.1 - 23.09.2015
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param organisation
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event,
	                                                   final Organisation organisation,
	                                                   Entreprise entreprise,
	                                                   EvenementOrganisationContext context,
	                                                   EvenementOrganisationOptions options) throws EvenementOrganisationException {

		// On décide qu'on a affaire à une création uniquement selon la présence d'un tiers entreprise dans Unireg, et rien d'autre.
		// TODO: Retrouver aussi les entreprises n'ayant pas d'id cantonal.
		if (entreprise != null) {
			return null;
		}

		// On doit connaître la catégorie pour continuer en mode automatique
		CategorieEntreprise category = CategorieEntrepriseHelper.getCategorieEntreprise(organisation, event.getDateEvenement());
		if (category != null) {

			// On crée une entreprise pour les organisations ayant un siège dans la canton de VD
			if (hasSitePrincipalVD(organisation, event.getDateEvenement())) {

				switch (category) {

				// On ne crée pas d'entreprise pour les entreprises individuelles
				case PP:
					return null;

				// Sociétés de personnes
				case SP:
					return new CreateEntrepriseSP(event, organisation, null, context, options);

				// Personnes morales
				case PM:
					return new CreateEntreprisePM(event, organisation, null, context, options);
				// Associations personne morale
				case APM:
					return new CreateEntrepriseAPM(event, organisation, null, context, options);

				// Fonds de placements
				case FDS_PLAC:
					return new CreateEntrepriseFDSPLAC(event, organisation, null, context, options);

				// Personnes morales de droit public
				case DP_PM:
					return new CreateEntrepriseDPPM(event, organisation, null, context, options);

				// Catégories qu'on ne peut pas traiter automatiquement, catégories éventuellement inconnues.
				case DP_APM:
				default:
					return new TraitementManuel(event, organisation, null, context, options, MSG_CREATION_AUTOMATIQUE_IMPOSSIBLE);
				}
			} else if (hasSiteVD(organisation, event.getDateEvenement())) {
				switch (category) {

				case PP:
					return null;
				default:
					return new CreateEntrepriseHorsVD(event, organisation, null, context, options);
				}
			} else {
				return null; // Pas de siège sur Vaud, pas de création
			}
		}

		// Catchall traitement manuel
		return new TraitementManuel(event, organisation, null, context, options, MSG_CREATION_AUTOMATIQUE_IMPOSSIBLE);
	}
}
