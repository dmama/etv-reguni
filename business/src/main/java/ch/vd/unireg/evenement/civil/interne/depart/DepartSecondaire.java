package ch.vd.unireg.evenement.civil.interne.depart;

import java.util.EnumSet;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.model.AdressesCiviles;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class DepartSecondaire extends Depart {

	private static final String IGNORE_VD = "Ignoré car départ secondaire vaudois";
	private static final Set<EtatEvenementCivil> ETATS_AVEC_MESSAGE_IGNORE_VD_POSSIBLE = EnumSet.of(EtatEvenementCivil.A_VERIFIER, EtatEvenementCivil.REDONDANT, EtatEvenementCivil.TRAITE);

	private final Adresse ancienneAdresse;
	private final Commune ancienneCommune;
	private final Integer numeroOfsEntiteForAnnonce;

	protected DepartSecondaire(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		final RegDate dateDepart = getDate();
		final AdressesCiviles anciennesAdresses = getAdresses(context, dateDepart);
		this.ancienneAdresse = anciennesAdresses.secondaireCourante;
		this.ancienneCommune = getCommuneByAdresse(context, ancienneAdresse, dateDepart);
		this.numeroOfsEntiteForAnnonce = getNumeroOfsCommuneAnnonce();

		final RegDate lendemainDepart = dateDepart.getOneDayAfter();
		final AdressesCiviles nouvellesAdresses = getAdresses(context, lendemainDepart);
		final Adresse nouvelleAdresseSecondaire = nouvellesAdresses.secondaireCourante;
		this.nouvelleCommune = getCommuneByAdresse(context, nouvelleAdresseSecondaire, lendemainDepart);
		this.nouvelleLocalisation = computeNouvelleLocalisation(ancienneAdresse, nouvelleAdresseSecondaire, nouvelleCommune);
	}

	protected DepartSecondaire(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Adresse adresseSecondaire, Commune communePrincipale,
	                           Adresse ancienneAdresseSecondaire, Commune ancienneCommuneSecondaire, EvenementCivilContext context, boolean isRegPP) throws EvenementCivilException {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, ancienneAdresseSecondaire, adresseSecondaire, communePrincipale, context, isRegPP);
		this.ancienneAdresse = ancienneAdresseSecondaire;
		this.ancienneCommune = ancienneCommuneSecondaire;
		this.numeroOfsEntiteForAnnonce = numeroOfsCommuneAnnonce;
		this.nouvelleLocalisation = computeNouvelleLocalisation(ancienneAdresse, adresseSecondaire, nouvelleCommune);
	}

	public DepartSecondaire(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options, Adresse ancienneAdresse) throws EvenementCivilException {
		super(event, context, options);

		final RegDate dateDepart = getDate();
		this.ancienneAdresse = ancienneAdresse;
		this.ancienneCommune = getCommuneByAdresse(context, ancienneAdresse, dateDepart);
		this.numeroOfsEntiteForAnnonce = ancienneCommune.getNoOFS();

		if (this.ancienneAdresse != null) {
			this.nouvelleLocalisation = this.ancienneAdresse.getLocalisationSuivante();
		}
		else {
			this.nouvelleLocalisation = null;
		}

		this.nouvelleCommune = findNouvelleCommuneByLocalisation(this.nouvelleLocalisation, context, dateDepart);

		// SIFISC-4912 Pour les événements ech, les départs secondaires vaudois a priori sont ignorés
		if (isDepartVaudois()) {
			final String message = String.format("%s : la nouvelle commune de résidence %s est toujours dans le canton.", IGNORE_VD, nouvelleCommune.getNomOfficiel());
			event.setCommentaireTraitement(message);
		}
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
	protected RegDate doHandleFermetureFors(EvenementCivilWarningCollector warnings, PersonnePhysique pp, ContribuableImpositionPersonnesPhysiques ctb, RegDate dateFermeture, MotifFor motifFermeture) throws EvenementCivilException {
		Audit.info(getNumeroEvenement(), "Traitement du départ secondaire");
		handleDepartResidenceSecondaire(pp, ctb, dateFermeture, motifFermeture);
		return dateFermeture;
	}

	/**
	 * Traite un depart d'une residence secondaire
	 */
	private void handleDepartResidenceSecondaire(PersonnePhysique pp, ContribuableImpositionPersonnesPhysiques contribuable, RegDate dateFermeture, MotifFor motifFermeture) throws EvenementCivilException {

		// For principal est sur la commune de départ d'une résidence secondaire
		final ForFiscalPrincipal forPrincipal = contribuable.getForFiscalPrincipalAt(dateFermeture);

		//SIFISC-15850
		//Dans le cas ou le départ n'est pas complet on ne fait rien, on ne touche à rien
		if (getDateDepartComplet(true) != null) {

			if (forPrincipal != null && forPrincipal.getNumeroOfsAutoriteFiscale().equals(getNumeroOfsEntiteForAnnonce())) {

				final Commune commune = (estEnSuisse() ? getNouvelleCommune() : null);
				final ForFiscalPrincipalPP ffp = contribuable.getForFiscalPrincipalAt(null);

				// [UNIREG-1921] si la commune du for principal ne change pas suite au départ secondaire, rien à faire!
				if (commune != null && ffp.getNumeroOfsAutoriteFiscale() == commune.getNoOFS() && commune.isVaudoise() && ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					// rien à faire sur les fors...
				}
				else {
					// fermeture de l'ancien for principal à la date du départ
					getService().closeForFiscalPrincipal(ffp, dateFermeture, motifFermeture);

					// l'individu a sa residence principale en suisse
					if (commune != null) {
						if (commune.isVaudoise()) {
							//Ces cas sont detectées en amont et mis en erreur
						}
						else {
							final ModeImposition modeImpostion = determineModeImpositionDepartHCHS(contribuable, dateFermeture, ffp, context.getTiersService());
							openForFiscalPrincipalHC(contribuable, dateFermeture.getOneDayAfter(), commune.getNoOFS(), modeImpostion, MotifFor.DEPART_HC);
						}
					}
					else if (ffp != null) {
						final ModeImposition modeImposition = determineModeImpositionDepartHCHS(contribuable, dateFermeture, ffp, context.getTiersService());
						final Integer nullableNoOfs = getNouvelleLocalisation() == null ? null : getNouvelleLocalisation().getNoOfs();
						final int numeroOfsLocalisation = nullableNoOfs == null ? getPaysInconnu().getNoOFS() : nullableNoOfs;
						openForFiscalPrincipalHS(contribuable, dateFermeture.getOneDayAfter(), numeroOfsLocalisation, modeImposition, MotifFor.DEPART_HS);
					}
				}
			}
		}
	}

	private void validateAbsenceForPrincipalPourDepartVaudois(EvenementCivilErreurCollector erreurs) {
		if (isDepartVaudois()) {
			final PersonnePhysique pp = getPrincipalPP();
			if (pp != null) {
				final MenageCommun menageCommun = context.getTiersService().findMenageCommun(pp, getDate());
				final ForFiscalPrincipal forFP;
				if (menageCommun != null) {
					forFP = menageCommun.getForFiscalPrincipalAt(getDate());
				}
				else {
					forFP = pp.getForFiscalPrincipalAt(getDate());
				}

				if (forFP != null && forFP.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					erreurs.addErreur(String.format("A la date de l'événement, la personne physique (ctb: %s) associée à l'individu possède un for principal vaudois actif (arrangement fiscal ?)",
					                                pp.getNumero()));
				}
			}
		}
	}

	@Override
	public boolean shouldResetCommentaireTraitement(EtatEvenementCivil etat, String commentaireTraitement) {
		// [SIFISC-6008] On ne dit pas qu'on ignore un événement civil qui est parti en erreur...
		// Si on a mis ce message (cf IGNORE_VD), c'est qu'on est dans le cas d'un départ vaudois reçu au travers d'un événement RCPers
		return super.shouldResetCommentaireTraitement(etat, commentaireTraitement) || (!ETATS_AVEC_MESSAGE_IGNORE_VD_POSSIBLE.contains(etat) && commentaireTraitement.startsWith(IGNORE_VD));
	}
}
