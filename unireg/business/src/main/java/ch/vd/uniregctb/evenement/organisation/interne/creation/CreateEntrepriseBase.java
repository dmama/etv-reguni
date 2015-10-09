package ch.vd.uniregctb.evenement.organisation.interne.creation;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.HandleStatus;
import ch.vd.uniregctb.evenement.organisation.interne.helper.CategorieEntreprise;
import ch.vd.uniregctb.evenement.organisation.interne.helper.CategorieEntrepriseHelper;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Classe de base implémentant la création d'une entreprise et de son établissement principal dans Unireg.
 *
 *  Spécification:
 *  - Ti01SE03-Identifier et traiter les mutations entreprise.doc - Version 1.1 - 23.09.2015
 *  - Ti02SE01-Créer automatiquement une entreprise.doc - Version 1.1 - 23.09.2015
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntrepriseBase extends EvenementOrganisationInterne {

	final private RegDate dateDeDebut;
	final private CategorieEntreprise category;
	final private SiteOrganisation sitePrincipal;
	final private Siege autoriteFiscalePrincipale;

	protected CreateEntrepriseBase(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                               EvenementOrganisationContext context,
	                               EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);

		/*
		  Demande du métier: date de référence pour la création à la date de l'événement + 1 jour
		  */
		dateDeDebut = getDateEvt().addDays(1);

		category = CategorieEntrepriseHelper.getCategorieEntreprise(getDateEvt(), getOrganisation());

		sitePrincipal = getOrganisation().getSitePrincipal(getDateEvt()).getPayload();

		autoriteFiscalePrincipale = determineAutoriteFiscalePrincipale(sitePrincipal, getDateEvt());

		// TODO: Ecrire plus de tests.

		// TODO: Générer événements fiscaux

		// TODO: Générer documents éditique
	}

	@NotNull
	public RegDate getDateDeDebut() {
		return dateDeDebut;
	}

	public CategorieEntreprise getCategory() {
		return category;
	}

	@NotNull
	public SiteOrganisation getSitePrincipal() {
		return sitePrincipal;
	}

	@NotNull
	public Siege getAutoriteFiscalePrincipale() {
		return autoriteFiscalePrincipale;
	}

	protected boolean inscritAuRC() {
		DonneesRC donneesRC = sitePrincipal.getDonneesRC();
		if (donneesRC != null) {
			DateRanged<StatusRC> statusRCDateRanged = DateRangeHelper.rangeAt(donneesRC.getStatus(), getDateEvt());
			if (statusRCDateRanged != null) {
				return statusRCDateRanged.getPayload() == StatusRC.INSCRIT;
			}
		}
		return false;
	}

	// TODO: A faire autrement
	protected Capital getCapital() {
		DonneesRC donneesRC = sitePrincipal.getDonneesRC();
		if (donneesRC != null) {
			return DateRangeHelper.rangeAt(donneesRC.getCapital(), getDateEvt());
		}
		return null;
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {

		// Création de l'entreprise
		createEntreprise(getOrganisation().getNumeroOrganisation(), dateDeDebut);

		// Création de l'établissement principal
		createAddEtablissement(sitePrincipal.getNumeroSite(), autoriteFiscalePrincipale, true, dateDeDebut);

		raiseStatusTo(HandleStatus.TRAITE);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {

		/*
		 Erreurs techniques fatale
		  */
		Assert.notNull(dateDeDebut);

		// Vérifier qu'il n'y a pas d'entreprise préexistante en base ? (Ca ne devrait pas se produire ici)
		Assert.isNull(getEntreprise());

		// TODO: Vérifier que la date de l'événement correspond bien à la date d'inscription au RC?
		// Devrait être superflu, cette exigeance étant une règle métier de RCEnt.

		/*
		 Problèmes métiers empêchant la progression
		  */

		if (sitePrincipal == null) {
			erreurs.addErreur(String.format("Aucun établissement principal trouvé pour la date du %s. [no organisation: %s]",
			                                RegDateHelper.dateToDisplayString(getDateDeDebut()), getOrganisation().getNumeroOrganisation()));
		}

		if (autoriteFiscalePrincipale == null) {
			erreurs.addErreur(String.format("Autorité fiscale introuvable pour la date du %s. [no organisation: %s]",
			                                RegDateHelper.dateToDisplayString(getDateDeDebut()), getOrganisation().getNumeroOrganisation()));
		}

		// TODO: Vérifier que le siège n'est pas sur une commune faîtière et passer en manuel si c'est le cas. (fractions de communes)


		// Vérifier la présence des autres données nécessaires ?
	}
}
