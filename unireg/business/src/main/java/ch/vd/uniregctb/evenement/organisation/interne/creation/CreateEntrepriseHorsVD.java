package ch.vd.uniregctb.evenement.organisation.interne.creation;

import org.springframework.util.Assert;

import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.tiers.CategorieEntrepriseHelper;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

/**
 * Evénement interne de création d'entreprises dont le siège principal est hors VD
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntrepriseHorsVD extends CreateEntreprise {

	protected CreateEntrepriseHorsVD(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                 EvenementOrganisationContext context,
	                                 EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public String describe() {
		return "Création d'une entreprise hors VD";
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		super.doHandle(warnings, suivis);

		final CategorieEntreprise categorieEntreprise = CategorieEntrepriseHelper.getCategorieEntreprise(getOrganisation(), getDateEvt());
		if (categorieEntreprise != null) {
			switch (categorieEntreprise) {
			case DPPM:
				if (hasCapital(getOrganisation(), getDateEvt())) {
					Domicile autoriteFiscalePrincipale1 = getAutoriteFiscalePrincipale();

					openForFiscalPrincipal(getDateDeDebut(),
					                       autoriteFiscalePrincipale1,
					                       MotifRattachement.DOMICILE,
					                       MotifFor.DEBUT_EXPLOITATION,
					                       GenreImpot.BENEFICE_CAPITAL,
					                       warnings, suivis);

					// Création du bouclement
					createAddBouclement(getDateDeDebut(), suivis);

					// Ajoute les for secondaires
					adapteForsSecondairesPourEtablissementsVD(getEntreprise(), getDateDeDebut(), warnings, suivis);
					warnings.addWarning("Une vérification manuelle est requise pour nouvelle entreprise de catégorie « DP/PM » avec capital non nul.");
				}
				break;
			case PM:
			case APM:
				Domicile autoriteFiscalePrincipale1 = getAutoriteFiscalePrincipale();

				openForFiscalPrincipal(getDateDeDebut(),
				                       autoriteFiscalePrincipale1,
				                       MotifRattachement.DOMICILE,
				                       MotifFor.DEBUT_EXPLOITATION,
				                       GenreImpot.BENEFICE_CAPITAL,
				                       warnings, suivis);

				// Création du bouclement
				createAddBouclement(getDateDeDebut(), suivis);

				// Ajoute les for secondaires
				adapteForsSecondairesPourEtablissementsVD(getEntreprise(), getDateDeDebut(), warnings, suivis);
				break;
			case SP:
				Domicile autoriteFiscalePrincipale = getAutoriteFiscalePrincipale();

				openForFiscalPrincipal(getDateDeDebut(),
				                       autoriteFiscalePrincipale,
				                       MotifRattachement.DOMICILE,
				                       MotifFor.DEBUT_EXPLOITATION,
				                       GenreImpot.REVENU_FORTUNE,
				                       warnings, suivis);
				warnings.addWarning("Une vérification manuelle est requise pour une nouvelle entreprise de catégorie SP dont le siège est hors Canton avec étab. sur VD");
				break;
			case FP:
				warnings.addWarning("Une vérification manuelle est requise pour nouvelle entreprise de type Fond de placements (FDS FLAC).");
				break;
			default:
				// Rien à faire
			}
		}
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings);

		if (getCategory() != null) {
			Assert.state(getCategory() != CategorieEntreprise.PP, String.format("Catégorie d'entreprise non supportée! %s", getCategory()));
		}
	}
}
