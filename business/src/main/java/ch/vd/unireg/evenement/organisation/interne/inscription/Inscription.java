package ch.vd.unireg.evenement.organisation.interne.inscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterneDeTraitement;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeEtatEntreprise;

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

	private final Etablissement etablissementPrincipal;

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
			final InscriptionRC inscriptionAvant = sitePrincipalAvant.getDonneesRC().getInscription(dateAvant);
			if (inscriptionAvant != null) {
				statusInscriptionAvant = inscriptionAvant.getStatus();
				dateInscriptionAvant = inscriptionAvant.getDateInscriptionCH();
				dateRadiationAvant = inscriptionAvant.getDateRadiationCH();
			}
		}

		final InscriptionRC inscriptionApres = sitePrincipalApres.getDonneesRC().getInscription(dateApres);
		if (inscriptionApres != null) {
			statusInscriptionApres = inscriptionApres.getStatus();
			dateInscriptionApres = inscriptionApres.getDateInscriptionCH();
			dateRadiationApres = inscriptionApres.getDateRadiationCH();
		}
		else {
			statusInscriptionApres = null;
			dateInscriptionApres = null;
			dateRadiationApres = null;
		}

		etablissementPrincipal = context.getTiersService().getEtablissementPrincipal(entreprise, dateApres);
	}

	@Override
	public String describe() {
		return "Inscription au RC";
	}


	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		final TiersService tiersService = getContext().getTiersService();

		// Fermer les surcharges  civiles ouvertes sur l'entreprise. Cela permet de prendre en compte d'éventuels changements survenus dans l'interval.
		tiersService.fermeSurchargesCiviles(getEntreprise(), getEvenement().getDateEvenement().getOneDayBefore());
		tiersService.fermeSurchargesCiviles(etablissementPrincipal, getEvenement().getDateEvenement().getOneDayBefore());

		warnings.addWarning("Une vérification manuelle est requise pour l'inscription au RC d’une entreprise déjà connue du registre fiscal.");
		changeEtatEntreprise(getEntreprise(), TypeEtatEntreprise.INSCRITE_RC, dateApres, suivis);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		// Erreurs techniques fatale
		if (dateAvant == null || dateApres == null || dateAvant != dateApres.getOneDayBefore()) {
			throw new IllegalArgumentException();
		}

		// Vérifier qu'il y a bien une entreprise préexistante en base ? (Ca ne devrait pas se produire ici)
		if (getEntreprise() == null) {
			throw new IllegalArgumentException();
		}

		// Vérifier qu'on est bien en présence d'une inscription
		if (statusInscriptionApres != StatusInscriptionRC.ACTIF && statusInscriptionApres != StatusInscriptionRC.EN_LIQUIDATION) {
			throw new IllegalArgumentException();
		}
		if (getOrganisation().isConnueInscriteAuRC(dateAvant)) {
			throw new IllegalArgumentException();
		}
		if (dateRadiationApres != null) {
			throw new IllegalArgumentException("Date de radiation présente après l'annonce. Nous ne sommes pas en présence d'une inscription.");
		}
		if (dateRadiationAvant != null) {
			throw new IllegalArgumentException("Date de radiation présente avant l'annonce. Nous ne sommes pas en présence d'une inscription mais d'une réinscription.");
		}
	}

	public RegDate getDateAvant() {
		return dateAvant;
	}

	public RegDate getDateApres() {
		return dateApres;
	}
}
