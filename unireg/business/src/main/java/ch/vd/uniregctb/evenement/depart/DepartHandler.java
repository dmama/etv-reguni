package ch.vd.uniregctb.evenement.depart;

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
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
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

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		Audit.info(target.getNumeroEvenement(), "Verification de la cohérence du départ");
		Depart depart = (Depart) target;

		if (depart.getType() == TypeEvenementCivil.DEPART_COMMUNE) {

			verifierMouvementIndividu(depart, false, erreurs, warnings);
			// Si le pays de destination est inconnue, on leve un warning
			if (findMotifFermeture(depart).equals(MotifFor.DEPART_HS)) {
				if (isPaysInconnu(depart)) {
					warnings.add(new EvenementCivilErreur(
							"Le nouveau pays n'a pas été trouvé : veuillez vérifier le motif de fermeture du for principal",
							TypeEvenementErreur.WARNING));
				}

			} // Verification de la nouvelle commune principale en hors canton
			else if (depart.getNouvelleCommunePrincipale() == null) {
				warnings.add(new EvenementCivilErreur(
						"La nouvelle commune principale n'a pas été trouvée : veuillez vérifier le motif de fermeture du for principal",
						TypeEvenementErreur.WARNING));
			}
			// Verification de la commune de départ
			if (depart.getAncienneCommunePrincipale() == null) {
				erreurs.add(new EvenementCivilErreur("La commune de départ n'a pas été trouvée"));
			}
		}
		// else { depart.getType() == TypeEvenementCivil.DEPART_SECONDAIRE

	}

	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		final Depart depart = (Depart) evenement;

		final PersonnePhysique pp = getTiersDAO().getPPByNumeroIndividu(depart.getNoIndividu());
		if (pp == null) {
			// si on ne connaissait pas le gaillard, c'est un problème
			throw new EvenementCivilHandlerException("Aucun habitant (ou ancien habitant) trouvé avec numéro d'individu " + depart.getNoIndividu());
		}

		final MotifFor motifFermeture = findMotifFermeture(depart);
		final RegDate dateFermeture = findDateFermeture(depart, pp, motifFermeture == MotifFor.DEMENAGEMENT_VD);
		final Contribuable contribuable = findContribuable(depart, pp, motifFermeture == MotifFor.DEMENAGEMENT_VD);

		if (depart.getType() == TypeEvenementCivil.DEPART_COMMUNE) {

			//[UNIREG-1996] on traite les deux habitants ensemble conformement à l'ancien fonctionement
			if (depart.isAncienTypeDepart()) {
				traiteHabitantOfAncienDepart(depart, pp);
			}
			else {

				// [UNIREG-1691] si la personne physique était déjà notée non-habitante, on ne fait que régulariser une situation bancale
				if (pp.isHabitant()) {
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

			int numeroOfsAutoriteFiscale = 0;
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


	/**Permet de transformer un habitant et son conjoint en non habitant dans le cas d'un ancien Type de départ
	 *
	 * @param depart
	 * @param habitant
	 */
		private void traiteHabitantOfAncienDepart(final Depart depart, final PersonnePhysique habitant) {
			final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(habitant, depart.getDate());
			final PersonnePhysique conjoint;
			if (couple != null) {
				conjoint = couple.getConjoint(habitant);
				if (conjoint!=null) {
					getService().changeHabitantenNH(conjoint);
				}
			}
			getService().changeHabitantenNH(habitant);
		}

	/**
	 * Renvoi true si l'habitant est celibataire, marié seul, ou marié et son conjoint est aussi parti
	 * @param depart
	 * @return
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

		return !(conjoint != null && conjoint.isHabitant());

	}

	/**
	 * Détermine si l'habitant est seul ou en ménage et renvoi dans ce cas son ménage.
	 * @param depart
	 * @param habitant
	 * @param demenagementVD
	 * @return
	 */
	private Contribuable findContribuable(Depart depart, PersonnePhysique habitant, boolean demenagementVD) {

		final RegDate dateEvenement = demenagementVD ? FiscalDateHelper.getDateEvenementFiscal(depart.getDate()) : depart.getDate();

		final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(habitant, dateEvenement);
		if (couple != null) {
			final MenageCommun menage = couple.getMenage();
			if (menage != null) {
				return menage;
			}
		}
		return habitant;
	}

	/**
	 * Calcule la date de fermeture du for fiscal principal en fonction de la date de départ et du canton de destination
	 *
	 * @param depart
	 * @param habitant
	 * @param demenagementVD
	 * @return la date de fermeture du for fiscal
	 */
	private RegDate findDateFermeture(Depart depart, PersonnePhysique habitant, boolean demenagementVD) {

		final RegDate dateEvenement = demenagementVD ? FiscalDateHelper.getDateEvenementFiscal(depart.getDate()) : depart.getDate();
		RegDate dateFermeture = dateEvenement;

		ForFiscalPrincipal forFiscal = habitant.getForFiscalPrincipalAt(dateEvenement);
		if (forFiscal == null) {
			final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(habitant, dateEvenement);
			if (couple != null) {
				final MenageCommun menage = couple.getMenage();
				if (menage != null) {
					forFiscal = menage.getForFiscalPrincipalAt(dateEvenement);
				}
			}
		}

		ModeImposition modeImposition = null;
		if (forFiscal != null) {
			modeImposition = forFiscal.getModeImposition();
		}

		if (ModeImposition.SOURCE == modeImposition || ModeImposition.MIXTE_137_1 == modeImposition || ModeImposition.MIXTE_137_2 == modeImposition) {
			if (dateEvenement.isAfter(RegDate.get(dateEvenement.year(), dateEvenement.month(), 25))) {
				dateFermeture = dateEvenement.getLastDayOfTheMonth();
			}
			// Depart Vers le canton de Neuchatel
			final CommuneSimple commune = depart.getNouvelleCommunePrincipale();
			if (commune != null && commune.getSigleCanton().equals("NE")) {
				// la date de départ est comprise entre le 1er et le 15 du mois courant
				if (dateEvenement.isBeforeOrEqual(RegDate.get(dateEvenement.year(), dateEvenement.month(), 15))) {
					// la date de fermeture est au dernier jour du mois précédent
					dateFermeture = RegDate.get(dateEvenement.year(), dateEvenement.month(), 1).getOneDayBefore();
				}
				// la date de l'évènement se situe après le 15 du mois courant
				else if (dateEvenement.isAfter(RegDate.get(dateEvenement.year(), dateEvenement.month(), 15))) {
					// la date de fermeture est au dernier jour du mois
					dateFermeture = dateEvenement.getLastDayOfTheMonth();
				}
			}
		}
		return dateFermeture;
	}

	/**
	 * calcule le motif de fermeture du for fiscal
	 *
	 * @param depart
	 * @return le motif de fermeture
	 */
	private MotifFor findMotifFermeture(Depart depart) {
		MotifFor motifFermeture = null;

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
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
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
			erreurs.add(new EvenementCivilErreur(e));
		}

		/*
		 * Le Depart du mort-vivant
		 */
		if (depart.getIndividu().getDateDeces() != null) {
			erreurs.add(new EvenementCivilErreur("L'individu est décédé"));
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
	public GenericEvenementAdapter createAdapter() {
		return new DepartAdapter();
	}

	protected final void validateDepartAdressePrincipale(Depart depart, List<EvenementCivilErreur> erreurs) {
		final Adresse adressePrincipal = depart.getAncienneAdressePrincipale();
		final CommuneSimple commune = depart.getAncienneCommunePrincipale();

		validateCoherenceAdresse(adressePrincipal, commune, depart, erreurs);

		// la nouvelle commune est toujours dans le canton de vaud
		final CommuneSimple nouvelleCommune = depart.getNouvelleCommunePrincipale();

		if (nouvelleCommune != null && nouvelleCommune.isVaudoise()) {
			erreurs.add(new EvenementCivilErreur("La nouvelle commune est toujours dans le canton de Vaud"));
		}
	}

	protected final void validateDepartAdresseSecondaire(Depart depart, List<EvenementCivilErreur> erreurs) {
		final Adresse adresseSecondaire = depart.getAncienneAdresseSecondaire();
		final CommuneSimple communeSecondaire = depart.getAncienneCommuneSecondaire();

		validateCoherenceAdresse(adresseSecondaire, communeSecondaire, depart, erreurs);
	}

	/**
	 * Permet de valider la cohérence d'une adresse fournie par l'événement départ
	 *
	 * @param adresse
	 * @param commune
	 * @param depart
	 * @param erreurs
	 */
	public void validateCoherenceAdresse(Adresse adresse, CommuneSimple commune, Depart depart, List<EvenementCivilErreur> erreurs) {

		if (adresse == null) {
			erreurs.add(new EvenementCivilErreur("Adresse de résidence avant départ inconnue"));
		}
		else if (adresse.getDateFin() == null) {
			erreurs.add(new EvenementCivilErreur("La date de fin de validité de la résidence est inconnue"));
		}
		// la date de départ est differente de la date de fin de validité de l'adresse
		else if (!depart.getDate().equals(adresse.getDateFin())) {
			erreurs.add(new EvenementCivilErreur(
					"La date de départ est différente de la date de fin de validité de l'adresse dans le canton"));
		}

		// La commune d'annonce est differente de la commune de résidence avant l'évenement
		// de départ
		//TODO (BNM) attention si depart.getNumeroOfsCommuneAnnonce correspond à une commune avec des fractions
		if (commune != null) {
			if ((!commune.isFraction() && commune.getNoOFS() != depart.getNumeroOfsCommuneAnnonce()) ||
					(commune.isFraction() && commune.getNumTechMere() != depart.getNumeroOfsCommuneAnnonce())) {
				erreurs.add(new EvenementCivilErreur("La commune d'annonce est differente de la dernière commune de résidence"));
			}
		} else if (adresse != null) {
			// si l'adresse est nulle, il y a déjà eu une erreur, donc on ne rajoute une erreur que
			// si l'adresse n'est pas nulle, justement...
			erreurs.add(new EvenementCivilErreur("Commune de résidence avant départ inconnue"));
		}
	}

	/**
	 * Permet d'ouvrir un for principal sur une commune hors canton
	 *
	 * @param contribuable
	 * @param dateOuverture
	 * @param numeroOfsAutoriteFiscale
	 * @param modeImposition
	 * @param motifOuverture
	 * @return
	 */
	protected ForFiscalPrincipal openForFiscalPrincipalHC(Contribuable contribuable, final RegDate dateOuverture, int numeroOfsAutoriteFiscale, ModeImposition modeImposition, MotifFor motifOuverture) {
		return getService().openForFiscalPrincipal(contribuable, dateOuverture, MotifRattachement.DOMICILE, numeroOfsAutoriteFiscale,
				TypeAutoriteFiscale.COMMUNE_HC, modeImposition, motifOuverture, false);
	}

	/**
	 * Permet d'ouvrir un for principal sur un pays
	 *
	 * @param contribuable
	 * @param dateOuverture
	 * @param numeroOfsAutoriteFiscale
	 * @param motifOuverture
	 * @return
	 */
	protected ForFiscalPrincipal openForFiscalPrincipalHS(Contribuable contribuable, final RegDate dateOuverture, int numeroOfsAutoriteFiscale, ModeImposition modeImposition, MotifFor motifOuverture) {
		return getService().openForFiscalPrincipal(contribuable, dateOuverture, MotifRattachement.DOMICILE, numeroOfsAutoriteFiscale,
				TypeAutoriteFiscale.PAYS_HS, modeImposition, motifOuverture, false);
	}

	/**
	 * Traite un depart d'une residence principale
	 *
	 * @param depart
	 * @param habitant
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
	 * @param depart
	 * @param forPrincipal
	 * @param habitant
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
