package ch.vd.uniregctb.evenement.organisation.interne.information;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.tiers.Entreprise;

import static ch.vd.uniregctb.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;

/**
 * Modification de capital à propager sans effet.
 *
 * @author Raphaël Marmier, 2015-11-02.
 */
public class ModificationCapitalStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModificationCapitalStrategy.class);

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public ModificationCapitalStrategy(EvenementOrganisationContext context, EvenementOrganisationOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param organisation
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event, final Organisation organisation, Entreprise entreprise) throws EvenementOrganisationException {

		// On ne s'occupe que d'entités déjà connues
		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		final DateRanged<SiteOrganisation> sitePrincipalAvantRange = organisation.getSitePrincipal(dateAvant);
		if (sitePrincipalAvantRange != null) {

			final Capital capitalAvant = organisation.getCapital(dateAvant);
			final Capital capitalApres = organisation.getCapital(dateApres);

			if (changementCapital(capitalAvant, capitalApres)) {
				LOGGER.info("Modification du capital -> Propagation.");
				return new InformationComplementaire(event, organisation, entreprise, context, options, TypeInformationComplementaire.MODIFICATION_CAPITAL);
			}
		}
		LOGGER.info("Pas de modification du capital.");
		return null;
	}

	private boolean changementCapital(@Nullable Capital capitalAvant, @Nullable Capital capitalApres) {
		if (capitalAvant == null || capitalApres == null) {
			return capitalAvant != null || capitalApres != null;
		}
		return ! capitalAvant.identicalTo(capitalApres);
	}
}
