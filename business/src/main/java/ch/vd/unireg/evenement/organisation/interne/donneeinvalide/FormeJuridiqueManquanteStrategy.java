package ch.vd.unireg.evenement.organisation.interne.donneeinvalide;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractEntrepriseStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.organisation.interne.MessageSuiviPreExecution;
import ch.vd.unireg.evenement.organisation.interne.MessageWarningPreExectution;
import ch.vd.unireg.evenement.organisation.interne.TraitementManuel;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Traitement des cas ou la forme juridique manque dans RCEnt.
 *
 * @author Raphaël Marmier, 2017-06-26.
 */
public class FormeJuridiqueManquanteStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public FormeJuridiqueManquanteStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
		super(context, options);
	}


	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event un événement entreprise civile reçu de RCEnt
	 */
	@Override
	public EvenementEntrepriseInterne matchAndCreate(EvenementEntreprise event, final EntrepriseCivile entrepriseCivile, Entreprise entreprise) throws EvenementEntrepriseException {

		final RegDate dateApres = event.getDateEvenement();
		final RegDate dateAvant = dateApres.getOneDayBefore();

		final FormeLegale formeLegaleAvant = entrepriseCivile.getFormeLegale(dateAvant);
		final FormeLegale formeLegale = entrepriseCivile.getFormeLegale(dateApres);

		/*
			Vrai l'entreprise civile existait dans RCEnt avant l'événement, faux si au contraire l'événement marque l'arrivée de l'entreprise civile dans RCEnt (premier snapshot).
		 */
		final boolean existing = isExisting(entrepriseCivile, dateApres);

		final boolean inscriteAuRC = entrepriseCivile.isInscriteAuRC(dateApres);
		final boolean inscriteIDE = entrepriseCivile.isInscriteIDE(dateApres);

		if (formeLegale == null) {
			// On avait une forme légale, mais on ne l'a plus...
			if (formeLegaleAvant != null) {
				return traiteDisparitionFormeLegale(event, entrepriseCivile, entreprise, formeLegaleAvant, inscriteAuRC, inscriteIDE);
			}
			// On n'a pas de forme légale du tout
			else {
				return traiteAbsenceFormeLegale(event, entrepriseCivile, entreprise, inscriteAuRC, inscriteIDE);
			}
		}
		else {
			// On n'avait pas de forme légale mais maintenant une est apparu
			if (formeLegaleAvant == null && existing)  {
				return traiteApparitionFormeLegale(event, entrepriseCivile, entreprise, formeLegale, inscriteAuRC, inscriteIDE);
			}
		}

		return null;
	}

	@NotNull
	private EvenementEntrepriseInterne traiteApparitionFormeLegale(EvenementEntreprise event, EntrepriseCivile entrepriseCivile, Entreprise entreprise, FormeLegale formeLegale, boolean inscriteAuRC,
	                                                               boolean inscriteIDE) throws EvenementEntrepriseException {
		if (inscriteAuRC || inscriteIDE) {
			final String message = String.format("Apparition de la forme juridique (legalForm) de l'entreprise civile au registre civil: %s. La forme juridique précédente manque. Traitement manuel.", formeLegale);
			Audit.info(event.getId(), message);
			return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, message);
		}
		else {
			final String message = String.format("Le registre civil indique maintenant la forme juridique (legalForm) de l'entreprise civile: %s. Elle n'était pas fournie auparavant. Vérification requise.", formeLegale);
			Audit.warn(event.getId(), message);
			return new MessageWarningPreExectution(event, entrepriseCivile, entreprise, context, options, message);
		}
	}

	@NotNull
	private EvenementEntrepriseInterne traiteDisparitionFormeLegale(EvenementEntreprise event, EntrepriseCivile entrepriseCivile, Entreprise entreprise, FormeLegale formeLegaleAvant, boolean inscriteAuRC,
	                                                                boolean inscriteIDE) throws EvenementEntrepriseException {
		if (inscriteAuRC || inscriteIDE) {
			final String message = String.format("La forme juridique (legalForm) de l'entreprise civile a disparu du registre civil! Dernière forme juridique: %s. Traitement manuel.", formeLegaleAvant);
			Audit.info(event.getId(), message);
			return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, message);
		}
		else {
			final String message = String.format("Le registre civil n'indique plus de forme juridique (legalForm) pour l'entreprise civile. Dernière forme juridique: %s. Vérification requise.", formeLegaleAvant);
			Audit.warn(event.getId(), message);
			return new MessageWarningPreExectution(event, entrepriseCivile, entreprise, context, options, message);
		}
	}

	@NotNull
	private EvenementEntrepriseInterne traiteAbsenceFormeLegale(EvenementEntreprise event, EntrepriseCivile entrepriseCivile, Entreprise entreprise, boolean inscriteAuRC, boolean inscriteIDE) throws
			EvenementEntrepriseException {
		if (inscriteAuRC || inscriteIDE) {
			final String message = "La forme juridique (legalForm) de l'entreprise civile est introuvable au registre civil. Traitement manuel.";
			Audit.info(event.getId(), message);
			return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, message);
		}
		else {
			final String message = "Le registre civil n'indique pas de forme juridique (legalForm) pour l'entreprise civile.";
			Audit.info(event.getId(), message);
			return new MessageSuiviPreExecution(event, entrepriseCivile, entreprise, context, options, message);
		}
	}

}
