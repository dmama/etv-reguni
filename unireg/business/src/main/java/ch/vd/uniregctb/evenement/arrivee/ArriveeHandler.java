package ch.vd.uniregctb.evenement.arrivee;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang.mutable.MutableBoolean;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.adresse.HistoriqueCommune;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.EtatCivilList;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeTiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementErreur;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Gère l'arrivée d'un individu dans les cas suivants:
 * <ul>
 * <li>déménagement d'une commune vaudoise à l'autre (intra-cantonal)</li>
 * <li>déménagement d'un canton à l'autre (inter-cantonal)</li>
 * <li>arrivée en Suisse</li>
 * </ul>
 */
public class ArriveeHandler extends EvenementCivilHandlerBase {

	// private static final Logger LOGGER = Logger.getLogger(ArriveeHandler.class);

	public enum ArriveeType {
		ARRIVEE_ADRESSE_PRINCIPALE, ARRIVEE_RESIDENCE_SECONDAIRE
	}

	private ServiceCivilService serviceCivilService;
	
	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	@Override
	public void checkCompleteness(EvenementCivil evenement, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {

		final Arrivee arrivee = (Arrivee) evenement;

		final ServiceInfrastructureService serviceInfra = getService().getServiceInfra();
		ArriveeType type = null;
		try {
			type = getArriveeType(serviceInfra, arrivee);
		}
		catch (EvenementCivilHandlerException e) {
			erreurs.add(new EvenementCivilErreur(e));
		}
		if (type == ArriveeType.ARRIVEE_ADRESSE_PRINCIPALE) {
			// Verification de la commune d'arrivée
			try {
				final CommuneSimple communeArrivee = getCommuneArrivee(serviceInfra, arrivee, type);
				if (communeArrivee == null) {
					erreurs.add(new EvenementCivilErreur("La nouvelle commune principale n'a pas été trouvée (adresse hors-Suisse ?)"));
				}
			}
			catch (InfrastructureException e) {
				erreurs.add(new EvenementCivilErreur("La nouvelle commune principale n'a pas été trouvée (" + e.getMessage() + ")", e));
			}

			verifierMouvementIndividu(arrivee, false, erreurs, warnings);
		}
	}

	private static CommuneSimple getCommuneArrivee(ServiceInfrastructureService serviceInfra, Arrivee arrivee, ArriveeType type) throws InfrastructureException {

		// [UNIREG-1995] si la commune d'annonce est renseignée dans l'événement, la prendre en compte
		// (ou si bien-sûr on tombe sur une commune fractionnée) sinon on prend la commune de l'adresse

		if (arrivee.getNumeroOfsCommuneAnnonce() != null && arrivee.getNumeroOfsCommuneAnnonce() > 0) {
			final Commune commune = serviceInfra.getCommuneByNumeroOfsEtendu(arrivee.getNumeroOfsCommuneAnnonce(), arrivee.getDate());
			if (commune == null || commune.isPrincipale()) {
				return getCommuneArriveeDepuisAdresse(arrivee, type);
			}
			else {
				return commune;
			}
		}
		else {
			return getCommuneArriveeDepuisAdresse(arrivee, type);
		}
	}

	private static CommuneSimple getCommuneArriveeDepuisAdresse(Arrivee arrivee, ArriveeType type) {
		switch (type) {
			case ARRIVEE_ADRESSE_PRINCIPALE:
				return arrivee.getNouvelleCommunePrincipale();

			case ARRIVEE_RESIDENCE_SECONDAIRE:
				return arrivee.getNouvelleCommuneSecondaire();

			default:
				throw new RuntimeException("Type d'arrivée inconnu : " + type);
		}
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		final Arrivee arrivee = (Arrivee) target;

		/*
		 * Validation des adresses
		 */
		try {
			final ServiceInfrastructureService serviceInfra = getService().getServiceInfra();
			final ArriveeType type = getArriveeType(serviceInfra, arrivee);
			switch (type) {
			case ARRIVEE_ADRESSE_PRINCIPALE:
				validateArriveeAdressePrincipale(arrivee, erreurs, warnings);
				validateForPrincipal(arrivee, erreurs);
				break;
			case ARRIVEE_RESIDENCE_SECONDAIRE:
				validateArriveeAdresseSecondaire(serviceInfra, arrivee, erreurs);
				break;
			default:
				Assert.fail();
			}
		}
		catch (EvenementCivilHandlerException e) {
			erreurs.add(new EvenementCivilErreur(e));
		}

		/*
		 * Le retour du mort-vivant
		 */
		if (arrivee.getIndividu().getDateDeces() != null) {
			erreurs.add(new EvenementCivilErreur("L'individu est décédé"));
		}

		/*
		 * On vérifie que si les individus sont inconnus dans la base fiscale, ils ne possèdent pas d'adresse dans le registre civil avant
		 * leur date d'arrivée
		 *
		 * [UNIREG-1457] Ce contrôle ne doit pas se faire sur les adresses secondaires
		 */
		if (isDansLeCanton(arrivee.getAncienneCommunePrincipale())) {
			final PersonnePhysique habitant = getTiersDAO().getPPByNumeroIndividu(arrivee.getNoIndividu());
			if (habitant == null) {
				final String message = "L'individu est inconnu dans registre fiscal mais possédait déjà une adresse dans le " +
						"registre civil avant son arrivée (incohérence entre les deux registres)";
				erreurs.add(new EvenementCivilErreur(message));
			}
		}
	}

	private void validateForPrincipal(Arrivee arrivee, List<EvenementCivilErreur> erreurs) {

		final MotifFor motifFor = getMotifOuverture(arrivee);
		if ( motifFor == MotifFor.ARRIVEE_HC || motifFor == MotifFor.ARRIVEE_HS ) {
			// Si le motif d'ouverture du for est arrivee HS ou HC alors , l'eventuel for principal actuel ne doit pas être vaudois
			final PersonnePhysique pp = getTiersDAO().getPPByNumeroIndividu(arrivee.getNoIndividu());
			if (pp != null) {
				final RapportEntreTiers rapportMenage = pp.getRapportSujetValidAt(arrivee.getDate(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);

				// seulement pour les PP qui ne sont pas en couple, car dans le cas des couples,
				// un membre peut déjà être arrivé lorsque le second arrive
				if (rapportMenage == null) {
					final ForFiscalPrincipal forFP = pp.getForFiscalPrincipalAt(arrivee.getDate());
					if (forFP != null && forFP.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						final String msg;
						msg = String.format("A la date de l'événement, la personne physique (ctb: %s) associée à l'individu a déjà un for principal vaudois", pp.getNumero());
						erreurs.add(new EvenementCivilErreur(msg));
					}
				}
			}
		}
	}

	protected final void validateArriveeAdressePrincipale(Arrivee arrivee, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		/*
		 * La date de début de la nouvelle adresse principale de l’individu est antérieure ou identique à la date de l'ancienne.
		 */
		final Adresse ancienneAdresse = arrivee.getAncienneAdressePrincipale();
		if (ancienneAdresse != null && ancienneAdresse.getDateDebut() != null && arrivee.getDate().isBeforeOrEqual(ancienneAdresse.getDateDebut())) {
			erreurs.add(new EvenementCivilErreur("La date d'arrivée principale est antérieure à la date de début de l'ancienne adresse"));
		}
		if(ancienneAdresse != null && (ancienneAdresse.getDateFin() == null || arrivee.getDate().isBeforeOrEqual(ancienneAdresse.getDateFin())) ){
			erreurs.add(new EvenementCivilErreur("La date d'arrivée principale est antérieure à la date de fin de l'ancienne adresse"));
		}

		try {
			/*
			 * La nouvelle adresse principale n’est pas dans le canton (il n’est pas obligatoire que l’adresse courrier soit dans le canton).
			 */
			final CommuneSimple nouvelleCommune = getCommuneArrivee(getService().getServiceInfra(), arrivee, ArriveeType.ARRIVEE_ADRESSE_PRINCIPALE);
			Assert.notNull(nouvelleCommune);
			if (!nouvelleCommune.isVaudoise()) {
				erreurs.add(new EvenementCivilErreur("La nouvelle commune principale est en dehors du canton"));
			}

			/*
			 * La commune d'annonce est différente de la commune d'arrivée cas possible avec les fractions if
			 * (arrivee.getNumeroOfsCommuneAnnonce().intValue() != nouvelleCommune.getNoTechnique()) { erreurs.add(new EvenementCivilErreur("La
			 * nouvelle commune principale ("+nouvelleCommune.getNomMinuscule()+ ","+nouvelleCommune.getNoTechnique()+") ne correspond pas à la
			 * commune d'annonce ("+arrivee.getNumeroOfsCommuneAnnonce()+")")); }
			 */

			/*
			 * Pour les communes du Sentier, il n'est pas possible de déterminer automatiquement le for principal. Un traitement manuel est
			 * nécessaire.
			 */
			if (nouvelleCommune.getNoOFSEtendu() == NO_OFS_FRACTION_SENTIER) {
				warnings.add(new EvenementCivilErreur("arrivée dans la fraction de commune du Sentier: "
						+ "veuillez vérifier la fraction de commune du for principal", TypeEvenementErreur.WARNING));
			}
		}
		catch (InfrastructureException e) {
			erreurs.add(new EvenementCivilErreur("La nouvelle commune principale est introuvable (" + e.getMessage() + ")", e));
		}
	}

	protected static void validateArriveeAdresseSecondaire(ServiceInfrastructureService infraService, Arrivee arrivee, List<EvenementCivilErreur> erreurs) {
		/*
		 * La date de début de la nouvelle adresse secondaire de l’individu est antérieure ou identique à la date de l'ancienne.
		 */
		final Adresse ancienneAdresse = arrivee.getAncienneAdresseSecondaire();
		if (ancienneAdresse != null && ancienneAdresse.getDateFin() != null && arrivee.getDate().isBeforeOrEqual(ancienneAdresse.getDateFin())) {
			erreurs.add(new EvenementCivilErreur("La date d'arrivée secondaire est antérieure à la date de fin de l'ancienne adresse"));
		}

		try {
			/*
			 * La nouvelle adresse secondaire n’est pas dans le canton (il n’est pas obligatoire que l’adresse courrier soit dans le canton).
			 */
			final CommuneSimple nouvelleCommune = getCommuneArrivee(infraService, arrivee, ArriveeType.ARRIVEE_RESIDENCE_SECONDAIRE);
			if (nouvelleCommune == null || !nouvelleCommune.isVaudoise()) {
				erreurs.add(new EvenementCivilErreur("La nouvelle commune secondaire est en dehors du canton"));
			}

			/*
			 * La commune d'annonce est différente de la commune d'arrivée cas possible avec les fractions if
			 * (arrivee.getNumeroOfsCommuneAnnonce().intValue() != nouvelleCommune.getNoTechnique()) { erreurs.add(new EvenementCivilErreur("La
			 * nouvelle commune secondaire ne correspond pas à la commune d'annonce")); }
			 */
		}
		catch (InfrastructureException e) {
			erreurs.add(new EvenementCivilErreur("La nouvelle commune secondaire est introuvable (" + e.getMessage() + ")", e));
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		final Arrivee arrivee = (Arrivee) evenement;

		if (isIndividuEnMenage(arrivee.getIndividu(), evenement.getDate())) {
			return handleIndividuEnMenage(arrivee, warnings);
		}
		else {
			return handleIndividuSeul(arrivee, warnings);
		}
	}

	private boolean isIndividuEnMenage(Individu individu, RegDate date ) {
		// Récupération de l'état civil de l'individu
		final EtatCivil etatCivil = individu.getEtatCivil(date);
		if (etatCivil == null) {
			throw new EvenementCivilHandlerException("Impossible de déterminer l'état civil de l'individu " + individu.getNoTechnique() +
					" à la date " + RegDateHelper.dateToDisplayString(date));
		}
		return EtatCivilHelper.estMarieOuPacse(etatCivil);
	}

	/**
	 * Gère l'arrivée d'un contribuable seul.
	 */
	protected final Pair<PersonnePhysique, PersonnePhysique> handleIndividuSeul(Arrivee arrivee, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {

		try {
			final Individu individu = arrivee.getIndividu();
			final RegDate dateArrivee = arrivee.getDate();
			final Long numeroEvenement = arrivee.getNumeroEvenement();

			/*
			 * Création à la demande de l'habitant
			 */
			// [UNIREG-770] rechercher si un Non-Habitant assujetti existe (avec Nom - Prénom)
			final MutableBoolean nouvelHabitant = new MutableBoolean(false);
			final PersonnePhysique habitant = getOrCreateHabitant(individu, dateArrivee, numeroEvenement, FindBehavior.ASSUJETTISSEMENT_OBLIGATOIRE_ERROR_IF_SEVERAL, nouvelHabitant);

			/*
			 * Mise-à-jour des adresses
			 *
			 * Les éventuelles adresses fiscales doivent rester valides en l'absence de demande explicite de
			 * changement de la part du contribuable, sauf pour le cas des adresses flagées "non-permanentes"
			 * qui doivent être férmées.Si aucune adresse fiscale n'existe, on prend les adresses civiles
			 * qui sont forcément valides.
			 */
			fermeAdresseTiersTemporaire(habitant, dateArrivee.getOneDayBefore());

			/*
			 * Mise-à-jour des fors fiscaux principaux du contribuable
			 */
			final ArriveeType type = getArriveeType(getService().getServiceInfra(), arrivee);

			final RegDate dateEvenement = getDateEvenementPourFor(arrivee);

			final boolean individuMajeur = FiscalDateHelper.isMajeurAt(individu, dateEvenement);
			final CommuneSimple communeArrivee = getCommuneArrivee(getService().getServiceInfra(), arrivee, type);

			/*
			 * Le for fiscal principal reste inchangé en cas d'arrivée en résidence secondaire, ou en cas d'arrivée en résidence principale
			 * d'un individu mineur.
			 */
			if (type == ArriveeType.ARRIVEE_ADRESSE_PRINCIPALE && individuMajeur) {

				MotifFor motifOuverture = getMotifOuverture(arrivee);
				final int numeroOfsNouveau = communeArrivee.getNoOFSEtendu();
				final ForFiscalPrincipal forFiscal = habitant.getForFiscalPrincipalAt(null);

				// détermination du mode d'imposition
				final ModeImposition modeImposition;
				if (forFiscal == null) {
					if (getService().isSuisseOuPermisCOuRefugie(habitant, dateEvenement)) {
						// s'il est suisse, titulaire d'un permis C ou a obtenu le statut de réfugié => ordinaire
						modeImposition = ModeImposition.ORDINAIRE;
					}
					else if (motifOuverture == MotifFor.ARRIVEE_HC || motifOuverture == MotifFor.ARRIVEE_HS || motifOuverture == null) {
						modeImposition = ModeImposition.SOURCE;
					}
					else {
						// une arrivée dans le canton, sans for pré-existant en n'arrivant pas de hors-Suisse ni de hors-Canton, cela ne devrait pas être possible, il me semble...
						modeImposition = null;
					}
				}
				else {
					if (motifOuverture == MotifFor.ARRIVEE_HC || motifOuverture == MotifFor.ARRIVEE_HS || motifOuverture == null) {
						if (getService().isSuisseOuPermisCOuRefugie(habitant, dateEvenement)) {
							modeImposition = ModeImposition.ORDINAIRE;
						}
						else {
							// Si l'individu est déjà présent en for secondaire, il passe mixte_1, sinon, il passe source
							final List<ForFiscal> fors = habitant.getForsFiscauxValidAt(dateEvenement);
							boolean hasForSecondaire = false;
							for (ForFiscal ff : fors) {
								// si on trouve au moins un for secondaire, alors mixte_1
								if (ff instanceof ForFiscalSecondaire) {
									hasForSecondaire = true;
									break;
								}
							}
							modeImposition = hasForSecondaire ? ModeImposition.MIXTE_137_1 : ModeImposition.SOURCE;
						}
					}
					else {
						modeImposition = forFiscal.getModeImposition();
					}
				}

				// détermination de la date d'ouverture
				final RegDate dateOuverture;
				if (isSourcier(modeImposition)) {
					dateOuverture = findDateOuvertureSourcier(dateEvenement, arrivee.getType(), arrivee.getAncienneCommunePrincipale());
				}
				else {
					dateOuverture = dateEvenement;
				}

				if (modeImposition != null) {
					if (motifOuverture == null) {
						motifOuverture = MotifFor.ARRIVEE_HS;
						warnings.add(new EvenementCivilErreur("ancienne adresse avant l'arrivée inconnue : veuillez indiquer le motif d'ouverture du for principal", TypeEvenementErreur.WARNING));
					}
					if (forFiscal == null) {
						Audit.info(numeroEvenement, "Création d'un for fiscal ordinaire avec mode d'imposition [" + modeImposition + "]");
						openForFiscalPrincipal(habitant, dateOuverture, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, numeroOfsNouveau, MotifRattachement.DOMICILE, motifOuverture, modeImposition,
								false);
					}
					else {
						Audit.info(numeroEvenement, "Mise-à-jour du fors fiscal avec mode d'imposition [" + modeImposition + "]");
						updateForFiscalPrincipal(habitant, dateOuverture, numeroOfsNouveau, motifOuverture, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, modeImposition, false);
					}
				}
			}

			return nouvelHabitant.booleanValue() ? new Pair<PersonnePhysique, PersonnePhysique>(habitant, null) : null;
		}
		catch (TiersException e) {
			throw new EvenementCivilHandlerException(e.getMessage(), e);
		}
		catch (InfrastructureException e) {
			throw new EvenementCivilHandlerException(e.getMessage(), e);
		}
	}

	protected List<PersonnePhysique> findNonHabitants(Individu individu, boolean assujettissementObligatoire) {

		// les critères de recherche
		final String nomPrenom = getService().getNomPrenom(individu);
		final RegDate dateNaissance = individu.getDateNaissance();
		final Sexe sexe = (individu.isSexeMasculin() ? Sexe.MASCULIN : Sexe.FEMININ);

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setTypeTiers(TypeTiers.NON_HABITANT);
		criteria.setTypeRechercheDuNom(TypeRecherche.EST_EXACTEMENT);
		// criteria.setDateNaissance(individu.getDateNaissance()); [UNIREG-1603] on ne filtre pas sur la date de naissance ici, pour prendre en compte les dates nulles
		criteria.setNomRaison(nomPrenom);

		final List<PersonnePhysique> nonHabitants = new ArrayList<PersonnePhysique>();

		final List<TiersIndexedData> results = getService().search(criteria);
		for (final TiersIndexedData tiersIndexedData : results) {
			final PersonnePhysique pp = (PersonnePhysique) getService().getTiers(tiersIndexedData.getNumero());
			// [UNIREG-770] le non habitant doit être assujetti
			if (assujettissementObligatoire && pp.getForFiscalPrincipalAt(null) == null) {
				continue;
			}
			// [UNIREG-1603] on filtre les dates naissance en laissant passer les dates nulles
			if (pp.getDateNaissance() != null && pp.getDateNaissance() != dateNaissance) {
				continue;
			}
			// [UNIREG-1603] on filtre le sexe en laissant passer les sexes non renseignés
			if (pp.getSexe() != null && pp.getSexe() != sexe) {
				continue;
			}
			// [UNIREG-1603] on filtre les non habitants qui possèdent un numéro d'individu
			if (pp.getNumeroIndividu() != null) {
				continue;
			}
			nonHabitants.add(pp);
		}

		if (nonHabitants.size() == 1) {
			// [UNIREG-1603] en cas de résultat unique, le candidat doit correspondre parfaitement aux critères
			final PersonnePhysique candidat = nonHabitants.get(0);
			if (candidat.getDateNaissance() == null || candidat.getSexe() == null) {
				nonHabitants.clear(); // le candidat n'est pas complet, on l'enlève
			}
		}

		return nonHabitants;
	}

	private enum FindBehavior {
		ASSUJETTISSEMENT_OBLIGATOIRE_ERROR_IF_SEVERAL(true, true),
		ASSUJETTISSEMENT_OBLIGATOIRE_NO_ERROR_IF_SEVERAL(true, false),
		ASSUJETTISSEMENT_NON_OBLIGATOIRE_ERROR_IF_SEVERAL(false, true),
		ASSUJETTISSEMENT_NON_OBLIGATOIRE_NO_ERROR_IF_SEVERAL(false, false)
		;
		
		private final boolean assujettissementObligatoire;
		
		private final boolean errorOnMultiples;
		
		private FindBehavior(boolean assujettissementObligatoire, boolean errorOnMultiples) {
			this.assujettissementObligatoire = assujettissementObligatoire;
			this.errorOnMultiples = errorOnMultiples;
		}
		
		public boolean isAssujettissementObligatoire() {
			return assujettissementObligatoire;
		}
		
		public boolean isErrorOnMultiples() {
			return errorOnMultiples;
		}
	}

	/**
	 * @param nouveau rempli en sortie, à <code>true</code> si un nouvel habitant a été créé, et <code>false</code> sinon
	 */
	private PersonnePhysique getOrCreateHabitant(Individu individu, RegDate dateEvenement, long evenementId, FindBehavior behavior, MutableBoolean nouveau) {

		final PersonnePhysique pp = getTiersDAO().getPPByNumeroIndividu(individu.getNoTechnique());
		final PersonnePhysique habitant;
		if (pp != null) {
			if (pp.isHabitantVD()) {
				nouveau.setValue(false);
				habitant = pp;
			}
			else {
				habitant = getService().changeNHenHabitant(pp, pp.getNumeroIndividu(), dateEvenement);
				Audit.info(evenementId, "Le non habitant " + habitant.getNumero() + " devient habitant");
				nouveau.setValue(true);
			}
		}
		else {
			final List<PersonnePhysique> nonHabitants = findNonHabitants(individu, behavior.isAssujettissementObligatoire());
			if (nonHabitants.size() == 1) {
				final PersonnePhysique nhab = nonHabitants.get(0);
				habitant = getService().changeNHenHabitant(nhab, individu.getNoTechnique(), dateEvenement);
				Audit.info(evenementId, "Le non habitant " + habitant.getNumero() + " devient habitant");
				nouveau.setValue(true);
			}
			else if (nonHabitants.size() == 0 || !behavior.isErrorOnMultiples()) {
				final PersonnePhysique nouvelHabitant = new PersonnePhysique(true);
				nouvelHabitant.setNumeroIndividu(individu.getNoTechnique());
				Audit.info(evenementId, "Un tiers a été créé pour le nouvel arrivant");
				habitant = (PersonnePhysique) getTiersDAO().save(nouvelHabitant);
				nouveau.setValue(true);
			}
			else {
				// [UNIREG-2650] Message d'erreur un peu plus explicite...
				final StringBuilder b = new StringBuilder();
				boolean first = true;
				for (PersonnePhysique candidat : nonHabitants) {
					if (!first) {
						b.append(", ");
					}
					b.append(FormatNumeroHelper.numeroCTBToDisplay(candidat.getNumero()));
					first = false;
				}

				final String message = String.format("Plusieurs tiers non-habitants assujettis potentiels trouvés (%s)", b.toString());
				throw new EvenementCivilHandlerException(message);
			}
		}

		return habitant;
	}

	private static boolean isSourcier(ModeImposition modeImposition) {
		return ModeImposition.SOURCE == modeImposition || ModeImposition.MIXTE_137_1 == modeImposition || ModeImposition.MIXTE_137_2 == modeImposition;
	}

	private static RegDate findDateDebutMenageAvant(Individu individu, RegDate limiteSuperieureEtDefaut) {
		final EtatCivilList etatsCivils = individu.getEtatsCivils();
		final RegDate dateDebutMenage;
		if (etatsCivils != null && etatsCivils.size() > 0) {
			final ListIterator<EtatCivil> iterator = etatsCivils.listIterator(etatsCivils.size());
			RegDate candidate = limiteSuperieureEtDefaut;
			while (iterator.hasPrevious()) {
				final EtatCivil etatCivil = iterator.previous();
				if (EnumTypeEtatCivil.MARIE.equals(etatCivil.getTypeEtatCivil()) || EnumTypeEtatCivil.PACS.equals(etatCivil.getTypeEtatCivil())) {
					if (etatCivil.getDateDebutValidite() == null) {
						// si si, ça arrive...
						break;
					}
					else if (etatCivil.getDateDebutValidite().isBeforeOrEqual(limiteSuperieureEtDefaut)) {
						candidate = etatCivil.getDateDebutValidite();
						break;
					}
				}
			}
			dateDebutMenage = candidate;
		}
		else {
			dateDebutMenage = limiteSuperieureEtDefaut;
		}
		return dateDebutMenage;
	}

	/**
	 * Gère l'arrive d'un contribuable en ménage commun.
	 */
	protected final Pair<PersonnePhysique, PersonnePhysique> handleIndividuEnMenage(Arrivee arrivee, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {

		final Individu individu = arrivee.getIndividu();
		Assert.notNull(individu); // prérequis
		final Individu conjoint = getServiceCivil().getConjoint(arrivee.getNoIndividu(), arrivee.getDate());

		final RegDate dateEvenement = getDateEvenementPourFor(arrivee);
		final Long numeroEvenement = arrivee.getNumeroEvenement();

		/*
		 * Récupération/création des habitants
		 */
		final MutableBoolean nouvelArrivant = new MutableBoolean(false);
		final PersonnePhysique arrivant = getOrCreateHabitant(individu, dateEvenement, numeroEvenement, FindBehavior.ASSUJETTISSEMENT_NON_OBLIGATOIRE_NO_ERROR_IF_SEVERAL, nouvelArrivant);
		final MutableBoolean nouveauConjoint = new MutableBoolean(false);
		final PersonnePhysique conjointArrivant;
		if (conjoint != null) {
			conjointArrivant = getOrCreateHabitant(conjoint, dateEvenement, numeroEvenement, FindBehavior.ASSUJETTISSEMENT_NON_OBLIGATOIRE_NO_ERROR_IF_SEVERAL, nouveauConjoint);
		}
		else {
			conjointArrivant = null;
		}

		/*
		 * Récupération/création du ménage commun et du rapports entre tiers
		 */

		// trouve la date du mariage (qui peut avoir eu lieu bien avant l'arrivée !)
		final RegDate dateDebutMenage = findDateDebutMenageAvant(individu, arrivee.getDate());
		final MenageCommun menageCommun = getOrCreateMenageCommun(arrivant, conjointArrivant, dateEvenement, dateDebutMenage, numeroEvenement);
		Assert.notNull(menageCommun);

		/*
		 * Mise-à-jour des adresses
		 *
		 * Les éventuelles adresses fiscales doivent rester valides en l'absence de demande explicite de
		 * changement de la part du contribuable, sauf pour le cas des adresses flagées "non-permanentes"
		 * qui doivent être férmées.Si aucune adresse fiscale n'existe, on prend les adresses civiles
		 * qui sont forcément valides.
		 */
		fermeAdresseTiersTemporaire(menageCommun, arrivee.getDate().getOneDayBefore());

		/*
		 * Mise-à-jour des fors fiscaux principaux du contribuable
		 */
		final ArriveeType type = getArriveeType(getService().getServiceInfra(), arrivee);
		if (type == ArriveeType.ARRIVEE_ADRESSE_PRINCIPALE) {
			/*
			 * Le for fiscal principal reste inchangé en cas d'arrivée en résidence secondaire.
			 */
			createOrUpdateForFiscalPrincipalOnCouple(arrivee, arrivant, menageCommun, warnings);
		}

		if (nouvelArrivant.booleanValue() || nouveauConjoint.booleanValue()) {
			final PersonnePhysique arrivantCree = nouvelArrivant.booleanValue() ? arrivant : null;
			final PersonnePhysique conjointCree = nouveauConjoint.booleanValue() ? conjointArrivant : null;
			return new Pair<PersonnePhysique, PersonnePhysique>(arrivantCree, conjointCree);
		}
		else {
			return null;
		}
	}

	private RegDate getDateEvenementPourFor(Arrivee arrivee) {
		final RegDate dateEvenement;
		if (TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE == arrivee.getType()) {
			// vérification du dépassement du 20/12
			dateEvenement = FiscalDateHelper.getDateEvenementFiscal(arrivee.getDate());
		}
		else {
			dateEvenement = arrivee.getDate();
		}
		return dateEvenement;
	}

	/**
	 * Calcule la date d'ouverture du for fiscal principal d'un sourcier en
	 * fonction de sa date d'arrivée et du canton d'origine.
	 *
	 * @param date
	 *            la date d'arrivée. Cette datte ne correspond pas forcement à
	 *            celle de l'événement car d'autres règles peuvent être
	 *            appliquées avant l'appel à cette méthode.
	 * @param type
	 *            Le type d'événement d'arrivée.
	 * @param commune
	 *            La commune d'origine.
	 * @return
	 */
	private RegDate findDateOuvertureSourcier(final RegDate date, final TypeEvenementCivil type, final CommuneSimple commune) {

		RegDate dateOuverture = date;

		/*
		 * - En cas d’arrivée d’un autre canton ou à l’étranger, si la date de l’événement survient
		 * 	 après le 25 du mois et que le mode d’imposition est l’une des formes d’impôt à la source,
		 *   le for principal est ouvert au premier jour du mois suivant.
		 */
		if (TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC == type || TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS == type) {
			if (date.isAfter(RegDate.get(date.year(), date.month(), 25))) {
				dateOuverture = RegDateHelper.getFirstDayOfNextMonth(date);
			}
		}

		/*
		 * - En cas d’arrivée du canton de Neuchâtel avec l’une des formes d’impôt à la source,
		 *   le for principal est ouvert au premier jour du mois de l’événement si la date de
		 *   l’événement est située entre le 1er et le 15 du mois, ces deux dates comprises,
		 *   et au premier jour du mois suivant si la date de l’événement est postérieure au 15 du mois.
		 */
		if (commune != null && commune.getSigleCanton().equalsIgnoreCase("NE")) {
			if (date.isBeforeOrEqual(RegDate.get(date.year(), date.month(), 15))) {
				dateOuverture = RegDate.get(date.year(), date.month(), 1);
			}
			else {
				dateOuverture = RegDateHelper.getFirstDayOfNextMonth(date);
			}
		}
		return dateOuverture;
	}

	/**
	 * Retourne la commune de domicile de la personne physique concernée
	 * @param date date de référence
	 * @param pp personne physique concernée
	 * @return commune de l'adresse de domicile, à la date donnée, de la personne physique donnée
	 * @throws DonneesCivilesException
	 * @throws InfrastructureException
	 */
	private CommuneSimple getCommuneDomicile(RegDate date, PersonnePhysique pp) throws DonneesCivilesException, InfrastructureException {
		final CommuneSimple commune;
		if (pp != null && pp.getNumeroIndividu() != null && pp.getNumeroIndividu() > 0) {
			final AdressesCiviles adresseDomicile = new AdressesCiviles(serviceCivilService.getAdresses(pp.getNumeroIndividu(), date, false));
			if (adresseDomicile != null) {
				commune = getService().getServiceInfra().getCommuneByAdresse(adresseDomicile.principale);
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

	/**
	 * On regarde les adresses de domicile des membres du couple (à la date d'arrivée) :
	 * <ul>
	 * <li>s'il n'y en a qu'une vaudoise -> on prend cette commune</li>
	 * <li>s'il y en a deux vaudoises, et qu'elles sont dans la même commune, alors on prend cette commune</li>
	 * <li>s'il y en a deux vaudoises, et qu'elles sont dans deux communes différentes, on ne touche à rien s'il y a déjà un for vaudois ouvert sur le couple, et on prend la commune du principal du couple sinon</li>
	 * <li>si on a pu déterminer une commune avec les conditions ci-dessus, on ouvre un for dessus à la date d'arrivée</li>
	 * </ul>
	 * @param arrivee événement d'arrivée
	 * @param arrivant personne physique concernée par l'arrivée
	 * @param ensemble ensemble du ménage et des personnes physiques le composant
	 * @return une commune selon les règles édictées plus haut
	 */
	private Pair<CommuneSimple, RegDate> getCommuneForSuiteArriveeCouple(Arrivee arrivee, PersonnePhysique arrivant, EnsembleTiersCouple ensemble) {

		final RegDate dateArrivee = arrivee.getDate();
		try {
			final PersonnePhysique principal = ensemble.getPrincipal();
			final PersonnePhysique conjoint = ensemble.getConjoint();
			final MenageCommun menage = ensemble.getMenage();

			RegDate dateDebutFor = dateArrivee;

			final ForFiscalPrincipal ffpArrivee = menage.getForFiscalPrincipalAt(dateArrivee);
			if (ffpArrivee != null && ffpArrivee.getDateFin() != null) {
				// un for existe, mais le for est déjà fermé... il ya eu d'autres modifications déjà après la date d'arrivée
				// -> a régler manuellement
				throw new EvenementCivilHandlerException("Il y a eu d'autres changements déjà pris en compte après l'arrivée"); 
			}
			else if (ffpArrivee != null) {

				// quelles sont les communes de domicile de l'autre membre du menage (par rapport à celui qui arrive) ?
				final PersonnePhysique autre = ensemble.getConjoint(arrivant);
				if (autre != null && autre.getNumeroIndividu() != null && autre.getNumeroIndividu() > 0) {
					final List<HistoriqueCommune> communes = serviceCivilService.getCommunesDomicileHisto(dateArrivee, autre.getNumeroIndividu(), false, true);

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
								final CommuneSimple ancienneCommune = communes.get(i - 1).getCommune();
								final String nomAncienneCommune = ancienneCommune != null ? String.format("%s (%d)", ancienneCommune.getNomMinuscule(), ancienneCommune.getNoOFSEtendu()) : "HC/HS";
								final CommuneSimple nouvelleCommune = communes.get(i).getCommune();
								final String nomNouvelleCommune = nouvelleCommune != null ? String.format("%s (%d)", nouvelleCommune.getNomMinuscule(), nouvelleCommune.getNoOFSEtendu()) : "HC/HS";
								if (i > 1) {
									b.append(", ");
								}
								b.append(nomAncienneCommune).append(" -> ").append(nomNouvelleCommune).append(" au ").append(communes.get(i).getDateDebut());
							}

							throw new EvenementCivilHandlerException(String.format("Le contribuable %d a déjà déménagé plus d'une fois après l'arrivée de son conjoint (%s)", autre.getNumero(), b.toString()));
						}
						else {
							// un seul déménagement...
							final CommuneSimple ancienneCommune = communes.get(0).getCommune();
							final CommuneSimple nouvelleCommune = communes.get(1).getCommune();
							if (ancienneCommune == null) {
								// arrivée HC/HS --> réglé plus bas
							}
							else if (nouvelleCommune == null) {
								// départ HC/HS --> réglé plus bas
							}
							else {
								// déménagement vaudois -> c'est seulement à partir du moment
								// où les deux conjoints on déménagés que le for doit bouger
								dateDebutFor = communes.get(1).getDateDebut();
							}
						}
					}
				}
			}

			final CommuneSimple residencePrincipal = getCommuneDomicile(dateDebutFor, principal);
			final CommuneSimple residenceConjoint = getCommuneDomicile(dateDebutFor, conjoint);
			final boolean principalVaudois = residencePrincipal != null && residencePrincipal.isVaudoise();
			final boolean conjointVaudois = residenceConjoint != null && residenceConjoint.isVaudoise();

			final CommuneSimple commune;

			// aucun vaudois -> erreur !!
			if (!principalVaudois && !conjointVaudois) {
				throw new EvenementCivilHandlerException(String.format("Aucun membre du ménage %d n'a une adresse de domicile vaudoise", menage.getNumero()));
			}
			else if (!conjointVaudois) {
				commune = residencePrincipal;
			}
			else if (!principalVaudois) {
				commune = residenceConjoint;
			}
			else if (residencePrincipal.getNoOFSEtendu() == residenceConjoint.getNoOFSEtendu()) {
				commune = residencePrincipal;      // même commune pour les deux conjoints -> ils sont tous les deux arrivés
			}
			else {
				// deux adresses vaudoises, mais sur des communes différentes
				// y a-t-il déjà un for vaudois ouvert sur le couple (cette méthode est appelée avant la fermeture des fors...)
				if (ffpArrivee != null && ffpArrivee.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					// si le for principal vaudois sur le couple correspond à l'une des communes du principal ou du conjoint, on ne touche à rien
					if (ffpArrivee.getNumeroOfsAutoriteFiscale() == residencePrincipal.getNoOFSEtendu() || ffpArrivee.getNumeroOfsAutoriteFiscale() == residenceConjoint.getNoOFSEtendu()) {
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

			return commune == null ? null : new Pair<CommuneSimple, RegDate>(commune, dateDebutFor);
		}
		catch (InfrastructureException e) {
			throw new EvenementCivilHandlerException(e.getMessage(), e);
		}
		catch (DonneesCivilesException e) {
			throw new EvenementCivilHandlerException(e.getMessage(), e);
		}
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
	 * @param arrivee l'événement d'arrivée
	 * @param arrivant personne physique concernée par l'arrivée
	 * @param menageCommun le ménage commun
	 * @param warnings liste des erreurs à peupler en cas de problème
	 */
	private void createOrUpdateForFiscalPrincipalOnCouple(final Arrivee arrivee, final PersonnePhysique arrivant,final MenageCommun menageCommun, List<EvenementCivilErreur> warnings) {

		Assert.notNull(arrivee);
		Assert.notNull(menageCommun);

		final EnsembleTiersCouple ensemble = getService().getEnsembleTiersCouple(menageCommun, arrivee.getDate());
		final Pair<CommuneSimple, RegDate> infosFor = getCommuneForSuiteArriveeCouple(arrivee, arrivant, ensemble);
		if (infosFor == null) {
			// pas de for à créer...
			return;
		}

		final RegDate dateEvenement = infosFor.getSecond();
		final CommuneSimple commune = infosFor.getFirst();

		try {

			final PersonnePhysique principal = ensemble.getPrincipal();
			final PersonnePhysique conjoint = ensemble.getConjoint();
			final ForFiscalPrincipal ffpHabitantPrincipal = principal.getForFiscalPrincipalAt(null);
			final ForFiscalPrincipal ffpHabitantConjoint = (conjoint == null ? null : conjoint.getForFiscalPrincipalAt(null));
			final ForFiscalPrincipal ffpMenage = menageCommun.getForFiscalPrincipalAt(null);
			final int numeroOfsNouveau = commune.getNoOFSEtendu();

			// pour un couple, le for principal est toujours sur le ménage commun
			if (ffpHabitantPrincipal != null) {
				throw new EvenementCivilHandlerException(String.format("Le contribuable principal [%s] du ménage [%s] possède un for fiscal principal individuel",
						FormatNumeroHelper.numeroCTBToDisplay(principal.getNumero()),
						FormatNumeroHelper.numeroCTBToDisplay(menageCommun.getNumero())));
			}
			if (ffpHabitantConjoint != null) {
				throw new EvenementCivilHandlerException(String.format("Le conjoint [%s] du ménage [%s] possède un for fiscal principal individuel",
						FormatNumeroHelper.numeroCTBToDisplay(conjoint.getNumero()),
						FormatNumeroHelper.numeroCTBToDisplay(menageCommun.getNumero())));
			}

			MotifFor motifOuverture = getMotifOuverture(arrivee);

			// détermination du mode d'imposition
			final ModeImposition modeImposition;
			if (ffpMenage == null) {
				if (getService().isSuisseOuPermisCOuRefugie(principal, dateEvenement) || (conjoint != null && (getService().isSuisseOuPermisCOuRefugie(conjoint, dateEvenement)))) {
					modeImposition = ModeImposition.ORDINAIRE;
				}
				else if (motifOuverture == MotifFor.ARRIVEE_HC || motifOuverture == MotifFor.ARRIVEE_HS) {
					modeImposition = ModeImposition.SOURCE;
				}
				else {
					// une arrivée dans le canton, sans for pré-existant en n'arrivant pas de hors-Suisse ni de hors-Canton, cela ne devrait pas être possible, il me semble...
					modeImposition = null;
				}
			}
			else {
				if (motifOuverture == MotifFor.ARRIVEE_HC || motifOuverture == MotifFor.ARRIVEE_HS || motifOuverture == null) {
					if (getService().isSuisseOuPermisCOuRefugie(principal, dateEvenement) || (conjoint != null && (getService().isSuisseOuPermisCOuRefugie(conjoint, dateEvenement)))) {
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
						modeImposition = hasForSecondaire ? ModeImposition.MIXTE_137_1 : ModeImposition.SOURCE;
					}
				}
				else {
					modeImposition = ffpMenage.getModeImposition();
				}
			}

			// détermination de la date d'ouverture
			final RegDate dateOuvertureFor;
			if (isSourcier(modeImposition)) {
				dateOuvertureFor = findDateOuvertureSourcier(dateEvenement, arrivee.getType(), commune);
			}
			else {
				dateOuvertureFor = dateEvenement;
			}

			if (modeImposition != null) {
				if (motifOuverture == null) {
					motifOuverture = MotifFor.ARRIVEE_HS;
					warnings.add(new EvenementCivilErreur("Ancienne adresse avant l'arrivée inconnue : veuillez indiquer le motif d'ouverture du for principal", TypeEvenementErreur.WARNING));
				}
				if (ffpMenage == null) {
					Audit.info(arrivee.getNumeroEvenement(), "Création d'un for fiscal principal sur le ménage commun avec mode d'imposition [" + modeImposition + "]");
					openForFiscalPrincipal(menageCommun, dateOuvertureFor, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, numeroOfsNouveau, MotifRattachement.DOMICILE, motifOuverture, modeImposition,
							false);
				}
				else {
					Audit.info(arrivee.getNumeroEvenement(), "Mise-à-jour for fiscal principal sur le ménage commun avec mode d'imposition [" + modeImposition + "]");
					updateForFiscalPrincipal(menageCommun, dateOuvertureFor, numeroOfsNouveau, motifOuverture, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, modeImposition, false);
				}
			}
		}
		catch (TiersException e) {
			throw new EvenementCivilHandlerException(e.getMessage(), e);
		}
	}

	/**
	 * Récupère le ménage commun avec le contribuable principal et le conjoint comme parties.
	 *
	 * @param habitantPrincipal
	 * @param habitantConjoint
	 * @param dateEvenement date de l'événement d'arrivée
	 * @param dateDebutMenage date de début des nouveaux rapports d'appartenance ménage en cas de création de ménage
	 * @param evenementId ID technique de l'événement d'arrivée
	 * @return
	 * @throws EvenementCivilHandlerException
	 *             si les deux habitants appartiennent à des ménages différents
	 */
	private MenageCommun getOrCreateMenageCommun(final PersonnePhysique habitantPrincipal, PersonnePhysique habitantConjoint, RegDate dateEvenement, RegDate dateDebutMenage, long evenementId) throws EvenementCivilHandlerException {

		final MenageCommun menageCommun;
		final MenageCommun menageCommunHabitantPrincipal = getMenageCommunActif(habitantPrincipal);
		final MenageCommun menageCommunHabitantConjoint = getMenageCommunActif(habitantConjoint);

		if (menageCommunHabitantPrincipal != null && menageCommunHabitantConjoint != null
				&& !menageCommunHabitantPrincipal.getId().equals(menageCommunHabitantConjoint.getId())) {
			/*
			 * Les deux contribuables appartiennent chacun à un ménage différent de l'autre
			 */
			throw new EvenementCivilHandlerException("L'individu et le conjoint ne partagent pas le même ménage commun");
		}
		else if (menageCommunHabitantPrincipal == null && menageCommunHabitantConjoint == null) {

			// [UNIREG-780] recherche d'un ancien ménage auquel auraient appartenu les deux contribuables
			final MenageCommun ancienMenage = getAncienMenageCommun(habitantPrincipal, habitantConjoint);
			if (ancienMenage != null) {
				/*
				 * Les arrivant ont appartenu à un même ménage
				 */
				menageCommun = ancienMenage;
				// ajout des tiers manquant au ménage commun
				boolean rapportPrincipalTrouve = false;
				boolean rapportConjointTrouve = false;
				final Set<RapportEntreTiers> rapportsObjet = menageCommun.getRapportsObjet();
				if (rapportsObjet != null) {
					for (RapportEntreTiers rapport : rapportsObjet) {
						if (!rapport.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(rapport.getType()) && rapport.isValidAt(dateEvenement)) {
							final Long tiersId = rapport.getSujetId();
							if (habitantPrincipal.getNumero().equals(tiersId)) {
								rapportPrincipalTrouve = true;
							}
							else if (habitantConjoint != null && habitantConjoint.getNumero().equals(tiersId)) {
								rapportConjointTrouve = true;
							}
						}
					}
				}
				if (!rapportPrincipalTrouve) {
					getService().addTiersToCouple(menageCommun, habitantPrincipal, dateEvenement, null);
				}
				if (!rapportConjointTrouve && habitantConjoint != null) {
					getService().addTiersToCouple(menageCommun, habitantConjoint, dateEvenement, null);
				}

				Audit.info(evenementId, String.format("Les arrivants ont appartenu au ménage commun [%d].", menageCommun.getNumero()));
			}
			else {
				/*
				 * Le ménage commun n'existe pas du tout et aucun des deux individus n'appartient à un quelconque ménage => on crée un ménage
				 * commun de toutes pièces
				 */
				final EnsembleTiersCouple ensemble = getService().createEnsembleTiersCouple(habitantPrincipal, habitantConjoint, dateDebutMenage, null);
				menageCommun = ensemble.getMenage();
				Audit.info(evenementId, String.format("Un nouveau ménage commun [%d] a été créé pour l'arrivée des nouveaux arrivants", menageCommun.getNumero()));
			}
		}
		else if (menageCommunHabitantPrincipal != null && menageCommunHabitantConjoint != null) {
			/*
			 * Les deux individus appartiennt déjà au même ménage => rien de spécial à faire
			 */
			Assert.isTrue(menageCommunHabitantPrincipal == menageCommunHabitantConjoint);
			Assert.isTrue(habitantConjoint == null
					|| habitantConjoint == getAutrePersonneDuMenage(menageCommunHabitantPrincipal, habitantPrincipal));
			Assert.isTrue(habitantConjoint == null
					|| habitantPrincipal == getAutrePersonneDuMenage(menageCommunHabitantConjoint, habitantConjoint));
			menageCommun = menageCommunHabitantPrincipal;
			Audit.info(evenementId, String.format("Le ménage commun [%d] des nouveaux arrivants existe déjà.", menageCommun.getNumero()));
		}
		else {
			if (menageCommunHabitantPrincipal != null) {
				/*
				 * L'individu principal appartient déjà à un ménage => on vérifie que le ménage en question ne possède bien qu'un seul
				 * membre actif et on rattache le conjoint au ménage
				 */
				final MenageCommun menage = menageCommunHabitantPrincipal;

				/*
				 * Vérification que l'on ajoute pas un deuxième conjoint
				 */
				final PersonnePhysique autrePersonne = getAutrePersonneDuMenage(menage, habitantPrincipal);
				if (autrePersonne != null) {
					if (habitantConjoint == null) {
						// [UNIREG-1184] marié seul dans le civil
						throw new EvenementCivilHandlerException(
								String.format("L'individu principal [%d] est en ménage commun avec une personne [%d] dans le fiscal alors qu'il est marié seul dans le civil",
										habitantPrincipal.getNumero(), autrePersonne.getNumero()));
					}
					else {
						throw new EvenementCivilHandlerException(
								String.format("L'individu principal [%d] est en ménage commun avec une personne [%d] autre que son conjoint [%d]",
										habitantPrincipal.getNumero(), autrePersonne.getNumero(), habitantConjoint.getNumero()));
					}
				}

				/*
				 * On ajoute le rapport entre l'habitant conjoint et le ménage existant
				 */
				if (habitantConjoint != null) {
					final RapportEntreTiers rapport = getService().addTiersToCouple(menage, habitantConjoint, dateDebutMenage, null);

					menageCommun = (MenageCommun) getTiersDAO().get(rapport.getObjetId());
					Audit.info(evenementId, String.format("L'arrivant [%d] a été attaché au ménage commun [%d] déjà existant", habitantConjoint.getNumero(), menageCommun.getNumero()));
				}
				else {
					menageCommun = menage;
				}

			}
			else {
				Assert.notNull(habitantConjoint);
				Assert.notNull(menageCommunHabitantConjoint);

				/*
				 * Le conjoint appartient déjà à un ménage => on vérifie que le ménage en question ne possède bien qu'un seul membre actif
				 * et on rattache l'individu principal au ménage
				 */
				final MenageCommun menage = menageCommunHabitantConjoint;

				/*
				 * Vérification que l'on ajoute pas un deuxième individu principal
				 */
				final PersonnePhysique autrePersonne = getAutrePersonneDuMenage(menage, habitantConjoint);
				if (autrePersonne != null) {
					final String message = String.format("L'individu conjoint [%s] est en ménage commun avec une personne [%s] autre que son individu principal[%s]",
														habitantConjoint, autrePersonne, habitantPrincipal);
					throw new EvenementCivilHandlerException(message);
				}

				/*
				 * On ajoute le rapport entre l'habitant principal et le ménage existant
				 * [UNIREG-1677] La date de début du nouveau rapport entre tiers doit être reprise sur le rapport existant
				 */
				final RapportEntreTiers rapportExistant = habitantConjoint.getRapportSujetValidAt(null, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				Assert.notNull(rapportExistant);

				final RapportEntreTiers rapport = getService().addTiersToCouple(menage, habitantPrincipal, rapportExistant.getDateDebut(), null);
				menageCommun = (MenageCommun) getTiersDAO().get(rapport.getObjetId());

				final String auditString = String.format("L'arrivant [%d] a été rattaché au ménage commun [%d] dèjà existant", habitantPrincipal.getNumero(), menageCommun.getNumero());
				Audit.info(evenementId, auditString);
			}
		}
		return menageCommun;
	}

	/**
	 * Recherche l'autre personne faisant partie du ménage commun.
	 *
	 * @param menageCommun
	 *            le ménage commun à analyser
	 * @param personne
	 *            la personne de référence du ménage commun
	 * @return l'autre personne du ménage, ou null si le ménage ne possède qu'une personne
	 * @throws EvenementCivilHandlerException
	 *             si plus d'une autre personne est trouvée (ménage à trois, ah là là...)
	 */
	private PersonnePhysique getAutrePersonneDuMenage(MenageCommun menageCommun, PersonnePhysique personne) throws EvenementCivilHandlerException {

		final RapportEntreTiers rapportAutrePersonne = getAppartenanceAuMenageAutrePersonne(menageCommun, personne);
		if (rapportAutrePersonne == null) {
			return null;
		}

		return (PersonnePhysique) getTiersDAO().get(rapportAutrePersonne.getSujetId());
	}

	/**
	 * Recherche l'autre personne faisant partie du ménage commun et retourne son rapport au ménage actif.
	 *
	 * @param menageCommun
	 *            le ménage commun à analyser
	 * @param personne
	 *            la personne de référence du ménage commun
	 * @return le rapport au ménage actif de l'autre personne du ménage, ou null si le ménage ne possède qu'une personne
	 * @throws EvenementCivilHandlerException
	 *             si plus d'une autre personne est trouvée (ménage à trois, ah là là...)
	 */
	private RapportEntreTiers getAppartenanceAuMenageAutrePersonne(MenageCommun menageCommun, PersonnePhysique personne)
			throws EvenementCivilHandlerException {

		RapportEntreTiers appartenanceAutrePersonne = null;

		final Set<RapportEntreTiers> rapportsObjet = menageCommun.getRapportsObjet();
		for (RapportEntreTiers rapportObjet : rapportsObjet) {
			if (!rapportObjet.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(rapportObjet.getType())
					&& rapportObjet.getDateFin() == null) {

				if (!rapportObjet.getSujetId().equals(personne.getId())) {
					if (appartenanceAutrePersonne != null) {
						final String message = String.format("Plus d'un conjoint trouvé pour le ménage commun [%s]", menageCommun);
						throw new EvenementCivilHandlerException(message);
					}
					appartenanceAutrePersonne = rapportObjet;
				}
			}
		}
		return appartenanceAutrePersonne;
	}

	/**
	 * Recherche le menage commun actif auquel est rattaché une personne
	 *
	 * @param personne
	 *            la personne potentiellement rattachée à un ménage commun
	 * @return le ménage commun trouvé, ou null si cette personne n'est pas rattaché au ménage.
	 * @throws EvenementCivilHandlerException
	 *             si plus d'un ménage commun est trouvé.
	 */
	private MenageCommun getMenageCommunActif(PersonnePhysique personne) throws EvenementCivilHandlerException {

		if (personne == null) {
			return null;
		}

		MenageCommun menageCommun = null;

		final Set<RapportEntreTiers> rapportsSujet = personne.getRapportsSujet();
		if (rapportsSujet != null) {
			for (RapportEntreTiers rapportSujet : rapportsSujet) {
				if (!rapportSujet.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(rapportSujet.getType())
						&& rapportSujet.getDateFin() == null) {
					/*
					 * le rapport de l'apartenance a été trouvé, on en déduit donc le tiers ménage
					 */
					if (menageCommun != null) {
						throw new EvenementCivilHandlerException("Plus d'un ménage commun trouvé pour la personne = ["
								+ personne.toString() + "]");
					}
					menageCommun = (MenageCommun) getTiersDAO().get(rapportSujet.getObjetId());
				}
			}
		}

		return menageCommun;
	}

	/**
	 * Cherche dans les rapport appartenance ménage, l'existence d'un ménage commun auquel auraient appartenu les deux contribuables.
	 *
	 * @param habitantPrincipal
	 *            le membre principal du ménage
	 * @param habitantConjoint
	 *            son conjoint
	 * @return le ménage commun trouvé, ou null si aucun trouvé.
	 */
	private MenageCommun getAncienMenageCommun(PersonnePhysique principal, PersonnePhysique conjoint) {

		MenageCommun ancienMenage = null;

		final Set<RapportEntreTiers> rapportsSujet = principal.getRapportsSujet();
		if (rapportsSujet != null) {
			for (RapportEntreTiers rapport : rapportsSujet) {
				if (!rapport.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(rapport.getType())
						&& rapport.getDateFin() != null) {

					final MenageCommun menage = (MenageCommun) getTiersDAO().get(rapport.getObjetId());

					final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(menage, rapport.getDateDebut());
					if (couple != null && couple.estComposeDe(principal, conjoint)) {

						if (ancienMenage != null) {
							final String message;
							if (conjoint != null) {
								message = "Plus d'un ménage commun trouvé pour les contribuables [" + principal.getNumero() + ", " + conjoint.getNumero() + "]";
							}
							else {
								message = "Plus d'un ménage commun trouvé pour le contribuable [" + principal.getNumero() + "]";
							}

							throw new EvenementCivilHandlerException(message);
						}
						ancienMenage = menage;
					}
				}
			}
		}
		return ancienMenage;
	}


	/**
	 *
	 * L'arrivée de type principale prime sur l'arrivée de type secondaire si le cas se présente.
	 *
	 * @param arrivee
	 *            l'objet d'arrivée à traiter
	 * @return le type d'arrivée déterminé
	 * @throws EvenementCivilHandlerException
	 *             si aucune des deux communes d'arrivée n'est dans le canton
	 *
	 */
	protected static ArriveeType getArriveeType(ServiceInfrastructureService serviceInfra, Arrivee arrivee) throws EvenementCivilHandlerException {
		final TypeEvenementCivil type = arrivee.getType();
		if (type == TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC || type == TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS
				|| type == TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE) {
			return ArriveeType.ARRIVEE_ADRESSE_PRINCIPALE;
		}
		else if (type == TypeEvenementCivil.ARRIVEE_SECONDAIRE) {
			return ArriveeType.ARRIVEE_RESIDENCE_SECONDAIRE;
		}

		try {
			final CommuneSimple nouvelleCommunePrincipale = getCommuneArrivee(serviceInfra, arrivee, ArriveeType.ARRIVEE_ADRESSE_PRINCIPALE);
			final CommuneSimple ancienneCommunePrincipale = arrivee.getAncienneCommunePrincipale();

			if ((ancienneCommunePrincipale == null && nouvelleCommunePrincipale != null)
					|| (nouvelleCommunePrincipale != null && nouvelleCommunePrincipale.getNoOFSEtendu() != ancienneCommunePrincipale.getNoOFSEtendu())) {
				return ArriveeType.ARRIVEE_ADRESSE_PRINCIPALE;
			}
			final CommuneSimple nouvelleCommuneSecondaire = getCommuneArrivee(serviceInfra, arrivee, ArriveeType.ARRIVEE_RESIDENCE_SECONDAIRE);
			final CommuneSimple ancienneCommuneSecondaire = arrivee.getAncienneCommuneSecondaire();
			if ((ancienneCommuneSecondaire == null && nouvelleCommuneSecondaire != null)
					|| (nouvelleCommuneSecondaire != null && nouvelleCommuneSecondaire.getNoOFSEtendu() != ancienneCommuneSecondaire.getNoOFSEtendu())) {
				return ArriveeType.ARRIVEE_RESIDENCE_SECONDAIRE;
			}
		}
		catch (InfrastructureException e) {
			throw new EvenementCivilHandlerException(e.getMessage(), e);
		}

		throw new EvenementCivilHandlerException("Aucune des adresses (principale ou secondaire) n'a changé");
	}



	protected MotifFor getMotifOuverture(Arrivee arrivee) {
		TypeEvenementCivil type = arrivee.getType();
		switch (type){
			case ARRIVEE_PRINCIPALE_HC :
				return MotifFor.ARRIVEE_HC;
			case ARRIVEE_PRINCIPALE_HS :
				return MotifFor.ARRIVEE_HS;
			case ARRIVEE_PRINCIPALE_VAUDOISE :
				return MotifFor.DEMENAGEMENT_VD;
			case ARRIVEE_DANS_COMMUNE :
				MotifFor motifOuverture = MotifFor.DEMENAGEMENT_VD;
				if(arrivee.getAncienneAdressePrincipale() == null)
				{
					motifOuverture = null;
				}
				else if(arrivee.getAncienneAdressePrincipale().getNoOfsPays() != null &&
						!arrivee.getAncienneAdressePrincipale().getNoOfsPays().equals(ServiceInfrastructureService.noOfsSuisse))
				{
					motifOuverture = MotifFor.ARRIVEE_HS;
				}
				else if(arrivee.getAncienneCommunePrincipale() == null)
				{
					motifOuverture = null;
				}
				else if(arrivee.getAncienneCommunePrincipale() != null && !arrivee.getAncienneCommunePrincipale().isVaudoise())
				{
					motifOuverture = MotifFor.ARRIVEE_HC;
				}
				return motifOuverture;
			default :
				return null;
		}
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
		types.add(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC);
		types.add(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS);
		types.add(TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE);
		types.add(TypeEvenementCivil.ARRIVEE_SECONDAIRE);
		return types;
	}

	protected boolean isDansLeCanton(CommuneSimple commune) {
		return commune != null && commune.isVaudoise();
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new ArriveeAdapter();
	}

}
