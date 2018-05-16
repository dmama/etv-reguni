package ch.vd.unireg.evenement.organisation.interne.donneeinvalide;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.unireg.evenement.organisation.interne.MessageSuiviPreExecution;
import ch.vd.unireg.evenement.organisation.interne.MessageWarningPreExectution;
import ch.vd.unireg.evenement.organisation.interne.TraitementManuel;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Traitement des cas ou la forme juridique manque dans RCEnt.
 *
 * @author Raphaël Marmier, 2017-06-26.
 */
public class FormeJuridiqueManquanteStrategy extends AbstractOrganisationStrategy {

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
	 * @param event un événement organisation reçu de RCEnt
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

		return null;
	}

	@NotNull
	private EvenementOrganisationInterne traiteApparitionFormeLegale(EvenementOrganisation event, Organisation organisation, Entreprise entreprise, FormeLegale formeLegale, boolean inscriteAuRC,
	                                                                 boolean inscriteIDE) throws EvenementOrganisationException {
		if (inscriteAuRC || inscriteIDE) {
			final String message = String.format("Apparition de la forme juridique (legalForm) de l'organisation au registre civil: %s. La forme juridique précédente manque. Traitement manuel.", formeLegale);
			Audit.info(event.getId(), message);
			return new TraitementManuel(event, organisation, entreprise, context, options, message);
		}
		else {
			final String message = String.format("Le registre civil indique maintenant la forme juridique (legalForm) de l'organisation: %s. Elle n'était pas fournie auparavant. Vérification requise.", formeLegale);
			Audit.warn(event.getId(), message);
			return new MessageWarningPreExectution(event, organisation, entreprise, context, options, message);
		}
	}

	@NotNull
	private EvenementOrganisationInterne traiteDisparitionFormeLegale(EvenementOrganisation event, Organisation organisation, Entreprise entreprise, FormeLegale formeLegaleAvant, boolean inscriteAuRC,
	                                                                  boolean inscriteIDE) throws EvenementOrganisationException {
		if (inscriteAuRC || inscriteIDE) {
			final String message = String.format("La forme juridique (legalForm) de l'organisation a disparu du registre civil! Dernière forme juridique: %s. Traitement manuel.", formeLegaleAvant);
			Audit.info(event.getId(), message);
			return new TraitementManuel(event, organisation, entreprise, context, options, message);
		}
		else {
			final String message = String.format("Le registre civil n'indique plus de forme juridique (legalForm) pour l'organisation. Dernière forme juridique: %s. Vérification requise.", formeLegaleAvant);
			Audit.warn(event.getId(), message);
			return new MessageWarningPreExectution(event, organisation, entreprise, context, options, message);
		}
	}

	@NotNull
	private EvenementOrganisationInterne traiteAbsenceFormeLegale(EvenementOrganisation event, Organisation organisation, Entreprise entreprise, boolean inscriteAuRC, boolean inscriteIDE) throws
			EvenementOrganisationException {
		if (inscriteAuRC || inscriteIDE) {
			final String message = "La forme juridique (legalForm) de l'organisation est introuvable au registre civil. Traitement manuel.";
			Audit.info(event.getId(), message);
			return new TraitementManuel(event, organisation, entreprise, context, options, message);
		}
		else {
			final String message = "Le registre civil n'indique pas de forme juridique (legalForm) pour l'organisation.";
			Audit.info(event.getId(), message);
			return new MessageSuiviPreExecution(event, organisation, entreprise, context, options, message);
		}
	}

}
