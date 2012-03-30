package ch.vd.uniregctb.evenement.civil.interne.depart;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
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
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class DepartPrincipal extends Depart {

	private Adresse ancienneAdresse;
	private Commune ancienneCommune;
	private final Integer numeroOfsEntiteForAnnonce;

	protected DepartPrincipal(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		final RegDate dateDepart = getDate();
		final AdressesCiviles anciennesAdresses = getAdresses(context, dateDepart);
		this.ancienneAdresse = anciennesAdresses.principale;
		this.ancienneCommune = getCommuneByAdresse(context, ancienneAdresse, dateDepart);
		this.numeroOfsEntiteForAnnonce = getNumeroOfsCommuneAnnonce();


	}

	/**
	 * Pour les tests seulement
	 */
	@SuppressWarnings({"JavaDoc"})
	protected DepartPrincipal(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Adresse ancienneAdressePrincipale, Commune ancienneCommunePrincipale,
	                          Adresse nouvelleAdressePrincipale, Commune nouvelleCommunePrincipale, EvenementCivilContext context, boolean isRegPP) throws EvenementCivilException {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, nouvelleAdressePrincipale, nouvelleCommunePrincipale, context, isRegPP);
		this.ancienneAdresse = ancienneAdressePrincipale;
		this.ancienneCommune = ancienneCommunePrincipale;
		this.numeroOfsEntiteForAnnonce = numeroOfsCommuneAnnonce;

	}

	public DepartPrincipal(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);
		final RegDate dateDepart = getDate();
		final AdressesCiviles anciennesAdresses = getAdresses(context, dateDepart);
		this.ancienneAdresse = anciennesAdresses.principale;
		this.ancienneCommune = getCommuneByAdresse(context, ancienneAdresse, dateDepart);
		this.numeroOfsEntiteForAnnonce = ancienneCommune.getNoOFS();

	}

	public void checkCompleteness(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) {
		Audit.info(getNumeroEvenement(), "Verification de la cohérence du départ");

	//Suppression de la verification de la nouvelle adresse. Celle ci n'est pas renseignée pour les évènements ech
	//	if (getNouvelleAdressePrincipale() == null) {
	//		warnings.addWarning("La nouvelle adresse principale de l'individu est vide");
	//	}

		if (getNumeroOfsEntiteForAnnonce() == null) {
			erreurs.addErreur("La commune d'annonce est vide");
		}

		verifierMouvementIndividu(this, false, erreurs, warnings);

		// Si le pays de destination est inconnu, on leve un warning
		if (findMotifFermetureFor() == MotifFor.DEPART_HS) {
			if (isPaysInconnu()) {
				warnings.addWarning("Le nouveau pays n'a pas été trouvé : veuillez vérifier le motif de fermeture du for principal");
			}

		} // Verification de la nouvelle commune principale en hors canton
		else if (getNouvelleCommunePrincipale() == null) {
			warnings.addWarning("La nouvelle commune principale n'a pas été trouvée : veuillez vérifier le motif de fermeture du for principal");
		}
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		checkCompleteness(erreurs, warnings);
		if (erreurs.hasErreurs()) {
			return;
		}

		super.validateSpecific(erreurs, warnings);

		Audit.info(getNumeroEvenement(), "Validation de la nouvelle adresse principale");
		validateCoherenceAdresse(ancienneAdresse, ancienneCommune, erreurs);


	}


	@Override
	protected Integer getNumeroOfsEntiteForAnnonce() {
		return numeroOfsEntiteForAnnonce;
	}

	private boolean isPaysInconnu() {
		return !isPaysEstConnu();
	}

	@Override
	protected void doHandleFermetureFors(PersonnePhysique pp, Contribuable ctb, RegDate dateFermeture, MotifFor motifFermeture) throws EvenementCivilException {
		// [UNIREG-2701] si l'ancienne adresse est inconnue, et que le tiers n'a aucun for non-annulé -> on laisse passer
		if (ancienneCommune == null) {

			// fors non-annulés sur le contribuable actif à la date de l'événement
			final List<ForFiscal> tousFors = ctb.getForsFiscauxNonAnnules(false);
			if (tousFors == null || tousFors.isEmpty()) {
				// pas de fors non-annulés -> tout va bien, on accepte l'événement sans autre
				Audit.info(getNumeroEvenement(), "Commune de départ inconnue mais contribuable sans for, départ traité sans autre");
			}
			else {
				throw new EvenementCivilException("La commune de départ n'a pas été trouvée");
			}
		}

		//[UNIREG-1996] on traite les deux habitants ensemble conformement à l'ancien fonctionement
		if (isAncienTypeDepart()) {
			traiteHabitantOfAncienDepart(pp);
		}
		else {

			// [UNIREG-1691] si la personne physique était déjà notée non-habitante, on ne fait que régulariser une situation bancale
			if (pp.isHabitantVD()) {
				context.getTiersService().changeHabitantenNH(pp);
			}

			/*
			 * [UNIREG-771] : L'événement de départ du premier doit passer l'individu de habitant à non habitant et ne rien faire d'autre
			 * (notamment au niveau des fors fiscaux)
			 */
			if (!isDepartComplet()) {
				return;
			}
		}

		Audit.info(getNumeroEvenement(), "Traitement du départ principal");

		/*
		 * Fermetures des adresses temporaires du fiscal
		 */
		fermeAdresseTiersTemporaire(ctb, dateFermeture);

		int numeroOfsAutoriteFiscale;
		if (motifFermeture == MotifFor.DEPART_HC) {
			final Commune nouvelleCommune = getNouvelleCommunePrincipale();
			if (nouvelleCommune != null) {
				numeroOfsAutoriteFiscale = nouvelleCommune.getNoOFS();
			}
			else {
				numeroOfsAutoriteFiscale = getPaysInconnu().getNoOFS();
				motifFermeture = MotifFor.DEPART_HS;        // si on prend le numéro OFS d'un pays, le départ est forcément HS, en fait
			}
		}
		else {
			// Depart hors suisse
			if (isPaysInconnu()) {
				numeroOfsAutoriteFiscale = getPaysInconnu().getNoOFS();
			}
			else {
				//Dans ce cas, le pays est connu est on a un numéro ofs non null
				//noinspection ConstantConditions
				numeroOfsAutoriteFiscale = getNouvelleLocalisation().getNoOfs();
			}
		}

		handleDepartResidencePrincipale(this, ctb, dateFermeture, motifFermeture, numeroOfsAutoriteFiscale);
	}

	/**
	 * Permet de transformer un habitant et son conjoint en non habitant dans le cas d'un ancien Type de départ
	 * @param pp     une personne physique
	 */
	private void traiteHabitantOfAncienDepart(PersonnePhysique pp) {
		final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(pp, getDate());
		final PersonnePhysique conjoint;
		if (couple != null) {
			conjoint = couple.getConjoint(pp);
			if (conjoint != null) {
				getService().changeHabitantenNH(conjoint);
			}
		}
		getService().changeHabitantenNH(pp);
	}

	/**
	 * @param depart un événement de départ
	 * @return <b>true</b> si l'habitant est celibataire, marié seul, ou marié et son conjoint est aussi parti; <b>false</b> autrement.
	 */
	private boolean isDepartComplet() {
		final PersonnePhysique habitant = getPrincipalPP();
		final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(habitant, getDate());
		final PersonnePhysique conjoint;
		if (couple != null) {
			conjoint = couple.getConjoint(habitant);
		}
		else {
			conjoint = null;
		}

		return !(conjoint != null && conjoint.isHabitantVD());
	}

	/**
	 * Traite un depart d'une residence principale
	 *
	 * @param depart         un événement de départ
	 * @param contribuable
	 * @param dateFermeture
	 * @param motifFermeture
	 */
	private void handleDepartResidencePrincipale(Depart depart, Contribuable contribuable, RegDate dateFermeture, MotifFor motifFermeture, int numeroOfsAutoriteFiscale) {

		Audit.info(depart.getNumeroEvenement(), String.format("Fermeture du for principal d'un contribuable au %s pour motif suivant: %s", RegDateHelper.dateToDisplayString(dateFermeture), motifFermeture));

		final ForFiscalPrincipal ffp = getService().closeForFiscalPrincipal(contribuable, dateFermeture, motifFermeture);
		if (ffp != null && ffp.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			throw new RuntimeException("Le for du contribuable est déjà hors du canton");
		}

		Audit.info(depart.getNumeroEvenement(), String.format("Ouverture du for principal d'un contribuable au %s pour motif suivant: %s", RegDateHelper.dateToDisplayString(dateFermeture.getOneDayAfter()), motifFermeture));

		if (ffp != null) {
			final ModeImposition modeImposition = determineModeImpositionDepartHCHS(contribuable, dateFermeture, ffp);
			if (motifFermeture == MotifFor.DEPART_HC) {
				openForFiscalPrincipalHC(contribuable, dateFermeture.getOneDayAfter(), numeroOfsAutoriteFiscale, modeImposition, motifFermeture);
			}
			else if (motifFermeture == MotifFor.DEPART_HS) {
				openForFiscalPrincipalHS(contribuable, dateFermeture.getOneDayAfter(), numeroOfsAutoriteFiscale, modeImposition, motifFermeture);
			}
			else {
				throw new RuntimeException("Départ en résidence principale, motif non supporté : " + motifFermeture);
			}
		}
	}


}
