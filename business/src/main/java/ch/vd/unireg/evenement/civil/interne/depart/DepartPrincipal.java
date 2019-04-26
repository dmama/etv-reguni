package ch.vd.unireg.evenement.civil.interne.depart;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.model.AdressesCiviles;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class DepartPrincipal extends Depart {

	private static final String IGNORE_VD = "Ignoré car départ vaudois";
	private static final Set<EtatEvenementCivil> ETATS_AVEC_MESSAGE_IGNORE_VD_POSSIBLE = EnumSet.of(EtatEvenementCivil.A_VERIFIER, EtatEvenementCivil.REDONDANT, EtatEvenementCivil.TRAITE);

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

		final RegDate lendemainDepart = dateDepart.getOneDayAfter();
		final AdressesCiviles nouvellesAdresses = getAdresses(context, lendemainDepart);
		final Adresse nouvelleAdressePrincipale = nouvellesAdresses.principale;
		this.nouvelleCommune = getCommuneByAdresse(context, nouvelleAdressePrincipale, lendemainDepart);
		this.nouvelleLocalisation = computeNouvelleLocalisation(ancienneAdresse, nouvelleAdressePrincipale, nouvelleCommune);

		// SIFISC-4230 Pour les événements regPP, les départs vaudois doivent partir en erreur
		if (isDepartVaudois()) {
			throw new EvenementCivilException("La nouvelle commune est toujours dans le canton de Vaud");
		}
	}

	/**
	 * Pour les tests seulement
	 */
	@SuppressWarnings({"JavaDoc"})
	protected DepartPrincipal(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Adresse ancienneAdressePrincipale, Commune ancienneCommunePrincipale,
	                          Adresse nouvelleAdressePrincipale, Commune nouvelleCommunePrincipale, EvenementCivilContext context, boolean isRegPP) throws EvenementCivilException {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, ancienneAdressePrincipale, nouvelleAdressePrincipale, nouvelleCommunePrincipale, context, isRegPP);
		this.ancienneAdresse = ancienneAdressePrincipale;
		this.ancienneCommune = ancienneCommunePrincipale;
		this.numeroOfsEntiteForAnnonce = numeroOfsCommuneAnnonce;
	}

	public DepartPrincipal(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options, Adresse ancienneAdresse) throws EvenementCivilException {
		super(event, context, options);

		final RegDate dateDepart = getDate();
		this.ancienneAdresse = ancienneAdresse;
		this.ancienneCommune = getCommuneByAdresse(context, ancienneAdresse, dateDepart);
		if (ancienneCommune.isFraction()) {
			    numeroOfsEntiteForAnnonce = ancienneCommune.getOfsCommuneMere();
		} else {
				numeroOfsEntiteForAnnonce = ancienneCommune.getNoOFS();
		}


		if (this.ancienneAdresse != null) {
			this.nouvelleLocalisation = this.ancienneAdresse.getLocalisationSuivante();
		}
		else {
			this.nouvelleLocalisation = null;
		}

		//Recherche de la nouvelle commune
		this.nouvelleCommune = findNouvelleCommuneByLocalisation(nouvelleLocalisation, context, getDate());

		// SIFISC-4230 Pour les evenements ech, les départs vaudois sont a priori ignorés
		if (isDepartVaudois()) {
			final String message = String.format("%s : la nouvelle commune de résidence %s est toujours dans le canton.", IGNORE_VD, nouvelleCommune.getNomOfficiel());
			event.setCommentaireTraitement(message);
		}
	}

	public void checkCompleteness(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) {
		context.audit.info(getNumeroEvenement(), "Verification de la cohérence du départ");

		if (getNumeroOfsEntiteForAnnonce() == null) {
			erreurs.addErreur("La commune d'annonce est vide");
		}

		verifierMouvementIndividu(this, false, erreurs, warnings);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		checkCompleteness(erreurs, warnings);
		if (erreurs.hasErreurs()) {
			return;
		}

		super.validateSpecific(erreurs, warnings);

		context.audit.info(getNumeroEvenement(), "Validation de la nouvelle adresse principale");
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
	protected RegDate doHandleFermetureFors(EvenementCivilWarningCollector warnings, PersonnePhysique pp, ContribuableImpositionPersonnesPhysiques ctb, RegDate dateFermeture, MotifFor motifFermeture) throws EvenementCivilException {
		// [UNIREG-2701] si l'ancienne adresse est inconnue, et que le tiers n'a aucun for non-annulé -> on laisse passer
		if (ancienneCommune == null) {

			// fors non-annulés sur le contribuable actif à la date de l'événement
			final List<ForFiscal> tousFors = ctb.getForsFiscauxNonAnnules(false);
			if (tousFors == null || tousFors.isEmpty()) {
				// pas de fors non-annulés -> tout va bien, on accepte l'événement sans autre
				context.audit.info(getNumeroEvenement(), "Commune de départ inconnue mais contribuable sans for, départ traité sans autre");
			}
			else {
				throw new EvenementCivilException("La commune de départ n'a pas été trouvée");
			}
		}

		// [UNIREG-771] : L'événement de départ du premier doit passer l'individu de habitant à non habitant et ne rien faire d'autre (notamment au niveau des fors fiscaux)
		final RegDate dateDepartComplet = getDateDepartComplet(false);
		if (!isAncienTypeDepart() && dateDepartComplet == null) {
			context.audit.info(getNumeroEvenement(), String.format("La résidence principale du conjoint à partir du %s est toujours sur VD, pas d'action sur les fors fiscaux", RegDateHelper.dateToDisplayString(dateFermeture.getOneDayAfter())));
			return dateFermeture;
		}

		final RegDate dateFermetureEffective = dateDepartComplet != null ? dateDepartComplet : dateFermeture;

		context.audit.info(getNumeroEvenement(), "Traitement du départ principal");

		/*
		 * Fermetures des adresses temporaires du fiscal
		 */
		fermeAdresseTiersTemporaire(ctb, dateFermetureEffective);

		int numeroOfsAutoriteFiscale;
		if (motifFermeture == MotifFor.DEPART_HC) {
			final Commune nouvelleCommune = getNouvelleCommune();
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

		handleDepartResidencePrincipale(this, ctb, dateFermetureEffective, motifFermeture, numeroOfsAutoriteFiscale);

		// [SIFISC-18224] l'événement de départ doit passer dans l'état "A VERIFIER" si les deux conjoints ne se retrouvent pas
		// dans le même type d'autorité fiscale
		if (!isToutLeMondeAvecLeMemeTypeAutoriteFiscaleApresDepartPrincipal(dateFermetureEffective)) {
			warnings.addWarning("Le type de destination entre les deux conjoints n'est pas identique (hors Suisse / hors canton). Veuillez contrôler la destination du for principal.");
		}

		return dateFermetureEffective;
	}

	/**
	 * @return <b>true</b> si l'habitant est celibataire, marié seul, ou marié et son conjoint est aussi parti; <b>false</b> autrement.
	 */
	private boolean isDepartComplet() {
		final PersonnePhysique habitant = getPrincipalPP();
		final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(habitant, getDate());
		if (couple != null) {
			final PersonnePhysique conjoint = couple.getConjoint(habitant);
			if (conjoint != null) {
				final Boolean hasResidencePrincipaleVaudoise = context.getTiersService().isHabitantResidencePrincipale(conjoint, getDate().getOneDayAfter());
				return hasResidencePrincipaleVaudoise != null && !hasResidencePrincipaleVaudoise;
			}
		}
		return true;
	}

	/**
	 * Traite un depart d'une residence principale
	 *
	 */
	private void handleDepartResidencePrincipale(Depart depart, ContribuableImpositionPersonnesPhysiques contribuable, RegDate dateFermeture, MotifFor motifFermeture, int numeroOfsAutoriteFiscale) throws EvenementCivilException {

		context.audit.info(depart.getNumeroEvenement(), String.format("Fermeture du for principal d'un contribuable au %s pour motif suivant: %s", RegDateHelper.dateToDisplayString(dateFermeture), motifFermeture));

		// [SIFISC-11521] si le for était déjà fermé à la même date pour le même motif, rien à faire...
		final ForFiscalPrincipalPP precedent = contribuable.getForFiscalPrincipalAt(dateFermeture);
		if (precedent != null && precedent.getDateFin() == dateFermeture && precedent.getMotifFermeture() == motifFermeture) {
			context.audit.info(depart.getNumeroEvenement(),
			           String.format("Le for principal du contribuable est déjà fermé au %s pour le motif '%s', pas de traitement supplémentaire à prévoir sur les fors",
			                         RegDateHelper.dateToDisplayString(dateFermeture), motifFermeture.getDescription(false)));
			return;
		}

		final ForFiscalPrincipalPP ffp = (ForFiscalPrincipalPP) getService().closeForFiscalPrincipal(contribuable, dateFermeture, motifFermeture);
		if (ffp != null && ffp.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			throw new RuntimeException("Le for du contribuable est déjà hors du canton");
		}

		context.audit.info(depart.getNumeroEvenement(),
		           String.format("Ouverture du for principal d'un contribuable au %s pour motif suivant: %s", RegDateHelper.dateToDisplayString(dateFermeture.getOneDayAfter()), motifFermeture));

		if (ffp != null) {
			final ModeImposition modeImposition = determineModeImpositionDepartHCHS(contribuable, dateFermeture, ffp, context.getTiersService());
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

	@Override
	public boolean shouldResetCommentaireTraitement(EtatEvenementCivil etat, String commentaireTraitement) {
		// [SIFISC-6008] On ne dit pas qu'on ignore un événement civil qui est parti en erreur...
		// Si on a mis ce message (cf IGNORE_VD), c'est qu'on est dans le cas d'un départ vaudois reçu au travers d'un événement RCPers
		return super.shouldResetCommentaireTraitement(etat, commentaireTraitement) || (!ETATS_AVEC_MESSAGE_IGNORE_VD_POSSIBLE.contains(etat) && commentaireTraitement.startsWith(IGNORE_VD));
	}
}
