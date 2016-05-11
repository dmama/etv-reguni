package ch.vd.uniregctb.evenement.organisation.interne.donneeinvalide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Evénements portant sur une succursale au RC rapportée de manière erronée comme une entreprise par RCEnt.
 *
 * @author Raphaël Marmier, 2016-04-12.
 */
public class FormeJuridiqueInvalideStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(FormeJuridiqueInvalideStrategy.class);

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
		final RegDate dateApres = event.getDateEvenement();

		final FormeLegale formeLegale = organisation.getFormeLegale(dateApres);
		if (formeLegale == FormeLegale.N_0151_SUCCURSALE_SUISSE_AU_RC
				|| formeLegale == FormeLegale.N_0111_FILIALE_ETRANGERE_AU_RC
				|| formeLegale == FormeLegale.N_0312_FILIALE_ETRANGERE_NON_AU_RC) {
			final String message;
			if (entreprise == null) {
				message = String.format(
						"L'organisation n°%d, nom: %s, est en fait une succursale au RC rapportée de manière erronée comme entreprise par RCEnt. Elle ne peut aboutir à la création d'un contribuable PM.",
						organisation.getNumeroOrganisation(), organisation.getNom(dateApres));
			} else {
				message = String.format(
						"L'organisation n°%d, nom: %s, est en fait une succursale au RC rapportée de manière erronée comme entreprise par RCEnt. Elle est pourtant associée à l'entreprise n°%s. Ce cas doit être corrigé.",
						organisation.getNumeroOrganisation(), organisation.getNom(dateApres), FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
			}
			return new TraitementManuel(event, organisation, entreprise, context, options, message);
		}

		LOGGER.info(String.format("La forme juridique est valide (%s).", formeLegale));
		return null;
	}
}
