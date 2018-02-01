package ch.vd.unireg.evenement.civil.interne.arrivee;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.adresse.HistoriqueCommune;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.DonneesCivilesException;
import ch.vd.unireg.common.EtatCivilHelper;
import ch.vd.unireg.common.FiscalDateHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.model.AdressesCiviles;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.TiersException;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public class ArriveePrincipale extends Arrivee {

	private final Adresse ancienneAdresse;
	private final Adresse nouvelleAdresse;
	private final Commune ancienneCommune;
	private final Commune nouvelleCommune;
	private final LocalisationType previousLocation;

	public ArriveePrincipale(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		final RegDate dateArrivee = getDate();
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		final AdressesCiviles anciennesAdresses = getAdresses(context, veilleArrivee);
		ancienneAdresse = anciennesAdresses.principale;
		ancienneCommune = getCommuneByAdresse(context, ancienneAdresse, veilleArrivee);

		final AdressesCiviles nouvellesAdresses = getAdresses(context, dateArrivee);
		nouvelleAdresse = nouvellesAdresses.principale;
		nouvelleCommune = getCommuneByAdresse(context, nouvelleAdresse, dateArrivee);

		previousLocation = computePreviousLocation(evenement.getType());
	}

	public ArriveePrincipale(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		final RegDate dateArrivee = getDate();
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		final AdressesCiviles anciennesAdresses = getAdresses(context, veilleArrivee);
		ancienneAdresse = anciennesAdresses.principale;
		ancienneCommune = getCommuneByAdresse(context, ancienneAdresse, veilleArrivee);

		final AdressesCiviles nouvellesAdresses = getAdresses(context, dateArrivee);
		nouvelleAdresse = nouvellesAdresses.principale;
		nouvelleCommune = getCommuneByAdresse(context, nouvelleAdresse, dateArrivee);

		final Localisation localisationPrecedente = nouvelleAdresse.getLocalisationPrecedente();
		previousLocation = localisationPrecedente != null ? computePreviousLocation(localisationPrecedente) : estimateLocation(ancienneCommune);

		// dans le cas de l'arrivée liée à une naissance, il n'y a pas de localisation précédente

		if (localisationPrecedente != null && localisationPrecedente.getNoOfs() == null) {
			throw new EvenementCivilException("Le numéro ofs de la provenance est inconnu");
		}
	}

	/**
	 * Pour les tests seulement
	 */
	@SuppressWarnings({"JavaDoc"})
	public ArriveePrincipale(Individu individu, Individu conjoint, TypeEvenementCivil type, RegDate dateArrivee, Integer communeAnnonce, Commune ancienneCommune, Commune nouvelleCommune, Adresse ancienneAdresse,
	                         Adresse nouvelleAdresse, EvenementCivilContext context) {
		super(individu, conjoint, dateArrivee, communeAnnonce, context);
		this.ancienneAdresse = ancienneAdresse;
		this.nouvelleAdresse = nouvelleAdresse;
		this.ancienneCommune = ancienneCommune;
		this.nouvelleCommune = nouvelleCommune;
		this.previousLocation = computePreviousLocation(type);
	}

	@Nullable
	private static LocalisationType estimateLocation(@Nullable Commune commune) {
		if (commune == null) {
			return null;
		}

		if (commune.isVaudoise()) {
			return LocalisationType.CANTON_VD;
		}
		else {
			return LocalisationType.HORS_CANTON;
		}
	}

	@Nullable
	private LocalisationType computePreviousLocation(Localisation avant) {
		final LocalisationType loc;
		if (avant != null && avant.getType() != null) {
			loc = avant.getType();
		}
		else {
			// inconnu...
			loc = null;
		}
		return loc;
	}

	@Nullable
	private LocalisationType computePreviousLocation(TypeEvenementCivil type) {
		final LocalisationType previousLocation;
		switch (type) {
		case ARRIVEE_PRINCIPALE_HC:
			previousLocation = LocalisationType.HORS_CANTON;
			break;
		case ARRIVEE_PRINCIPALE_HS:
			previousLocation = LocalisationType.HORS_SUISSE;
			break;
		case ARRIVEE_PRINCIPALE_VAUDOISE:
		case DEMENAGEMENT_DANS_COMMUNE:     // dans le cas de déménagement dans une commune fusionnée civilement mais pas encore fiscalement
			previousLocation = LocalisationType.CANTON_VD;
			break;
		case ARRIVEE_DANS_COMMUNE:
			if (ancienneCommune == null) {
				if (ancienneAdresse != null && ancienneAdresse.getNoOfsPays() != null && ancienneAdresse.getNoOfsPays() != ServiceInfrastructureService.noOfsSuisse) {
					previousLocation = LocalisationType.HORS_SUISSE;
				}
				else {
					previousLocation = null;
				}
			}
			else if (ancienneCommune.isVaudoise()) {
				previousLocation = LocalisationType.CANTON_VD;
			}
			else {
				previousLocation = LocalisationType.HORS_CANTON;
			}
			break;
		default:
			throw new IllegalArgumentException("Type d'événement non supporté par cette classe d'événement interne : " + type);
		}
		return previousLocation;
	}

	public Adresse getAncienneAdresse() {
		return ancienneAdresse;
	}

	public Adresse getNouvelleAdresse() {
		return nouvelleAdresse;
	}

	public Commune getAncienneCommune() {
		return ancienneCommune;
	}

	public Commune getNouvelleCommune() {
		return nouvelleCommune;
	}

	public void checkCompleteness(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) {

		if (nouvelleCommune == null) {
			erreurs.addErreur("La nouvelle commune principale n'a pas été trouvée (adresse hors-Suisse ?)");
		}

		if (nouvelleAdresse == null) {
			erreurs.addErreur("La nouvelle adresse principale de l'individu est vide");
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
		validateArriveeAdressePrincipale(erreurs, warnings);
		validateForPrincipal(erreurs);
		validateCoherenceCivilFiscal(erreurs);

		// On vérifie que l'individu arrive de hors-canton ou hors-Suisse s'il est inconnu dans Unireg, autrement il y a un problème de cohérence.
		final PersonnePhysique habitant = getPrincipalPP();
		if (habitant == null) {
			if (isDansLeCanton(ancienneCommune)) {
				final String message = "L'individu est inconnu dans registre fiscal mais possédait déjà une adresse dans le " +
						"registre civil avant son arrivée (incohérence entre les deux registres)";
				erreurs.addErreur(message);
			}
			else {
				// [SIFISC-5286] on vérifie aussi la localisation précédente, si l'information est présente
				final Localisation localisationPrecedente = nouvelleAdresse.getLocalisationPrecedente();
				if (localisationPrecedente != null && localisationPrecedente.getType() == LocalisationType.CANTON_VD) {
					final String message = "L'individu est inconnu dans registre fiscal mais arrive depuis une commune vaudoise (incohérence entre les deux registres)";
					erreurs.addErreur(message);
				}
			}
		}
	}

	private void validateForPrincipal(EvenementCivilErreurCollector erreurs) {
		final MotifFor motifFor = getMotifOuvertureFor();
		if (motifFor == MotifFor.ARRIVEE_HC || motifFor == MotifFor.ARRIVEE_HS) {
			// Si le motif d'ouverture du for est arrivee HS ou HC, alors l'eventuel for principal actuel ne doit pas être vaudois
			final PersonnePhysique pp = getPrincipalPP();
			if (pp != null) {
				final RapportEntreTiers rapportMenage = pp.getRapportSujetValidAt(getDate(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				if (rapportMenage != null) {
					MenageCommun mc = (MenageCommun)context.getTiersService().getTiers(rapportMenage.getObjetId());
					final ForFiscalPrincipal forFP = mc.getForFiscalPrincipalAt(getDate());
					if (forFP != null && forFP.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && nouvelleCommune.getNoOFS() != forFP.getNumeroOfsAutoriteFiscale()) {
						erreurs.addErreur(String.format("A la date de l'événement, la personne physique (ctb: %s) associée à l'individu a un for principal vaudois " +
								"différent de celui du menage commun (ctb:%s) qu'il est sensé rejoindre", pp.getNumero(),mc.getNumero()));
					}
				}

				// seulement pour les PP qui ne sont pas en couple, car dans le cas des couples,
				// un membre peut déjà être arrivé lorsque le second arrive
				if (rapportMenage == null && !isArriveeRedondantePourIndividuSeul()) { // [SIFISC-5466] si l'événement est redondant, il est correct de déjà avoir un for principal vaudois
					final ForFiscalPrincipal forFP = pp.getForFiscalPrincipalAt(getDate());
					if (forFP != null && forFP.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						erreurs.addErreur(String.format("A la date de l'événement, la personne physique (ctb: %s) associée à l'individu a déjà un for principal vaudois", pp.getNumero()));
					}
				}
			}
		}
	}

	private void validateCoherenceCivilFiscal(@NotNull EvenementCivilErreurCollector erreurs) {

		final PersonnePhysique pp = getPrincipalPP();
		if (pp == null) {
			// arrivée d'une personne physique inconnue, rien à faire
			return;
		}

		final RapportEntreTiers rapportMenage = pp.getRapportSujetValidAt(getDate(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
		if (rapportMenage == null) {
			// la personne n'appartient pas à un ménage, rien à faire
			return;
		}

		final EtatCivil etatCivil = Optional.ofNullable(getIndividu())
				.map(i -> i.getEtatCivil(getDate()))
				.orElse(null);
		final boolean individuSeul = !EtatCivilHelper.estMarieOuPacse(etatCivil);

		// [SIFISC-17204] si l'individu est seul (célibataire ou séparé/divorcé) au civil mais dans un ménage-commun au fiscal => traitement manuel
		if (individuSeul) {
			final TypeEtatCivil typeEtatCivil = Optional.ofNullable(etatCivil)
					.map(EtatCivil::getTypeEtatCivil)
					.orElse(null);
			final MenageCommun menage = (MenageCommun) context.getTiersService().getTiers(rapportMenage.getObjetId());
			erreurs.addErreur("La personne arrivante (n°" + FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()) + ") est seule au civil (" + typeEtatCivil + ") " +
					                  "mais appartient à un ménage-commun au fiscal (n°" + FormatNumeroHelper.numeroCTBToDisplay(menage.getNumero()) + "). Veuillez traiter l'événement manuellement.");
		}
	}

	private void validateArriveeAdressePrincipale(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) {

		/*
		 * La date de début de la nouvelle adresse principale de l’individu est antérieure ou identique à la date de l'ancienne.
		 */
		if (ancienneAdresse != null && ancienneAdresse.getDateDebut() != null && getDate().isBeforeOrEqual(ancienneAdresse.getDateDebut())) {
			erreurs.addErreur("La date d'arrivée principale est antérieure à la date de début de l'ancienne adresse");
		}
		if (ancienneAdresse != null && (ancienneAdresse.getDateFin() == null || getDate().isBeforeOrEqual(ancienneAdresse.getDateFin())) ){
			erreurs.addErreur("La date d'arrivée principale est antérieure à la date de fin de l'ancienne adresse");
		}

		/*
		 * La nouvelle adresse principale n’est pas dans le canton (il n’est pas obligatoire que l’adresse courrier soit dans le canton).
		 */
		if (!isDansLeCanton(nouvelleCommune)) {
			erreurs.addErreur("La nouvelle commune principale est en dehors du canton");
		}

		/*
		 * La commune d'annonce est différente de la commune d'arrivée cas possible avec les fractions if
		 * (arrivee.getNumeroOfsCommuneAnnonce().intValue() != nouvelleCommune.getNoTechnique()) { erreurs.add(new EvenementCivilExterneErreur("La
		 * nouvelle commune principale ("+nouvelleCommune.getNomOfficiel()+ ","+nouvelleCommune.getNoTechnique()+") ne correspond pas à la
		 * commune d'annonce ("+arrivee.getNumeroOfsCommuneAnnonce()+")")); }
		 */

		/*
		 * Pour les communes du Sentier, il n'est pas possible de déterminer automatiquement le for principal. Un traitement manuel est
		 * nécessaire.
		 */
		if (nouvelleCommune.getNoOFS() == NO_OFS_FRACTION_SENTIER) {
			warnings.addWarning("arrivée dans la fraction de commune du Sentier: veuillez vérifier la fraction de commune du for principal");
		}
	}

	@Override
	protected RegDate getDateArriveeEffective(RegDate date) {
		// [UNIREG-2212] Il faut décaler la date du for en cas d'arrivée vaudoise après le 20 décembre
		if (previousLocation == LocalisationType.CANTON_VD) {
			// vérification du dépassement du 20/12
			return FiscalDateHelper.getDateOuvertureForFiscal(date);
		}
		else {
			return date;
		}
	}

	private MotifFor getMotifOuvertureFor() {
		// fonction de l'ancienne commune exclusivement
		final MotifFor motif;
		if (previousLocation != null) {
		    switch (previousLocation) {
		        case HORS_CANTON:
			        motif = MotifFor.ARRIVEE_HC;
			        break;
		        case CANTON_VD:
			        motif = MotifFor.DEMENAGEMENT_VD;
			        break;
		        case HORS_SUISSE:
			        motif = MotifFor.ARRIVEE_HS;
			        break;
		        default:
			        throw new IllegalArgumentException("Valeur invalide : " + previousLocation);
		    }
		}
		else {
			motif = null;
		}
		return motif;
	}

	@NotNull
	private ModeImposition getSourceOuMixteSuivantAgeRetraite(RegDate dateArrivee, PersonnePhysique pp) {
		final TiersService tiersService = context.getTiersService();
		final Sexe sexe = tiersService.getSexe(pp);
		final RegDate dateNaissance = tiersService.getDateNaissance(pp);

		if (sexe == null || dateNaissance == null) {
			// on ne sait pas -> source
			return ModeImposition.SOURCE;
		}

		final ParametreAppService parametreAppService = context.getParametreAppService();
		final Integer ageRetraite;
		if (sexe == Sexe.MASCULIN) {
			ageRetraite = parametreAppService.getAgeRentierHomme();
		}
		else {
			ageRetraite = parametreAppService.getAgeRentierFemme();
		}
		if (ageRetraite == null) {
			// on ne peut pas savoir -> source
			return ModeImposition.SOURCE;
		}

		final RegDate dateRetraite = dateNaissance.addYears(ageRetraite);
		if (dateRetraite.isBeforeOrEqual(dateArrivee)) {
			Audit.info(getNumeroEvenement(), "Mode d'imposition mixte car âge de la retraite atteint.");
			return ModeImposition.MIXTE_137_1;
		}
		else {
			return ModeImposition.SOURCE;
		}
	}

	@NotNull
	private ModeImposition getSourceOuMixteSuivantAgeRetraite(RegDate dateArrivee, PersonnePhysique principal, PersonnePhysique conjoint) {
		final ModeImposition prn = getSourceOuMixteSuivantAgeRetraite(dateArrivee, principal);
		final ModeImposition cjt = getSourceOuMixteSuivantAgeRetraite(dateArrivee, conjoint);
		if (prn == ModeImposition.MIXTE_137_1 || cjt == ModeImposition.MIXTE_137_1) {
			return ModeImposition.MIXTE_137_1;
		}
		else {
			return prn;
		}
	}

	/**
	 * @param first date de début d'un trou
	 * @param last date de début de "l'après trou"
	 * @return si le trou dure moins
	 */
	private static boolean isDifferenceTwoYearsOrLess(RegDate first, RegDate last) {
		return first.addYears(2).isAfter(last);
	}

	private static final class RattrapageDepartHSInconnu {

		final ForFiscalPrincipalPP forVaudoisPrecedantDepart;
		final ForFiscalPrincipalPP forHorsSuisseInconnu;

		public RattrapageDepartHSInconnu(ForFiscalPrincipalPP forVaudoisPrecedantDepart, ForFiscalPrincipalPP forHorsSuisseInconnu) {
			this.forVaudoisPrecedantDepart = forVaudoisPrecedantDepart;
			this.forHorsSuisseInconnu = forHorsSuisseInconnu;
		}
	}

	@Override
	protected void doHandleCreationForIndividuSeul(PersonnePhysique habitant, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		// Le for fiscal principal reste inchangé en cas d'arrivée en résidence principale d'un individu mineur.

		final RegDate dateArriveeEffective = getDateArriveeEffective(getDate());
		if (FiscalDateHelper.isMajeurAt(getIndividu(), dateArriveeEffective)) {

			MotifFor motifOuverture = getMotifOuvertureFor();
			final int numeroOfsNouveau = nouvelleCommune.getNoOFS();
			final ForFiscalPrincipalPP forFiscal = habitant.getForFiscalPrincipalAt(null);

			// détermination du mode d'imposition
			final ModeImposition modeImposition;
			final RattrapageDepartHSInconnu rattrapageDepartHSInconnu;
			try {
				if (forFiscal == null) {
					if (getService().isSuisseOuPermisC(habitant, dateArriveeEffective)) {
						// s'il est suisse ou titulaire d'un permis C => ordinaire
						modeImposition = ModeImposition.ORDINAIRE;
					}
					else if (motifOuverture == MotifFor.ARRIVEE_HC || motifOuverture == MotifFor.ARRIVEE_HS || motifOuverture == null) {
						modeImposition = getSourceOuMixteSuivantAgeRetraite(dateArriveeEffective, habitant);
					}
					else {
						// une arrivée dans le canton, sans for pré-existant en n'arrivant pas de hors-Suisse ni de hors-Canton, cela ne devrait pas être possible, il me semble...
						modeImposition = null;
					}
					rattrapageDepartHSInconnu = null;
				}
				else {
					if (motifOuverture == MotifFor.ARRIVEE_HC || motifOuverture == MotifFor.ARRIVEE_HS || motifOuverture == null) {
						if (getService().isSuisseOuPermisC(habitant, dateArriveeEffective)) {
							modeImposition = ModeImposition.ORDINAIRE;
						}
						else {
							// Si l'individu est déjà présent en for secondaire, il passe mixte_1, sinon, il passe source
							final List<ForFiscal> fors = habitant.getForsFiscauxValidAt(dateArriveeEffective);
							boolean hasForSecondaire = false;
							for (ForFiscal ff : fors) {
								// si on trouve au moins un for secondaire, alors mixte_1
								if (ff instanceof ForFiscalSecondaire) {
									hasForSecondaire = true;
									break;
								}
							}
							modeImposition = hasForSecondaire ? ModeImposition.MIXTE_137_1 : getSourceOuMixteSuivantAgeRetraite(dateArriveeEffective, habitant);
						}
						rattrapageDepartHSInconnu = null;
					}
					else {
						// déménagement vaudois d'après la provenance

						// [SIFISC-5451] détermination du for référent (peut ne pas être le "for fiscal" si celui ci est HS pays inconnu
						// alors que la provenance indiquée est vaudoise et que cela ne fait pas trop longtemps qu'on a quitté le canton)
						final ForFiscalPrincipalPP forPrecedent = habitant.getForFiscalPrincipalAt(forFiscal.getDateDebut().getOneDayBefore());
						final ForFiscalPrincipalPP forReferent;
						if (forPrecedent != null
								&& forFiscal.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS
								&& forFiscal.getNumeroOfsAutoriteFiscale() == ServiceInfrastructureService.noPaysInconnu
								&& forPrecedent.getMotifFermeture() == MotifFor.DEPART_HS
								&& forPrecedent.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {

							// nous sommes clairement dans le cas d'un départ HS pays inconnu erroné (au moment du traitement du départ, nous avions sans doute trouvé une destination vide..)

							// il reste cependant quelques trucs à vérifier :
							// - la commune d'avant le départ HS
							// - les dates
							final Localisation localisationPrecedente = nouvelleAdresse.getLocalisationPrecedente();
							if (localisationPrecedente == null
									|| localisationPrecedente.getType() != LocalisationType.CANTON_VD
									|| !forPrecedent.getNumeroOfsAutoriteFiscale().equals(localisationPrecedente.getNoOfs())) {

								// problème -> la commune n'est pas la même
								throw new EvenementCivilException("Tentative de rattrapage d'un départ pour pays inconnu avortée en raison de communes vaudoises différentes.");
							}
							else if (!isDifferenceTwoYearsOrLess(forFiscal.getDateDebut(), dateArriveeEffective)) {
								// problème -> ça fait trop longtemps !
								throw new EvenementCivilException("Tentative de rattrapage d'un départ pour pays inconnu avortée en raison de la date de départ, trop vieille.");
							}

							forReferent = forPrecedent;
							rattrapageDepartHSInconnu = new RattrapageDepartHSInconnu(forPrecedent, forFiscal);
						}
						else {
							forReferent = forFiscal;
							rattrapageDepartHSInconnu = null;
						}

						// récupération du mode d'imposition sur le for référent
						if (forReferent.getModeImposition() == ModeImposition.SOURCE) {
							modeImposition = getSourceOuMixteSuivantAgeRetraite(dateArriveeEffective, habitant);
						}
						else {
							modeImposition = forReferent.getModeImposition();
						}
					}
				}
			}
			catch (TiersException e) {
				throw new EvenementCivilException(e.getMessage(), e);
			}

			// détermination de la date d'ouverture
			final RegDate dateOuverture = dateArriveeEffective; // [UNIREG-2212] La date d'ouverture est toujours la date d'événement

			if (modeImposition != null) {
				if (motifOuverture == null) {
					motifOuverture = MotifFor.ARRIVEE_HS;
					warnings.addWarning("Ancienne adresse avant l'arrivée inconnue : veuillez indiquer le motif d'ouverture du for principal.");
				}
				if (forFiscal == null) {
					Audit.info(getNumeroEvenement(), "Création d'un for fiscal ordinaire avec mode d'imposition [" + modeImposition + ']');
					openForFiscalPrincipal(habitant, dateOuverture, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, numeroOfsNouveau, MotifRattachement.DOMICILE, motifOuverture, modeImposition);
				}
				else if (rattrapageDepartHSInconnu != null) {
					Audit.info(getNumeroEvenement(), "Rattrapage d'un ancien départ HS pour pays inconnu");
					openForFiscalPrincipalAvecRattrapage(habitant, dateOuverture, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, numeroOfsNouveau, MotifRattachement.DOMICILE, MotifFor.DEMENAGEMENT_VD, modeImposition, rattrapageDepartHSInconnu);
				}
				else {
					Audit.info(getNumeroEvenement(), "Mise-à-jour du fors fiscal avec mode d'imposition [" + modeImposition + ']');
					updateForFiscalPrincipal(habitant, dateOuverture, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, numeroOfsNouveau, MotifRattachement.DOMICILE, motifOuverture, modeImposition);
				}
			}
		}
	}

	private void openForFiscalPrincipalAvecRattrapage(ContribuableImpositionPersonnesPhysiques ctb,
	                                                  RegDate dateOuverture,
	                                                  TypeAutoriteFiscale taf,
	                                                  int noOfs,
	                                                  MotifRattachement motifRattachement,
	                                                  MotifFor motifOuverture,
	                                                  ModeImposition modeImposition,
	                                                  RattrapageDepartHSInconnu infoRattrapage) {

		// annulation du for principal hs
		infoRattrapage.forHorsSuisseInconnu.setAnnule(true);
		context.getEvenementFiscalService().publierEvenementFiscalAnnulationFor(infoRattrapage.forHorsSuisseInconnu);

		// annulation du for précédent et remplacement par un for identique qui se termine à la veille de notre nouvelle date d'ouverture
		infoRattrapage.forVaudoisPrecedantDepart.setAnnule(true);
		context.getEvenementFiscalService().publierEvenementFiscalAnnulationFor(infoRattrapage.forVaudoisPrecedantDepart);

		// remplacement -> génération du for principal avant le départ
		getService().addForPrincipal(ctb,
		                             infoRattrapage.forVaudoisPrecedantDepart.getDateDebut(),
		                             infoRattrapage.forVaudoisPrecedantDepart.getMotifOuverture(),
		                             null,
		                             null,
		                             infoRattrapage.forVaudoisPrecedantDepart.getMotifRattachement(),
		                             infoRattrapage.forVaudoisPrecedantDepart.getNumeroOfsAutoriteFiscale(),
		                             infoRattrapage.forVaudoisPrecedantDepart.getTypeAutoriteFiscale(),
		                             infoRattrapage.forVaudoisPrecedantDepart.getModeImposition());

		// ouverture du for suite à l'arrivée
		getService().addForPrincipal(ctb,
		                             dateOuverture,
		                             motifOuverture,
		                             null,
		                             null,
		                             motifRattachement,
		                             noOfs,
		                             taf,
		                             modeImposition);
	}

	@Override
	protected void doHandleCreationForMenage(PersonnePhysique arrivant, MenageCommun menageCommun, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		/*
		 * Le for fiscal principal reste inchangé en cas d'arrivée en résidence secondaire.
		 */
		createOrUpdateForFiscalPrincipalOnCouple(arrivant, menageCommun, warnings);
	}

	/**
	 * Crée ou met-à-jour le for fiscal principal pour le contribuable principal, son conjoint et le ménage - en fonction de leur état civil
	 * et fiscal.
	 * <p/>
	 * On regarde les adresses de domicile des membres du couple :
	 * <ul>
	 * <li>s'il n'y en a qu'une vaudoise -> on prend cette commune</li>
	 * <li>s'il y en a deux vaudoises, et qu'elles sont dans la même commune, alors on prend cette commune</li>
	 * <li>s'il y en a deux vaudoises, et qu'elles sont dans deux communes différentes, on ne touche à rien s'il y a déjà un for vaudois ouvert sur le couple, et on prend la commune du principal du couple sinon</li>
	 * <li>si on a pu déterminer une commune avec les conditions ci-dessus, on ouvre un for dessus à la date d'arrivée</li>
	 * </ul>
	 * @param arrivant personne physique concernée par l'arrivée
	 * @param menageCommun le ménage commun
	 * @param warnings liste des erreurs à peupler en cas de problème
	 * @throws EvenementCivilException en cas de souci
	 */
	private void createOrUpdateForFiscalPrincipalOnCouple(PersonnePhysique arrivant, MenageCommun menageCommun, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		Assert.notNull(menageCommun);

		final EnsembleTiersCouple ensemble = getService().getEnsembleTiersCouple(menageCommun, getDate());
		//SIFISC-6065
		//Cause de cet effet de bord: aucun composant du ménage trouvé à cette date
		if (ensemble.getPrincipal() == null && ensemble.getConjoint() == null) {
			throw new EvenementCivilException(String.format("L'arrivant(e) [%s] a un état civil marié ou pacsé à la date de l'évènement ainsi qu'un ménage commun. Cependant, aucun lien d'appartenance ménage n'a " +
					"été trouvé pour cette date: [%s]. Vérifier si il n'y a pas une incohérence entre les dates civiles et fiscales",
					FormatNumeroHelper.numeroCTBToDisplay(arrivant.getNumero()),
					RegDateHelper.dateToDashString(getDate())));
		}

		final Pair<Commune, RegDate> infosFor = getCommuneForSuiteArriveeCouple(arrivant, ensemble);
		if (infosFor == null) {
			// pas de for à créer...
			return;
		}

		// [UNIREG-2212] Il faut décaler la date du for en cas d'arrivée vaudoise après le 20 décembre
		final RegDate dateEvenement = getDateArriveeEffective(infosFor.getSecond());
		final Commune commune = infosFor.getFirst();

		try {

			final PersonnePhysique principal = ensemble.getPrincipal();
			final PersonnePhysique conjoint = ensemble.getConjoint();
			final ForFiscalPrincipalPP ffpHabitantPrincipal = principal.getForFiscalPrincipalAt(null);
			final ForFiscalPrincipalPP ffpHabitantConjoint = (conjoint == null ? null : conjoint.getForFiscalPrincipalAt(null));
			final ForFiscalPrincipalPP ffpMenage = menageCommun.getForFiscalPrincipalAt(null);
			final int numeroOfsNouveau = commune.getNoOFS();

			// pour un couple, le for principal est toujours sur le ménage commun
			if (ffpHabitantPrincipal != null) {
				throw new EvenementCivilException(String.format("Le contribuable principal [%s] du ménage [%s] possède un for fiscal principal individuel",
						FormatNumeroHelper.numeroCTBToDisplay(principal.getNumero()),
						FormatNumeroHelper.numeroCTBToDisplay(menageCommun.getNumero())));
			}
			if (ffpHabitantConjoint != null) {
				throw new EvenementCivilException(String.format("Le conjoint [%s] du ménage [%s] possède un for fiscal principal individuel",
						FormatNumeroHelper.numeroCTBToDisplay(conjoint.getNumero()),
						FormatNumeroHelper.numeroCTBToDisplay(menageCommun.getNumero())));
			}

			MotifFor motifOuverture = getMotifOuvertureFor();

			if (ffpMenage != null && ffpMenage.getDateFin() == null && ffpMenage.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				// SIFISC-26927 dans le cas d'une arrivée HS/HC d'un conjoint sur un ménage qui possède déjà un for fiscal actif sur une commune vaudoise,
				//              le motif d'ouverture doit être DEMENAGEMENT_VD (et non pas ARRIVEE_HS ou ARRIVEE_HC)
				motifOuverture = MotifFor.DEMENAGEMENT_VD;
			}

			// détermination du mode d'imposition
			final ModeImposition modeImposition;
			final RattrapageDepartHSInconnu rattrapageDepartHSInconnu;
			if (ffpMenage == null) {
				if (getService().isSuisseOuPermisC(principal, dateEvenement) || (conjoint != null && (getService().isSuisseOuPermisC(conjoint, dateEvenement)))) {
					modeImposition = ModeImposition.ORDINAIRE;
				}
				else if (motifOuverture == MotifFor.ARRIVEE_HC || motifOuverture == MotifFor.ARRIVEE_HS) {
					modeImposition = getSourceOuMixteSuivantAgeRetraite(dateEvenement, principal, conjoint);
				}
				else {
					// une arrivée dans le canton, sans for pré-existant en n'arrivant pas de hors-Suisse ni de hors-Canton, cela ne devrait pas être possible, il me semble...
					modeImposition = null;
				}
				rattrapageDepartHSInconnu = null;
			}
			else {
				if (motifOuverture == MotifFor.ARRIVEE_HC || motifOuverture == MotifFor.ARRIVEE_HS || motifOuverture == null) {
					if (getService().isSuisseOuPermisC(principal, dateEvenement) || (conjoint != null && (getService().isSuisseOuPermisC(conjoint, dateEvenement)))) {
						modeImposition = ModeImposition.ORDINAIRE;
					}
					else {
						// Si le couple est déjà présent en for secondaire, il passe mixte_1, sinon, il passe source
						final List<ForFiscal> fors = menageCommun.getForsFiscauxValidAt(dateEvenement);
						boolean hasForSecondaire = false;
						for (ForFiscal ff : fors) {
							// si on trouve au moins un for secondaire, alors mixte_1
							if (ff instanceof ForFiscalSecondaire) {
								hasForSecondaire = true;
								break;
							}
						}
						modeImposition = hasForSecondaire ? ModeImposition.MIXTE_137_1 : getSourceOuMixteSuivantAgeRetraite(dateEvenement, principal, conjoint);
					}
					rattrapageDepartHSInconnu = null;
				}
				else {
					// déménagement vaudois d'après la provenance

					// [SIFISC-5451] détermination du for référent (peut ne pas être le "for fiscal" si celui ci est HS pays inconnu
					// alors que la provenance indiquée est vaudoise et que cela ne fait pas trop longtemps qu'on a quitté le canton)
					final ForFiscalPrincipalPP forPrecedent = menageCommun.getForFiscalPrincipalAt(ffpMenage.getDateDebut().getOneDayBefore());
					final ForFiscalPrincipalPP forReferent;
					if (forPrecedent != null
							&& ffpMenage.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS
							&& ffpMenage.getNumeroOfsAutoriteFiscale() == ServiceInfrastructureService.noPaysInconnu
							&& forPrecedent.getMotifFermeture() == MotifFor.DEPART_HS
							&& forPrecedent.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {

						// nous sommes clairement dans le cas d'un départ HS pays inconnu erroné (au moment du traitement du départ, nous avions sans doute trouvé une destination vide..)

						// il reste cependant quelques trucs à vérifier :
						// - la commune d'avant le départ HS
						// - les dates
						final Localisation localisationPrecedente = nouvelleAdresse.getLocalisationPrecedente();
						if (localisationPrecedente == null
								|| localisationPrecedente.getType() != LocalisationType.CANTON_VD
								|| !forPrecedent.getNumeroOfsAutoriteFiscale().equals(localisationPrecedente.getNoOfs())) {

							// problème -> la commune n'est pas la même
							throw new EvenementCivilException("Tentative de rattrapage d'un départ pour pays inconnu avortée en raison de communes vaudoises différentes.");
						}
						else if (!isDifferenceTwoYearsOrLess(ffpMenage.getDateDebut(), dateEvenement)) {
							// problème -> ça fait trop longtemps !
							throw new EvenementCivilException("Tentative de rattrapage d'un départ pour pays inconnu avortée en raison de la date de départ, trop vieille.");
						}

						forReferent = forPrecedent;
						rattrapageDepartHSInconnu = new RattrapageDepartHSInconnu(forPrecedent, ffpMenage);
					}
					else {
						forReferent = ffpMenage;
						rattrapageDepartHSInconnu = null;
					}

					// récupération du mode d'imposition sur le for référent
					if (forReferent.getModeImposition() == ModeImposition.SOURCE) {
						modeImposition = getSourceOuMixteSuivantAgeRetraite(dateEvenement, principal, conjoint);
					}
					else {
						modeImposition = forReferent.getModeImposition();
					}
				}
			}

			// détermination de la date d'ouverture
			final RegDate dateOuvertureFor = dateEvenement; // [UNIREG-2212] La date d'ouverture est toujours la date d'événement

			if (modeImposition != null) {
				if (motifOuverture == null) {
					motifOuverture = MotifFor.ARRIVEE_HS;
					warnings.addWarning("Ancienne adresse avant l'arrivée inconnue : veuillez indiquer le motif d'ouverture du for principal.");
				}
				if (ffpMenage == null) {
					Audit.info(getNumeroEvenement(), "Création d'un for fiscal principal sur le ménage commun avec mode d'imposition [" + modeImposition + ']');
					openForFiscalPrincipal(menageCommun, dateOuvertureFor, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, numeroOfsNouveau, MotifRattachement.DOMICILE, motifOuverture, modeImposition);
				}
				if (rattrapageDepartHSInconnu != null) {
					Audit.info(getNumeroEvenement(), "Rattrapage d'un ancien départ HS pour pays inconnu");
					openForFiscalPrincipalAvecRattrapage(menageCommun, dateOuvertureFor, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, numeroOfsNouveau, MotifRattachement.DOMICILE, MotifFor.DEMENAGEMENT_VD, modeImposition, rattrapageDepartHSInconnu);
				}
				else {
					Audit.info(getNumeroEvenement(), "Mise-à-jour de la commune du for fiscal principal sur le ménage commun avec mode d'imposition [" + modeImposition + ']');
					updateForFiscalPrincipal(menageCommun, dateOuvertureFor, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, numeroOfsNouveau, MotifRattachement.DOMICILE, motifOuverture, modeImposition);
				}
			}
		}
		catch (TiersException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}
	}

	/**
	 * On regarde les adresses de domicile des membres du couple (à la date d'arrivée) :
	 * <ul>
	 * <li>s'il n'y en a qu'une vaudoise -> on prend cette commune</li>
	 * <li>s'il y en a deux vaudoises, et qu'elles sont dans la même commune, alors on prend cette commune</li>
	 * <li>s'il y en a deux vaudoises, et qu'elles sont dans deux communes différentes, on ne touche à rien s'il y a déjà un for vaudois ouvert sur le couple, et on prend la commune du principal du couple sinon</li>
	 * <li>si on a pu déterminer une commune avec les conditions ci-dessus, on ouvre un for dessus à la date d'arrivée</li>
	 * </ul>
	 * @param arrivant personne physique concernée par l'arrivée
	 * @param ensemble ensemble du ménage et des personnes physiques le composant
	 * @return une commune selon les règles édictées plus haut
	 * @throws ch.vd.unireg.evenement.civil.common.EvenementCivilException en cas de souci
	 */
	private Pair<Commune, RegDate> getCommuneForSuiteArriveeCouple(PersonnePhysique arrivant, EnsembleTiersCouple ensemble) throws EvenementCivilException {

		final RegDate dateArrivee = getDate();
		try {
			final PersonnePhysique principal = ensemble.getPrincipal();
			final PersonnePhysique conjoint = ensemble.getConjoint();
			final MenageCommun menage = ensemble.getMenage();

			RegDate dateDebutFor = dateArrivee;

			final ForFiscalPrincipal ffpArrivee = menage.getForFiscalPrincipalAt(dateArrivee);
			if (ffpArrivee != null && ffpArrivee.getDateFin() != null) {
				// un for existe, mais le for est déjà fermé... il ya eu d'autres modifications déjà après la date d'arrivée
				// -> a régler manuellement
				throw new EvenementCivilException("Il y a eu d'autres changements déjà pris en compte après l'arrivée");
			}
			else if (ffpArrivee != null) {

				// quelles sont les communes de domicile de l'autre membre du menage (par rapport à celui qui arrive) ?
				final PersonnePhysique autre = ensemble.getConjoint(arrivant);
				if (autre != null && autre.getNumeroIndividu() != null && autre.getNumeroIndividu() > 0) {
					final List<HistoriqueCommune> communes = context.getServiceCivil().getCommunesDomicileHisto(dateArrivee, autre.getNumeroIndividu(), false, true);

					// s'il n'y en a pas (personne inconnue, ou HC/HS), ou une seule, il n'y a pas de problème : le conjoint (= l'autre)
					// n'a pas déménagé (en tout cas en ce qui concerne les fors) depuis l'arrivée
					if (communes != null && communes.size() > 1) {

						// il y a eu un déménagement (au moins)...

						// résumons-nous :
						// - il y avait déjà un for sur le ménage à la date d'arrivée, for toujours ouvert
						// - nous avons déjà connaissance du déménagement du conjoint de l'arrivant APRES cette arrivée-ci
						// --> les événements ne sont pas arrivés dans le bon ordre...

						// s'il y a plus d'un déménagement marquant (on ne compte pas les déménagements qui n'ont aucun lien avec le canton)
						// alors cela devient compliqué... --> traitemant manuel
						if (communes.size() > 2) {
							final StringBuilder b = new StringBuilder();
							for (int i = 1 ; i < communes.size() ; ++ i) {
								final Commune ancienneCommune = communes.get(i - 1).getCommune();
								final String nomAncienneCommune = ancienneCommune != null ? String.format("%s (%d)", ancienneCommune.getNomOfficiel(), ancienneCommune.getNoOFS()) : "HC/HS";
								final Commune nouvelleCommune = communes.get(i).getCommune();
								final String nomNouvelleCommune = nouvelleCommune != null ? String.format("%s (%d)", nouvelleCommune.getNomOfficiel(), nouvelleCommune.getNoOFS()) : "HC/HS";
								if (i > 1) {
									b.append(", ");
								}
								b.append(nomAncienneCommune).append(" -> ").append(nomNouvelleCommune).append(" au ").append(communes.get(i).getDateDebut());
							}

							throw new EvenementCivilException(String.format("Le contribuable %d a déjà déménagé plus d'une fois après l'arrivée de son conjoint (%s)", autre.getNumero(), b.toString()));
						}
						else {
							// un seul déménagement...
							final Commune ancienneCommune = communes.get(0).getCommune();
							final Commune nouvelleCommune = communes.get(1).getCommune();
							if (ancienneCommune != null && nouvelleCommune != null) {
								// déménagement vaudois -> c'est seulement à partir du moment
								// où les deux conjoints on déménagés que le for doit bouger
								dateDebutFor = communes.get(1).getDateDebut();
							}
						}
					}
				}
			}

			final Commune residencePrincipal = getCommuneDomicile(dateDebutFor, principal);
			final Commune residenceConjoint = getCommuneDomicile(dateDebutFor, conjoint);
			final boolean principalVaudois = residencePrincipal != null && residencePrincipal.isVaudoise();
			final boolean conjointVaudois = residenceConjoint != null && residenceConjoint.isVaudoise();

			final Commune commune;

			// aucun vaudois -> erreur !!
			if (!principalVaudois && !conjointVaudois) {
				throw new EvenementCivilException(String.format("Aucun membre du ménage %d n'a une adresse de domicile vaudoise", menage.getNumero()));
			}
			else if (!conjointVaudois) {
				commune = residencePrincipal;
			}
			else if (!principalVaudois) {
				commune = residenceConjoint;
			}
			else if (residencePrincipal.getNoOFS() == residenceConjoint.getNoOFS()) {
				commune = residencePrincipal;      // même commune pour les deux conjoints -> ils sont tous les deux arrivés
			}
			else {
				// deux adresses vaudoises, mais sur des communes différentes
				// y a-t-il déjà un for vaudois ouvert sur le couple (cette méthode est appelée avant la fermeture des fors...)
				if (ffpArrivee != null && ffpArrivee.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					// si le for principal vaudois sur le couple correspond à l'une des communes du principal ou du conjoint, on ne touche à rien
					if (ffpArrivee.getNumeroOfsAutoriteFiscale() == residencePrincipal.getNoOFS() || ffpArrivee.getNumeroOfsAutoriteFiscale() == residenceConjoint.getNoOFS()) {
						commune = null;
					}
					else {
						// sinon, les deux ont déménagé, et le for passe sur la commune de domicile du membre principal
						commune = residencePrincipal;
					}
				}
				else {
					// pas de for antérieur à l'arrivée, donc on doit en créer un
					commune = residencePrincipal;
				}
			}

			return commune == null ? null : new Pair<>(commune, dateDebutFor);
		}
		catch (ServiceInfrastructureException | DonneesCivilesException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}
	}

	/**
	 * Retourne la commune de domicile de la personne physique concernée
	 * @param date date de référence
	 * @param pp personne physique concernée
	 * @return commune de l'adresse de domicile, à la date donnée, de la personne physique donnée
	 * @throws DonneesCivilesException
	 * @throws ServiceInfrastructureException
	 */
	private Commune getCommuneDomicile(RegDate date, PersonnePhysique pp) throws DonneesCivilesException, ServiceInfrastructureException {
		final Commune commune;
		if (pp != null && pp.getNumeroIndividu() != null && pp.getNumeroIndividu() > 0) {
			final AdressesCiviles adresseDomicile = context.getServiceCivil().getAdresses(pp.getNumeroIndividu(), date, false);
			if (adresseDomicile.principale != null) {
				commune = context.getServiceInfra().getCommuneByAdresse(adresseDomicile.principale, date);
			}
			else {
				commune = null;
			}
		}
		else {
			// personne inconnue au civil...
			commune = null;
		}
		return commune;
	}

	@Override
	protected boolean isArriveeRedondantePourIndividuSeul() {
		// l'événement sera considéré comme redondant si
		//  - le tiers contribuable existe déjà
		//  - si majeur, ce tiers a un for principal ouvert sur la bonne commune à la bonne date avec un motif d'arrivée (jamais de redondance sur l'arrivée d'un mineur)
		boolean isRedondant = getPrincipalPP() != null;
		if (isRedondant) {
			final RegDate dateArrivee = getDateArriveeEffective(getDate());
			final boolean isMajeur = FiscalDateHelper.isMajeurAt(getIndividu(), dateArrivee);
			isRedondant = isMajeur && isForDejaBon(getPrincipalPP(), dateArrivee, true);
		}
		return isRedondant;
	}

	private boolean isForDejaBon(Contribuable ctb, RegDate dateArrivee, boolean beginDateMustMatch) {
		final ForFiscalPrincipal ffp = ctb.getForFiscalPrincipalAt(dateArrivee);
		final MotifFor motifAttendu = getMotifOuvertureFor();
		final int ofsCommuneArrivee = nouvelleCommune.getNoOFS();
		return ffp != null && (!beginDateMustMatch || ffp.getDateDebut() == dateArrivee) && motifAttendu == ffp.getMotifOuverture()
				&& ofsCommuneArrivee == ffp.getNumeroOfsAutoriteFiscale() && ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}
	@Override
	protected boolean isArriveeRedondanteAnterieurPourIndividuEnMenage(){
		boolean isRedondant = getPrincipalPP() != null;
		if (isRedondant) {
			final RegDate dateArrivee = getDateArriveeEffective(getDate());
			final EnsembleTiersCouple coupleExistant = context.getTiersService().getEnsembleTiersCouple(getPrincipalPP(), dateArrivee);
			if (coupleExistant != null) {
				final List<ForFiscalPrincipal> listFfp = coupleExistant.getMenage().getForsFiscauxPrincipauxOuvertsApres(dateArrivee);
				isRedondant = existForArriveeOuvertApres(listFfp, dateArrivee);
			}
			else {
				isRedondant = false;
			}
		}
		return isRedondant;
	}

	@Override
	protected boolean isConjointMarieSeul() {
		boolean isConjointMarieSeul = false;
		final RegDate dateArrivee = getDateArriveeEffective(getDate());
		final Individu individuConjoint = context.getServiceCivil().getConjoint(getNoIndividu(), dateArrivee);
		if (individuConjoint != null) {
			final PersonnePhysique conjoint = context.getTiersDAO().getPPByNumeroIndividu(individuConjoint.getNoTechnique(), true);
			if (conjoint == null) {
				isConjointMarieSeul = false;
			}
			else {
				final EnsembleTiersCouple coupleExistant = context.getTiersService().getEnsembleTiersCouple(conjoint, dateArrivee);
				isConjointMarieSeul = coupleExistant != null && (coupleExistant.getConjoint() == null || coupleExistant.getPrincipal() == null);
			}
		}
		return isConjointMarieSeul;
	}

	@Override
	protected boolean isArriveeRedondantePosterieurPourIndividuEnMenage() {
		boolean isRedondant = getPrincipalPP() != null;
		if (isRedondant) {
			final MotifFor motifOuverture = getMotifOuvertureFor();
			final RegDate dateArrivee = getDateArriveeEffective(getDate());
			final EnsembleTiersCouple coupleExistant = context.getTiersService().getEnsembleTiersCouple(getPrincipalPP(), dateArrivee);
			if (coupleExistant != null) {
				final ForFiscalPrincipal ffp = coupleExistant.getMenage().getForFiscalPrincipalAt(dateArrivee);
				isRedondant = ffp != null && dateArrivee.isAfter(ffp.getDateDebut()) &&
						(MotifFor.ARRIVEE_HC == ffp.getMotifOuverture() || MotifFor.ARRIVEE_HS == ffp.getMotifOuverture()) &&
						(MotifFor.ARRIVEE_HC == motifOuverture || MotifFor.ARRIVEE_HS == motifOuverture);
			}
			else {
				// SIFISC-6926 : si on n'a pas de couple, on n'a pas le droit de dire que c'est redondant !!!
				isRedondant = false;
			}
		}
		return isRedondant;
	}

	private static boolean existForArriveeOuvertApres(List<ForFiscalPrincipal> listeFor, RegDate dateArrivee) {
		for (ForFiscalPrincipal forFiscalPrincipal : listeFor) {
			if (MotifFor.ARRIVEE_HC == forFiscalPrincipal.getMotifOuverture() || MotifFor.ARRIVEE_HS == forFiscalPrincipal.getMotifOuverture()) {
				if (dateArrivee.isBefore(forFiscalPrincipal.getDateDebut())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected boolean isArriveeRedondantePourIndividuEnMenage() {
		// l'événement sera considéré comme redondant si
		//   - le tiers contribuable PP existe déjà, ainsi que celui de son couple (lié au même conjoint si couple complet)
		//   - le for du couple a été ouvert à la bonne date sur la bonne commune avec le bon motif
		boolean isRedondant = getPrincipalPP() != null;
		if (isRedondant) {
			final RegDate dateArrivee = getDateArriveeEffective(getDate());
			final EnsembleTiersCouple coupleExistant = context.getTiersService().getEnsembleTiersCouple(getPrincipalPP(), dateArrivee);
			isRedondant = (coupleExistant != null && isForDejaBon(coupleExistant.getMenage(), dateArrivee, false));
			if (isRedondant) {
				// reste le conjoint à vérifier
				final Individu individuConjoint = context.getServiceCivil().getConjoint(getNoIndividu(), dateArrivee);
				if (individuConjoint != null) {
					final PersonnePhysique conjoint = context.getTiersDAO().getPPByNumeroIndividu(individuConjoint.getNoTechnique(), true);
					if (conjoint == null) {
						// visiblement, le conjoint n'a pas encore été créé chez nous... Il reste donc des trucs à faire
						isRedondant = false;
					}
					else {
						final PersonnePhysique conjointFiscal = coupleExistant.getConjoint(getPrincipalPP());
						isRedondant = conjointFiscal != null && conjointFiscal.getNumero().equals(conjoint.getNumero());
					}
				}
			}
		}
		return isRedondant;
	}
}
