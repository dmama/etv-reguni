package ch.vd.uniregctb.evenement.organisation.interne.creation;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterneDeTraitement;
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
public class CreateEntrepriseHorsVD extends EvenementOrganisationInterneDeTraitement {

	RegDate dateDeCreation;
	boolean isCreation;

	final private CategorieEntreprise category;
	final private SiteOrganisation sitePrincipal;
	final private Domicile autoriteFiscalePrincipale;

	protected CreateEntrepriseHorsVD(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                 EvenementOrganisationContext context,
	                                 EvenementOrganisationOptions options,
	                                 RegDate dateDeCreation,
	                                 boolean isCreation) {
		super(evenement, organisation, entreprise, context, options);

		this.dateDeCreation = dateDeCreation;
		this.isCreation = isCreation;

		sitePrincipal = organisation.getSitePrincipal(getDateEvt()).getPayload();

		autoriteFiscalePrincipale = sitePrincipal.getDomicile(getDateEvt());

		category = CategorieEntrepriseHelper.getCategorieEntreprise(getOrganisation(), getDateEvt());

	}

	@Override
	public String describe() {
		return "Création d'une entreprise hors VD";
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		final String messageWarning = "Une vérification manuelle est requise pour une nouvelle entreprise de catégorie « %s » dont le siège est Hors Canton avec étab. sur VD.";

		/*
		 * Comme la solution actuelle est d'utiliser la date d'événement comme début des fors secondaires (date correspondant au début des périodes de domiciles),
		 * on fait de même pour l'entreprise, les établissements et for principal hors canton.
		 */

		// Création de l'entreprise
		createEntreprise(getDateEvt(), suivis);

		// Création de l'établissement principal
		createAddEtablissement(sitePrincipal.getNumeroSite(), autoriteFiscalePrincipale, true, getDateEvt(), suivis);

		// Création des établissement secondaires
		for (SiteOrganisation site : getOrganisation().getSitesSecondaires(getDateEvt())) {
			addEtablissementSecondaire(site, getDateEvt(), warnings, suivis);
		}

		if (category != null) {
			switch (category) {
			case DPPM:
				if (hasCapital(getOrganisation(), getDateEvt())) {
					openForFiscalPrincipal(getDateEvt(),
					                       autoriteFiscalePrincipale,
					                       MotifRattachement.DOMICILE,
					                       MotifFor.DEBUT_EXPLOITATION,
					                       GenreImpot.BENEFICE_CAPITAL,
					                       warnings, suivis);

					// Création du bouclement
					createAddBouclement(getDateEvt(), isCreation, suivis);

					// Ajoute les for secondaires
					adapteForsSecondairesPourEtablissementsVD(getEntreprise(), getDateEvt(), warnings, suivis);
					warnings.addWarning(String.format(messageWarning , category.getLibelle() + " (capital non nul)"));
				}
				break;
			case PM:
			case APM:
				openForFiscalPrincipal(getDateEvt(),
				                       autoriteFiscalePrincipale,
				                       MotifRattachement.DOMICILE,
				                       MotifFor.DEBUT_EXPLOITATION,
				                       GenreImpot.BENEFICE_CAPITAL,
				                       warnings, suivis);

				// Création du bouclement
				createAddBouclement(getDateEvt(), isCreation, suivis);

				// Ajoute les for secondaires
				adapteForsSecondairesPourEtablissementsVD(getEntreprise(), getDateEvt(), warnings, suivis);
				break;
			case SP:
				openForFiscalPrincipal(getDateEvt(),
				                       autoriteFiscalePrincipale,
				                       MotifRattachement.DOMICILE,
				                       MotifFor.DEBUT_EXPLOITATION,
				                       GenreImpot.REVENU_FORTUNE,
				                       warnings, suivis);
				break;
			case FP:
				warnings.addWarning(String.format(messageWarning, category.getLibelle()));
				break;
			default:
				// Rien à faire
			}
		}
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		if (category != null) {
			Assert.state(category != CategorieEntreprise.PP, String.format("Catégorie d'entreprise non supportée! %s", category));
		}
	}


}
