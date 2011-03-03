package ch.vd.uniregctb.evenement.civil.common;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseAutreTiers;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.engine.EvenementHandlerRegistrar;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseCivil;
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
	 * @see ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandler#setRegistrar(ch.vd.uniregctb.evenement.civil.engine.EvenementHandlerRegistrar)
	 */
	private EvenementHandlerRegistrar registrar;

	/**
	 * La DAO d'accès aux tiers
	 */
	private TiersDAO tiersDAO;

	protected DataEventService dataEventService;

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
	 * Validation commune l'objet target passé en paramètre.
	 *
	 * @param target
	 * @param erreurs
	 */
	protected abstract void validateSpecific(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings);

	/**
	 * @see ch.vd.uniregctb.evenement.civil.common.www#validate(ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne, java.util.List, java.util.List)
	 */
	public void validate(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		validateCommon(target, erreurs, warnings);
		if (erreurs.isEmpty()) {
			validateSpecific(target, erreurs, warnings);
		}
	}

	private void validateCommon(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {

		/*
		 * Vérifie que les éléments de base sont renseignés
		 */
		if (evenement.getDate() == null) {
			erreurs.add(new EvenementCivilExterneErreur("L'événement n'est pas daté"));
			return;
		}

		if (evenement.getNumeroOfsCommuneAnnonce() == null) {
			erreurs.add(new EvenementCivilExterneErreur("La commune d'annonce n'est pas renseignée"));
			return;
		}

		/*
		 * La date de l'événement se situe dans le futur.
		 */
		if (evenement.getDate().isAfter(RegDate.get())) {
			erreurs.add(new EvenementCivilExterneErreur("La date de l'événement est dans le futur"));
		}

		/*
		 * Cas particulier de l'arrivée ou de la naissance : le ou les contribuables ne sont en principe pas présents.
		 */
		if (isContribuableObligatoirementConnuAvantTraitement(evenement)) {
			/*
			 * Il n’existe pas de tiers contribuable correspondant à l’individu, assujetti ou non (mineur, conjoint) correspondant à
			 * l’individu.
			 */
			if (evenement.getPrincipalPPId() == null) {
				erreurs.add(new EvenementCivilExterneErreur("Aucun tiers contribuable ne correspond au numero d'individu " + evenement.getNoIndividu()));
			}
			
			/*
			 * Il n’existe pas de tiers contribuable correspondant au conjoint, assujetti ou non (mineur, conjoint) correspondant à
			 * l’individu.
			 */
			if (evenement.getNoIndividuConjoint() != null && evenement.getConjointPPId() == null) {
				erreurs.add(new EvenementCivilExterneErreur("Aucun tiers contribuable ne correspond au numero d'individu du conjoint " + evenement.getNoIndividuConjoint()));
			}
		}

		// en tout cas, l'individu devrait exister dans le registre civil !
		final Individu individu = evenement.getIndividu();
		if (individu == null) {
			erreurs.add(new EvenementCivilExterneErreur("L'individu est introuvable dans le registre civil!"));
		}
	}

	protected boolean isContribuableObligatoirementConnuAvantTraitement(EvenementCivilInterne evenement) {
		return evenement.isContribuablePresentBefore();
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
		adresse.setType(TypeAdresseCivil.COURRIER);
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
	protected PersonnePhysique getPersonnePhysiqueOrThrowException(Long noIndividu) throws EvenementCivilHandlerException {
		return getPersonnePhysiqueOrThrowException(noIndividu, false);
	}

	/**
	 * @param doNotAutoFlush
	 *            si vrai, les modifications courantes de la session hibernate ne sont pas flushées dans le base (évite d'incrémenter le
	 *            numéro de version, mais si l'habitant vient d'être créé et n'existe pas encore en base, il ne sera pas trouvé).
	 * @return l'habitant (ou ancien habitant) correspondant à son numéro d'individu.
	 * @throws EvenementCivilHandlerException
	 *             si aucun habitant (ou ancien habitant) ne correspond au numéro d'individu donné.
	 */
	protected PersonnePhysique getPersonnePhysiqueOrThrowException(Long noIndividu, boolean doNotAutoFlush) throws EvenementCivilHandlerException {
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
	protected PersonnePhysique getPersonnePhysiqueOrFillErrors(Long noIndividu, List<EvenementCivilExterneErreur> errors) {
		final PersonnePhysique habitant = getService().getPersonnePhysiqueByNumeroIndividu(noIndividu);
		if (habitant == null) {
			errors.add(new EvenementCivilExterneErreur("L'habitant avec le numéro d'individu = " + noIndividu
							+ " n'existe pas dans le registre."));
		}
		return habitant;
	}

	public static void addValidationResults(List<EvenementCivilExterneErreur> errors, List<EvenementCivilExterneErreur> warnings, ValidationResults resultat) {
		if (resultat.hasErrors()) {
			for (String erreur : resultat.getErrors()) {
				errors.add(new EvenementCivilExterneErreur(erreur));
			}
		}
		if (resultat.hasWarnings()) {
			for (String warning : resultat.getWarnings()) {
				warnings.add(new EvenementCivilExterneErreur(warning));
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
	 * @see ch.vd.uniregctb.evenement.civil.common.www#setService(ch.vd.uniregctb.tiers.TiersService)
	 */
	public void setService(TiersService service) {
		this.service = service;
	}

	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
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
	 * @see ch.vd.uniregctb.evenement.civil.common.www#getTiersDAO()
	 */
	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	/**
	 * @see ch.vd.uniregctb.evenement.civil.common.www#setTiersDAO(ch.vd.uniregctb.tiers.TiersDAO)
	 */
	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	/**
	 * @see ch.vd.uniregctb.evenement.civil.common.www#getEvenementFiscalService()
	 */
	public EvenementFiscalService getEvenementFiscalService() {
		return evenementFiscalService;
	}

	/**
	 * @see ch.vd.uniregctb.evenement.civil.common.www#setEvenementFiscalService(ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService)
	 */
	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setRegistrar(EvenementHandlerRegistrar registrar) {
		this.registrar = registrar;
	}

	public abstract EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException;

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
			final Set<PersonnePhysique> pps = service.getPersonnesPhysiques((MenageCommun) contribuable);
			for (PersonnePhysique pp : pps) {
				fermeAdresseTiersTemporaire(pp, date);
			}
		}
	}
}
