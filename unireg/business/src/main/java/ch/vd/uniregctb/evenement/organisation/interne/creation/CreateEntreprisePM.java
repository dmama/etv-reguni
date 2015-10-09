package ch.vd.uniregctb.evenement.organisation.interne.creation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.helper.CategorieEntreprise;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Evénement interne de création d'entreprise de catégorie "Personnes morales de droit public" (DP/PM)
 *
 *  Spécification:
 *  - Ti01SE03-Identifier et traiter les mutations entreprise.doc - Version 0.6 - 08.09.2015
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntreprisePM extends CreateEntrepriseBase {

	protected CreateEntreprisePM(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                             EvenementOrganisationContext context,
	                             EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.doHandle(warnings);

		Siege autoriteFiscalePrincipale = getAutoriteFiscalePrincipale();

		MotifFor motifOuverture = determineMotifOuvertureFor();

		openForFiscalPrincipal(getDateDeDebut(),
		                       autoriteFiscalePrincipale.getTypeAutoriteFiscale(),
		                       autoriteFiscalePrincipale.getNoOfs(),
		                       MotifRattachement.DOMICILE,
		                       motifOuverture, warnings);

		// Création du bouclement
		createAddBouclement(getDateDeDebut());

		// Gestion des sites secondaires non supportée pour l'instant, en attente du métier.
		if (false) {
			for (SiteOrganisation site : getOrganisation().getSitesSecondaires(getDateEvt())) {
				handleEtablissementsSecondaires(autoriteFiscalePrincipale, site, warnings);
			}
		}
	}

	private void handleEtablissementsSecondaires(Siege siegePrincipal, SiteOrganisation site, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		long numeroSite = site.getNumeroSite();
		ensureNotExistsEtablissement(numeroSite, getDateDeDebut());

		final Siege autoriteFiscale = determineAutoriteFiscaleSiteSecondaire(site, getDateEvt());

		final List<Integer> autoritesAvecForSecondaire = new ArrayList<>();

		if (autoriteFiscale.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			createAddEtablissement(site.getNumeroSite(), autoriteFiscale, false, getDateDeDebut());

			if (siegePrincipal.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
					(!autoritesAvecForSecondaire.contains(autoriteFiscale.getNoOfs()))) {
				openForFiscalSecondaire(getDateDeDebut(), autoriteFiscale.getTypeAutoriteFiscale(), autoriteFiscale.getNoOfs(), MotifRattachement.ETABLISSEMENT_STABLE, MotifFor.DEBUT_EXPLOITATION, warnings);
				autoritesAvecForSecondaire.add(autoriteFiscale.getNoOfs());
			}
		}
	}


	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings);

		Assert.state(getCategory() == CategorieEntreprise.PM, String.format("Catégorie d'entreprise non supportée! %s", getCategory()));

		DateRanged<FormeLegale> formeLegaleRange = DateRangeHelper.rangeAt(getOrganisation().getFormeLegale(), getDateDeDebut());
		if (getCategory() == null) {
			erreurs.addErreur(String.format("Catégorie introuvable pour l'organisation no %s de forme juridique %s, en date du %s.", getOrganisation().getNumeroOrganisation(),
			                                formeLegaleRange != null ? formeLegaleRange.getPayload() : "inconnue", RegDateHelper.dateToDisplayString(getDateDeDebut())));
		}

		if (!inscritAuRC()) {
			erreurs.addErreur("Inscription au RC manquante.");
		}
	}
}
