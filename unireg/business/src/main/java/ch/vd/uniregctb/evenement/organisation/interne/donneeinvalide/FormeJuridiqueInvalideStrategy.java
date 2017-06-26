package ch.vd.uniregctb.evenement.organisation.interne.donneeinvalide;

import java.util.EnumSet;
import java.util.Set;

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

	private static final Set<FormeLegale> FORMES_LEGALES_INVALIDES = EnumSet.of(
																				FormeLegale.N_0111_FILIALE_ETRANGERE_AU_RC,
	                                                                            FormeLegale.N_0113_FORME_JURIDIQUE_PARTICULIERE,
	                                                                            FormeLegale.N_0118_PROCURATIONS_NON_COMMERCIALES,
	                                                                            FormeLegale.N_0119_CHEF_INDIVISION,
	                                                                            FormeLegale.N_0151_SUCCURSALE_SUISSE_AU_RC, // Erreur de données dans RCEnt, établissement secondaire présenté comme établissement principal.
	                                                                            FormeLegale.N_0312_FILIALE_ETRANGERE_NON_AU_RC
	);

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public FormeJuridiqueInvalideStrategy(EvenementOrganisationContext context, EvenementOrganisationOptions options) {
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
		final RegDate dateApres = event.getDateEvenement();

		final FormeLegale formeLegale = organisation.getFormeLegale(dateApres);
		if (formeLegale == null) {
			LOGGER.info("La forme juridique (LegalForm) est absente des données civiles.");
			return null;
		}

		if (FORMES_LEGALES_INVALIDES.contains(formeLegale)) {
			final String message;
			if (entreprise == null) {
				message = String.format("L'organisation n°%d, nom: '%s', possède dans RCEnt une forme juridique non-acceptée par Unireg. Elle ne peut aboutir à la création d'un contribuable.",
				                        organisation.getNumeroOrganisation(),
				                        organisation.getNom(dateApres));
			}
			else {
				message = String.format("L'organisation n°%d, nom: '%s', possède dans RCEnt une forme juridique non-acceptée par Unireg. Elle est pourtant associée à l'entreprise n°%s. Ce cas doit être corrigé.",
				                        organisation.getNumeroOrganisation(),
				                        organisation.getNom(dateApres),
				                        FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
			}
			return new TraitementManuel(event, organisation, entreprise, context, options, message);
		}

		LOGGER.info(String.format("La forme juridique est valide (%s).", formeLegale));
		return null;
	}
}
