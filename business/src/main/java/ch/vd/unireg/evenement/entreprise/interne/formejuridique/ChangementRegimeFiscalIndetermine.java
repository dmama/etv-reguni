package ch.vd.unireg.evenement.entreprise.interne.formejuridique;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterneDeTraitement;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.RegimeFiscal;

/**
 * Classe de base implémentant détection de changement de catégorie.
 *
 * @author Raphaël Marmier, 2015-10-15
 */
public class ChangementRegimeFiscalIndetermine extends EvenementEntrepriseInterneDeTraitement {

	private final RegDate dateAvant;
	private final RegDate dateApres;

	private final RegimeFiscal regimeFiscalCHAvant;
	private final RegimeFiscal regimeFiscalVDAvant;

	public ChangementRegimeFiscalIndetermine(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise,
	                                         EvenementEntrepriseContext context,
	                                         EvenementEntrepriseOptions options) {
		super(evenement, entrepriseCivile, entreprise, context, options);

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
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {

		changeRegimesFiscauxVDCH(getEntreprise(), getEntrepriseCivile(), regimeFiscalCHAvant, regimeFiscalVDAvant, dateApres, true, suivis);
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {

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
