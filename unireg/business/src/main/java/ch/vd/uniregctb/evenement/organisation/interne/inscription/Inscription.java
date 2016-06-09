package ch.vd.uniregctb.evenement.organisation.interne.inscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
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
import ch.vd.uniregctb.type.TypeEtatEntreprise;

/**
 * @author Raphaël Marmier, 2016-02-23
 */
public class Inscription extends EvenementOrganisationInterneDeTraitement {

	private static final Logger LOGGER = LoggerFactory.getLogger(Inscription.class);
	private final RegDate dateAvant;
	private final RegDate dateApres;

	private SiteOrganisation sitePrincipalAvant = null;
	private final SiteOrganisation sitePrincipalApres;

	private StatusInscriptionRC statusInscriptionAvant = null;
	private final StatusInscriptionRC statusInscriptionApres;

	private RegDate dateInscriptionAvant = null;
	private final RegDate dateInscriptionApres;

	private RegDate dateRadiationAvant = null;
	private final RegDate dateRadiationApres;

	public Inscription(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                   EvenementOrganisationContext context,
	                   EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);

		dateApres = evenement.getDateEvenement();
		dateAvant = dateApres.getOneDayBefore();

		final DateRanged<SiteOrganisation> sitePrincipalAvantRange = organisation.getSitePrincipal(dateAvant);
		sitePrincipalApres = organisation.getSitePrincipal(dateApres).getPayload();

		if (sitePrincipalAvantRange != null) {
			sitePrincipalAvant = sitePrincipalAvantRange.getPayload();
			statusInscriptionAvant = sitePrincipalAvant.getDonneesRC().getStatusInscription(dateAvant);
			dateInscriptionAvant = sitePrincipalAvant.getDonneesRC().getDateInscription(dateAvant);
			dateRadiationAvant = sitePrincipalAvant.getDonneesRC().getDateRadiation(dateAvant);
		}

		statusInscriptionApres = sitePrincipalApres.getDonneesRC().getStatusInscription(dateApres);
		dateInscriptionApres = sitePrincipalApres.getDonneesRC().getDateInscription(dateApres);
		dateRadiationApres = sitePrincipalApres.getDonneesRC().getDateRadiation(dateApres);
	}

	@Override
	public String describe() {
		return "Inscription au RC";
	}


	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		warnings.addWarning("Une vérification manuelle est requise pour l'inscription au RC d’une entreprise déjà connue du registre fiscale.");
		changeEtatEntreprise(getEntreprise(), TypeEtatEntreprise.INSCRITE_RC, dateApres, suivis);
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

		// Vérifier qu'on est bien en présence d'une inscription
		Assert.state(statusInscriptionApres == StatusInscriptionRC.ACTIF || statusInscriptionApres == StatusInscriptionRC.EN_LIQUIDATION);
		Assert.state(!getOrganisation().isInscritAuRC(dateAvant));
		Assert.isNull(dateRadiationApres, "Date de radiation présente après l'annonce. Nous ne sommes pas en présence d'une inscription.");
		Assert.isNull(dateRadiationAvant, "Date de radiation présente avant l'annonce. Nous ne sommes pas en présence d'une inscription mais d'une réinscription.");
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

	public RegDate getDateInscriptionAvant() {
		return dateInscriptionAvant;
	}

	public RegDate getDateInscriptionApres() {
		return dateInscriptionApres;
	}

	public StatusInscriptionRC getStatusInscriptionAvant() {
		return statusInscriptionAvant;
	}
}
