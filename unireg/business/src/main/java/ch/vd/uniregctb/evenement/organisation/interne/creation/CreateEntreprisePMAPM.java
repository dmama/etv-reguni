package ch.vd.uniregctb.evenement.organisation.interne.creation;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.CategorieEntreprise;
import ch.vd.uniregctb.evenement.organisation.interne.CategorieEntrepriseHelper;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.HandleStatus;
import ch.vd.uniregctb.evenement.organisation.interne.helper.BouclementHelper;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeRegimeFiscal;

/**
 * Evénement interne de création d'entreprise de catégories "Personne morale" et "Association Personne Morale" (PM et APM)
 *
 *  Spécification:
 *  - Ti01SE03-Identifier et traiter les mutations entreprise.doc - Version 1.1 - 23.09.2015
 *  - Ti02SE01-Créer automatiquement une entreprise.doc - Version 1.1 - 23.09.2015
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntreprisePMAPM extends EvenementOrganisationInterne {

	protected CreateEntreprisePMAPM(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                EvenementOrganisationContext context,
	                                EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);

		/*
		   TODO: Utiliser la date d'inscription au RC quand elle existe (cf. specs?)
		   - Implique pas mal de tests
		   - Peut-elle être différente de la date de l'annonce? Elle ne devrait pas, d'après les specs.
		   - Si elle peut l'être, c'est qu'on a un cas de correction. On devrait le traiter en manuel?
		  */

		// TODO: Ecrire les tests.

		// TODO: Générer événements fiscaux

		// TODO: Générer documents éditique
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {

		final SiteOrganisation sitePrincipal = getSitePrincipal(getOrganisation(), getDate()).getPayload();

		final Siege siegePrincipal = getSiege(sitePrincipal);
		if (siegePrincipal == null) { // Indique un établissement "probablement" à l'étranger, que nous ne savons pas traiter pour l'instant.
			throw new EvenementOrganisationException(
					String.format(
							"Siège introuvable pour le site principal %s de l'organisation %s %s. Site probablement à l'étranger. Impossible pour le moment de créer le domicile de l'établissement principal.",
							sitePrincipal.getNumeroSite(), getNoOrganisation(), DateRangeHelper.rangeAt(getOrganisation().getNom(), getDate())));
		}
		// TODO: Vérifier que le siège n'est pas sur une commune faîtière et passer en manuel si c'est le cas. (fractions de communes)

		final Entreprise entreprise = createEntreprise();
		final Etablissement etablissementPrincipal = createEtablissement(sitePrincipal, true);
		final DomicileEtablissement domicilePrincipal = createDomicileEtablissement(etablissementPrincipal, siegePrincipal);

		getContext().getTiersService().addRapport(new ActiviteEconomique(getDate(), null, entreprise, etablissementPrincipal), entreprise, etablissementPrincipal);
		// Ouverture For: date d'inscription au RC + un jour.
		getContext().getTiersService().addForPrincipal(entreprise, getDate().addDays(1), MotifFor.DEBUT_EXPLOITATION, null, null, MotifRattachement.DOMICILE,
		                                               domicilePrincipal.getNumeroOfsAutoriteFiscale(), domicilePrincipal.getTypeAutoriteFiscale());

		if (false) { // Non géré pour l'instant, en attente du métier
			for (SiteOrganisation site : getSitesSecondaires(getOrganisation(), getDate())) {
				handleEtablissementsSecondaires(entreprise, domicilePrincipal, site);
			}
		}

		createAddBouclement(entreprise, getDate());

		Audit.info(String.format("Entreprise créée avec le numéro %s", entreprise.getNumero()));
		return HandleStatus.TRAITE;
	}

	private void handleEtablissementsSecondaires(Entreprise entreprise, DomicileEtablissement domicilePrincipal, SiteOrganisation site) throws EvenementOrganisationException {
		Etablissement etablissement = context.getTiersDAO().getEtablissementByNumeroSite(site.getNumeroSite());
		if (etablissement != null) {
			throw new EvenementOrganisationException(
					String.format("Trouvé un établissement existant %s pour l'organisation en création %s %s. Impossible de continuer.",
					              site.getNumeroSite(), getNoOrganisation(), DateRangeHelper.rangeAt(getOrganisation().getNom(), getDate())));
		}

		final Siege siege = getSiege(site);
		if (siege == null) {
			throw new EvenementOrganisationException(
					String.format(
							"Siège introuvable pour le site secondaire %s de l'organisation %s %s. Site probablement à l'étranger. Impossible pour le moment de créer le domicile de l'établissement secondaire.",
							site.getNumeroSite(), getNoOrganisation(), DateRangeHelper.rangeAt(getOrganisation().getNom(), getDate())));
		}

		final List<Integer> autoritesAvecForSecondaire = new ArrayList<>();

		if (siege.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			etablissement = createEtablissement(site, false);

			final DomicileEtablissement domicile = createDomicileEtablissement(etablissement, siege);

			getContext().getTiersService().addRapport(new ActiviteEconomique(getDate(), null, entreprise, etablissement), entreprise, etablissement);

			if (domicilePrincipal.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
					(!autoritesAvecForSecondaire.contains(domicile.getNumeroOfsAutoriteFiscale()))) {
				getContext().getTiersService().addForSecondaire(entreprise, getDate(), null,
				                                                MotifRattachement.ETABLISSEMENT_STABLE, domicile.getNumeroOfsAutoriteFiscale(),
				                                                TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifFor.DEBUT_EXPLOITATION, null);
				autoritesAvecForSecondaire.add(siege.getNoOfs());
			}
		}
	}

	private void createAddBouclement(Entreprise entreprise, RegDate creationDate) {
		final Bouclement bouclement = BouclementHelper.createBouclementSelonSemestre(creationDate);
		bouclement.setEntreprise(entreprise);
		getContext().getTiersDAO().addAndSave(entreprise, bouclement);
	}

	private Siege getSiege(SiteOrganisation site) throws EvenementOrganisationException {
		Siege siege = null;
		if (site.getSieges() != null) {
			siege = DateRangeHelper.rangeAt(site.getSieges(), getDate());
			if (siege == null) {
				throw new EvenementOrganisationException(String.format("Siège introuvable pour le site %s de l'organisation %s %s",
				                                                       site.getNumeroSite(), getNoOrganisation(), DateRangeHelper.rangeAt(getOrganisation().getNom(), getDate())));
			}
		}
		return siege;
	}

	@NotNull
	private DomicileEtablissement createDomicileEtablissement(Etablissement etablissement, Siege siege) {
		final DomicileEtablissement domicile = new DomicileEtablissement(getDate(), null, siege.getTypeAutoriteFiscale(), siege.getNoOfs(), etablissement);
		return getContext().getTiersDAO().addAndSave(etablissement, domicile);
	}

	@NotNull
	private Etablissement createEtablissement(SiteOrganisation site, boolean principal) {
		Audit.info(String.format("Création d'un établissement %s pour l'organisation %s", getNoOrganisation(), principal ? "principal" : "secondaire"));
		final Etablissement etablissement = new Etablissement();
		etablissement.setNumeroEtablissement(site.getNumeroSite());
		etablissement.setPrincipal(true);
		return (Etablissement) getContext().getTiersDAO().save(etablissement);
	}

	@NotNull
	private Entreprise createEntreprise() {
		Audit.info(String.format("Création d'une entreprise pour l'organisation %s", getNoOrganisation()));
		final Entreprise entreprise = new Entreprise();
		// Le numéro
		entreprise.setNumeroEntreprise(getNoOrganisation());
		// Le régime fiscal VD + CH
		entreprise.addRegimeFiscal(new RegimeFiscal(getDate(), null, RegimeFiscal.Portee.CH, TypeRegimeFiscal.ORDINAIRE));
		entreprise.addRegimeFiscal(new RegimeFiscal(getDate(), null, RegimeFiscal.Portee.VD, TypeRegimeFiscal.ORDINAIRE));
		return (Entreprise) getContext().getTiersDAO().save(entreprise);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {

		// Vérifier qu'on est bien en présence d'un type qu'on supporte.
		final CategorieEntreprise category = getCategorieEntreprise(getDate(), getOrganisation());
		if (!(category == CategorieEntreprise.PM) && !(category == CategorieEntreprise.APM)) {
			erreurs.addErreur(String.format("Catégorie d'entreprise non supportée! %s", category));
		}

		// Vérifier qu'il n'y a pas d'entreprise préexistante en base ?
		if (getEntreprise() != null) {
			erreurs.addErreur(String.format("Une entreprise no %s de type %s existe déjà dans Unireg pour l'organisation %s:%s!",
			                                getEntreprise().getNumero(),
			                                getEntreprise().getType(),
											getNoOrganisation(),
											DateRangeHelper.rangeAt(getOrganisation().getNom(), getDate())));
		}
		// Vérifier la présence des données nécessaires (no ofs siege, type de site, dateRC pour une PM, etc...)
	}

	// TODO: A déplacer dans RCEntOrganisation avec tests et version mock, ou alors mieux, générer dans l'adapter.

	private List<DateRanged<SiteOrganisation>> getSitePrincipaux(Organisation organisation) {
		List<DateRanged<SiteOrganisation>> sitePrincipaux = new ArrayList<>();
		for (SiteOrganisation site : organisation.getDonneesSites()) {
			for (DateRanged<TypeDeSite> siteRange : site.getTypeDeSite()) {
				if (siteRange != null && siteRange.getPayload() == TypeDeSite.ETABLISSEMENT_PRINCIPAL) {
					sitePrincipaux.add(new DateRanged<>(siteRange.getDateDebut(), siteRange.getDateFin(), site));
				}
			}
		}
		return sitePrincipaux;
	}

	private DateRanged<SiteOrganisation> getSitePrincipal(Organisation organisation, RegDate date) {
		return DateRangeHelper.rangeAt(getSitePrincipaux(organisation), date);
	}

	private List<SiteOrganisation> getSitesSecondaires(Organisation organisation, RegDate date) {
		List<SiteOrganisation> siteSecondaires = new ArrayList<>();
		for (SiteOrganisation site : organisation.getDonneesSites()) {
			for (DateRanged<TypeDeSite> siteRange : site.getTypeDeSite()) {
				if (siteRange != null && siteRange.getPayload() == TypeDeSite.ETABLISSEMENT_SECONDAIRE && siteRange.isValidAt(date)) {
					siteSecondaires.add(site);
				}
			}
		}
		return siteSecondaires;
	}

	// TODO: Déplacer dans l'adapter?
	@Nullable
	private static CategorieEntreprise getCategorieEntreprise(RegDate date, Organisation organisation) {
		final DateRanged<FormeLegale> fl = DateRangeHelper.rangeAt(organisation.getFormeLegale(), date);
		return fl == null ? null : CategorieEntrepriseHelper.map(fl.getPayload());
	}

}
