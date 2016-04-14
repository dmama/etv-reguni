package ch.vd.uniregctb.evenement.organisation.interne.information;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.common.ComparisonHelper;
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
public class ModificationStatutsStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModificationStatutsStrategy.class);

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
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

		// On ne s'occupe que d'entités déjà connues
		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		RegDate statutsAvant = null;
		RegDate statutsApres = null;
		final DateRanged<SiteOrganisation> sitePrincipalAvantRange = organisation.getSitePrincipal(dateAvant);
		if (sitePrincipalAvantRange != null) {
			DateRanged<RegDate> statutsAvantDateRanged = DateRangeHelper.rangeAt(sitePrincipalAvantRange.getPayload().getDonneesRC().getDateStatuts(), dateAvant);
			if (statutsAvantDateRanged != null) {
				statutsAvant = statutsAvantDateRanged.getPayload();
			}
			final DateRanged<RegDate> statutsApresDateRanged = DateRangeHelper.rangeAt(organisation.getSitePrincipal(dateApres).getPayload().getDonneesRC().getDateStatuts(), dateApres);
			if (statutsApresDateRanged != null) {
				statutsApres = statutsApresDateRanged.getPayload();
			}
			if (!ComparisonHelper.areEqual(statutsAvant, statutsApres)) {
				LOGGER.info("Modification des statuts de l'entreprise -> Propagation.");
				return new InformationComplementaire(event, organisation, entreprise, context, options, TypeInformationComplementaire.MODIFICATION_STATUTS);
			}
		}
		LOGGER.info("Pas de modification des statuts de l'entreprise.");
		return null;
	}
}
