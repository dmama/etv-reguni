package ch.vd.unireg.evenement.organisation.interne.donneeinvalide;

import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.unireg.evenement.organisation.interne.MessageSuiviPreExecution;
import ch.vd.unireg.evenement.organisation.interne.MessageWarningPreExectution;
import ch.vd.unireg.evenement.organisation.interne.TraitementManuel;
import ch.vd.unireg.tiers.Entreprise;

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

		if (formeLegale == null) {
			// On avait une forme légale, mais on ne l'a plus...
			if (formeLegaleAvant != null) {
				return traiteDisparitionFormeLegale(event, organisation, entreprise, formeLegaleAvant, inscriteAuRC, inscriteIDE);
			}
			// On n'a pas de forme légale du tout
			else {
				return traiteAbsenceFormeLegale(event, organisation, entreprise, inscriteAuRC, inscriteIDE);
			}
		}
		else {
			// On n'avait pas de forme légale mais maintenant une est apparu
			if (formeLegaleAvant == null && existing)  {
				return traiteApparitionFormeLegale(event, organisation, entreprise, formeLegale, inscriteAuRC, inscriteIDE);
			}
		}


		LOGGER.info("La forme juridique (legalForm) est correctement fournie par le registre civil.");
		return null;
	}

	@NotNull
	private EvenementOrganisationInterne traiteApparitionFormeLegale(EvenementOrganisation event, Organisation organisation, Entreprise entreprise, FormeLegale formeLegale, boolean inscriteAuRC,
	                                                                 boolean inscriteIDE) throws EvenementOrganisationException {
		if (inscriteAuRC || inscriteIDE) {
			return new TraitementManuel(event, organisation, entreprise, context, options,
			                            String.format("Apparition de la forme juridique (legalForm) de l'organisation au registre civil: %s. La forme juridique précédente manque. Traitement manuel.", formeLegale));
		}
		else {
			return new MessageWarningPreExectution(event, organisation, entreprise, context, options,
			                                       String.format("Le registre civil indique maintenant la forme juridique (legalForm) de l'organisation: %s. Elle n'était pas fournie auparavant. Vérification requise.", formeLegale));
		}
	}

	@NotNull
	private EvenementOrganisationInterne traiteDisparitionFormeLegale(EvenementOrganisation event, Organisation organisation, Entreprise entreprise, FormeLegale formeLegaleAvant, boolean inscriteAuRC,
	                                                                  boolean inscriteIDE) throws EvenementOrganisationException {
		if (inscriteAuRC || inscriteIDE) {
			return new TraitementManuel(event, organisation, entreprise, context, options,
			                            String.format("La forme juridique (legalForm) de l'organisation a disparu du registre civil! Dernière forme juridique: %s. Traitement manuel.", formeLegaleAvant));
		}
		else {
			return new MessageWarningPreExectution(event, organisation, entreprise, context, options,
			                                       String.format("Le registre civil n'indique plus de forme juridique (legalForm) pour l'organisation. Dernière forme juridique: %s. Vérification requise.", formeLegaleAvant));
		}
	}

	@NotNull
	private EvenementOrganisationInterne traiteAbsenceFormeLegale(EvenementOrganisation event, Organisation organisation, Entreprise entreprise, boolean inscriteAuRC, boolean inscriteIDE) throws
			EvenementOrganisationException {
		if (inscriteAuRC || inscriteIDE) {
			return new TraitementManuel(event, organisation, entreprise, context, options,
			                            "La forme juridique (legalForm) de l'organisation est introuvable au registre civil. Traitement manuel.");
		}
		else {
			return new MessageSuiviPreExecution(event, organisation, entreprise, context, options,
			                                    "Le registre civil n'indique pas de forme juridique (legalForm) pour l'organisation.");
		}
	}

}
