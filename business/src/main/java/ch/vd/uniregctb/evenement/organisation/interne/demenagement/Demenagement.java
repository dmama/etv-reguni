package ch.vd.uniregctb.evenement.organisation.interne.demenagement;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
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
import ch.vd.uniregctb.evenement.organisation.interne.HandleStatus;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.utils.RangeUtil;

/**
 * @author Raphaël Marmier, 2015-10-13
 */
public abstract class Demenagement extends EvenementOrganisationInterneDeTraitement {

	private final RegDate dateAvant;
	private final RegDate dateApres;

	private final SiteOrganisation sitePrincipalAvant;
	private final SiteOrganisation sitePrincipalApres;

	private final Domicile siegeAvant;
	private final Domicile siegeApres;

	private final Etablissement etablissementPrincipalAvant;
	private final Etablissement etablissementPrincipalApres;

	public Demenagement(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                    EvenementOrganisationContext context,
	                    EvenementOrganisationOptions options,
	                    Domicile siegeAvant,
	                    Domicile siegeApres) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);

		dateApres = evenement.getDateEvenement();
		dateAvant = dateApres.getOneDayBefore();

		this.siegeAvant = siegeAvant;
		this.siegeApres = siegeApres;

		final DateRanged<SiteOrganisation> sitePrincipalRange = organisation.getSitePrincipal(dateAvant);
		if (sitePrincipalRange != null) {
			sitePrincipalAvant = sitePrincipalRange.getPayload();
			etablissementPrincipalAvant = getEtablissementByNumeroSite(sitePrincipalAvant.getNumeroSite());
		} else {
			sitePrincipalAvant = null;
			etablissementPrincipalAvant = RangeUtil.getAssertLast(context.getTiersService().getEtablissementsPrincipauxEntreprise(entreprise), dateAvant).getPayload();
		}
		sitePrincipalApres = organisation.getSitePrincipal(dateApres).getPayload();
		etablissementPrincipalApres = getEtablissementByNumeroSite(sitePrincipalApres.getNumeroSite());
	}

	@Override
	public abstract void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException;

	/**
	 * Changement de siège principal avec établissement stable. Déménage le for principal s'il en existe un.
	 *
	 * @param etablissementPrincipal L'établissement dont l'autorité fiscale change.
	 * @param dateDebutNouveauSiege la date à laquelle on souhaite que le déménagement soit effectif
	 * @param motifFor Le motif du changement
	 * @param warnings
	 */
	protected void changeSiegeEtablissement(Etablissement etablissementPrincipal, RegDate dateDebutNouveauSiege, MotifFor motifFor, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		signaleDemenagement(etablissementPrincipal, getSiegeAvant(), getSiegeApres(), dateDebutNouveauSiege, suivis);

		final ForFiscalPrincipal forFiscalPrincipal = getEntreprise().getForFiscalPrincipalAt(dateDebutNouveauSiege.getOneDayBefore());

		if (forFiscalPrincipal != null && forFiscalPrincipal.isValidAt(dateDebutNouveauSiege)) {
			GenreImpot genreImpot = forFiscalPrincipal.getGenreImpot();
			closeForFiscalPrincipal(dateDebutNouveauSiege.getOneDayBefore(), motifFor, suivis);
			openForFiscalPrincipal(dateDebutNouveauSiege, getSiegeApres(), MotifRattachement.DOMICILE, motifFor, genreImpot, warnings, suivis);
		}
		else {
			// SIFISC-23172: Ne pas laisser à l'état redondant lorsqu'il n'y a rien à faire. Cette bidouille doit disparaître avec une vraie prise en charge de la redondance.
			raiseStatusTo(HandleStatus.TRAITE);
		}
	}

	/**
	 * Effectue un changement de siège. Il y a effectivement trois types de changements de siège:
	 * - Changement de l'autorité fiscale de l'établissement principal
	 * - Changement de l'établissement fiscal
	 * - Une combinaison des deux résultant en un changment d'autorité fiscale du siège principal en vigueur.
	 *
	 * TODO: Traiter les deux compposantes du problème séparément? (changement de l'établissement, et déplacement de l'autorité fiscale prnicipale)
	 *
	 * @param motifFor Le motif d'ouverture/fermeture du for
	 * @param warnings
	 * @throws EvenementOrganisationException
	 */
	protected void effectueChangementSiege(MotifFor motifFor, RegDate dateDebutNouveauSiege, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		// On a affaire à un établissement inconnu
		final Etablissement etablissementPrincipalApres = getEtablissementPrincipalApres();
		if (etablissementPrincipalApres == null) {
			throw new EvenementOrganisationException("Changement de siège avec création d'un nouvel établissement principal. Veuillez traiter l'événement manuellement.");
		}
		// Ok, on connait le nouvel etablissement
		else {
			// On est sur le même établissement -> changer le domicile, fermer l'ancien for et ouvrir un nouveau (s'il n'y a pas d'établissement avant, on considère qu'il n'y a pas de changement)
			if (getEtablissementPrincipalAvant().getNumero().equals(etablissementPrincipalApres.getNumero())) {
				changeSiegeEtablissement(etablissementPrincipalApres, dateDebutNouveauSiege, motifFor, warnings, suivis);
			}
			// On n'est pas sur le même établissement -> Changer les domiciles de chaque établissement pour refléter les domiciles respectifs, fermer l'ancien for et ouvrir un nouveau.
			else {
				throw new EvenementOrganisationException("Changement de siège avec changement d'établissement principal. Veuillez traiter l'événement manuellement.");
			}
		}
	}

	protected void verifieSurchargeAcceptable(RegDate dateDebutSurcharge, SurchargeCorrectiveRange surchargeCorrectiveRange) throws EvenementOrganisationException {
		if (!surchargeCorrectiveRange.isAcceptable()) {
			throw new EvenementOrganisationException(
					String.format("Refus de créer dans Unireg surcharge corrective remontant à %s, %d jours avant la date de l'événement. La tolérance étant de %d jours. " +
							              "Il y a probablement une erreur d'identification ou un problème de date.",
					              RegDateHelper.dateToDisplayString(dateDebutSurcharge),
					              surchargeCorrectiveRange.getEtendue(),
					              OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC)
			);
		}
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		/*
		 Erreurs techniques fatale
		  */
		Assert.notNull(dateAvant);
		Assert.notNull(dateApres);
		Assert.isTrue(dateAvant.equals(dateApres.getOneDayBefore()));

		// Vérifier qu'il y a bien une entreprise préexistante en base ? (Ca ne devrait pas se produire ici)
		Assert.notNull(getEntreprise());

		// On doit avoir deux autorités fiscales
		Assert.isTrue(
				(getSiegeAvant() != null && getSiegeApres() != null)
		);

		Assert.notNull(getEtablissementPrincipalAvant().getNumeroEtablissement(), "L'établissement principal ne semble pas rattaché à son pendant civil RCEnt!");
		Assert.notNull(getEtablissementPrincipalApres().getNumeroEtablissement(), "L'établissement principal ne semble pas rattaché à son pendant civil RCEnt!");

		// Ce serait étrange de ne pas avoir de changement finalement
		Assert.isTrue(getSiegeAvant() != getSiegeApres(), "Pas un déménagement de siège, la commune n'a pas changé!");

		// Si on n'a pas d'établissement principal après, c'est qu'on ne l'a pas trouvé en recherchant avec le numéro de site principal après, donc ce dernier est nouveau.
		if (!getEtablissementPrincipalAvant().getNumeroEtablissement().equals(sitePrincipalApres.getNumeroSite())) {
			erreurs.addErreur("Changement de siège avec changement d'établissement principal. Veuillez traiter l'événement manuellement.");
		}
	}

	public RegDate getDateAvant() {
		return dateAvant;
	}

	public RegDate getDateApres() {
		return dateApres;
	}

	public Domicile getSiegeAvant() {
		return siegeAvant;
	}

	public Domicile getSiegeApres() {
		return siegeApres;
	}

	public SiteOrganisation getSitePrincipalAvant() {
		return sitePrincipalAvant;
	}

	public SiteOrganisation getSitePrincipalApres() {
		return sitePrincipalApres;
	}

	public Etablissement getEtablissementPrincipalAvant() {
		return etablissementPrincipalAvant;
	}

	public Etablissement getEtablissementPrincipalApres() {
		return etablissementPrincipalApres;
	}
}
