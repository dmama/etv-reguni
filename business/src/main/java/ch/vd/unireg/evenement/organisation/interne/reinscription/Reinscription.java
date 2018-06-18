package ch.vd.unireg.evenement.organisation.interne.reinscription;

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
import ch.vd.unireg.interfaces.organisation.data.EtablissementCivil;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.TiersService;

/**
 * @author Raphaël Marmier, 2015-11-11
 */
public class Reinscription extends EvenementOrganisationInterneDeTraitement {

	private static final Logger LOGGER = LoggerFactory.getLogger(Reinscription.class);
	private final RegDate dateAvant;
	private final RegDate dateApres;

	private final EtablissementCivil etablissementPrincipalAvant;
	private final EtablissementCivil etablissementPrincipalApres;

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

		etablissementPrincipalAvant = organisation.getEtablissementPrincipal(dateAvant).getPayload();
		etablissementPrincipalApres = organisation.getEtablissementPrincipal(dateApres).getPayload();

		final InscriptionRC inscriptionAvant = etablissementPrincipalAvant.getDonneesRC().getInscription(dateAvant);
		final InscriptionRC inscriptionApres = etablissementPrincipalApres.getDonneesRC().getInscription(dateApres);

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

		// Erreurs techniques fatale
		if (dateAvant == null || dateApres == null || dateAvant != dateApres.getOneDayBefore()) {
			throw new IllegalArgumentException();
		}

		// Vérifier qu'il y a bien une entreprise préexistante en base ? (Ca ne devrait pas se produire ici)
		// On doit avoir deux autorités fiscales
		if (getEntreprise() == null) {
			throw new IllegalArgumentException();
		}

		// Vérifier qu'on est bien en présence d'une réinscription
		if (statusInscriptionApres != StatusInscriptionRC.ACTIF && statusInscriptionApres != StatusInscriptionRC.EN_LIQUIDATION) {
			throw new IllegalArgumentException();
		}
		if (statusInscriptionAvant != StatusInscriptionRC.RADIE) {
			throw new IllegalArgumentException();
		}
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

	public EtablissementCivil getEtablissementPrincipalAvant() {
		return etablissementPrincipalAvant;
	}

	public EtablissementCivil getEtablissementPrincipalApres() {
		return etablissementPrincipalApres;
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
