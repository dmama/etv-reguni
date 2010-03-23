package ch.vd.uniregctb.evenement.common;

import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.tiers.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseAutreTiers;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.Mouvement;
import ch.vd.uniregctb.evenement.arrivee.Arrivee;
import ch.vd.uniregctb.evenement.depart.Depart;
import ch.vd.uniregctb.evenement.engine.EvenementHandlerRegistrar;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Interface commune aux classes capables de traiter un événement d'état civil.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 *
 */
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public abstract class EvenementCivilHandlerBase implements EvenementCivilHandler, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilHandlerBase.class);

	public static long NO_OFS_FRACTION = 0;
	public static long NO_OFS_FRACTION_SENTIER = 8000;
	public static long NO_OFS_L_ABBAYE = 5871;
	public static long NO_OFS_LE_CHENIT = 5872;

	/**
	 * Service d'accès aux données de la couche civile.
	 *
	 * Note: ce service est déclaré 'privé' pour forcer l'utilisation de getService() depuis les classes de bases. Ceci permet de contourner
	 * un problème de proxy CGLIB rencontré avec l'ArriveeHandler (= le service était bien setté sur le handler concret, mais pas sur proxy
	 * et lors de l'exécution d'une méthode protégée le service était vu comme null).
	 */
	private TiersService service;

	/**
	 * Service des règles et actions "métier".
	 */
	private MetierService metier;

	/**
	 * Service des événements fiscaux permettant de publier toutes modifications apporté à un tiers.
	 */
	private EvenementFiscalService evenementFiscalService;

	/**
	 * Service qui permet denregistrer le handler pour processing
	 *
	 * @see ch.vd.uniregctb.evenement.common.EvenementCivilHandler#setRegistrar(ch.vd.uniregctb.evenement.engine.EvenementHandlerRegistrar)
	 */
	private EvenementHandlerRegistrar registrar;

	/**
	 * La DAO d'accès aux tiers
	 */
	private TiersDAO tiersDAO;



	/**
	 * Le service civil
	 */
	private ServiceCivilService serviceCivil;

	/**
	 * Renvoie le type d'evenement que ce handler supporte
	 *
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	protected abstract Set<TypeEvenementCivil> getHandledType();

	public void afterPropertiesSet() throws Exception {
		Set<TypeEvenementCivil> types = getHandledType();
		if(types == null) {
			LOGGER.warn("Le handler " + getClass().getSimpleName() + " ne supporte aucun evenement civil!");
		}
		else {
			for (final TypeEvenementCivil type : types) {
				registrar.register(type, this);
			}
		}
	}

	/**
	 * @see ch.vd.uniregctb.evenement.common.www#checkCompleteness(ch.vd.uniregctb.evenement.EvenementCivil, java.util.List, java.util.List)
	 */
	public abstract void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings);

	/**
	 * Validation commune l'objet target passé en paramètre.
	 *
	 * @param target
	 * @param erreurs
	 */
	protected abstract void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings);

	/**
	 * @see ch.vd.uniregctb.evenement.common.www#validate(ch.vd.uniregctb.evenement.EvenementCivil, java.util.List, java.util.List)
	 */
	public void validate(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		validateCommon(target, erreurs, warnings);
		validateSpecific(target, erreurs, warnings);
	}

	/**
	 * @see ch.vd.uniregctb.evenement.common.www#handle(ch.vd.uniregctb.evenement.EvenementCivil)
	 */
	public abstract void handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException;

	private void validateCommon(EvenementCivil evenement, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {

		/*
		 * Vérifie que les éléments de base sont renseignés
		 */
		if (evenement.getDate() == null) {
			erreurs.add(new EvenementCivilErreur("L'événement n'est pas daté"));
			return;
		}

		if (evenement.getNumeroOfsCommuneAnnonce() == null) {
			erreurs.add(new EvenementCivilErreur("La commune d'annonce n'est pas renseignée"));
			return;
		}

		/*
		 * La date de l'événement se situe dans le futur.
		 */
		if (evenement.getDate().isAfter(RegDate.get())) {
			erreurs.add(new EvenementCivilErreur("La date de l'événement est dans le futur"));
		}

		/*
		 * Cas particulier de l'arrivée ou de la naissance : le ou les contribuables ne sont en principe pas présents.
		 */
		if (evenement.isContribuablePresentBefore()) {
			/*
			 * Il n’existe pas de tiers contribuable correspondant à l’individu, assujetti ou non (mineur, conjoint) correspondant à
			 * l’individu.
			 */
			final Individu individu = evenement.getIndividu();
			final PersonnePhysique tiers = service.getTiersDAO().getPPByNumeroIndividu(individu.getNoTechnique());
			if (tiers == null) {
				erreurs.add(new EvenementCivilErreur("Aucun tiers contribuable ne correspond au numero d'individu "
						+ individu.getNoTechnique()));
			}
			/*
			 * Il n’existe pas de tiers contribuable correspondant au conjoint, assujetti ou non (mineur, conjoint) correspondant à
			 * l’individu.
			 */
			final Individu conjoint = serviceCivil.getConjoint(individu.getNoTechnique(),evenement.getDate());
			if (conjoint != null) {

				if (evenement.getConjoint() != null && conjoint.getNoTechnique() != evenement.getConjoint().getNoTechnique()) {
					erreurs.add(new EvenementCivilErreur("Le numero d'individu du conjoint ("+conjoint.getNoTechnique()+") est différent que celui dans l'événement ("+evenement.getConjoint().getNoTechnique()+")"));
				}
				else {

					final PersonnePhysique tiersConjoint = service.getTiersDAO().getPPByNumeroIndividu(conjoint.getNoTechnique());
					if (tiersConjoint == null) {
						erreurs.add(new EvenementCivilErreur("Aucun tiers contribuable ne correspond au numero d'individu du conjoint "
								+ conjoint.getNoTechnique()));
					}
				}
			}
		}
	}

	/**
	 * Ouvre un nouveau for fiscal principal.
	 * <p>
	 * Cette méthode est une version spécialisée pour les événements fiscaux qui assume que:
	 * <ul>
	 * <li>le type d'autorité fiscale est toujours une commune vaudoise</li>
	 * <li>le motif de rattachement est toujours domicile/séjour</li>
	 * <li>le mode d'imposition est toujours ordinaire</li>
	 * </ul>
	 *
	 * @param contribuable
	 *            le contribuable sur lequel le nouveau for est ouvert
	 * @param dateOuverture
	 *            la date à laquelle le nouveau for est ouvert
	 * @param numeroOfsAutoriteFiscale
	 *            le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param changeHabitantFlag 
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalPrincipal openForFiscalPrincipalDomicileVaudoisOrdinaire(Contribuable contribuable, final RegDate dateOuverture,
			int numeroOfsAutoriteFiscale, MotifFor motifOuverture, boolean changeHabitantFlag) {
		return openForFiscalPrincipal(contribuable, dateOuverture, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, numeroOfsAutoriteFiscale,
				MotifRattachement.DOMICILE, motifOuverture, ModeImposition.ORDINAIRE, changeHabitantFlag);
	}

	/**
	 * Ouvre un nouveau for fiscal principal.
	 *
	 * @param contribuable
	 *            le contribuable sur lequel le nouveau for est ouvert
	 * @param dateOuverture
	 *            la date à laquelle le nouveau for est ouvert
	 * @param numeroOfsAutoriteFiscale
	 *            le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param changeHabitantFlag 
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalPrincipal openForFiscalPrincipal(Contribuable contribuable, final RegDate dateOuverture,
			TypeAutoriteFiscale typeAutoriteFiscale, int numeroOfsAutoriteFiscale, MotifRattachement rattachement,
			MotifFor motifOuverture, ModeImposition modeImposition, boolean changeHabitantFlag) {
		Assert.notNull(motifOuverture, "Le motif d'ouverture est obligatoire sur un for principal dans le canton");
		return getService().openForFiscalPrincipal(contribuable, dateOuverture, rattachement, numeroOfsAutoriteFiscale,
				typeAutoriteFiscale, modeImposition, motifOuverture, changeHabitantFlag);
	}

	/**
	 * Met-à-jour (= ferme l'ancien et ouvre un nouveau) le for fiscal principal d'un contribuable lors d'un changement de commune. Aucun changement n'est enregistré si la nouvelle commune n'est pas
	 * différente de la commune actuelle.
	 *
	 * @param contribuable             le contribuable en question.
	 * @param dateChangement           la date de début de validité du nouveau for.
	 * @param numeroOfsAutoriteFiscale le numéro OFS étendue de l'autorité fiscale du nouveau for.
	 * @param motifFermetureOuverture  le motif de fermeture du for existant et le motif d'ouverture du nouveau for
	 * @param typeAutorite             le type d'autorité fiscale
	 * @param modeImposition           le mode d'imposition du nouveau for. Peut être <b>null</b> auquel cas le mode d'imposition de l'ancien for est utilisé.
	 * @param changeHabitantFlag
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalPrincipal updateForFiscalPrincipal(Contribuable contribuable, final RegDate dateChangement, int numeroOfsAutoriteFiscale, MotifFor motifFermetureOuverture,
	                                                      TypeAutoriteFiscale typeAutorite, ModeImposition modeImposition, boolean changeHabitantFlag) {

		ForFiscalPrincipal forFiscalPrincipal = contribuable.getForFiscalPrincipalAt(null);
		Assert.notNull(forFiscalPrincipal);
		final Integer numeroOfsActuel = forFiscalPrincipal.getNumeroOfsAutoriteFiscale();

		// On ne ferme et ouvre les fors que si nécessaire
		if (numeroOfsActuel == null || !numeroOfsActuel.equals(numeroOfsAutoriteFiscale)) {
			closeForFiscalPrincipal(contribuable, dateChangement.getOneDayBefore(), motifFermetureOuverture);
			if (modeImposition == null) {
				modeImposition = forFiscalPrincipal.getModeImposition();
			}
			forFiscalPrincipal = openForFiscalPrincipal(contribuable, dateChangement, typeAutorite, numeroOfsAutoriteFiscale, forFiscalPrincipal.getMotifRattachement(), motifFermetureOuverture,
					modeImposition, changeHabitantFlag);
		}
		return forFiscalPrincipal;
	}

	/**
	 * Ferme le for fiscal principal d'un contribuable.
	 * <p>
	 * Note: cette méthode est définie à ce niveau par soucis de symétrie avec les méthodes openForFiscalPrincipal et
	 * updateForFiscalPrincipal.
	 *
	 * @param contribuable
	 *            le contribuable concerné
	 * @param dateFermeture
	 *            la date de fermeture du for
	 */
	protected void closeForFiscalPrincipal(Contribuable contribuable, RegDate dateFermeture, MotifFor motifFermeture) {
		getService().closeForFiscalPrincipal(contribuable, dateFermeture, motifFermeture);
	}

	/**
	 * Ferme tous les fors fiscaux d'un contribuable.
	 * <p>
	 * Note: cette méthode est définie à ce niveau par soucis de symétrie avec les méthodes openForFiscalPrincipal et
	 * updateForFiscalPrincipal.
	 *
	 * @param contribuable
	 *            le contribuable concerné.
	 * @param dateFermeture
	 *            la date de fermeture des fors.
	 */
	protected void closeAllForsFiscaux(Contribuable contribuable, RegDate dateFermeture, MotifFor motifFermeture) {
		getService().closeAllForsFiscaux(contribuable, dateFermeture, motifFermeture);
	}

	/**
	 * Clone un for fiscal principal et ajoute le nouveau for sur le contribuable spécifié.
	 * <p>
	 * Cette méthode est une version spécialisée pour les événements fiscaux qui assume que le mode d'imposition est toujours ordinaire.
	 *
	 * @param contribuable
	 *            le contribuable en question.
	 * @param forFiscalPrincipal
	 *            le for à cloner.
	 * @param dateOuverture
	 *            la date d'ouverture du nouveau for fiscal principal.
	 * @param changeHabitantFlag 
	 * @return le nouveau for fiscal principal cloné.
	 */
	protected ForFiscalPrincipal cloneForFiscalPrincipal(Contribuable contribuable, ForFiscalPrincipal forFiscalPrincipal,
			RegDate dateOuverture, MotifFor motifOuverture, boolean changeHabitantFlag) {

		return getService().openForFiscalPrincipal(contribuable, dateOuverture, forFiscalPrincipal.getMotifRattachement(),
				forFiscalPrincipal.getNumeroOfsAutoriteFiscale(), forFiscalPrincipal.getTypeAutoriteFiscale(), 
				ModeImposition.ORDINAIRE, motifOuverture, changeHabitantFlag);
	}

	/**
	 * Vérifie la non-existence d'un Tiers.
	 *
	 * @param noIndividu
	 * @throws EvenementCivilHandlerException
	 *             si un ou plusieurs tiers sont trouvés
	 */
	protected void verifieNonExistenceTiers(Long noIndividu) throws EvenementCivilHandlerException {
		if (service.getPersonnePhysiqueByNumeroIndividu(noIndividu) != null) {
			throw new EvenementCivilHandlerException("Le tiers existe déjà avec cet individu " + noIndividu
					+ " alors que c'est une naissance");
		}
	}

	/**
	 * Crée une adresse civile de type courrier rattaché au numero d'individu donné
	 *
	 * @return l'adresse Civile
	 */
	protected AdresseCivile createAdresseCivile() {
		AdresseCivile adresse = new AdresseCivile();
		adresse.setType(EnumTypeAdresse.COURRIER);
		return adresse;
	}

	/**
	 * Crée une adresse pointant vers un autre Tiers
	 *
	 * @param noIndividu
	 *            le numéro d'individu
	 * @return l'adresse AutreTiers
	 */
	protected AdresseAutreTiers createAdresseAutreTiers(Tiers autreTiers, TypeAdresseTiers type) {
		AdresseAutreTiers adresse = new AdresseAutreTiers();
		adresse.setTiers(autreTiers);
		adresse.setUsage(type);
		return adresse;
	}

	/**
	 * @return l'habitant (ou ancien habitant) correspondant à son numéro d'individu.
	 * @throws EvenementCivilHandlerException
	 *             si aucun habitant (ou ancien habitant) ne correspond au numéro d'individu donné.
	 */
	protected PersonnePhysique getHabitantOrThrowException(Long noIndividu) throws EvenementCivilHandlerException {
		return getHabitantOrThrowException(noIndividu, false);
	}

	/**
	 * @param doNotAutoFlush
	 *            si vrai, les modifications courantes de la session hibernate ne sont pas flushées dans le base (évite d'incrémenter le
	 *            numéro de version, mais si l'habitant vient d'être créé et n'existe pas encore en base, il ne sera pas trouvé).
	 * @return l'habitant (ou ancien habitant) correspondant à son numéro d'individu.
	 * @throws EvenementCivilHandlerException
	 *             si aucun habitant (ou ancien habitant) ne correspond au numéro d'individu donné.
	 */
	protected PersonnePhysique getHabitantOrThrowException(Long noIndividu, boolean doNotAutoFlush) throws EvenementCivilHandlerException {
		final PersonnePhysique habitant = tiersDAO.getPPByNumeroIndividu(noIndividu, doNotAutoFlush);
		if (habitant == null) {
			throw new EvenementCivilHandlerException("L'habitant avec le numéro d'individu = " + noIndividu
					+ " n'existe pas dans le registre.");
		}
		return habitant;
	}

	/**
	 * @param noIndividu
	 *            le numéro d'individu
	 * @param errors
	 *            la collection des erreurs qui sera remplie automatiquement si l'habitant n'existe pas
	 * @return l'habitant (ou ancien habitant) correspondant à son numéro d'individu, ou <b>null<b> si aucun habitant (ou ancien habitant) ne correspond au numéro d'individu donné.
	 */
	protected PersonnePhysique getHabitantOrFillErrors(Long noIndividu, List<EvenementCivilErreur> errors) {
		final PersonnePhysique habitant = getService().getPersonnePhysiqueByNumeroIndividu(noIndividu);
		if (habitant == null) {
			errors.add(new EvenementCivilErreur("L'habitant avec le numéro d'individu = " + noIndividu
							+ " n'existe pas dans le registre."));
		}
		return habitant;
	}

	protected static void addValidationResults(List<EvenementCivilErreur> errors, List<EvenementCivilErreur> warnings, ValidationResults resultat) {
		if (resultat.hasErrors()) {
			for (String erreur : resultat.getErrors()) {
				errors.add(new EvenementCivilErreur(erreur));
			}
		}
		if (resultat.hasWarnings()) {
			for (String warning : resultat.getWarnings()) {
				warnings.add(new EvenementCivilErreur(warning));
			}
		}
	}

	/**
	 * @return the service
	 */
	protected TiersService getService() {
		return service;
	}

	/**
	 * @see ch.vd.uniregctb.evenement.common.www#setService(ch.vd.uniregctb.tiers.TiersService)
	 */
	public void setService(TiersService service) {
		this.service = service;
	}

	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	public ServiceCivilService getServiceCivil() {
		return serviceCivil;
	}

	public MetierService getMetier() {
		return metier;
	}

	public void setMetier(MetierService metier) {
		this.metier = metier;
	}

	/**
	 * @see ch.vd.uniregctb.evenement.common.www#getTiersDAO()
	 */
	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	/**
	 * @see ch.vd.uniregctb.evenement.common.www#setTiersDAO(ch.vd.uniregctb.tiers.TiersDAO)
	 */
	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	/**
	 * @see ch.vd.uniregctb.evenement.common.www#getEvenementFiscalService()
	 */
	public EvenementFiscalService getEvenementFiscalService() {
		return evenementFiscalService;
	}

	/**
	 * @see ch.vd.uniregctb.evenement.common.www#setEvenementFiscalService(ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService)
	 */
	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setRegistrar(EvenementHandlerRegistrar registrar) {
		this.registrar = registrar;
	}

	public abstract GenericEvenementAdapter createAdapter();
	
	/**
	 * Permet de faire les verifications standards sur les adresses et les
	 * individus en cas de départ ou d'arrivée
	 * 
	 * @param target
	 * @param regroupementObligatoire en cas où le regroupement de deux membres d'un couple est obligatoire pour le movement
	 * @param erreurs
	 * @param warnings
	 */
	protected void verifierMouvementIndividu(Mouvement target, boolean regroupementObligatoire, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		final Mouvement mouvement = target;

		String message = null;

		/*
		 * Vérifie les adresses
		 */
		if (mouvement.getNouvelleAdressePrincipale() == null) {
			if (target instanceof Depart) {
				warnings.add(new EvenementCivilErreur("La nouvelle adresse principal de l'individu est vide"));
			}
			else if (target instanceof Arrivee) {
				erreurs.add(new EvenementCivilErreur("La nouvelle adresse principal de l'individu est vide"));
			}

		}

		if (mouvement.getNumeroOfsCommuneAnnonce() == null) {
			erreurs.add(new EvenementCivilErreur("La commune d'annonce est vide"));
		}
		/*
		 * Vérifie les individus
		 */
		final ServiceCivilService serviceCivil = getService().getServiceCivilService();
		final Individu individuPrincipal = mouvement.getIndividu();

		if (individuPrincipal == null) {
			if (target instanceof Depart) {
				message="Impossible de récupérer l'individu concerné par le départ";
			}else if (target instanceof Arrivee) {
				message="Impossible de récupérer l'individu concerné par l'arrivé";
			}

			erreurs.add(new EvenementCivilErreur(message));
		}
		else {
			final EtatCivil etatCivil = serviceCivil.getEtatCivilActif(mouvement.getIndividu().getNoTechnique(), mouvement.getDate());
			if (etatCivil == null) {
				erreurs.add(new EvenementCivilErreur("L'individu principal ne possède pas d'état civil à la date de l'événement"));
			}

			if (EtatCivilHelper.estMarieOuPacse(etatCivil)) {
				/*
				 * si l'individu est marié ou pacsé, on vérifie que le conjoint est spécifié de manière cohérente
				 */
				final Individu conjointDeIndividu =serviceCivil.getConjoint(mouvement.getIndividu().getNoTechnique(),mouvement.getDate());
				final Individu conjointDeMouvement = mouvement.getConjoint();

				if (conjointDeIndividu == null && conjointDeMouvement == null) {
					/*
					 * nous avons un marie seul (= dont le conjoint n'habite pas dans le canton) -> rien à faire
					 */
				}
				else if (conjointDeIndividu != null && conjointDeMouvement == null) {
					if (regroupementObligatoire && !EtatCivilHelper.estSepare(individuPrincipal.getEtatCivil(target.getDate()))) {
						if (target instanceof Depart) {
							message="L'évenement de départ du conjoint n'a pas été reçu";

						}
						else if (target instanceof Arrivee) {
							message="L'évenement d'arrivée du conjoint n'a pas été reçu";

						}
						erreurs.add(new EvenementCivilErreur(message));
					}
				}
				else if (conjointDeIndividu == null && conjointDeMouvement != null) {
					EvenementCivilErreur erreur = new EvenementCivilErreur(
							"Un conjoint est spécifié dans l'événement alors que l'individu principal n'en possède pas");
					erreurs.add(erreur);
				}
				else {
					/*
					 * erreur si l'id du conjoint reçu ne correspond pas à celui de l'état civil
					 */
					if (conjointDeIndividu.getNoTechnique() != conjointDeMouvement.getNoTechnique()) {
						EvenementCivilErreur erreur = new EvenementCivilErreur(
								"Mauvais regroupement : le conjoint déclaré dans l'événement et celui dans le registre civil diffèrent");
						erreurs.add(erreur);
					}
				}
			}
		}
	}

	/**
	 * Permet de faire les verifications standards sur les adresses et les
	 * individus en cas de départ ou d'arrivée
	 * 
	 * @param target
	 * @param erreurs
	 * @param warnings
	 */
	protected void verifierMouvementIndividu(Mouvement target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings){
		verifierMouvementIndividu(target, true, erreurs, warnings);
	}

	/**
	 * Ferme les adresses temporaires du tiers, ou des tiers s'il s'agit d'un ménage commun.
	 *
	 * @param contribuable
	 * @param date date de fermeture des adresses temporaires trouvées
	 */
	protected void fermeAdresseTiersTemporaire(Contribuable contribuable, RegDate date) {

		getService().fermeAdresseTiersTemporaire(contribuable, date);

		// fermeture des adresses des tiers
		if (contribuable instanceof MenageCommun) {
			for (PersonnePhysique pp : ((MenageCommun) contribuable).getPersonnesPhysiques()) {
				fermeAdresseTiersTemporaire(pp, date);
			}
		}
	}
}
