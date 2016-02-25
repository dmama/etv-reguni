package ch.vd.uniregctb.evenement.organisation.interne.creation;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

/**
 * Evénement interne de création d'entreprise de catégorie "Personnes morales de droit public" (DP/PM)
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntrepriseDPPM extends CreateEntreprise {

	protected CreateEntrepriseDPPM(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                               EvenementOrganisationContext context,
	                               EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public String describe() {
		return "Création d'une entreprise de catégorie DPPM";
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		super.doHandle(warnings, suivis);

		MotifFor motifOuverture = determineMotifOuvertureFor();

		openForFiscalPrincipal(getDateDeDebut(),
		                       getAutoriteFiscalePrincipale(),
		                       MotifRattachement.DOMICILE,
		                       motifOuverture,
		                       warnings, suivis);

		// Création du bouclement
		createAddBouclement(getDateDeDebut(), suivis);

		// Ajoute les for secondaires
		openForSecondairesPourEtablissementsVD(getDateDeDebut(), getEntreprise(), warnings, suivis);

		warnings.addWarning("Une vérification manuelle est requise pour nouvelle entreprise de type DP/PM.");
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings);

		if (getCategory() == null) {
			FormeLegale formeLegale = getOrganisation().getFormeLegale(getDateDeDebut());
			erreurs.addErreur(String.format("Catégorie introuvable pour l'organisation no %s de forme juridique %s, en date du %s.", getOrganisation().getNumeroOrganisation(),
			                                formeLegale != null ? formeLegale : "inconnue", RegDateHelper.dateToDisplayString(getDateDeDebut())));
		}

		Assert.state(getCategory() == CategorieEntreprise.DPPM, String.format("Catégorie d'entreprise non supportée! %s", getCategory()));

		if (!hasCapital()) {
			erreurs.addErreur(String.format("Création impossible, capital introuvable. %s", getOrganisationDescription()));
		}
	}
}
