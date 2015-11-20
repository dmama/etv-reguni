package ch.vd.uniregctb.evenement.organisation.interne.formejuridique;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.tiers.CategorieEntrepriseHelper;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.type.CategorieEntreprise;

/**
 * Classe de base implémentant détection de changement de catégorie.
 *
 * @author Raphaël Marmier, 2015-10-15
 */
public class ChangementCategorieAPMVersPM extends EvenementOrganisationInterne {

	private final RegDate dateAvant;
	private final RegDate dateApres;

	private final RegimeFiscal regimeFiscalCHAvant;
	private final RegimeFiscal regimeFiscalVDAvant;

	public ChangementCategorieAPMVersPM(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                    EvenementOrganisationContext context,
	                                    EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);

		dateApres = evenement.getDateEvenement();
		dateAvant = dateApres.getOneDayBefore();

		regimeFiscalCHAvant = getAndValidateOpen(extractRegimesFiscauxCH(), dateAvant);
		regimeFiscalVDAvant = getAndValidateOpen(extractRegimesFiscauxVD(), dateApres);

		// TODO: Ecrire plus de tests?

		// TODO: Générer événements fiscaux

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
	public void doHandle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {

		closeRegimesFiscauxOrdinairesCHVD(regimeFiscalCHAvant, regimeFiscalVDAvant, dateAvant);
		openRegimesFiscauxOrdinairesCHVD(getEntreprise(), getOrganisation(), dateApres);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {

		/*
		 Erreurs techniques fatale
		  */
		Assert.notNull(dateAvant);
		Assert.notNull(dateApres);
		Assert.isTrue(dateAvant.equals(dateApres.getOneDayBefore()));

		// Vérifier qu'il y a bien une entreprise préexistante en base ? (Ca ne devrait pas se produire ici)
		Assert.notNull(getEntreprise());

		final CategorieEntreprise categoryAvant = CategorieEntrepriseHelper.getCategorieEntreprise(getOrganisation(), dateAvant);
		final CategorieEntreprise categoryApres = CategorieEntrepriseHelper.getCategorieEntreprise(getOrganisation(), dateApres);
		Assert.isTrue(categoryAvant == CategorieEntreprise.APM && categoryApres == CategorieEntreprise.PM);

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
