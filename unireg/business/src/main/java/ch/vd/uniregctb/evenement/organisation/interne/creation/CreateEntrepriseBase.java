package ch.vd.uniregctb.evenement.organisation.interne.creation;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
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
 * Evénement interne de création d'entreprise de catégories "Personne morale" et "Association Personne Morale" (PM et APM)
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

	public RegDate getDateDeDebut() {
		return dateDeDebut;
	}

	public CategorieEntreprise getCategory() {
		return category;
	}

	public SiteOrganisation getSitePrincipal() {
		return sitePrincipal;
	}

	public Siege getAutoriteFiscalePrincipale() {
		return autoriteFiscalePrincipale;
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {

		// Création de l'entreprise
		createEntreprise(getOrganisation().getNumeroOrganisation(), dateDeDebut);

		// Création de l'établissement principal
		createAddEtablissement(sitePrincipal.getNumeroSite(), autoriteFiscalePrincipale, true, dateDeDebut);

		return HandleStatus.TRAITE;
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {

		// TODO: Vérifier que le siège n'est pas sur une commune faîtière et passer en manuel si c'est le cas. (fractions de communes)

		// TODO: Vérifier que la date de l'événement correspond bien à la date d'inscription au RC?
		// Devrait être superflu, cette exigeance étant inscrite profondément dans RCEnt.

		// Vérifier qu'on est bien en présence d'un type qu'on supporte.
		if (!(category == CategorieEntreprise.PM) && !(category == CategorieEntreprise.APM)) {
			erreurs.addErreur(String.format("Catégorie d'entreprise non supportée! %s", category));
		}

		// Vérifier qu'il n'y a pas d'entreprise préexistante en base ?
		if (getEntreprise() != null) {
			erreurs.addErreur(String.format("Une entreprise no %s de type %s existe déjà dans Unireg pour l'organisation %s:%s!",
			                                getEntreprise().getNumero(),
			                                getEntreprise().getType(),
											getNoOrganisation(),
											DateRangeHelper.rangeAt(getOrganisation().getNom(), getDateEvt())));
		}

		// Vérifier la présence des données nécessaires (no ofs siege, type de site, dateRC pour une PM, etc...)?
	}
}
