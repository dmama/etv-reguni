package ch.vd.uniregctb.evenement.civil.interne.depart;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Localisation;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class DepartSecondaire extends Depart {

	private final Adresse ancienneAdresse;
	private final Commune ancienneCommune;
	private final Integer numeroOfsEntiteForAnnonce;
	private final Localisation nouvelleLocalisationSecondaire;
	private final Commune nouvelleCommuneSecondaire;

	protected DepartSecondaire(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		final RegDate dateDepart = getDate();
		final AdressesCiviles anciennesAdresses = getAdresses(context, dateDepart);
		this.ancienneAdresse = anciennesAdresses.secondaire;
		this.ancienneCommune = getCommuneByAdresse(context, ancienneAdresse, dateDepart);
		this.numeroOfsEntiteForAnnonce = getNumeroOfsCommuneAnnonce();
		final RegDate lendemainDepart = dateDepart.getOneDayAfter();
		final AdressesCiviles nouvellesAdresses = getAdresses(context, lendemainDepart);
		final Adresse nouvelleAdresseSecondaire = nouvellesAdresses.secondaire;
		nouvelleCommuneSecondaire = getCommuneByAdresse(context, nouvelleAdresseSecondaire, lendemainDepart);
		this.nouvelleLocalisationSecondaire = computeNouvelleLocalisation(nouvelleAdresseSecondaire);
	}

	protected DepartSecondaire(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Adresse adressePrincipale, Commune communePrincipale,
	                           Adresse ancienneAdresseSecondaire, Commune ancienneCommuneSecondaire, EvenementCivilContext context, boolean isRegPP) throws EvenementCivilException {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, adressePrincipale, communePrincipale, context, isRegPP);
		this.ancienneAdresse = ancienneAdresseSecondaire;
		this.ancienneCommune = ancienneCommuneSecondaire;
		this.numeroOfsEntiteForAnnonce = numeroOfsCommuneAnnonce;
		this.nouvelleLocalisationSecondaire =  computeNouvelleLocalisation(adressePrincipale);
		this.nouvelleCommuneSecondaire = communePrincipale;

	}

	public DepartSecondaire(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);

		final RegDate dateDepart = getDate();
		final AdressesCiviles anciennesAdresses = getAdresses(context, dateDepart);
		this.ancienneAdresse = anciennesAdresses.secondaire;
		this.ancienneCommune = getCommuneByAdresse(context, ancienneAdresse, dateDepart);
		this.numeroOfsEntiteForAnnonce = ancienneCommune.getNoOFS();

		final RegDate lendemainDepart = dateDepart.getOneDayAfter();
		final AdressesCiviles nouvellesAdresses = getAdresses(context, lendemainDepart);
		final Adresse nouvelleAdresseSecondaire = nouvellesAdresses.secondaire;
		nouvelleCommuneSecondaire = getCommuneByAdresse(context, nouvelleAdresseSecondaire, lendemainDepart);
		this.nouvelleLocalisationSecondaire = computeNouvelleLocalisation(nouvelleAdresseSecondaire);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		super.validateSpecific(erreurs, warnings);

		Audit.info(getNumeroEvenement(), "Validation du départ de résidence secondaire");
		validateAbsenceForPrincipalPourDepartVaudois(erreurs);
		validateCoherenceAdresse(ancienneAdresse, ancienneCommune, erreurs);
	}


	@Override
	protected Integer getNumeroOfsEntiteForAnnonce() {
		return numeroOfsEntiteForAnnonce;
	}


	@Override
	protected void doHandleFermetureFors(PersonnePhysique pp, Contribuable ctb, RegDate dateFermeture, MotifFor motifFermeture) throws EvenementCivilException {
		Audit.info(getNumeroEvenement(), "Traitement du départ secondaire");
		handleDepartResidenceSecondaire(ctb, dateFermeture, motifFermeture);
	}

	/**
	 * Traite un depart d'une residence secondaire
	 *
	 * @param contribuable
	 * @param dateFermeture
	 * @param motifFermeture
	 */
	private void handleDepartResidenceSecondaire(Contribuable contribuable, RegDate dateFermeture, MotifFor motifFermeture) {

		final ForFiscalPrincipal forPrincipal = contribuable.getForFiscalPrincipalAt(dateFermeture);
		// For principal est sur la commune de départ d'une résidence secondaire

		//TODO (BNM) attention si depart.getNumeroOfsCommuneAnnonce correspond à une commune avec des fractions
		//ce cas d'arrangement fiscal ne sera pas détecté, il faut mettre l'événement en erreur pour
		//traitement manuel

		if (forPrincipal != null && forPrincipal.getNumeroOfsAutoriteFiscale().equals(getNumeroOfsEntiteForAnnonce())) {

			final ServiceInfrastructureService serviceInfra = context.getServiceInfra();
			Commune commune = null;
			if (estEnSuisse()) {
				commune = getNouvelleCommunePrincipale();
			}

			final ForFiscalPrincipal ffp = contribuable.getForFiscalPrincipalAt(null);

			// [UNIREG-1921] si la commune du for principal ne change pas suite au départ secondaire, rien à faire!
			if (commune != null && ffp.getNumeroOfsAutoriteFiscale() == commune.getNoOFSEtendu() && commune.isVaudoise() && ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				// rien à faire sur les fors...
			}
			else {
				// fermeture de l'ancien for principal à la date du départ
				getService().closeForFiscalPrincipal(ffp, dateFermeture, motifFermeture);

				// l'individu a sa residence principale en suisse
				if (estEnSuisse()) {
					if (commune.isVaudoise()) {
						//Ces cas sont detectées en amont et mis en erreur
					}
					else {
						final ModeImposition modeImpostion = determineModeImpositionDepartHCHS(contribuable, dateFermeture, ffp);
						openForFiscalPrincipalHC(contribuable, dateFermeture.getOneDayAfter(), commune.getNoOFS(), modeImpostion, MotifFor.DEPART_HC);
					}
				}
				else if (ffp != null) {
					final ModeImposition modeImposition = determineModeImpositionDepartHCHS(contribuable, dateFermeture, ffp);
					final Integer nullableNoOfs = getNouvelleLocalisation()==null ? null : getNouvelleLocalisation().getNoOfs();
					final int numeroOfsLocalisation = nullableNoOfs == null ? getPaysInconnu().getNoOFS() : nullableNoOfs;
					openForFiscalPrincipalHS(contribuable, dateFermeture.getOneDayAfter(), numeroOfsLocalisation, modeImposition, MotifFor.DEPART_HS);
				}
			}
		}
	}

	private boolean isDepartSecondaireVaudois() {
		return getNouvelleCommuneSecondaire() != null && getNouvelleCommuneSecondaire().isVaudoise();
	}

	private Commune getNouvelleCommuneSecondaire() {
		return nouvelleCommuneSecondaire;
	}

	private void validateAbsenceForPrincipalPourDepartVaudois(EvenementCivilErreurCollector erreurs) {
		if (isDepartSecondaireVaudois()) {
			final PersonnePhysique pp = getPrincipalPP();
			if (pp != null) {
				ForFiscalPrincipal forFP = null;
				final MenageCommun menageCommun = context.getTiersService().findMenageCommun(pp, getDate());

				if (menageCommun != null) {
					forFP = menageCommun.getForFiscalPrincipalAt(getDate());
				}
				else {
					forFP = pp.getForFiscalPrincipalAt(getDate());
				}

				if (forFP != null && forFP.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					erreurs.addErreur(String.format("A la date de l'événement, la personne physique (ctb: %s) associée à l'individu possède un for principal vaudois sur sa résidence secondaire(Arrangement fiscal?)",
							pp.getNumero()));
				}
			}
		}
	}
}
