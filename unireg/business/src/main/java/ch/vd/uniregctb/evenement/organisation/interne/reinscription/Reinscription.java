package ch.vd.uniregctb.evenement.organisation.interne.reinscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
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
import ch.vd.uniregctb.tiers.TiersService;

/**
 * @author Raphaël Marmier, 2015-11-11
 */
public class Reinscription extends EvenementOrganisationInterneDeTraitement {

	private static final Logger LOGGER = LoggerFactory.getLogger(Reinscription.class);
	private final RegDate dateAvant;
	private final RegDate dateApres;

	private final SiteOrganisation sitePrincipalAvant;
	private final SiteOrganisation sitePrincipalApres;

	private final StatusInscriptionRC statusInscriptionAvant;
	private final StatusInscriptionRC statusInscriptionApres;

	private final RegDate dateRadiationAvant;
	private final RegDate dateRadiationApres;

	private final Etablissement etablissementPrincipal;

	public Reinscription(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                     EvenementOrganisationContext context,
	                     EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);

		dateApres = evenement.getDateEvenement();
		dateAvant = dateApres.getOneDayBefore();

		sitePrincipalAvant = organisation.getSitePrincipal(dateAvant).getPayload();
		sitePrincipalApres = organisation.getSitePrincipal(dateApres).getPayload();

		final InscriptionRC inscriptionAvant = sitePrincipalAvant.getDonneesRC().getInscription(dateAvant);
		final InscriptionRC inscriptionApres = sitePrincipalApres.getDonneesRC().getInscription(dateApres);

		statusInscriptionAvant = inscriptionAvant != null ? inscriptionAvant.getStatus() : null;
		statusInscriptionApres = inscriptionApres != null ? inscriptionApres.getStatus() : null;

		dateRadiationAvant = inscriptionAvant != null ? inscriptionAvant.getDateRadiationCH() : null;
		dateRadiationApres = inscriptionApres != null ? inscriptionApres.getDateRadiationCH() : null;

		etablissementPrincipal = context.getTiersService().getEtablissementPrincipal(entreprise, dateApres);
	}

	@Override
	public String describe() {
		return "Réinscription au RC";
	}


	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		final TiersService tiersService = getContext().getTiersService();

		// Fermer les surcharges  civiles ouvertes sur l'entreprise. Cela permet de prendre en compte d'éventuels changements survenus dans l'interval.
		tiersService.fermeSurchargesCiviles(getEntreprise(), getEvenement().getDateEvenement().getOneDayBefore());
		tiersService.fermeSurchargesCiviles(etablissementPrincipal, getEvenement().getDateEvenement().getOneDayBefore());

		warnings.addWarning("Réinscription de l’entreprise au RC. Veuillez vérifier et faire le nécessaire à la main.");
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

		// Vérifier qu'on est bien en présence d'une réinscription
		Assert.state(statusInscriptionApres == StatusInscriptionRC.ACTIF || statusInscriptionApres == StatusInscriptionRC.EN_LIQUIDATION);
		Assert.state(statusInscriptionAvant == StatusInscriptionRC.RADIE);
		// Malheureusement, c'est le cas normal dans RCEnt
		//Assert.isNull(dateRadiationApres, "Date de radiation toujours présente après l'annonce. Nous ne sommes pas en présence d'une réinscription.");
		// Peut ne pas être vrai. Un date de radiation peut être présente dans RCEnt sur une entreprise précédament déménagée HC puis revenue sur VD.
		//Assert.notNull(dateRadiationAvant, "Date de radiation absente avant l'annonce. Nous ne sommes pas en présence d'une réinscription.");
	}

	public RegDate getDateAvant() {
		return dateAvant;
	}

	public RegDate getDateApres() {
		return dateApres;
	}

	public SiteOrganisation getSitePrincipalAvant() {
		return sitePrincipalAvant;
	}

	public SiteOrganisation getSitePrincipalApres() {
		return sitePrincipalApres;
	}

	public RegDate getDateRadiationAvant() {
		return dateRadiationAvant;
	}

	public RegDate getDateRadiationApres() {
		return dateRadiationApres;
	}

	public StatusInscriptionRC getStatusInscriptionAvant() {
		return statusInscriptionAvant;
	}
}
