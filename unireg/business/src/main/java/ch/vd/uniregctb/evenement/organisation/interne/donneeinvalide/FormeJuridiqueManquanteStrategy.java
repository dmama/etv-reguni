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
import ch.vd.uniregctb.evenement.organisation.interne.MessageSuiviPreExecution;
import ch.vd.uniregctb.evenement.organisation.interne.MessageWarningPreExectution;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Traitement des cas ou la forme juridique manque dans RCEnt.
 *
 * @author Raphaël Marmier, 2017-06-26.
 */
public class FormeJuridiqueManquanteStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(FormeJuridiqueManquanteStrategy.class);

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
	public FormeJuridiqueManquanteStrategy(EvenementOrganisationContext context, EvenementOrganisationOptions options) {
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
		final RegDate dateAvant = dateApres.getOneDayBefore();

		final FormeLegale formeLegaleAvant = organisation.getFormeLegale(dateAvant);
		final FormeLegale formeLegale = organisation.getFormeLegale(dateApres);

		/*
			Vrai l'organisation existait dans RCEnt avant l'événement, faux si au contraire l'événement marque l'arrivée de l'organisation dans RCEnt (premier snapshot).
		 */
		final boolean existing = isExisting(organisation, dateApres);

		final boolean inscriteAuRC = organisation.isInscriteAuRC(dateApres);
		final boolean inscriteIDE = organisation.isInscriteIDE(dateApres);

		/*
			Toute absence de forme juridique dans ce cas est une erreur menant à un traitement manuel.
		 */
		if (inscriteAuRC || inscriteIDE) {
			if (formeLegaleAvant == null && existing || formeLegale == null) {
				return new TraitementManuel(event, organisation, entreprise, context, options, messageFormeJuridiqueManquante(organisation, entreprise));
			}
		}
		else {
			/*
				Toute absence de forme juridique dans ce cas n'est pas une erreur, mais est signalée ou mise à vérifier selon le cas.
			 */
			if (existing) {
				if (formeLegaleAvant == null) {
					if (formeLegale == null) {
						return new MessageSuiviPreExecution(event, organisation, entreprise, context, options, "Il n'y a pas de forme juridique (legalForm) au civil pour l'organisation non inscrite au RC ni à l'IDE.");
					}
					else {
						return new MessageWarningPreExectution(event, organisation, entreprise, context, options,
						                                       String.format("Nouvelle forme juridique (legalForm) pour l'organisation non inscrite au RC, qui n'en avait pas: %s", formeLegale));
					}
				} else {
					if (formeLegale == null) {
						return new MessageWarningPreExectution(event, organisation, entreprise, context, options,
						                                       String.format("La forme juridique (legalForm) pour l'organisation non inscrite au RC a disparu du registre civil! Ancienne forme juridique: %s.", formeLegaleAvant));
					}
				}
			}
		}

		LOGGER.info(String.format("La forme juridique (legalForm) est présente: (%s).", formeLegale));
		return null;
	}

	private String messageFormeJuridiqueManquante(Organisation organisation, Entreprise entreprise) {
		if (entreprise != null) {
			return String.format("La forme juridique (legalForm) est introuvable dans les données civiles de l'entreprise n°%s (organisation n°%d).",
			                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
			                     organisation.getNumeroOrganisation());
		}
		else {
			return String.format("La forme juridique (legalForm) est introuvable dans les données civiles de l'organisation n°%d.",
			                     organisation.getNumeroOrganisation());
		}
	}
}
