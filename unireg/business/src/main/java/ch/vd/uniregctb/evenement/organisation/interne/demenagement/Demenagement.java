package ch.vd.uniregctb.evenement.organisation.interne.demenagement;

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
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

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
	                    EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);

		dateApres = evenement.getDateEvenement();
		dateAvant = dateApres.getOneDayBefore();

		sitePrincipalAvant = organisation.getSitePrincipal(dateAvant).getPayload();
		sitePrincipalApres = organisation.getSitePrincipal(dateApres).getPayload();

		siegeAvant = sitePrincipalAvant.getDomicile(dateAvant);
		siegeApres = sitePrincipalApres.getDomicile(dateApres);

		etablissementPrincipalAvant = getEtablissementByNumeroSite(sitePrincipalAvant.getNumeroSite());
		etablissementPrincipalApres = getEtablissementByNumeroSite(sitePrincipalApres.getNumeroSite());
	}

	@Override
	public abstract void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException;

	/**
	 * Changement de siège principal avec établissement stable
	 *
	 * @param etablissementPrincipal L'établissement dont l'autorité fiscale change.
	 * @param motifFor Le motif du changement
	 * @param warnings
	 */
	protected void changeSiegeEtablissement(Etablissement etablissementPrincipal, MotifFor motifFor, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		signaleDemenagement(etablissementPrincipal, getSiegeAvant(), getSiegeApres(), getDateApres(), suivis);

		final ForFiscalPrincipal forFiscalPrincipal = getEntreprise().getForFiscalPrincipalAt(getDateAvant());
		if (forFiscalPrincipal == null) {
			throw new EvenementOrganisationException("Aucun for trouvé pour l'établissement principal.");
		}
		closeForFiscalPrincipal(getDateAvant(), motifFor, suivis);
		openForFiscalPrincipal(getDateApres(), getSiegeApres(), MotifRattachement.DOMICILE, motifFor, warnings, suivis);
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
	protected void effectueChangementSiege(MotifFor motifFor, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		// On a affaire à un établissement inconnu
		final Etablissement etablissementPrincipalApres = getEtablissementPrincipalApres();
		if (etablissementPrincipalApres == null) {
			throw new EvenementOrganisationException("Changement de siège avec création d'un nouvel établissement principal. Veuillez traiter l'événement manuellement.");
		}
		// Ok, on connait le nouvel etablissement
		else {
			// On est sur le même établissement -> changer le domicile, fermer l'ancien for et ouvrir un nouveau
			if (getEtablissementPrincipalAvant().getNumero().equals(etablissementPrincipalApres.getNumero())) {
				changeSiegeEtablissement(etablissementPrincipalApres, motifFor, warnings, suivis);
			}
			// On n'est pas sur le même établissement -> Changer les domiciles de chaque établissement pour refléter les domiciles respectifs, fermer l'ancien for et ouvrir un nouveau.
			else {
				throw new EvenementOrganisationException("Changement de siège avec changement d'établissement principal. Veuillez traiter l'événement manuellement.");
			}
		}
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		/*
		 Erreurs techniques fatale
		  */
		Assert.notNull(dateAvant);
		Assert.notNull(dateApres);
		Assert.isTrue(dateAvant.equals(dateApres.getOneDayBefore()));

		// Vérifier qu'il y a bien une entreprise préexistante en base ? (Ca ne devrait pas se produire ici)
		Assert.notNull(getEntreprise());

		// Nouvel établissement
		Assert.notNull(etablissementPrincipalApres ,"Changement de siège avec création d'un nouvel établissement principal. Veuillez traiter l'événement manuellement.");

		// Changement d'établissement non supporté actuellement.
		Assert.state(getEtablissementPrincipalAvant().getNumero().equals(etablissementPrincipalApres.getNumero()),
		             "Changement de siège avec changement d'établissement principal. Veuillez traiter l'événement manuellement.");
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
