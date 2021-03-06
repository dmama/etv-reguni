package ch.vd.unireg.evenement.entreprise.interne.reinscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterneDeTraitement;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.TiersService;

/**
 * @author Raphaël Marmier, 2015-11-11
 */
public class Reinscription extends EvenementEntrepriseInterneDeTraitement {

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

	public Reinscription(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise,
	                     EvenementEntrepriseContext context,
	                     EvenementEntrepriseOptions options) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options);

		dateApres = evenement.getDateEvenement();
		dateAvant = dateApres.getOneDayBefore();

		etablissementPrincipalAvant = entrepriseCivile.getEtablissementPrincipal(dateAvant).getPayload();
		etablissementPrincipalApres = entrepriseCivile.getEtablissementPrincipal(dateApres).getPayload();

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
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {

		final TiersService tiersService = getContext().getTiersService();

		// Fermer les surcharges  civiles ouvertes sur l'entreprise. Cela permet de prendre en compte d'éventuels changements survenus dans l'interval.
		tiersService.fermeSurchargesCiviles(getEntreprise(), getEvenement().getDateEvenement().getOneDayBefore());
		tiersService.fermeSurchargesCiviles(etablissementPrincipal, getEvenement().getDateEvenement().getOneDayBefore());

		warnings.addWarning("Réinscription de l'entreprise au RC. Veuillez vérifier et faire le nécessaire à la main.");
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {

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
