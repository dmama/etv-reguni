package ch.vd.uniregctb.evenement.organisation.interne.creation;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.audit.Audit;
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
import ch.vd.uniregctb.evenement.organisation.interne.helper.EntrepriseHelper;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

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
		   TODO: Utiliser la date d'inscription au RC quand elle existe (cf. specs)
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

		final SiteOrganisation sitePrincipal = getOrganisation().getSitePrincipal(getDate()).getPayload();

		final Siege siegePrincipal = sitePrincipal.getSiege(getDate());
		if (siegePrincipal == null) { // Indique un établissement "probablement" à l'étranger. Nous ne savons pas traiter ce cas pour l'instant.
			throw new EvenementOrganisationException(
					String.format(
							"Siège introuvable pour le site principal %s de l'organisation %s %s. Site probablement à l'étranger. Impossible de créer le domicile de l'établissement principal.",
							sitePrincipal.getNumeroSite(), getNoOrganisation(), DateRangeHelper.rangeAt(getOrganisation().getNom(), getDate())));
		}
		// TODO: Vérifier que le siège n'est pas sur une commune faîtière et passer en manuel si c'est le cas. (fractions de communes)

		final Entreprise entreprise = EntrepriseHelper.createEntreprise(getOrganisation().getNumeroOrganisation(), getDate(), getContext());
		final Etablissement etablissementPrincipal = EntrepriseHelper.createEtablissement(sitePrincipal, true, getContext());
		final DomicileEtablissement domicilePrincipal = EntrepriseHelper.createDomicileEtablissement(etablissementPrincipal, siegePrincipal, getDate(), getContext());

		getContext().getTiersService().addRapport(new ActiviteEconomique(getDate(), null, entreprise, etablissementPrincipal), entreprise, etablissementPrincipal);
		// Ouverture For: date d'inscription au RC + un jour.
		getContext().getTiersService().addForPrincipal(entreprise, getDate().addDays(1), MotifFor.DEBUT_EXPLOITATION, null, null, MotifRattachement.DOMICILE,
		                                               domicilePrincipal.getNumeroOfsAutoriteFiscale(), domicilePrincipal.getTypeAutoriteFiscale());

		if (false) { // Non géré pour l'instant, en attente du métier
			for (SiteOrganisation site : getOrganisation().getSitesSecondaires(getDate())) {
				handleEtablissementsSecondaires(entreprise, domicilePrincipal, site);
			}
		}

		EntrepriseHelper.createAddBouclement(entreprise, getDate(), getContext());

		Audit.info(String.format("Entreprise créée avec le numéro %s", entreprise.getNumero()));
		return HandleStatus.TRAITE;
	}

	private void handleEtablissementsSecondaires(Entreprise entreprise, DomicileEtablissement domicilePrincipal, SiteOrganisation site) throws EvenementOrganisationException {
		long numeroSite = site.getNumeroSite();
		ensureNotExistsEtablissement(numeroSite);

		final Siege siege = site.getSiege(getDate());
		if (siege == null) {
			throw new EvenementOrganisationException(
					String.format(
							"Siège introuvable pour le site secondaire %s de l'organisation %s %s. Site probablement à l'étranger. Impossible pour le moment de créer le domicile de l'établissement secondaire.",
							numeroSite, getNoOrganisation(), DateRangeHelper.rangeAt(getOrganisation().getNom(), getDate())));
		}

		final List<Integer> autoritesAvecForSecondaire = new ArrayList<>();

		if (siege.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			Etablissement etablissement = EntrepriseHelper.createEtablissement(site, false, getContext());

			final DomicileEtablissement domicile = EntrepriseHelper.createDomicileEtablissement(etablissement, siege, getDate(), getContext());

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

	private void ensureNotExistsEtablissement(long numeroSite) throws EvenementOrganisationException {
		Etablissement etablissement = context.getTiersDAO().getEtablissementByNumeroSite(numeroSite);
		if (etablissement != null) {
			throw new EvenementOrganisationException(
					String.format("Trouvé un établissement existant %s pour l'organisation en création %s %s. Impossible de continuer.",
					              numeroSite, getNoOrganisation(), DateRangeHelper.rangeAt(getOrganisation().getNom(), getDate())));
		}
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {

		// Vérifier qu'on est bien en présence d'un type qu'on supporte.
		final CategorieEntreprise category = CategorieEntrepriseHelper.getCategorieEntreprise(getDate(), getOrganisation());
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
}
