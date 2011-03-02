package ch.vd.uniregctb.evenement.civil.interne.depart;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.Individu;
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
 * Gère le départ d'un individu dans les cas suivants:
 * <ul>
 * <li>DEPART_SECONDAIRE : déménagement d'une commune vaudoise à l'autre (intra-cantonal) pour l'adresse secondaire</li>
 * <li>DEPART_COMMUNE : déménagement d'un canton à l'autre (inter-cantonal) ou Départ de Suisse</li>
 * </ul>
 */
public class DepartHandler extends EvenementCivilHandlerBase {

	public void checkCompleteness(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		Audit.info(target.getNumeroEvenement(), "Verification de la cohérence du départ");
		Depart depart = (Depart) target;

		if (depart.getType() == TypeEvenementCivil.DEPART_COMMUNE) {

			verifierMouvementIndividu(depart, false, erreurs, warnings);
			// Si le pays de destination est inconnue, on leve un warning
			if (findMotifFermeture(depart) == MotifFor.DEPART_HS) {
				if (isPaysInconnu(depart)) {
					warnings.add(new EvenementCivilExterneErreur(
							"Le nouveau pays n'a pas été trouvé : veuillez vérifier le motif de fermeture du for principal",
							TypeEvenementErreur.WARNING));
				}

			} // Verification de la nouvelle commune principale en hors canton
			else if (depart.getNouvelleCommunePrincipale() == null) {
				warnings.add(new EvenementCivilExterneErreur(
						"La nouvelle commune principale n'a pas été trouvée : veuillez vérifier le motif de fermeture du for principal",
						TypeEvenementErreur.WARNING));
			}
		}
	}

	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		final Depart depart = (Depart) evenement;

		final PersonnePhysique pp = getTiersDAO().getPPByNumeroIndividu(depart.getNoIndividu());
		if (pp == null) {
			// si on ne connaissait pas le gaillard, c'est un problème
			throw new EvenementCivilHandlerException("Aucun habitant (ou ancien habitant) trouvé avec numéro d'individu " + depart.getNoIndividu());
		}

		final MotifFor motifFermeture = findMotifFermeture(depart);
		final RegDate dateFermeture = findDateFermeture(depart, motifFermeture == MotifFor.DEMENAGEMENT_VD);
		final Contribuable contribuable = findContribuable(depart, pp, motifFermeture == MotifFor.DEMENAGEMENT_VD);

		if (depart.getType() == TypeEvenementCivil.DEPART_COMMUNE) {

			// [UNIREG-2701] si l'ancienne adresse est inconnue, et que le tiers n'a aucun for non-annulé -> on laisse passer
			if (depart.getAncienneCommunePrincipale() == null) {

				// fors non-annulés sur le contribuable actif à la date de l'événement
				final List<ForFiscal> tousFors = contribuable.getForsFiscauxNonAnnules(false);
				if (tousFors == null || tousFors.size() == 0) {
					// pas de fors non-annulés -> tout va bien, on accepte l'événement sans autre
					Audit.info(evenement.getNumeroEvenement(), "Commune de départ inconnue mais contribuable sans for, départ traité sans autre");
					return null;
				}
				else {
					throw new EvenementCivilHandlerException("La commune de départ n'a pas été trouvée");
				}
			}

			//[UNIREG-1996] on traite les deux habitants ensemble conformement à l'ancien fonctionement
			if (depart.isAncienTypeDepart()) {
				traiteHabitantOfAncienDepart(depart, pp);
			}
			else {

				// [UNIREG-1691] si la personne physique était déjà notée non-habitante, on ne fait que régulariser une situation bancale
				if (pp.isHabitantVD()) {
					getService().changeHabitantenNH(pp);
				}

				/*
				 * [UNIREG-771] : L'événement de départ du premier doit passer l'individu de habitant à non habitant et ne rien faire d'autre
				 * (notamment au niveau des fors fiscaux)
				 */
				if (!isDepartComplet(depart)) {
					return null;
				}
			}

			Audit.info(depart.getNumeroEvenement(), "Traitement du départ principal");

			/*
			 * Fermetures des adresses temporaires du fiscal
			 */
			fermeAdresseTiersTemporaire(contribuable, evenement.getDate().getOneDayBefore());

			int numeroOfsAutoriteFiscale;
			if (motifFermeture == MotifFor.DEPART_HC) {
				if (depart.getNouvelleCommunePrincipale() != null) {
					numeroOfsAutoriteFiscale = depart.getNouvelleCommunePrincipale().getNoOFS();
				}
				else {
					numeroOfsAutoriteFiscale = depart.getPaysInconnu().getNoOFS();
				}
			}
			else {
				// Depart hors suisse
				if (isPaysInconnu(depart)) {

					numeroOfsAutoriteFiscale = depart.getPaysInconnu().getNoOFS();
				}
				else {
					numeroOfsAutoriteFiscale = depart.getNouvelleAdressePrincipale().getNoOfsPays();
				}
			}
			handleDepartResidencePrincipale(depart, contribuable, dateFermeture, motifFermeture, numeroOfsAutoriteFiscale);
		}
		else {
			Assert.isEqual(TypeEvenementCivil.DEPART_SECONDAIRE, depart.getType());
			Audit.info(depart.getNumeroEvenement(), "Traitement du départ secondaire");
			handleDepartResidenceSecondaire(depart, contribuable, dateFermeture, motifFermeture);
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
			try {
				estEnSuisse = getService().getServiceInfra().estEnSuisse(nouvelleAdressePrincipale);
			}
			catch (InfrastructureException e) {
				throw new RuntimeException(e);
			}
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
	protected void validateSpecific(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		final Depart depart = (Depart) target;
		/*
		 * Validation des adresses
		 */
		try {
			if (depart.getType() == TypeEvenementCivil.DEPART_COMMUNE) {
				Audit.info(depart.getNumeroEvenement(), "Validation de la nouvelle adresse principale");
				validateDepartAdressePrincipale(depart, erreurs);
			}
			else { // depart.getType() == TypeEvenementCivil.DEPART_SECONDAIRE
				Audit.info(depart.getNumeroEvenement(), "Validation du départ de résidence secondaire");
				validateDepartAdresseSecondaire(depart, erreurs);
			}
		}
		catch (EvenementCivilHandlerException e) {
			erreurs.add(new EvenementCivilExterneErreur(e));
		}

		/*
		 * Le Depart du mort-vivant
		 */
		if (depart.getIndividu().getDateDeces() != null) {
			erreurs.add(new EvenementCivilExterneErreur("L'individu est décédé"));
		}
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.DEPART_COMMUNE);
		types.add(TypeEvenementCivil.DEPART_SECONDAIRE);
		return types;
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new DepartAdapter(event, context, this);
	}

	protected final void validateDepartAdressePrincipale(Depart depart, List<EvenementCivilExterneErreur> erreurs) {
		final Adresse adressePrincipale = depart.getAncienneAdressePrincipale();
		final CommuneSimple commune = depart.getAncienneCommunePrincipale();

		validateCoherenceAdresse(adressePrincipale, commune, depart, erreurs);

		// la nouvelle commune est toujours dans le canton de vaud
		final CommuneSimple nouvelleCommune = depart.getNouvelleCommunePrincipale();

		if (nouvelleCommune != null && nouvelleCommune.isVaudoise()) {
			erreurs.add(new EvenementCivilExterneErreur("La nouvelle commune est toujours dans le canton de Vaud"));
		}
	}

	protected final void validateDepartAdresseSecondaire(Depart depart, List<EvenementCivilExterneErreur> erreurs) {
		final Adresse adresseSecondaire = depart.getAncienneAdresseSecondaire();
		final CommuneSimple communeSecondaire = depart.getAncienneCommuneSecondaire();

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
	public void validateCoherenceAdresse(Adresse adresse, CommuneSimple commune, Depart depart, List<EvenementCivilExterneErreur> erreurs) {

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
			CommuneSimple commune = null;
			try {
				estEnSuisse = serviceInfra.estEnSuisse(adressePrincipale);
				if (estEnSuisse) {
					commune = serviceInfra.getCommuneByAdresse(adressePrincipale);
				}
			}
			catch (InfrastructureException e) {
				throw new RuntimeException("la nouvelle adresse principale est inconnue", e);
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
}
