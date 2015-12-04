package ch.vd.uniregctb.evenement.organisation.interne.reinscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
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
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;

/**
 * @author Raphaël Marmier, 2015-11-11
 */
public class Reinscription extends EvenementOrganisationInterneDeTraitement {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReinscriptionStrategy.class);
	private final RegDate dateAvant;
	private final RegDate dateApres;

	private final SiteOrganisation sitePrincipalAvant;
	private final SiteOrganisation sitePrincipalApres;

	private final StatusRC statusAvant;
	private final StatusInscriptionRC statusInscriptionAvant;

	private final RegDate dateRadiationAvant;
	private final RegDate dateRadiationApres;

	public Reinscription(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                     EvenementOrganisationContext context,
	                     EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);

		dateApres = evenement.getDateEvenement();
		dateAvant = dateApres.getOneDayBefore();

		sitePrincipalAvant = organisation.getSitePrincipal(dateAvant).getPayload();
		sitePrincipalApres = organisation.getSitePrincipal(dateApres).getPayload();

		statusAvant = sitePrincipalAvant.getDonneesRC().getStatus(dateAvant);
		statusInscriptionAvant = sitePrincipalAvant.getDonneesRC().getStatusInscription(dateAvant);

		dateRadiationAvant = sitePrincipalAvant.getDonneesRC().getDateRadiation(dateAvant);
		dateRadiationApres = sitePrincipalApres.getDonneesRC().getDateRadiation(dateApres);
	}

	@Override
	public String describe() {
		return "Réinscription au RC";
	}


	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		ForFiscalPrincipalPM dernierForPrincipal = getEntreprise().getDernierForFiscalPrincipal();
		if (dernierForPrincipal != null) {
			if (dernierForPrincipal.isValidAt(dateApres)) {
				LOGGER.info(String.format("Réinscription RC de l'entreprise %s: un for actif est déjà présent en date du %s",
				                          getEntreprise().getNumero(), dateApres));
			} else {
				reopenForFiscalPrincipal(dernierForPrincipal, suivis);
			}
			warnings.addWarning("Une vérification manuelle est requise pour la réinscription au RC d’une entreprise radiée.");
		} else {
			LOGGER.info(String.format("Réinscription RC de l'entreprise %s: aucun for trouvé. Pas de changement.", getEntreprise().getNumero()));
			raiseStatusTo(HandleStatus.TRAITE);
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

		// Vérifier qu'on est bien en présence d'une réinscription
		Assert.state(statusAvant == StatusRC.INSCRIT);
		Assert.state(statusInscriptionAvant == StatusInscriptionRC.RADIE);
		Assert.isNull(dateRadiationApres, "Date de radiation toujours présente après l'annonce. Nous ne sommes pas en présence d'une réinscription.");
		Assert.notNull(dateRadiationAvant, "Date de radiation absente avant l'annonce. Nous ne sommes pas en présence d'une réinscription.");
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

	public StatusRC getStatusAvant() {
		return statusAvant;
	}

	public StatusInscriptionRC getStatusInscriptionAvant() {
		return statusInscriptionAvant;
	}
}
