package ch.vd.unireg.evenement.entreprise.interne.inscription;

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
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeEtatEntreprise;

/**
 * @author Raphaël Marmier, 2016-02-23
 */
public class Inscription extends EvenementEntrepriseInterneDeTraitement {

	private static final Logger LOGGER = LoggerFactory.getLogger(Inscription.class);
	private final RegDate dateAvant;
	private final RegDate dateApres;
	private RegDate dateInscription;

	private EtablissementCivil etablissementPrincipalAvant = null;
	private final EtablissementCivil etablissementPrincipalApres;

	private StatusInscriptionRC statusInscriptionAvant = null;
	private final StatusInscriptionRC statusInscriptionApres;

	private RegDate dateInscriptionAvant = null;
	private final RegDate dateInscriptionApres;

	private RegDate dateRadiationAvant = null;
	private final RegDate dateRadiationApres;

	private final Etablissement etablissementPrincipal;

	public Inscription(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise,
	                   EvenementEntrepriseContext context,
	                   EvenementEntrepriseOptions options, RegDate dateInscription) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options);

		dateApres = evenement.getDateEvenement();
		this.dateInscription = dateInscription;
		dateAvant = dateApres.getOneDayBefore();

		final DateRanged<EtablissementCivil> etablissementPrincipalAvantRange = entrepriseCivile.getEtablissementPrincipal(dateAvant);
		etablissementPrincipalApres = entrepriseCivile.getEtablissementPrincipal(dateApres).getPayload();

		if (etablissementPrincipalAvantRange != null) {
			etablissementPrincipalAvant = etablissementPrincipalAvantRange.getPayload();
			final InscriptionRC inscriptionAvant = etablissementPrincipalAvant.getDonneesRC().getInscription(dateAvant);
			if (inscriptionAvant != null) {
				statusInscriptionAvant = inscriptionAvant.getStatus();
				dateInscriptionAvant = inscriptionAvant.getDateInscriptionCH();
				dateRadiationAvant = inscriptionAvant.getDateRadiationCH();
			}
		}

		final InscriptionRC inscriptionApres = etablissementPrincipalApres.getDonneesRC().getInscription(dateApres);
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
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {

		final TiersService tiersService = getContext().getTiersService();

		// Fermer les surcharges  civiles ouvertes sur l'entreprise. Cela permet de prendre en compte d'éventuels changements survenus dans l'interval.
		tiersService.fermeSurchargesCiviles(getEntreprise(), getEvenement().getDateEvenement().getOneDayBefore());
		tiersService.fermeSurchargesCiviles(etablissementPrincipal, getEvenement().getDateEvenement().getOneDayBefore());

		warnings.addWarning("Une vérification manuelle est requise pour l'inscription au RC d’une entreprise déjà connue du registre fiscal.");
		changeEtatEntreprise(getEntreprise(), TypeEtatEntreprise.INSCRITE_RC, dateInscription, suivis);
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {

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
		if (getEntrepriseCivile().isConnueInscriteAuRC(dateAvant)) {
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
