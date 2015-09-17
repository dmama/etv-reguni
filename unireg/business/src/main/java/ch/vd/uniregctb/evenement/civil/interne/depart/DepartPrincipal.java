package ch.vd.uniregctb.evenement.civil.interne.depart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

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
		Audit.info(getNumeroEvenement(), "Verification de la cohérence du départ");

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
	protected RegDate doHandleFermetureFors(PersonnePhysique pp, ContribuableImpositionPersonnesPhysiques ctb, RegDate dateFermeture, MotifFor motifFermeture) throws EvenementCivilException {
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

		// [UNIREG-771] : L'événement de départ du premier doit passer l'individu de habitant à non habitant et ne rien faire d'autre (notamment au niveau des fors fiscaux)
		final RegDate dateDepartComplet = getDateDepartComplet();
		if (!isAncienTypeDepart() && dateDepartComplet == null) {
			Audit.info(getNumeroEvenement(), String.format("La résidence principale du conjoint à partir du %s est toujours sur VD, pas d'action sur les fors fiscaux", RegDateHelper.dateToDisplayString(dateFermeture.getOneDayAfter())));
			return dateFermeture;
		}

		final RegDate dateFermetureEffective = dateDepartComplet != null ? dateDepartComplet : dateFermeture;

		Audit.info(getNumeroEvenement(), "Traitement du départ principal");

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
		return dateFermetureEffective;
	}

	/**
	 * Dans le cas du départ d'une personne seule, c'est la date du départ. Dans le cas du départ d'une personne en couple, l'idée est de renvoyer ici la prochaine date
	 * (égale ou postérieure à la date du départ en cours de traitement) à laquelle le conjoint est également parti (soit du canton, soit de sa résidence vaudoise
	 * au moment du départ traité)... Si un tel moment n'existe pas (ou pas encore), on retournera <i>null</i>.
	 */
	@Nullable
	private RegDate getDateDepartComplet() throws EvenementCivilException {
		final PersonnePhysique habitant = getPrincipalPP();
		final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(habitant, getDate());
		if (couple != null) {
			final PersonnePhysique conjoint = couple.getConjoint(habitant);
			if (conjoint != null) {
				// il faut donc trouver un historique (postérieur ou égal à la date du départ traité) des lieux de résidence du conjoint
				final List<LieuResidence> lieuxResidenceConjoint = getLieuxResidencePrincipale(conjoint);
				final LieuResidence residenceAuMomentDuDepart = DateRangeHelper.rangeAt(lieuxResidenceConjoint, getDate());
				final RegDate dateDepartComplet;
				if (residenceAuMomentDuDepart != null && residenceAuMomentDuDepart.typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					dateDepartComplet = residenceAuMomentDuDepart.getDateFin();
				}
				else {
					dateDepartComplet = getDate();
				}
				return dateDepartComplet;
			}
		}
		return getDate();
	}

	private List<LieuResidence> getLieuxResidencePrincipale(PersonnePhysique pp) throws EvenementCivilException {
		if (pp.isConnuAuCivil()) {
			try {
				final AdressesCivilesHistoriques adresses = context.getServiceCivil().getAdressesHisto(pp.getNumeroIndividu(), false);
				final List<Adresse> residences = adresses.principales;
				if (residences.isEmpty()) {
					return Collections.emptyList();
				}
				else {
					final List<LieuResidence> lieux = new ArrayList<>(residences.size() * 2);
					final MovingWindow<Adresse> adresseWindow = new MovingWindow<>(residences);
					while (adresseWindow.hasNext()) {
						final MovingWindow.Snapshot<Adresse> snapshot = adresseWindow.next();
						final Adresse adresse = snapshot.getCurrent();
						final LieuResidence lieu = new LieuResidence(adresse.getDateDebut(), adresse.getDateFin(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, adresse.getNoOfsCommuneAdresse());
						lieux.add(lieu);

						// une adresse de destination sur le canton devrait être reprise dans les résidences suivantes -> pas la peine de la prendre en compte maintenant
						if (adresse.getLocalisationSuivante() != null && adresse.getLocalisationSuivante().getType() != LocalisationType.CANTON_VD) {
							final Adresse adresseSuivante = snapshot.getNext();
							if (adresseSuivante == null || RegDateHelper.isAfter(adresseSuivante.getDateDebut(), adresse.getDateFin().getOneDayAfter(), NullDateBehavior.EARLIEST)) {
								final RegDate dateFin = adresseSuivante == null ? null : adresseSuivante.getDateDebut().getOneDayBefore();
								final TypeAutoriteFiscale typeAutoriteFiscale =
										adresse.getLocalisationSuivante().getType() == LocalisationType.HORS_CANTON ? TypeAutoriteFiscale.COMMUNE_HC : TypeAutoriteFiscale.PAYS_HS;
								final LieuResidence suivant = new LieuResidence(adresse.getDateFin().getOneDayAfter(), dateFin, typeAutoriteFiscale, adresse.getLocalisationSuivante().getNoOfs());
								lieux.add(suivant);
							}
						}
					}
					return lieux;
				}
			}
			catch (DonneesCivilesException e) {
				throw new EvenementCivilException(e);
			}
		}
		else {
			return Collections.emptyList();
		}
	}

	private static class LieuResidence implements CollatableDateRange {
		private final TypeAutoriteFiscale typeAutoriteFiscale;
		private final int noOfs;
		private final RegDate dateDebut;
		private final RegDate dateFin;

		private LieuResidence(RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer noOfs) {
			this.dateDebut = dateDebut;
			this.typeAutoriteFiscale = typeAutoriteFiscale;
			this.noOfs = noOfs == null ? -1 : noOfs;
			this.dateFin = dateFin;
		}

		@Override
		public boolean isCollatable(DateRange next) {
			return DateRangeHelper.isCollatable(this, next) && next instanceof LieuResidence && ((LieuResidence) next).noOfs == noOfs && ((LieuResidence) next).typeAutoriteFiscale == typeAutoriteFiscale;
		}

		@Override
		public DateRange collate(DateRange next) {
			return new LieuResidence(dateDebut, next.getDateFin(), typeAutoriteFiscale, noOfs);
		}

		@Override
		public boolean isValidAt(RegDate date) {
			return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
		}

		@Override
		public RegDate getDateDebut() {
			return dateDebut;
		}

		@Override
		public RegDate getDateFin() {
			return dateFin;
		}
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

		Audit.info(depart.getNumeroEvenement(), String.format("Fermeture du for principal d'un contribuable au %s pour motif suivant: %s", RegDateHelper.dateToDisplayString(dateFermeture), motifFermeture));

		// [SIFISC-11521] si le for était déjà fermé à la même date pour le même motif, rien à faire...
		final ForFiscalPrincipalPP precedent = contribuable.getForFiscalPrincipalAt(dateFermeture);
		if (precedent != null && precedent.getDateFin() == dateFermeture && precedent.getMotifFermeture() == motifFermeture) {
			Audit.info(depart.getNumeroEvenement(),
			           String.format("Le for principal du contribuable est déjà fermé au %s pour le motif '%s', pas de traitement supplémentaire à prévoir sur les fors",
			                         RegDateHelper.dateToDisplayString(dateFermeture), motifFermeture.getDescription(false)));
			return;
		}

		final ForFiscalPrincipalPP ffp = (ForFiscalPrincipalPP) getService().closeForFiscalPrincipal(contribuable, dateFermeture, motifFermeture);
		if (ffp != null && ffp.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			throw new RuntimeException("Le for du contribuable est déjà hors du canton");
		}

		Audit.info(depart.getNumeroEvenement(),
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
