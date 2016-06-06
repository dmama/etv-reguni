package ch.vd.uniregctb.evenement.organisation.interne.creation;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
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
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.type.CategorieEntreprise;

/**
 * Classe de base implémentant la création d'une entreprise et de son établissement principal dans Unireg.
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public abstract class CreateEntreprise extends EvenementOrganisationInterneDeTraitement {

	final private RegDate dateDeCreation;
	final private RegDate dateOuvertureFiscale;
	final private boolean isCreation;
	final private CategorieEntreprise category;
	final private SiteOrganisation sitePrincipal;
	final private Domicile autoriteFiscalePrincipale;

	protected CreateEntreprise(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                           EvenementOrganisationContext context,
	                           EvenementOrganisationOptions options,
	                           RegDate dateDeCreation,
	                           RegDate dateOuvertureFiscale,
	                           boolean isCreation) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);

		sitePrincipal = organisation.getSitePrincipal(getDateEvt()).getPayload();
		if (dateDeCreation == null) {
			throw new EvenementOrganisationException("Date nulle pour la création d'une entreprise. Probablement une erreur de programmation à ce stade..");
		}
		this.dateDeCreation = dateDeCreation;
		this.dateOuvertureFiscale = dateOuvertureFiscale;
		this.isCreation = isCreation;

		category = CategorieEntrepriseHelper.getCategorieEntreprise(getOrganisation(), getDateEvt());

		autoriteFiscalePrincipale = sitePrincipal.getDomicile(getDateEvt());

		// TODO: Ecrire plus de tests?

		// TODO: Générer événements fiscaux

		// TODO: Générer documents éditique
	}

	@NotNull
	public RegDate getDateDeCreation() {
		return dateDeCreation;
	}

	public RegDate getDateOuvertureFiscale() {
		return dateOuvertureFiscale;
	}

	public boolean isCreation() {
		return isCreation;
	}

	public CategorieEntreprise getCategory() {
		return category;
	}

	@NotNull
	public SiteOrganisation getSitePrincipal() {
		return sitePrincipal;
	}

	@NotNull
	public Domicile getAutoriteFiscalePrincipale() {
		return autoriteFiscalePrincipale;
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		// Création de l'entreprise
		createEntreprise(dateDeCreation, suivis);

		// Création de l'établissement principal
		createAddEtablissement(sitePrincipal.getNumeroSite(), autoriteFiscalePrincipale, true, dateDeCreation, suivis);
		if (dateDeCreation.isBefore(getDateEvt())) {
			appliqueDonneesCivilesSurPeriode(getEntreprise(), new DateRangeHelper.Range(dateDeCreation, getDateEvt().getOneDayBefore()), getDateEvt(), warnings, suivis);
		}

		// Création des établissement secondaires
		for (SiteOrganisation site : getOrganisation().getSitesSecondaires(getDateEvt())) {
			Etablissement etablissementSecondaire = addEtablissementSecondaire(site, dateDeCreation, warnings, suivis);
			if (dateDeCreation.isBefore(getDateEvt())) {
				appliqueDonneesCivilesSurPeriode(etablissementSecondaire, new DateRangeHelper.Range(dateDeCreation, getDateEvt().getOneDayBefore()), getDateEvt(), warnings, suivis);
			}
		}
		if (!isCreation()) {
			regleDateDebutPremierExerciceCommercial(getEntreprise(), dateDeCreation, suivis);
		}
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		/*
		 Erreurs techniques fatale
		  */
		Assert.notNull(dateDeCreation);

		// Vérifier qu'il n'y a pas d'entreprise préexistante en base ? (Ca ne devrait pas se produire ici)
		Assert.isNull(getEntreprise());

		/*
		 Problèmes métiers empêchant la progression
		  */

		if (sitePrincipal == null) {
			erreurs.addErreur(String.format("Aucun établissement principal trouvé pour la date du %s. [no organisation: %d]",
			                                RegDateHelper.dateToDisplayString(getDateDeCreation()), getOrganisation().getNumeroOrganisation()));
		}

		if (autoriteFiscalePrincipale == null) {
			erreurs.addErreur(String.format("Autorité fiscale introuvable pour la date du %s. [no organisation: %d]",
			                                RegDateHelper.dateToDisplayString(getDateDeCreation()), getOrganisation().getNumeroOrganisation()));
		}

		// TODO: Vérifier que le siège n'est pas sur une commune faîtière et passer en manuel si c'est le cas. (fractions de communes)


		// Vérifier la présence des autres données nécessaires ?
	}

	protected boolean inscritAuRC() {
		return OrganisationHelper.isInscritAuRC(getOrganisation(), getDateEvt());
	}
}
