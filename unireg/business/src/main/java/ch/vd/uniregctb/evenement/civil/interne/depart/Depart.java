package ch.vd.uniregctb.evenement.civil.interne.depart;

import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.mouvement.Mouvement;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementErreur;

/**
 * Evénement de départ d'un individu dans les cas suivants:
 * <ul>
 * <li>DEPART_SECONDAIRE : déménagement d'une commune vaudoise à l'autre (intra-cantonal) pour l'adresse secondaire</li>
 * <li>DEPART_COMMUNE : déménagement d'un canton à l'autre (inter-cantonal) ou Départ de Suisse</li>
 * </ul>
 */
public class Depart extends Mouvement {

	/** LOGGER log4J */
	protected static Logger LOGGER = Logger.getLogger(Depart.class);

	private Adresse ancienneAdressePrincipale;
	private Commune nouvelleCommunePrincipale;
	private Commune ancienneCommunePrincipale;
	private Adresse ancienneAdresseSecondaire;
	private Commune ancienneCommuneSecondaire;
	private Adresse ancienneAdresseCourrier;

	private Pays paysInconnu;

	protected Depart(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		if (evenement.getNumeroIndividuConjoint()!=null) {
			isAncienTypeDepart = true;
		}

		final RegDate dateDepart = evenement.getDateEvenement();
		final RegDate lendemainDepart = dateDepart.getOneDayAfter();

		// on récupère les anciennes adresses (= à la date d'événement)
		final AdressesCiviles adresses;
		try {
			adresses = new AdressesCiviles(context.getServiceCivil().getAdresses(super.getNoIndividu(), dateDepart, false));
		}
		catch (DonneesCivilesException e) {
			throw new EvenementCivilException(e);
		}

		this.ancienneAdressePrincipale = adresses.principale;
		this.ancienneAdresseCourrier = adresses.courrier;
		this.ancienneAdresseSecondaire=adresses.secondaire;

		try {
			// on récupère la commune de l'adresse principale avant le départ
			this.ancienneCommunePrincipale = context.getServiceInfra().getCommuneByAdresse(ancienneAdressePrincipale, dateDepart);

			// on récupère la commune de l'adresse principale avant le départ
			this.ancienneCommuneSecondaire = context.getServiceInfra().getCommuneByAdresse(ancienneAdresseSecondaire, dateDepart);

			// on récupère la commune de la nouvelle adresse principal
			this.nouvelleCommunePrincipale = context.getServiceInfra().getCommuneByAdresse(getNouvelleAdressePrincipale(), lendemainDepart);

			this.paysInconnu = context.getServiceInfra().getPaysInconnu();
		}
		catch (ServiceInfrastructureException e) {
			throw new EvenementCivilException(e);
		}
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Depart(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Commune ancienneCommunePrincipale, Commune nouvelleCommunePrincipale,
	                 Adresse ancienneAdressePrincipale, Adresse nouvelleAdressePrincipale, Adresse ancienneAdresseCourrier, Adresse nouvelleAdresseCourrier, Commune ancienneCommuneSecondaire,
	                 Adresse ancienneAdresseSecondaire, boolean departPrincipal, EvenementCivilContext context) {
		super(individu, conjoint, (departPrincipal ? TypeEvenementCivil.DEPART_COMMUNE : TypeEvenementCivil.DEPART_SECONDAIRE), date, numeroOfsCommuneAnnonce, nouvelleAdressePrincipale, null,
				nouvelleAdresseCourrier, context);
		this.ancienneAdressePrincipale = ancienneAdressePrincipale;
		this.nouvelleCommunePrincipale = nouvelleCommunePrincipale;
		this.ancienneCommunePrincipale = ancienneCommunePrincipale;
		this.ancienneAdresseCourrier = ancienneAdresseCourrier;
		this.ancienneAdresseSecondaire = ancienneAdresseSecondaire;
		this.ancienneCommuneSecondaire = ancienneCommuneSecondaire;
		this.paysInconnu = context.getServiceInfra().getPaysInconnu();
	}

	/**
	 * Indique si l'evenement est un ancien type de départ
	 */
	private boolean isAncienTypeDepart = false;

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		Audit.info(getNumeroEvenement(), "Verification de la cohérence du départ");

		if (getType() == TypeEvenementCivil.DEPART_COMMUNE) {

			verifierMouvementIndividu(this, false, erreurs, warnings);
			// Si le pays de destination est inconnue, on leve un warning
			if (findMotifFermeture(this) == MotifFor.DEPART_HS) {
				if (isPaysInconnu(this)) {
					warnings.add(new EvenementCivilExterneErreur(
							"Le nouveau pays n'a pas été trouvé : veuillez vérifier le motif de fermeture du for principal",
							TypeEvenementErreur.WARNING));
				}

			} // Verification de la nouvelle commune principale en hors canton
			else if (getNouvelleCommunePrincipale() == null) {
				warnings.add(new EvenementCivilExterneErreur(
						"La nouvelle commune principale n'a pas été trouvée : veuillez vérifier le motif de fermeture du for principal",
						TypeEvenementErreur.WARNING));
			}
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {

		final PersonnePhysique pp = context.getTiersDAO().getPPByNumeroIndividu(getNoIndividu());
		if (pp == null) {
			// si on ne connaissait pas le gaillard, c'est un problème
			throw new EvenementCivilException("Aucun habitant (ou ancien habitant) trouvé avec numéro d'individu " + getNoIndividu());
		}

		final MotifFor motifFermeture = findMotifFermeture(this);
		final RegDate dateFermeture = findDateFermeture(this, motifFermeture == MotifFor.DEMENAGEMENT_VD);
		final Contribuable contribuable = findContribuable(this, pp, motifFermeture == MotifFor.DEMENAGEMENT_VD);

		if (getType() == TypeEvenementCivil.DEPART_COMMUNE) {

			// [UNIREG-2701] si l'ancienne adresse est inconnue, et que le tiers n'a aucun for non-annulé -> on laisse passer
			if (getAncienneCommunePrincipale() == null) {

				// fors non-annulés sur le contribuable actif à la date de l'événement
				final List<ForFiscal> tousFors = contribuable.getForsFiscauxNonAnnules(false);
				if (tousFors == null || tousFors.size() == 0) {
					// pas de fors non-annulés -> tout va bien, on accepte l'événement sans autre
					Audit.info(getNumeroEvenement(), "Commune de départ inconnue mais contribuable sans for, départ traité sans autre");
					return null;
				}
				else {
					throw new EvenementCivilException("La commune de départ n'a pas été trouvée");
				}
			}

			//[UNIREG-1996] on traite les deux habitants ensemble conformement à l'ancien fonctionement
			if (isAncienTypeDepart()) {
				traiteHabitantOfAncienDepart(this, pp);
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
				if (!isDepartComplet(this)) {
					return null;
				}
			}

			Audit.info(getNumeroEvenement(), "Traitement du départ principal");

			/*
			 * Fermetures des adresses temporaires du fiscal
			 */
			fermeAdresseTiersTemporaire(contribuable, getDate().getOneDayBefore());

			int numeroOfsAutoriteFiscale;
			if (motifFermeture == MotifFor.DEPART_HC) {
				if (getNouvelleCommunePrincipale() != null) {
					numeroOfsAutoriteFiscale = getNouvelleCommunePrincipale().getNoOFS();
				}
				else {
					numeroOfsAutoriteFiscale = getPaysInconnu().getNoOFS();
				}
			}
			else {
				// Depart hors suisse
				if (isPaysInconnu(this)) {

					numeroOfsAutoriteFiscale = getPaysInconnu().getNoOFS();
				}
				else {
					numeroOfsAutoriteFiscale = getNouvelleAdressePrincipale().getNoOfsPays();
				}
			}
			handleDepartResidencePrincipale(this, contribuable, dateFermeture, motifFermeture, numeroOfsAutoriteFiscale);
		}
		else {
			Assert.isEqual(TypeEvenementCivil.DEPART_SECONDAIRE, getType());
			Audit.info(getNumeroEvenement(), "Traitement du départ secondaire");
			handleDepartResidenceSecondaire(this, contribuable, dateFermeture, motifFermeture);
		}

		return null;
	}


	/**
	 * Permet de transformer un habitant et son conjoint en non habitant dans le cas d'un ancien Type de départ
	 *
	 * @param depart un événement de départ
	 * @param pp     une personne physique
	 */
	private void traiteHabitantOfAncienDepart(final Depart depart, final PersonnePhysique pp) {
		final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(pp, depart.getDate());
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
	private boolean isDepartComplet(Depart depart) {

		final Individu individuPrincipal = depart.getIndividu();
		final PersonnePhysique habitant = getService().getPersonnePhysiqueByNumeroIndividu(individuPrincipal.getNoTechnique());
		final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(habitant, depart.getDate());
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
	 * Détermine si l'habitant est seul ou en ménage et renvoi dans ce cas son ménage.
	 *
	 * @param depart         un événement de départ
	 * @param pp             une personne physique
	 * @param demenagementVD <b>vrai</b> s'il s'agit d'un déménagement entre commune vaudoise; <b>false</b> autrement.
	 * @return le contribuable concerné par le déménagement
	 */
	private Contribuable findContribuable(Depart depart, PersonnePhysique pp, boolean demenagementVD) {

		final RegDate dateEvenement = findDateFermeture(depart, demenagementVD);

		final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(pp, dateEvenement);
		if (couple != null) {
			final MenageCommun menage = couple.getMenage();
			if (menage != null) {
				return menage;
			}
		}
		return pp;
	}

	/**
	 * [UNIREG-2212] Calcule la date de fermeture du for fiscal principal en fonction de la date de départ
	 *
	 * @param depart         un événement de départ
	 * @param demenagementVD <b>vrai</b> s'il s'agit d'un déménagement entre commune vaudoise; <b>false</b> autrement.
	 * @return la date de fermeture du for fiscal
	 */
	private RegDate findDateFermeture(Depart depart, boolean demenagementVD) {

		final RegDate dateFermeture;
		if (demenagementVD) {
			dateFermeture = FiscalDateHelper.getDateFermetureForFiscal(depart.getDate());
		}
		else {
			dateFermeture = depart.getDate();
		}

		return dateFermeture;
	}

	/**
	 * calcule le motif de fermeture du for fiscal
	 *
	 * @param depart         un événement de départ
	 * @return le motif de fermeture
	 */
	private MotifFor findMotifFermeture(Depart depart) {
		MotifFor motifFermeture;

		// Départ vers l'etranger
		Adresse nouvelleAdressePrincipale = depart.getNouvelleAdressePrincipale();
		if (nouvelleAdressePrincipale != null && nouvelleAdressePrincipale.getNoOfsPays() != null) {
			boolean estEnSuisse;
			estEnSuisse = getService().getServiceInfra().estEnSuisse(nouvelleAdressePrincipale);
			if (!estEnSuisse) {
				motifFermeture = MotifFor.DEPART_HS;
			}
			else if (!depart.getNouvelleCommunePrincipale().isVaudoise()) {

				motifFermeture = MotifFor.DEPART_HC;

			}
			else {
				// on ne devrait jamais avoir ce cas pour un départ: c'est un déménagement
				// Audit.warn("La commune de destination est dans le canton de Vaud une erreur a du se produire");
				/*
				 * msi (16.09.2008) : en fait, il s'agit d'un cas valide où un contribuable bénéficiant d'un arrangement fiscal (= for
				 * principal ouvert sur une résidence secondaire dans le canton) quitte sa résidence secondaire pour sa résidence principale
				 * elle-même située dans le canton : dans ce cas il s'agit bien d'un départ de la résidence secondaire, mais il se traduit
				 * par un déménagement vaudois.
				 */
				motifFermeture = MotifFor.DEMENAGEMENT_VD;
			}
		}
		else {
			Audit.warn("Le pays de destination est inconnu");
			motifFermeture = MotifFor.DEPART_HS;
		}

		return motifFermeture;
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {
		/*
		 * Validation des adresses
		 */
		if (getType() == TypeEvenementCivil.DEPART_COMMUNE) {
			Audit.info(getNumeroEvenement(), "Validation de la nouvelle adresse principale");
			validateDepartAdressePrincipale(this, erreurs);
		}
		else { // depart.getType() == TypeEvenementCivil.DEPART_SECONDAIRE
			Audit.info(getNumeroEvenement(), "Validation du départ de résidence secondaire");
			validateDepartAdresseSecondaire(this, erreurs);
		}

		/*
		 * Le Depart du mort-vivant
		 */
		if (getIndividu().getDateDeces() != null) {
			erreurs.add(new EvenementCivilExterneErreur("L'individu est décédé"));
		}
	}


	protected final void validateDepartAdressePrincipale(Depart depart, List<EvenementCivilExterneErreur> erreurs) {
		final Adresse adressePrincipale = depart.getAncienneAdressePrincipale();
		final Commune commune = depart.getAncienneCommunePrincipale();

		validateCoherenceAdresse(adressePrincipale, commune, depart, erreurs);

		// la nouvelle commune est toujours dans le canton de vaud
		final Commune nouvelleCommune = depart.getNouvelleCommunePrincipale();

		if (nouvelleCommune != null && nouvelleCommune.isVaudoise()) {
			erreurs.add(new EvenementCivilExterneErreur("La nouvelle commune est toujours dans le canton de Vaud"));
		}
	}

	protected final void validateDepartAdresseSecondaire(Depart depart, List<EvenementCivilExterneErreur> erreurs) {
		final Adresse adresseSecondaire = depart.getAncienneAdresseSecondaire();
		final Commune communeSecondaire = depart.getAncienneCommuneSecondaire();

		validateCoherenceAdresse(adresseSecondaire, communeSecondaire, depart, erreurs);
	}

	/**
	 * Permet de valider la cohérence d'une adresse fournie par l'événement départ
	 *
	 * @param adresse
	 * @param commune
	 * @param depart         un événement de départ
	 * @param erreurs
	 */
	public void validateCoherenceAdresse(Adresse adresse, Commune commune, Depart depart, List<EvenementCivilExterneErreur> erreurs) {

		if (adresse == null) {
			erreurs.add(new EvenementCivilExterneErreur("Adresse de résidence avant départ inconnue"));
		}
		else if (adresse.getDateFin() == null) {
			erreurs.add(new EvenementCivilExterneErreur("La date de fin de validité de la résidence est inconnue"));
		}
		// la date de départ est differente de la date de fin de validité de l'adresse
		else if (!depart.getDate().equals(adresse.getDateFin())) {
			erreurs.add(new EvenementCivilExterneErreur(
					"La date de départ est différente de la date de fin de validité de l'adresse dans le canton"));
		}

		// La commune d'annonce est differente de la commune de résidence avant l'évenement
		// de départ
		//TODO (BNM) attention si depart.getNumeroOfsCommuneAnnonce correspond à une commune avec des fractions
		if (commune != null) {
			if ((!commune.isFraction() && commune.getNoOFS() != depart.getNumeroOfsCommuneAnnonce()) ||
					(commune.isFraction() && commune.getNumTechMere() != depart.getNumeroOfsCommuneAnnonce())) {
				erreurs.add(new EvenementCivilExterneErreur("La commune d'annonce est differente de la dernière commune de résidence"));
			}
		}
	}

	/**
	 * Permet d'ouvrir un for principal sur une commune hors canton
	 *
	 * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param modeImposition           le mode d'imposition du for fiscal principal
	 * @param motifOuverture           le motif d'ouverture du for fiscal principal
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalPrincipal openForFiscalPrincipalHC(Contribuable contribuable, final RegDate dateOuverture, int numeroOfsAutoriteFiscale, ModeImposition modeImposition,
	                                                      MotifFor motifOuverture) {
		return getService()
				.openForFiscalPrincipal(contribuable, dateOuverture, MotifRattachement.DOMICILE, numeroOfsAutoriteFiscale, TypeAutoriteFiscale.COMMUNE_HC, modeImposition, motifOuverture, false);
	}

	/**
	 * Permet d'ouvrir un for principal sur un pays
	 *
	 * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param modeImposition           le mode d'imposition du for fiscal principal
	 * @param motifOuverture           le motif d'ouverture du for fiscal principal
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalPrincipal openForFiscalPrincipalHS(Contribuable contribuable, final RegDate dateOuverture, int numeroOfsAutoriteFiscale, ModeImposition modeImposition,
	                                                      MotifFor motifOuverture) {
		return getService()
				.openForFiscalPrincipal(contribuable, dateOuverture, MotifRattachement.DOMICILE, numeroOfsAutoriteFiscale, TypeAutoriteFiscale.PAYS_HS, modeImposition, motifOuverture, false);
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

	private static ModeImposition determineModeImpositionDepartHCHS(Contribuable contribuable, RegDate dateFermeture, ForFiscalPrincipal ffp) {

		Assert.notNull(ffp);

		final ModeImposition modeImpositionAncien = ffp.getModeImposition();
		final ModeImposition modeImposition;
		if (isSourcierPur(modeImpositionAncien)) {
			// Un sourcier pur reste à la source.
			modeImposition = ModeImposition.SOURCE;
		}
		else if (isSourcierMixte(modeImpositionAncien)) {
			// [UNIREG-1849] passe à l'ordinaire si for secondaire, sinon à la source
			final List<ForFiscal> ffs = contribuable.getForsFiscauxValidAt(dateFermeture);
			boolean hasForSecondaire = false;
			for (ForFiscal ff : ffs) {
				if (ff instanceof ForFiscalSecondaire) {
					hasForSecondaire = true;
					break;
				}
			}
			if (hasForSecondaire) {
				modeImposition = ModeImposition.ORDINAIRE;
			}
			else {
				modeImposition = ModeImposition.SOURCE;
			}
		}
		else {
			// tous les autres cas passent à l'ordinaire (ordinaire, dépense, indigent...)
			modeImposition = ModeImposition.ORDINAIRE;
		}
		return modeImposition;
	}

	private static boolean isSourcierPur(ModeImposition modeImposition) {
		return ModeImposition.SOURCE == modeImposition;
	}

	private static boolean isSourcierMixte(ModeImposition modeImposition) {
		return ModeImposition.MIXTE_137_1 == modeImposition || ModeImposition.MIXTE_137_2 == modeImposition;
	}

	/**
	 * Traite un depart d'une residence secondaire
	 *
	 * @param depart         un événement de départ
	 * @param contribuable
	 * @param dateFermeture
	 * @param motifFermeture
	 */
	private void handleDepartResidenceSecondaire(Depart depart, Contribuable contribuable, RegDate dateFermeture, MotifFor motifFermeture) {

		final ForFiscalPrincipal forPrincipal = contribuable.getForFiscalPrincipalAt(depart.getDate());
		// For principal est sur la commune de départ d'une résidence secondaire

		//TODO (BNM) attention si depart.getNumeroOfsCommuneAnnonce correspond à une commune avec des fractions
		//ce cas d'arrangement fiscal ne sera pas détecté, il faut mettre l'événement en erreur pour
		//traitement manuel

		if (forPrincipal != null && forPrincipal.getNumeroOfsAutoriteFiscale().equals(depart.getNumeroOfsCommuneAnnonce())) {

			final ServiceInfrastructureService serviceInfra = getService().getServiceInfra();

			final Adresse adressePrincipale = depart.getNouvelleAdressePrincipale();

			final boolean estEnSuisse;
			Commune commune = null;
			estEnSuisse = serviceInfra.estEnSuisse(adressePrincipale);
			if (estEnSuisse) {
				commune = serviceInfra.getCommuneByAdresse(adressePrincipale, depart.getDate().getOneDayAfter());
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
				if (estEnSuisse) {
					if (commune.isVaudoise()) {
						/*
						 * passage d'une commune vaudoise ouverte sur une résidence secondaire à une commune vaudoise ouverte sur la résidence
						 * principale : il s'agit donc d'un déménagement vaudois
						 */
						openForFiscalPrincipal(contribuable, dateFermeture.getOneDayAfter(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, commune.getNoOFS(), MotifRattachement.DOMICILE, MotifFor.DEMENAGEMENT_VD, ffp.getModeImposition(), false);
					}
					else {
						final ModeImposition modeImpostion = determineModeImpositionDepartHCHS(contribuable, dateFermeture, ffp);
						openForFiscalPrincipalHC(contribuable, dateFermeture.getOneDayAfter(), commune.getNoOFS(), modeImpostion, MotifFor.DEPART_HC);
					}
				}
				else if (ffp != null) {
					final ModeImposition modeImposition = determineModeImpositionDepartHCHS(contribuable, dateFermeture, ffp);
					openForFiscalPrincipalHS(contribuable, dateFermeture.getOneDayAfter(), adressePrincipale.getNoOfsPays(), modeImposition, MotifFor.DEPART_HS);
				}
			}
		}
	}

	private static boolean isPaysInconnu(Depart depart) {

		boolean paysInconnu = false;

		if (depart.getNouvelleAdressePrincipale() != null) {

			if (depart.getNouvelleAdressePrincipale().getNoOfsPays() == null) {
				paysInconnu = true;
			}
		}
		else {
			paysInconnu = true;
		}
		return paysInconnu;
	}

	public Adresse getNouvelleAdressePrincipale() {
		return getAdressePrincipale();
	}

	public Commune getNouvelleCommunePrincipale() {
		return this.nouvelleCommunePrincipale;
	}

	public Adresse getAncienneAdressePrincipale() {
		return ancienneAdressePrincipale;
	}

	public Commune getAncienneCommunePrincipale() {
		return ancienneCommunePrincipale;
	}

	public Adresse getAncienneAdresseSecondaire() {
		return ancienneAdresseSecondaire;
	}

	public Commune getAncienneCommuneSecondaire() {
		return ancienneCommuneSecondaire;
	}

	public Pays getPaysInconnu() {
		return paysInconnu;
	}

	public boolean isAncienTypeDepart() {
		return isAncienTypeDepart;
	}
}
