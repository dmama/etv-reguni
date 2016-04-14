package ch.vd.uniregctb.evenement.organisation.interne.creation;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

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
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Classe de base implémentant la création d'une entreprise et de son établissement principal dans Unireg.
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public abstract class CreateEntreprise extends EvenementOrganisationInterneDeTraitement {

	final private RegDate dateDeCreation;
	final private CategorieEntreprise category;
	final private SiteOrganisation sitePrincipal;
	final private Domicile autoriteFiscalePrincipale;

	protected CreateEntreprise(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                           EvenementOrganisationContext context,
	                           EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);

		sitePrincipal = organisation.getSitePrincipal(getDateEvt()).getPayload();

		/*
		  Demande du métier: date de référence pour la création à la date de l'événement + 1 jour
		  */
		if (organisation.isInscritAuRC(getDateEvt())) {
			if (isCreation()) {
				if (sitePrincipal.getDomicile(getDateEvt()).getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					dateDeCreation = sitePrincipal.getDateInscriptionRCVd(getDateEvt()).getOneDayAfter();
					if (dateDeCreation == null) {
						throw new EvenementOrganisationException("Date d'inscription au régistre vaudois du commerce introuvable pour l'établissement principal en création.");
					}
				}
				else {
					dateDeCreation = sitePrincipal.getDateInscriptionRC(getDateEvt()).getOneDayAfter();
				}
			} else { // Une arrivée
				dateDeCreation = sitePrincipal.getDateInscriptionRCVd(getDateEvt());
				if (dateDeCreation == null) {
					throw new EvenementOrganisationException("Date d'inscription au régistre vaudois du commerce introuvable pour l'établissement principal en création.");
				}
			}
		} else {
			if (isCreation()) {
				dateDeCreation = getDateEvt().getOneDayAfter();
			} else {
				dateDeCreation = getDateEvt();
			}
		}

		category = CategorieEntrepriseHelper.getCategorieEntreprise(getOrganisation(), getDateEvt());

		autoriteFiscalePrincipale = sitePrincipal.getDomicile(getDateEvt());

		if (autoriteFiscalePrincipale == null) { // Indique un établissement "probablement" à l'étranger. Nous ne savons pas traiter ce cas pour l'instant.
			throw new EvenementOrganisationException(
					String.format(
							"Autorité fiscale (siège) introuvable pour le site principal %s de l'organisation %s %s. Site probablement à l'étranger. Impossible de créer le domicile de l'établissement principal.",
							sitePrincipal.getNumeroSite(), getNoOrganisation(), getOrganisation().getNom(getDateEvt())));
		}

		// TODO: Ecrire plus de tests?

		// TODO: Générer événements fiscaux

		// TODO: Générer documents éditique
	}

	@NotNull
	public RegDate getDateDeCreation() {
		return dateDeCreation;
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

		// Création des établissement secondaires
		for (SiteOrganisation site : getOrganisation().getSitesSecondaires(getDateEvt())) {
			addEtablissementSecondaire(site, getDateEvt(), warnings, suivis);
		}
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {

		/*
		 Erreurs techniques fatale
		  */
		Assert.notNull(dateDeCreation);

		// Vérifier qu'il n'y a pas d'entreprise préexistante en base ? (Ca ne devrait pas se produire ici)
		Assert.isNull(getEntreprise());

		// TODO: Vérifier que la date de l'événement correspond bien à la date d'inscription au RC?
		// Devrait être superflu, cette exigeance étant une règle métier de RCEnt.

		/*
		 Problèmes métiers empêchant la progression
		  */

		if (sitePrincipal == null) {
			erreurs.addErreur(String.format("Aucun établissement principal trouvé pour la date du %s. [no organisation: %s]",
			                                RegDateHelper.dateToDisplayString(getDateDeCreation()), getOrganisation().getNumeroOrganisation()));
		}

		if (autoriteFiscalePrincipale == null) {
			erreurs.addErreur(String.format("Autorité fiscale introuvable pour la date du %s. [no organisation: %s]",
			                                RegDateHelper.dateToDisplayString(getDateDeCreation()), getOrganisation().getNumeroOrganisation()));
		}

		// TODO: Vérifier que le siège n'est pas sur une commune faîtière et passer en manuel si c'est le cas. (fractions de communes)


		// Vérifier la présence des autres données nécessaires ?
	}

	protected boolean inscritAuRC() {
		return OrganisationHelper.isInscritAuRC(getOrganisation(), getDateEvt());
	}
}
