package ch.vd.unireg.evenement.organisation.interne.formejuridique;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterneDeTraitement;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.RegimeFiscal;

/**
 * Classe de base implémentant détection de changement de catégorie.
 *
 * @author Raphaël Marmier, 2015-10-15
 */
public class ChangementRegimeFiscalIndetermine extends EvenementOrganisationInterneDeTraitement {

	private final RegDate dateAvant;
	private final RegDate dateApres;

	private final RegimeFiscal regimeFiscalCHAvant;
	private final RegimeFiscal regimeFiscalVDAvant;

	public ChangementRegimeFiscalIndetermine(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                         EvenementOrganisationContext context,
	                                         EvenementOrganisationOptions options) {
		super(evenement, organisation, entreprise, context, options);

		dateApres = evenement.getDateEvenement();
		dateAvant = dateApres.getOneDayBefore();

		regimeFiscalCHAvant = getAndValidateOpen(extractRegimesFiscauxCH(), dateAvant);
		regimeFiscalVDAvant = getAndValidateOpen(extractRegimesFiscauxVD(), dateAvant);

		// TODO: Générer documents éditique
	}

	public RegDate getDateAvant() {
		return dateAvant;
	}

	public RegDate getDateApres() {
		return dateApres;
	}

	public RegimeFiscal getRegimeFiscalCHAvant() {
		return regimeFiscalCHAvant;
	}

	public RegimeFiscal getRegimeFiscalVDAvant() {
		return regimeFiscalVDAvant;
	}

	@Override
	public String describe() {
		return "Changement de forme juridique avec régime fiscal à déterminer";
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		changeRegimesFiscauxVDCH(getEntreprise(), getOrganisation(), regimeFiscalCHAvant, regimeFiscalVDAvant, dateApres, true, suivis);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		/*
		 Erreurs techniques fatale
		  */
		if (dateAvant == null || dateApres == null || dateAvant != dateApres.getOneDayBefore()) {
			throw new IllegalArgumentException();
		}

		// Vérifier qu'il y a bien une entreprise préexistante en base ? (Ca ne devrait pas se produire ici)
		if (getEntreprise() == null) {
			throw new IllegalArgumentException();
		}

		/*
		 Problèmes métiers empêchant la progression
		  */
		if (regimeFiscalCHAvant == null) {
			erreurs.addErreur("Régime fiscal ordinaire CH introuvable, déjà fermé ou incohérent. Veuillez traiter manuellement.");
		}
		if (regimeFiscalVDAvant == null) {
			erreurs.addErreur("Régime fiscal ordinaire VD introuvable, déjà fermé ou incohérent. Veuillez traiter manuellement.");
		}
	}
}
