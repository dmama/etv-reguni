package ch.vd.uniregctb.evenement.arrivee;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EnsembleTiersCouple;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeTiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
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
				final Commune communeArrivee = getCommuneArrivee(serviceInfra, arrivee, type);
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

	private static Commune getCommuneArrivee(ServiceInfrastructureService serviceInfra, Arrivee arrivee, ArriveeType type) throws InfrastructureException {

		// [UNIREG-1995] si la commune d'annonce est renseignée dans l'événement, la prendre en compte
		// (ou si bien-sûr on tombe sur une commune fractionnée) sinon on prend la commune de l'adresse

		if (arrivee.getNumeroOfsCommuneAnnonce() != null && arrivee.getNumeroOfsCommuneAnnonce() > 0) {
			final Commune commune = serviceInfra.getCommuneByNumeroOfsEtendu(arrivee.getNumeroOfsCommuneAnnonce());
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

	private static Commune getCommuneArriveeDepuisAdresse(Arrivee arrivee, ArriveeType type) {
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
			final PersonnePhysique habitant = getTiersDAO().getPPByNumeroIndividu(arrivee.getIndividu().getNoTechnique());
			if (habitant == null) {
				final String message = "L'individu est inconnu dans registre fiscal mais possédait déjà une adresse dans le " +
						"registre civil avant son arrivée (incohérence entre les deux registres)";
				erreurs.add(new EvenementCivilErreur(message));
			}
		}
	}

	private void validateForPrincipal(Arrivee arrivee, List<EvenementCivilErreur> erreurs) {

		MotifFor motifFor = getMotifOuverture(arrivee);
		if ( motifFor == MotifFor.ARRIVEE_HC || motifFor == MotifFor.ARRIVEE_HS ) {
			// Si le motif d'ouverture du for est arrivee HS ou HC alors , l'eventuel for principal actuel ne doit pas être vaudois
			PersonnePhysique pp = getTiersDAO().getPPByNumeroIndividu(arrivee.getIndividu().getNoTechnique());
			if (pp!= null) {
				RapportEntreTiers rapportMenage = pp.getRapportSujetValidAt(arrivee.getDate(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				Contribuable ctb = pp;
				if (rapportMenage != null) {
					ctb = (Contribuable)rapportMenage.getObjet();
				}
				ForFiscalPrincipal forFP = ctb.getForFiscalPrincipalAt(arrivee.getDate());
				if (forFP != null && forFP.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					String msg;
					if (rapportMenage != null) {
						msg = String.format(
							"A la date de l'événement, le ménage commun (ctb: %s) au quel appartient la personne physique associée l'individu a déja une for principal vaudois",
							ctb.getNumero());
					} else {
						msg = String.format(
								"A la date de l'événement, la personne physique (ctb: %s) associée à l'individu a déja une for principal vaudois",
								ctb.getNumero());
					}
					erreurs.add(new EvenementCivilErreur(msg));
				}
			}
		}
	}

	protected final void validateArriveeAdressePrincipale(Arrivee arrivee, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		/*
		 * La date de début de la nouvelle adresse principale de l’individu est antérieure ou identique à la date de l'ancienne.
		 */
		final Adresse ancienneAdresse = arrivee.getAncienneAdressePrincipale();
		if (ancienneAdresse != null && ancienneAdresse.getDateDebutValidite() != null && arrivee.getDate().isBeforeOrEqual(ancienneAdresse.getDateDebutValidite())) {
			erreurs.add(new EvenementCivilErreur("La date d'arrivée principale est antérieure à la date de début de l'ancienne adresse"));
		}
		if(ancienneAdresse != null && (ancienneAdresse.getDateFinValidite() == null || arrivee.getDate().isBeforeOrEqual(ancienneAdresse.getDateFinValidite())) ){
			erreurs.add(new EvenementCivilErreur("La date d'arrivée principale est antérieure à la date de fin de l'ancienne adresse"));
		}

		try {
			/*
			 * La nouvelle adresse principale n’est pas dans le canton (il n’est pas obligatoire que l’adresse courrier soit dans le canton).
			 */
			final Commune nouvelleCommune = getCommuneArrivee(getService().getServiceInfra(), arrivee, ArriveeType.ARRIVEE_ADRESSE_PRINCIPALE);
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
		if (ancienneAdresse != null && ancienneAdresse.getDateFinValidite() != null && arrivee.getDate().isBeforeOrEqual(ancienneAdresse.getDateFinValidite())) {
			erreurs.add(new EvenementCivilErreur("La date d'arrivée secondaire est antérieure à la date de fin de l'ancienne adresse"));
		}

		try {
			/*
			 * La nouvelle adresse secondaire n’est pas dans le canton (il n’est pas obligatoire que l’adresse courrier soit dans le canton).
			 */
			final Commune nouvelleCommune = getCommuneArrivee(infraService, arrivee, ArriveeType.ARRIVEE_RESIDENCE_SECONDAIRE);
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
	public void handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		final Arrivee arrivee = (Arrivee) evenement;

		if (isIndividuEnMenage(arrivee.getIndividu(), evenement.getDate())) {
			handleIndividuEnMenage(arrivee, warnings);
		}
		else {
			handleIndividuSeul(arrivee, warnings);
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
	protected final void handleIndividuSeul(Arrivee arrivee, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {

		try {
			final Individu individu = arrivee.getIndividu();
			final RegDate dateArrivee = arrivee.getDate();
			final Long numeroEvenement = arrivee.getNumeroEvenement();

			/*
			 * Création à la demande de l'habitant
			 */
			// [UNIREG-770] rechercher si un Non-Habitant assujetti existe (avec Nom - Prénom)
			final PersonnePhysique habitant = getOrCreateHabitant(individu, dateArrivee, numeroEvenement, FindBehavoir.ASSUJETTISSEMENT_OBLIGATOIRE_ERROR_IF_SEVERAL);

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

			RegDate dateEvenement = dateArrivee;
			if (TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE == arrivee.getType()) {
				// vérification du dépassement du 20/12
				dateEvenement = FiscalDateHelper.getDateEvenementFiscal(dateEvenement);
			}

			final boolean individuMajeur = FiscalDateHelper.isMajeurAt(individu, dateEvenement);
			final Commune communeArrivee = getCommuneArrivee(getService().getServiceInfra(), arrivee, type);

			/*
			 * Le for fiscal principal reste inchangé en cas d'arrivée en résidence secondaire, ou en cas d'arrivée en résidence principale
			 * d'un individu mineur.
			 */
			if (type == ArriveeType.ARRIVEE_ADRESSE_PRINCIPALE && individuMajeur) {

				MotifFor motifOuverture = getMotifOuverture(arrivee);

				final ForFiscalPrincipal forFiscal = habitant.getForFiscalPrincipalAt(null);
				if (forFiscal == null) {
					// s'il est suisse, titulaire d'un permis C ou a obtenu le statut de réfugié => ordinaire
					if (!getService().isEtrangerSansPermisC(habitant, dateEvenement) || getService().isHabitantRefugie(habitant, dateEvenement)) {
						Audit.info(numeroEvenement, "Création d'un for fiscal ordinaire");
						final int numeroOfsNouveau = communeArrivee.getNoOFSEtendu();
						if(motifOuverture == null){
							motifOuverture = MotifFor.ARRIVEE_HS;
							warnings.add(new EvenementCivilErreur("ancienne adresse avant l'arrivée inconnue : "
									+ "veuillez indiquer le motif d'ouverture du for principal", TypeEvenementErreur.WARNING));
						}
						openForFiscalPrincipalDomicileVaudoisOrdinaire(habitant, dateEvenement, numeroOfsNouveau, motifOuverture, false);
					}
					else if (motifOuverture == MotifFor.ARRIVEE_HC || motifOuverture == MotifFor.ARRIVEE_HS || motifOuverture == null) {
						Audit.info(arrivee.getNumeroEvenement(), "Création d'un for fiscal source");
						final int numeroOfsNouveau = communeArrivee.getNoOFSEtendu();
						if(motifOuverture == null){
							motifOuverture = MotifFor.ARRIVEE_HS;
							warnings.add(new EvenementCivilErreur("ancienne adresse avant l'arrivée inconnue : "
									+ "veuillez indiquer le motif d'ouverture du for principal", TypeEvenementErreur.WARNING));
						}
						final RegDate dateOuverture = findDateOuvertureSourcier(dateEvenement, arrivee.getType(), arrivee.getAncienneCommunePrincipale());
						openForFiscalPrincipal(habitant, dateOuverture, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, numeroOfsNouveau,
								MotifRattachement.DOMICILE, motifOuverture, ModeImposition.SOURCE, false);
					}
				}
				else {
					Audit.info(numeroEvenement, "Mise a jour des fors fiscaux avec conservation du mode d'imposition");
					final int numeroOfsNouveau = communeArrivee.getNoOFSEtendu();
					if(motifOuverture == null){
						motifOuverture = MotifFor.ARRIVEE_HS;
						warnings.add(new EvenementCivilErreur("ancienne adresse avant l'arrivée inconnue : "
								+ "veuillez indiquer le motif d'ouverture du for principal", TypeEvenementErreur.WARNING));
					}

					RegDate dateOuvertureNouveauFor = dateEvenement;
					if (isSourcier(forFiscal.getModeImposition())) {
						dateOuvertureNouveauFor = findDateOuvertureSourcier(dateEvenement, arrivee.getType(), communeArrivee);
					}

					updateForFiscalPrincipal(habitant, dateOuvertureNouveauFor, numeroOfsNouveau, motifOuverture, motifOuverture, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, false);
				}
			}
		}
		catch (TiersException e) {
			throw new EvenementCivilHandlerException(e.getMessage(), e);
		}
		catch (InfrastructureException e) {
			throw new EvenementCivilHandlerException(e.getMessage(), e);
		}
	}

	private List<PersonnePhysique> findNonHabitants(Individu individu, boolean assujettissementObligatoire) {

		final List<PersonnePhysique> nonHabitants = new ArrayList<PersonnePhysique>();

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setTypeTiers(TypeTiers.NON_HABITANT);
		criteria.setTypeRechercheDuNom(TypeRecherche.EST_EXACTEMENT);
		criteria.setDateNaissance(individu.getDateNaissance());
		final HistoriqueIndividu histo = individu.getDernierHistoriqueIndividu();
		criteria.setNomRaison(histo.getPrenom() + " " + histo.getNom());

		final List<TiersIndexedData> results = getService().search(criteria);
		for (Iterator<TiersIndexedData> itResults = results.iterator(); itResults.hasNext(); ) {
			final TiersIndexedData tiersIndexedData = itResults.next();
			final PersonnePhysique pp = (PersonnePhysique) getService().getTiers(tiersIndexedData.getNumero());
			// [UNIREG-770] le non habitant doit être assujetti
			if (!assujettissementObligatoire || (assujettissementObligatoire && pp.getForFiscalPrincipalAt(null) != null)) {
				nonHabitants.add(pp);
			}
		}
		return nonHabitants;
	}

	private enum FindBehavoir {
		ASSUJETTISSEMENT_OBLIGATOIRE_ERROR_IF_SEVERAL(true, true),
		ASSUJETTISSEMENT_OBLIGATOIRE_NO_ERROR_IF_SEVERAL(true, false),
		ASSUJETTISSEMENT_NON_OBLIGATOIRE_ERROR_IF_SEVERAL(false, true),
		ASSUJETTISSEMENT_NON_OBLIGATOIRE_NO_ERROR_IF_SEVERAL(false, false)
		;
		
		private final boolean assujettissementObligatoire;
		
		private final boolean errorOnMultiples;
		
		private FindBehavoir(boolean assujettissementObligatoire, boolean errorOnMultiples) {
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
	
	private PersonnePhysique getOrCreateHabitant(Individu individu, RegDate dateEvenement, long evenementId, FindBehavoir behavoir) {

		final PersonnePhysique pp = getTiersDAO().getPPByNumeroIndividu(individu.getNoTechnique());
		final PersonnePhysique habitant;
		if (pp != null) {
			if (pp.isHabitant()) {
				habitant = pp;
			}
			else {
				habitant = getService().changeNHenHabitant(pp, pp.getNumeroIndividu(), dateEvenement);
				Audit.info(evenementId, "Le non habitant " + habitant.getNumero() + " devient habitant");
			}
		}
		else {
			final List<PersonnePhysique> nonHabitants = findNonHabitants(individu, behavoir.isAssujettissementObligatoire());

			if (nonHabitants.size() == 1) {
				final PersonnePhysique nhab = nonHabitants.get(0);
				habitant = getService().changeNHenHabitant(nhab, individu.getNoTechnique(), dateEvenement);
				Audit.info(evenementId, "Le non habitant " + habitant.getNumero() + " devient habitant");
			}
			else if (nonHabitants.size() == 0 || !behavoir.isErrorOnMultiples()) {
				final PersonnePhysique nouvelHabitant = new PersonnePhysique(true);
				nouvelHabitant.setNumeroIndividu(individu.getNoTechnique());
				Audit.info(evenementId, "Un tiers a été créé pour le nouvel arrivant");
				habitant = (PersonnePhysique) getTiersDAO().save(nouvelHabitant);
			}
			else {
				throw new EvenementCivilHandlerException("Plusieurs tiers non-habitants assujettis potentiels trouvés");
			}
		}

		return habitant;
	}

	private boolean isSourcier(ModeImposition modeImposition) {
		return ModeImposition.SOURCE == (modeImposition ) || ModeImposition.MIXTE_137_1.equals(modeImposition) || ModeImposition.MIXTE_137_2.equals(modeImposition);
	}

	/**
	 * Gère l'arrive d'un contribuable en ménage commun.
	 */
	protected final void handleIndividuEnMenage(Arrivee arrivee, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {

		final Individu individu = arrivee.getIndividu();
		final Individu conjoint = individu.getConjoint();
		Assert.notNull(individu); // prérequis

		RegDate dateEvenement = arrivee.getDate();
		final Long numeroEvenement = arrivee.getNumeroEvenement();

		/*
		 * Récupération/création des habitants
		 */
		final PersonnePhysique habitantPrincipal = getOrCreateHabitant(individu, dateEvenement, numeroEvenement, FindBehavoir.ASSUJETTISSEMENT_NON_OBLIGATOIRE_NO_ERROR_IF_SEVERAL);
		final PersonnePhysique habitantConjoint;
		if (conjoint != null) {
			habitantConjoint = getOrCreateHabitant(conjoint, dateEvenement, numeroEvenement, FindBehavoir.ASSUJETTISSEMENT_NON_OBLIGATOIRE_NO_ERROR_IF_SEVERAL);
		}
		else {
			habitantConjoint = null;
		}

		if (TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE == arrivee.getType()) {
			// vérification du dépassement du 20/12
			dateEvenement = FiscalDateHelper.getDateEvenementFiscal(dateEvenement);
		}

		/*
		 * Récupération/création du ménage commun et du rapports entre tiers
		 */

		final MenageCommun menageCommun = getOrCreateMenageCommun(habitantPrincipal, habitantConjoint, dateEvenement, numeroEvenement);
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
			createOrUpdateForFiscalPrincipal(arrivee, habitantPrincipal, habitantConjoint, menageCommun, warnings);
		}
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
	private RegDate findDateOuvertureSourcier(final RegDate date, final TypeEvenementCivil type, final Commune commune) {

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
	 * Crée ou met-à-jour le for fiscal principal pour le contribuable principal, son conjoint et le ménage - en fonction de leur état civil
	 * et fiscal.
	 *
	 * @param arrivee
	 *            l'événement d'arrivée
	 * @param habitantPrincipal
	 *            le contribuable principal
	 * @param habitantConjoint
	 *            le conjoint (peut être null)
	 * @param menageCommun
	 *            le ménage commun
	 */
	private void createOrUpdateForFiscalPrincipal(final Arrivee arrivee, final PersonnePhysique habitantPrincipal, PersonnePhysique habitantConjoint,
			final MenageCommun menageCommun, List<EvenementCivilErreur> warnings) {

		Assert.notNull(arrivee);
		Assert.notNull(habitantPrincipal);
		Assert.notNull(menageCommun);

		RegDate dateEvenement = arrivee.getDate();
		if (TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE == arrivee.getType()) {
			// vérification du dépassement du 20/12
			dateEvenement = FiscalDateHelper.getDateEvenementFiscal(dateEvenement);
		}

		try {

			final ForFiscalPrincipal ffpHabitantPrincipal = habitantPrincipal.getForFiscalPrincipalAt(null);
			final ForFiscalPrincipal ffpHabitantConjoint = (habitantConjoint == null ? null : habitantConjoint.getForFiscalPrincipalAt(null));
			final ForFiscalPrincipal ffpMenage = menageCommun.getForFiscalPrincipalAt(null);
			final Commune communeArrivee = getCommuneArrivee(getService().getServiceInfra(), arrivee, ArriveeType.ARRIVEE_ADRESSE_PRINCIPALE);
			final int numeroOfsNouveau = communeArrivee.getNoOFSEtendu();

			// pour un couple, le for principal est toujours sur le ménage commun
			if (ffpHabitantPrincipal != null) {
				throw new EvenementCivilHandlerException("Le contribuable principal du ménage [" + menageCommun
						+ "] possède un for fiscal principal individuel");
			}
			if (ffpHabitantConjoint != null) {
				throw new EvenementCivilHandlerException("Le conjoint du ménage [" + menageCommun
						+ "] possède un for fiscal principal individuel");
			}
			MotifFor motifOuverture = getMotifOuverture(arrivee);
			if(motifOuverture == null){
				motifOuverture = MotifFor.ARRIVEE_HS;
				warnings.add(new EvenementCivilErreur("ancienne adresse avant l'arrivée inconnue : "
						+ "veuillez indiquer le motif d'ouverture du for principal", TypeEvenementErreur.WARNING));
			}
			if (ffpMenage == null) {
				if (!getService().isEtrangerSansPermisC(habitantPrincipal, dateEvenement) ||
					getService().isHabitantRefugie(habitantPrincipal, dateEvenement) ||
					(habitantConjoint != null &&
							(!getService().isEtrangerSansPermisC(habitantConjoint, dateEvenement) ||
							getService().isHabitantRefugie(habitantConjoint, dateEvenement)))) {
					openForFiscalPrincipalDomicileVaudoisOrdinaire(menageCommun, dateEvenement, numeroOfsNouveau, motifOuverture, false);
					Audit.info(arrivee.getNumeroEvenement(), "Création d'un for fiscal principal ordinaire sur le ménage commun");
				}
				else if (motifOuverture == MotifFor.ARRIVEE_HC || motifOuverture == MotifFor.ARRIVEE_HS) {
					final RegDate dateOuverture = findDateOuvertureSourcier(dateEvenement, arrivee.getType(), communeArrivee);
					openForFiscalPrincipal(menageCommun, dateOuverture, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, numeroOfsNouveau,
						MotifRattachement.DOMICILE, motifOuverture, ModeImposition.SOURCE, false);
					Audit.info(arrivee.getNumeroEvenement(), "Création d'un for fiscal principal sourcier sur le ménage commun");
				}
			}
			else {

				RegDate dateOuvertureNouveauFor = dateEvenement;
				if (isSourcier(ffpMenage.getModeImposition())) {
					dateOuvertureNouveauFor = findDateOuvertureSourcier(dateEvenement, arrivee.getType(), communeArrivee);
				}

				updateForFiscalPrincipal(menageCommun, dateOuvertureNouveauFor, numeroOfsNouveau, motifOuverture, motifOuverture, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, false);
				Audit.info(arrivee.getNumeroEvenement(), "Mise à jour du for fiscal principal sur le ménage commun");
			}
		}
		catch (TiersException e) {
			throw new EvenementCivilHandlerException(e.getMessage(), e);
		}
		catch (InfrastructureException e) {
			throw new EvenementCivilHandlerException(e.getMessage(), e);
		}
	}

	/**
	 * Récupère le ménage commun avec le contribuable principal et le conjoint comme parties.
	 *
	 * @param habitantPrincipal
	 * @param habitantConjoint
	 * @param dateEvenement
	 * @return
	 * @throws EvenementCivilHandlerException
	 *             si les deux habitants appartiennent à des ménages différents
	 */
	private MenageCommun getOrCreateMenageCommun(final PersonnePhysique habitantPrincipal, PersonnePhysique habitantConjoint, final RegDate dateEvenement,
			long evenementId) throws EvenementCivilHandlerException {

		final MenageCommun menageCommun;
		final MenageCommun menageCommunHabitantPrincipal = getMenageCommunActif(habitantPrincipal);
		final MenageCommun menageCommunHabitantConjoint = getMenageCommunActif(habitantConjoint);

		if (menageCommunHabitantPrincipal != null && menageCommunHabitantConjoint != null
				&& menageCommunHabitantPrincipal.getId() != menageCommunHabitantConjoint.getId()) {
			/*
			 * Les deux contribuables appartiennent chacun à un ménage différent de l'autre
			 */
			throw new EvenementCivilHandlerException("L'individu et le conjoint ne partagent pas le même ménage commun");
		}
		else if (menageCommunHabitantPrincipal == null && menageCommunHabitantConjoint == null) {

			// [UNIREG-780] recherche d'un ancien ménage auquel auraient appartenu les deux contribuables
			MenageCommun ancienMenage = getAncienMenageCommun(habitantPrincipal, habitantConjoint);
			if (ancienMenage != null) {
				/*
				 * Les arrivant ont appartenu à un même ménage
				 */
				menageCommun = ancienMenage;
				// ajout des tiers manquant au ménage commun
				boolean rapportPrincipalTrouve = false;
				boolean rapportConjointTrouve = false;;
				final Set<RapportEntreTiers> rapportsObjet = menageCommun.getRapportsObjet();
				if (rapportsObjet != null) {
					for (RapportEntreTiers rapport : rapportsObjet) {
						if (!rapport.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(rapport.getType()) && rapport.isValidAt(dateEvenement)) {
							final Tiers tiers = rapport.getSujet();
							if (habitantPrincipal.getNumero().equals(tiers.getNumero())) {
								rapportPrincipalTrouve = true;
							}
							else if (habitantConjoint != null && habitantConjoint.getNumero().equals(tiers.getNumero())) {
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

				Audit.info(evenementId, "Les arrivants ont appartenu au ménage commun [" + menageCommun.getNumero() + "].");
			}
			else {
				/*
				 * Le ménage commun n'existe pas du tout et aucun des deux individus n'appartient à un quelconque ménage => on crée un ménage
				 * commun de toutes pièces
				 */
				EnsembleTiersCouple ensemble = getService().createEnsembleTiersCouple(habitantPrincipal, habitantConjoint, dateEvenement, null);
				menageCommun = ensemble.getMenage();
				Audit.info(evenementId, "Un nouveau ménage commun [" + menageCommun.getNumero()
						+ "] a été créé pour l'arrivée des nouveaux arrivants");
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
			Audit.info(evenementId, "Le ménage commun [" + menageCommun.getNumero() + "] des nouveaux arrivants existe déjà.");
		}
		else {
			if (menageCommunHabitantPrincipal != null) {
				/*
				 * L'individu principal appartient déjà à un ménage => on vérifie que le ménage en question ne possède bien qu'un seul
				 * membre actif et on rattache le conjoint au ménage
				 */
				MenageCommun menage = menageCommunHabitantPrincipal;

				/*
				 * Vérification que l'on ajoute pas un deuxième conjoint
				 */
				PersonnePhysique autrePersonne = getAutrePersonneDuMenage(menage, habitantPrincipal);
				if (autrePersonne != null) {
					if (habitantConjoint == null) {
						// [UNIREG-1184] marié seul dans le civil
						throw new EvenementCivilHandlerException("L'individu principal [" + habitantPrincipal.getNumero()
								+ "] est en ménage commun avec une personne [" + autrePersonne.getNumero()
								+ "] dans le fiscal alors qu'il est marié seul dans le civil");
					}
					else {
						throw new EvenementCivilHandlerException("L'individu principal [" + habitantPrincipal.getNumero()
								+ "] est en ménage commun avec une personne [" + autrePersonne.getNumero() + "] autre que son conjoint ["
								+ habitantConjoint.getNumero() + "]");
					}
				}

				/*
				 * On ajoute le rapport entre l'habitant conjoint et le ménage existant
				 */
				if (habitantConjoint != null) {
					RapportEntreTiers rapport = getService().addTiersToCouple(menage, habitantConjoint, dateEvenement, null);

					menageCommun = (MenageCommun) rapport.getObjet();
					Audit.info(evenementId, "L'arrivant [" + habitantConjoint.getNumero() + "] a été attaché au ménage commun ["
							+ menageCommun.getNumero() + "] déjà existant");
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
				MenageCommun menage = menageCommunHabitantConjoint;

				/*
				 * Vérification que l'on ajoute pas un deuxième individu principal
				 */
				PersonnePhysique autrePersonne = getAutrePersonneDuMenage(menage, habitantConjoint);
				if (autrePersonne != null) {
					throw new EvenementCivilHandlerException("L'individu conjoint [" + habitantConjoint
							+ "] est en ménage commun avec une personne [" + autrePersonne + "] autre que son individu principal ["
							+ habitantPrincipal + "]");
				}

				/*
				 * On ajoute le rapport entre l'habitant principal et le ménage existant
				 */
				RapportEntreTiers rapport = getService().addTiersToCouple(menage, habitantPrincipal, dateEvenement, null);
				menageCommun = (MenageCommun) rapport.getObjet();

				Audit.info(evenementId, "L'arrivant [" + habitantPrincipal.getNumero() + "] a été attaché au ménage commun ["
						+ menageCommun.getNumero() + "] déjà existant");
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
		final PersonnePhysique autrePersonne = (rapportAutrePersonne == null ? null : (PersonnePhysique) rapportAutrePersonne.getSujet());
		return autrePersonne;
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

				if (rapportObjet.getSujet() != personne) {
					if (appartenanceAutrePersonne != null) {
						throw new EvenementCivilHandlerException("Plus d'un conjoint trouvé pour la ménage commun = ["
								+ menageCommun.toString() + "]");
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
					menageCommun = (MenageCommun) rapportSujet.getObjet();
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

					final MenageCommun menage = (MenageCommun) rapport.getObjet();

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
			final Commune nouvelleCommunePrincipale = getCommuneArrivee(serviceInfra, arrivee, ArriveeType.ARRIVEE_ADRESSE_PRINCIPALE);
			final Commune ancienneCommunePrincipale = arrivee.getAncienneCommunePrincipale();

			if ((ancienneCommunePrincipale == null && nouvelleCommunePrincipale != null)
					|| (nouvelleCommunePrincipale != null && nouvelleCommunePrincipale.getNoOFSEtendu() != ancienneCommunePrincipale.getNoOFSEtendu())) {
				return ArriveeType.ARRIVEE_ADRESSE_PRINCIPALE;
			}
			final Commune nouvelleCommuneSecondaire = getCommuneArrivee(serviceInfra, arrivee, ArriveeType.ARRIVEE_RESIDENCE_SECONDAIRE);
			final Commune ancienneCommuneSecondaire = arrivee.getAncienneCommuneSecondaire();
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

	protected boolean isDansLeCanton(Commune commune) {
		return commune != null && commune.isVaudoise();
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new ArriveeAdapter();
	}

}
