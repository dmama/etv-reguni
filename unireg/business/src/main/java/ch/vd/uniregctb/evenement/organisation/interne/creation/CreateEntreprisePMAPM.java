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
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeRegimeFiscal;

/**
 * Evénement interne de création d'entreprise de catégories "Personne morale" et "Association Personne Morale" (PM et APM)
 *
 *  Spécification:
 *  - Ti01SE03-Identifier et traiter les mutations entreprise.doc - Version 0.6 - 08.09.2015
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntreprisePMAPM extends EvenementOrganisationInterne {

	protected CreateEntreprisePMAPM(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                EvenementOrganisationContext context,
	                                EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {

		final Entreprise entreprise = createEntreprise();

		final SiteOrganisation sitePrincipal = getSitePrincipal(getOrganisation(), getDate()).getPayload();
		final Etablissement etablissementPrincipal = createEtablissement(sitePrincipal, true);

		final Siege siegePrincipal = getSiege(sitePrincipal);
		if (siegePrincipal == null) { // Indique un établissement à l'étranger
			throw new EvenementOrganisationException(
					String.format("Siège introuvable pour le site principal %s de l'organisation %s %s. Site probablement à l'étranger. Impossible pour le moment de créer le domicile de l'établissement principal.",
					              etablissementPrincipal.getNumeroEtablissement(), getNoOrganisation(), DateRangeHelper.rangeAt(getOrganisation().getNom(), getDate())));
		}
		final DomicileEtablissement domicilePrincipal = createDomicileEtablissement(etablissementPrincipal, siegePrincipal);

		getContext().getTiersService().addRapport(new ActiviteEconomique(getDate(), null, entreprise, etablissementPrincipal), entreprise, etablissementPrincipal);

		getContext().getTiersService().addForPrincipal(entreprise, getDate(), MotifFor.DEBUT_EXPLOITATION, null, null, MotifRattachement.DOMICILE,
		                                               domicilePrincipal.getNumeroOfsAutoriteFiscale(), domicilePrincipal.getTypeAutoriteFiscale());

		for (SiteOrganisation site : getSitesSecondaires(getOrganisation(), getDate())) {
			final Siege siege = getSiege(site);
			final List<Integer> autoritesAvecForSecondaire = new ArrayList<>();

			// Si le siège est null, on considère qu'il est étranger
			if (siege != null && siege.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				final Etablissement etablissement = createEtablissement(site, false);

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

		createAddBouclement(entreprise);

		Audit.info(String.format("Entreprise créée avec le numéro %s", entreprise.getNumero()));
		return HandleStatus.TRAITE;
	}

	private void createAddBouclement(Entreprise entreprise) {
		final Bouclement bouclement = new Bouclement();
		bouclement.setEntreprise(entreprise);
		bouclement.setPeriodeMois(12);
		bouclement.setAncrage(DayMonth.get(12, 31));
		bouclement.setDateDebut(getDate());
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

		// Vérifier la présence des données nécessaires (no ofs siege, type de site, etc...)
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
