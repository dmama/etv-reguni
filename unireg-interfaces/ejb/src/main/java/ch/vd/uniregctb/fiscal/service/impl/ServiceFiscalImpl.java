package ch.vd.uniregctb.fiscal.service.impl;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.common.model.Adresse;
import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.common.model.impl.AdresseImpl;
import ch.vd.ifosdi.metier.exceptions.BusinessException;
import ch.vd.ifosdi.metier.registre.AdresseCourrierSDI;
import ch.vd.ifosdi.metier.registre.ContribuableSDI;
import ch.vd.ifosdi.metier.registre.DeclarationSDI;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.infrastructure.service.ServiceInfrastructure;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.civil.model.EtatCivil;
import ch.vd.registre.civil.model.Individu;
import ch.vd.registre.civil.model.impl.EtatCivilImpl;
import ch.vd.registre.civil.model.impl.HistoriqueIndividuImpl;
import ch.vd.registre.civil.model.impl.IndividuImpl;
import ch.vd.registre.civil.service.ServiceCivil;
import ch.vd.registre.common.model.CoordonneesFinancieres;
import ch.vd.registre.common.model.InstitutionFinanciere;
import ch.vd.registre.common.model.impl.CoordonneesFinancieresImpl;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.registre.fiscal.model.Contribuable;
import ch.vd.registre.fiscal.model.ContribuableFoyer;
import ch.vd.registre.fiscal.model.ContribuableRetourInfoDi;
import ch.vd.registre.fiscal.model.DeclarationQuittance;
import ch.vd.registre.fiscal.model.EnumCodeRetourDeclarationQuittance;
import ch.vd.registre.fiscal.model.EnumCritereRechercheContribuable;
import ch.vd.registre.fiscal.model.EnumTypeAssujettissement;
import ch.vd.registre.fiscal.model.EnumTypeImposition;
import ch.vd.registre.fiscal.model.For;
import ch.vd.registre.fiscal.model.RechercherNoContribuable;
import ch.vd.registre.fiscal.model.ResultatRechercheContribuable;
import ch.vd.registre.fiscal.model.ResultatRechercherNoContribuable;
import ch.vd.registre.fiscal.model.impl.AssujettissementImpl;
import ch.vd.registre.fiscal.model.impl.ContribuableFoyerImpl;
import ch.vd.registre.fiscal.model.impl.ContribuableImpl;
import ch.vd.registre.fiscal.model.impl.ContribuableIndividuImpl;
import ch.vd.registre.fiscal.model.impl.ContribuableRetourInfoDiImpl;
import ch.vd.registre.fiscal.model.impl.DeclarationImpotImpl;
import ch.vd.registre.fiscal.model.impl.ForImpl;
import ch.vd.registre.fiscal.model.impl.ResultatRechercheContribuableImpl;
import ch.vd.registre.fiscal.model.impl.ResultatRechercherNoContribuableImpl;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.evenement.common.EnsembleTiersCouple;
import ch.vd.uniregctb.fiscal.helper.ContribuableUniregHelper;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.service.ServiceCivilException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.situationfamille.VueSituationFamille;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.webservices.tiers.SearchTiers;
import ch.vd.uniregctb.webservices.tiers.TiersInfo;
import ch.vd.uniregctb.webservices.tiers.TiersInfoArray;
import ch.vd.uniregctb.webservices.tiers.TiersPort;
import ch.vd.uniregctb.webservices.tiers.UserLogin;


/**
 * Classe implémentant les différents appels au businness de Unireg CTB.
 *
 * @author Baba NGOM (xsibnm)
 * @version $Revision: 1.0 $
 */
public class ServiceFiscalImpl {

	/** Le loggeur log4j. */
	private static final Logger LOGGER = Logger.getLogger(ServiceFiscalImpl.class);
	private static final Logger LOGGER_ERROR = Logger.getLogger(LOGGER.getName() + ".err");

	/**
	 * La DAO d'accès aux tiers
	 */
	private TiersDAO tiersDAO;
	private ModeleDocumentDAO modeleDocumentDAO;
	private PeriodeFiscaleDAO periodeFiscaleDAO;
	/**
	 * Service d'accès au infrastructure
	 */
	private ServiceInfrastructure serviceInfrastructure;

	/**
	 * Service d'accès au civil
	 */
	private ServiceCivil serviceCivil;

	/**
	 * Service d'accès aux adresse du contribuable
	 */
	private AdresseService adresseService;

	/**
	 * Service sur les tiers
	 */
	private TiersService tiersService;

	/**
	 * Service sur les situations de famille
	 */
	private SituationFamilleService situationFamilleService;

	/**
	 * Service pour les declarations d'impots
	 */
	private DeclarationImpotService diService;

	private HibernateTemplate hibernateTemplate;

	private IbanValidator ibanValidator;

	private TiersPort serviceTiersPort;






	private UserLogin login;



	// requete de Recherhche des contribuable foyer pour un contribuable individu
	//le rapport doit être valide entre les dates de recherches
	//la date de debut du rapport doit être inférieur ou egal a la date de fin de recherche
	//la date de fin de rapport doit être nulle ou superieur a la date de début de recherche


	final private static String menageFoyer = // --------------------------------
	"SELECT rapport.objet.numero                                            "
			+ "FROM                                                         "
			+ "     RapportEntreTiers as rapport                            "
			+ "WHERE                                                        "
			+ "     rapport.sujet.numero = :numeroContribuable AND          "
			+ "     rapport.dateDebut <= :fin AND                         "
			+ "    (rapport.dateFin is null OR rapport.dateFin >= :debut) AND "
			+ "     rapport.class = AppartenanceMenage                      "
			+ "ORDER BY rapport.objet.numero ASC                            ";

	// requete de Recherhche des contribuable modifiees

	// requete pour choisir 1000 contribuables

	// requete de Recherhche des contribuable sans DI sur une période



	final private static String contribuablesSansDI = // --------------------------------
		"SELECT DISTINCT tiers.id                                                               "
				+ "FROM                                                                         "
				+ "     Tiers as tiers                                                          "
				+ "INNER JOIN                                                                   "
			    + "     tiers.forsFiscaux AS forFiscal                                          "
				+ "WHERE                                                                        "
				+ "     tiers.id >= :contribuableStart                                          "
				+ " AND forFiscal.dateDebut < :dateRecherche                                    "
				+ " AND forFiscal.annulationDate is NULL                                        "
				+ " AND (forFiscal.dateFin is null OR forFiscal.dateFin >= :dateRecherche)      "
				+ " AND                                                                         "
				+ "   0 =  (                                                                    "
				+ "            SELECT COUNT(declaration)                                        "
				+ "                   FROM                                                      "
				+ "                       DeclarationImpotOrdinaire as declaration              "
				+ "                   WHERE                                                     "
				+ "                        declaration.tiers.id =  tiers.id                     "
				+ "                   AND  declaration.periode.annee = :annee                   "
				+ "                   AND  declaration.annulationDate is NULL                   "
				+ "       )                                                                     "
				+ "ORDER BY tiers.id asc                                                        ";





	/** package visibility (instead of private) in order to enable unit-test */
	static void assertReceivedDataRelatesToInputParameterGetContribuable(long noContribuable,
			int annee, boolean recupererContribuableFoyer,
			boolean throwsException, boolean retrieveI107,
			Contribuable result) throws RegistreException {
		// Assert received data relates to input parameter
		if(result != null) {
			final long noContribuableFromUnireg = result.getNoContribuable();
			// Assert received data relates to input parameter
			if(noContribuable != noContribuableFromUnireg) {
				boolean foundMatch = false;
				if(result  instanceof ContribuableFoyer) {
					final Collection membres = ((ContribuableFoyer)result).getMembres();
					if(membres != null) {
						for (Iterator iterator = membres.iterator(); iterator
								.hasNext();) {
							Object myObject = iterator.next();
							if (myObject instanceof Contribuable) {
								Contribuable contribuable = (Contribuable) myObject;
								if ( contribuable != null ) {
									if( noContribuable == contribuable.getNoContribuable() ) {
										foundMatch = true;
										break;
									}
								}
							}
						}
					}
				}
				if( !foundMatch ) {
					final String nomCourrier1 = result.getNomCourrier1();
					final String msg = "Incoherence entre la donnee en entree[" + noContribuable + "] et le resultat[" + noContribuableFromUnireg + "], nomCourrier1[" + nomCourrier1 + "]. Autres parametres: annee[" + annee + "], recupererContribuableFoyer[" + recupererContribuableFoyer + "], throwsException[" + throwsException + "], retrieveI107[" + retrieveI107 + "]";
			        LOGGER_ERROR.error("ServiceFiscalImpl:getContribuable;" + msg);
					throw new RegistreException(msg);
				}
			}
		}
	}



	/**
	 * @see ServiceFiscalBean#getAssujettissements(long, int)
	 *
	 * @param noContribuable
	 *            le numéro du contribuable.
	 * @param annee
	 *            l'année de validité.
	 * @return la liste des assujettissements, valides durant l'année, du contribuable.
	 * @throws RegistreException
	 *             si aucun contribuable, pour l'année donnée, n'est identifié par le numéro en paramètre ou si un problème métier survient
	 *             lors de l'invocation du service.
	 */
	public Collection getAssujettissements(long noContribuable, int annee) throws RegistreException {
		Collection result = new ArrayList();

		return result;
	}

	/**
	 * @see ServiceFiscalBean#getContribuable(long, int, boolean)
	 *
	 * @param noContribuable
	 *            le numéro du contribuable.
	 * @param annee
	 *            l'année de validité.
	 * @param recupererContribuableFoyer
	 *            indique si le contribuable foyer correspondant au
	 * @return le contribuable populé avec les données de l'année de validité.
	 * @throws RegistreException
	 *             si un problème métier survient lors de l'invocation du service.
	 * @throws RemoteException
	 */

	/**
	 * @param noContribuable
	 * @param annee
	 * @param recupererContribuableFoyer
	 * @param throwsException
	 * @return
	 * @throws RegistreException
	 */
	@Transactional(readOnly = true)
	public Contribuable getContribuable(long noContribuable, int annee, boolean recupererContribuableFoyer, boolean throwsException,boolean retrieveI107)
			throws RegistreException {

		ch.vd.uniregctb.tiers.Contribuable contribuableUnireg = getTiersDAO().getContribuableByNumero(noContribuable);
		// Un contribuable a été trouvé
		if (contribuableUnireg != null) {
			if (!retrieveI107 && contribuableUnireg.isDebiteurInactif()) {
				throw new RegistreException("Ce contribuable est considéré comme inexistant car il est inactif");
			}

			ContribuableImpl result = null;


			Range periodeRecherche = setUpPeriodeRecherche(annee);

			try {
				result = (ContribuableImpl) getContribuableInfoGenerale(noContribuable, periodeRecherche, contribuableUnireg,
						recupererContribuableFoyer);
			}
			catch (RegistreException ex) {
				if (throwsException || result == null) {
					throw ex;
				}
			}

			/**
			 * RÃ©cupÃ©ration, si existant, des informations sur le contribuable principal.
			 */

			ContribuableIndividuImpl contribuablePrincipal = null;
			IndividuImpl individuPrincipal = null;
			Individu individuFromCivil = null; // Individu complet
			EnsembleTiersCouple ensembleCouple = null;
			PersonnePhysique personnePhysiquePrincipale = null;
			ContribuableIndividuImpl contribuableConjoint = null;
			PersonnePhysique personnePhysiqueConjoint = null;
			IndividuImpl individuConjoint = null;
			long numeroIndividu = 0;

			/** RÃ©cupÃ©ration de l'individu principal. */

			/** Recherche de l'individu principal */

			// Personne physique
			if (contribuableUnireg instanceof PersonnePhysique) {
				personnePhysiquePrincipale = (PersonnePhysique) contribuableUnireg;
				ensembleCouple = tiersService.getEnsembleTiersCouple(personnePhysiquePrincipale, periodeRecherche.getDateFin());
				// si l'indivu appartient a un couple, et qu'on doit ramener le foyer
				// on ramène le contribuable principal du couple
				if (ensembleCouple != null) {
					personnePhysiquePrincipale = ensembleCouple.getPrincipal();
					// dans le cas d'un habitant on recherche son numero d'indivdu
					if (personnePhysiquePrincipale.isHabitant()) {

						numeroIndividu = personnePhysiquePrincipale.getNumeroIndividu();
					}
					else {
						numeroIndividu = 0;
					}
				}
				// Cas d'un habitant celibataire ou lorsque l'on ne recupere pas le foyer
				else if (ensembleCouple == null && personnePhysiquePrincipale.isHabitant()) {
					numeroIndividu = personnePhysiquePrincipale.getNumeroIndividu();
				}

			}
			// Menage
			else if (contribuableUnireg instanceof MenageCommun) {
				ensembleCouple = tiersService.getEnsembleTiersCouple((MenageCommun) contribuableUnireg, periodeRecherche.getDateFin());

				personnePhysiquePrincipale = ensembleCouple.getPrincipal();
				// dans le cas d'un habitant on recherche son numero d'indivdu
				if (personnePhysiquePrincipale != null && personnePhysiquePrincipale.isHabitant()) {

					numeroIndividu = personnePhysiquePrincipale.getNumeroIndividu();
				}
				else {
					numeroIndividu = 0;
				}

			}

			// verifier que le contribuable n'est pas un non habitant
			if (!contribuableUnireg.getNatureTiers().equals(ch.vd.uniregctb.tiers.Contribuable.NATURE_NONHABITANT)) {

				try {

					individuFromCivil = serviceCivil.getIndividu(numeroIndividu, RegDate.get().year());
				}
				catch (ServiceCivilException e) {
					throw new RegistreException("Problème durant la recherche des informations dans le registre civil  ", e);
				}
				catch (RemoteException e) {

					throw new RegistreException("Problème d'accés au service civile distant  ", e);
				}
				if (individuFromCivil != null) {
					individuPrincipal = setUpIndividu(individuFromCivil, personnePhysiquePrincipale, periodeRecherche);
				}
			}

			/** RÃ©cupÃ©ration du contribuable principal. */
			contribuablePrincipal = new ContribuableIndividuImpl();
			if (personnePhysiquePrincipale != null) {
				contribuablePrincipal.setNoContribuable(personnePhysiquePrincipale.getNumero());
			}
			contribuablePrincipal.setEntiteCivile(individuPrincipal);

			/** RÃ©cupÃ©ration, si existant, des informations sur le conjoint. */
			if (ensembleCouple != null) {

				personnePhysiqueConjoint = ensembleCouple.getConjoint();
				if (personnePhysiqueConjoint != null && personnePhysiqueConjoint.isHabitant()) {

					/** RÃ©cupÃ©ration de l'individu conjoint. */
					individuConjoint = new IndividuImpl();
					Individu individuConjointFromCivil = null;

					try {
						individuConjointFromCivil = serviceCivil.getIndividuEtendu(personnePhysiqueConjoint.getNumeroIndividu(), periodeRecherche
								.getDateFin().year());

					}
					catch (ServiceCivilException e) {
						throw new RegistreException("Problème durant la recherche des informations dans le registre civil  ", e);
					}
					catch (RemoteException e) {

						throw new RegistreException("Problème d'accés au service civile distant  ", e);
					}
					if (individuConjointFromCivil != null) {
						individuConjoint = setUpIndividu(individuConjointFromCivil, personnePhysiqueConjoint, periodeRecherche);
						individuConjoint.setConjoint(individuPrincipal);
					}
				}
				/** RÃ©cupÃ©ration du contribuable conjoint. */
				contribuableConjoint = new ContribuableIndividuImpl();
				if (personnePhysiqueConjoint != null) {
					contribuableConjoint.setNoContribuable(personnePhysiqueConjoint.getNumero());
				}

				contribuableConjoint.setEntiteCivile(individuConjoint);
			}

			/** RÃ©cupÃ©ration, si existant, des information sur les enfants. */
			List individusEnfants = new ArrayList();
			List contribuablesEnfants = new ArrayList();
			if (individuFromCivil != null && individuFromCivil.getEnfants() != null) {

				/** ItÃ©ration sur le tableau d'individus enfants. */
				Iterator iteratorEnfant = individuFromCivil.getEnfants().iterator();

				while (iteratorEnfant.hasNext()) {
					/** Recuperation de l'individu enfant */
					Individu enfantCourant = (Individu) iteratorEnfant.next();
					//l'historique enfants est sensé être complété par le  registre civil
					/** Récupération du contribuable enfant. */
					ContribuableIndividuImpl contribuableEnfant = new ContribuableIndividuImpl();
					PersonnePhysique personnePhysiqueEnfant = getTiersDAO().getHabitantByNumeroIndividu(enfantCourant.getNoTechnique());
					if (personnePhysiqueEnfant != null) {
						contribuableEnfant.setNoContribuable(personnePhysiqueEnfant.getNumero());
						contribuableEnfant.setEntiteCivile(enfantCourant);
						contribuablesEnfants.add(contribuableEnfant);
					}

					/**
					 * Ajout du contribuable enfant et de l'individu enfant aux listes.
					 */
					individusEnfants.add(enfantCourant);

				}
			}

			/** mise Ã  jour des attributs de l'individu principal. */
			if (individuPrincipal != null) {
				individuPrincipal.setEnfants(individusEnfants);
				individuPrincipal.setConjoint(individuConjoint);
			}

			final boolean isContribuableFoyer = true;
			if (isContribuableFoyer) {
				/** Definition des membres du foyer. */
				List<Contribuable> membres = new ArrayList<Contribuable>();
				if (contribuablePrincipal != null)
					membres.add(contribuablePrincipal);
				membres.addAll(contribuablesEnfants);
				if (contribuableConjoint != null) {
					membres.add(contribuableConjoint);
				}
				((ContribuableFoyerImpl) result).setMembres(membres);

				((ContribuableFoyerImpl) result).setPrincipal(contribuablePrincipal);
				result.setEntiteCivile(individuPrincipal);
			}
			else {
				result.setEntiteCivile(individuPrincipal);
			}
			// Cross-Check in order to avoid propagation of potential data incoherence comming from Proxy (or Java code above)
			assertReceivedDataRelatesToInputParameterGetContribuable(noContribuable, annee,
					recupererContribuableFoyer, throwsException, retrieveI107,result);

			return result;
		}
		else {
			return null;
		}

	}

	/**
	 * ramene les infos communes à contribuable menage ou individu
	 *
	 * @param noContribuable
	 * @param annee
	 * @param recupererContribuableFoyer
	 * @return
	 * @throws RegistreException
	 */
	private Contribuable getContribuableInfoGenerale(long noContribuable, Range periodeRecherche,
			ch.vd.uniregctb.tiers.Contribuable contribuableUnireg, boolean recupererContribuableFoyer) throws RegistreException {

		ContribuableImpl result = null;
		ForImpl dernierFor = null;
		ForFiscalPrincipal forPrincipal = null;

		// Un habitant a été trouvé
		if (contribuableUnireg != null) {

			result = new ContribuableFoyerImpl();

			// On ramène le couple s'il existe et si on le demande
			if (recupererContribuableFoyer) {
				contribuableUnireg = ContribuableUniregHelper.resolveContribuable(contribuableUnireg, periodeRecherche,tiersService);
			}
			result.setNoContribuable(contribuableUnireg.getNumero());

			forPrincipal = contribuableUnireg.getDernierForFiscalPrincipal();

			/**
			 * Recuperation, si existant, du dernier assujettissement et de la dernière déclaration d'impots du contribuable.
			 */
			AssujettissementImpl dernierAssujettissement = null;
			Range dateAssujetissementUnireg = null;
			try {
				dateAssujetissementUnireg = ContribuableUniregHelper.getDateAssujetissement(contribuableUnireg, periodeRecherche);
			}
			catch (AssujettissementException e1) {
				LOGGER_ERROR.info("Erreur durant le calcul de la date de debut d'assujetissement:  du contribuable "+noContribuable+" " +e1.getMessage());
				throw new RegistreException("Erreur durant le calcul de la date de debut d'assujetissement: "+ e1.getMessage());

			}

			// Si le contribuable est assujetti sur la période
			if (dateAssujetissementUnireg.getDateDebut() != null) {

				/**
				 * RÃ©cupÃ©ration, si existante, de la derniÃ¨re dÃ©claration d'impÃ´t du contribuable.
				 */
				DeclarationImpotImpl derniereDeclarationImpot = null;
				// on recherche la declaration d'impot pour la période N
				Declaration derniereDeclarationUnireg = ContribuableUniregHelper.getDerniereDeclaration(contribuableUnireg,
						periodeRecherche);
				if (derniereDeclarationUnireg != null) {

					derniereDeclarationImpot = new DeclarationImpotImpl();

					derniereDeclarationImpot.setDateDernierEtat(derniereDeclarationUnireg.getDernierEtat().getDateDebut().asJavaDate());

					derniereDeclarationImpot.setDernierEtatDeclarationImpot(ContribuableUniregHelper
							.convertEtatDeclaration(derniereDeclarationUnireg.getDernierEtat().getEtat()));
					derniereDeclarationImpot.setPeriodeFiscale(derniereDeclarationUnireg.getPeriode().getAnnee());
				}

				dernierAssujettissement = new AssujettissementImpl();
				// pas applicable pour unireg
				dernierAssujettissement.setNoSequence(0);
				// Début de période

				dernierAssujettissement.setDateDebut(dateAssujetissementUnireg.getDateDebut().asJavaDate());

				if (dateAssujetissementUnireg.getDateFin() != null) {

					dernierAssujettissement.setDateFin(dateAssujetissementUnireg.getDateFin().asJavaDate());
				}
				else {
					dernierAssujettissement.setDateFin(null);
				}

				/**
				 * Selon la description du proxy, il s'agit de l'assujettissement LILIC.
				 */
				dernierAssujettissement.setTypeAssujettissement(EnumTypeAssujettissement.LILIC);
				dernierAssujettissement.setDerniereDeclarationImpot(derniereDeclarationImpot);
			}
			// construction du dernier for
			if (forPrincipal!=null) {
				dernierFor = ContribuableUniregHelper.convertFor(forPrincipal, contribuableUnireg, serviceInfrastructure,tiersService);
			}


			/** Informations complementaires */

			// Recuperations des informations courrier du contribuables
			AdresseEnvoiDetaillee adresseEnvoi = null;
			try {
				adresseEnvoi = adresseService.getAdresseEnvoi(contribuableUnireg, RegDate.get());

			}
			catch (AdressesResolutionException e) {
				LOGGER_ERROR.info("Erreur lors du chargement de l'adresse du contribuable "+noContribuable+" " +e.getMessage());
				throw new RegistreException("Erreur lors du chargement de l'adresse du contribuable: "+ e.getMessage());
			}
			if (adresseEnvoi!=null) {
				result.setFormuleDePolitesse(adresseEnvoi.getSalutations());
				if (adresseEnvoi.getNomPrenom()!=null && !adresseEnvoi.getNomPrenom().isEmpty()) {
					result.setNomCourrier1(adresseEnvoi.getNomPrenom().get(0));
					result.setNomCourrier2("");
					if (adresseEnvoi.getNomPrenom().size() > 1) {

						result.setNomCourrier2(adresseEnvoi.getNomPrenom().get(1));

					}
					else if (adresseEnvoi.getComplement()!= null) {
						result.setNomCourrier2(adresseEnvoi.getComplement());
					}
				}

				result.setEmail(StringUtils.strip(contribuableUnireg.getAdresseCourrierElectronique()));
				result.setNumeroTelephoniqueFixe(StringUtils.strip(contribuableUnireg.getNumeroTelephonePrive()));
				result.setNumeroTelephoniquePortable(StringUtils.strip(contribuableUnireg.getNumeroTelephonePortable()));
			}


			/** RÃ©cupÃ©ration de l'adresse du contribuable. */
			AdresseImpl adresse = new AdresseImpl();
			AdresseGenerique adresseCourrierUnireg = null;

			try {

				adresseCourrierUnireg = adresseService.getAdressesFiscales(contribuableUnireg, RegDate.get()).courrier;
				//ON recupère la dernière adresse connue
				if (adresseCourrierUnireg == null) {

					adresseCourrierUnireg = adresseService.getAdressesFiscales(contribuableUnireg, null).courrier;
				}


			}
			catch (AdressesResolutionException e) {
				LOGGER_ERROR.info("Problème durant la resolution de l'adresse contribuable "+noContribuable+" " +e.getMessage());
				throw new RegistreException("Problème durant la resolution de l'adresse contribuable dans Unireg: "+ e.getMessage());
			}


			// Si l'adresse existe
			if (adresseCourrierUnireg != null) {

				// on expose toujours l'adresse courrier la plus récente, ce qui signifie qu'il n'est pas possible de connaître les adresses
				// courrier plus anciennes et donc que toutes notions de date de début ou de fin est caduque.
				adresse.setDateDebutValidite(null);
				adresse.setDateFinValidite(null);

				// Récupération de la localité postale
				adresse.setLocaliteAbregeMinuscule(adresseCourrierUnireg.getLocalite());
				adresse.setLocaliteCompletMinuscule(adresseCourrierUnireg.getLocaliteComplete());

				if (adresseCourrierUnireg.getNumero() == null) {
					adresse.setNumero("");
				}
				else {
					adresse.setNumero(adresseCourrierUnireg.getNumero());
				}

				adresse.setNumeroOrdrePostal(adresseCourrierUnireg.getNumeroOrdrePostal());
				adresse.setNumeroPostal(adresseCourrierUnireg.getNumeroPostal());
				if (adresseCourrierUnireg.getNumeroPostalComplementaire() != null
						&& !adresseCourrierUnireg.getNumeroPostalComplementaire().equals("0")) {
					adresse.setNumeroPostalComplementaire(adresseCourrierUnireg.getNumeroPostalComplementaire());
				}

				if (adresseCourrierUnireg.getRue() == null) {
					adresse.setRue("");
				}
				else {
					adresse.setRue(adresseCourrierUnireg.getRue());
				}


				/**
				 * L'adresse retournée est toujours l'adresse courrier du contribuable.
				 */
				adresse.setTypeAdresse(EnumTypeAdresse.COURRIER);

				/** Si c'est une adresse en suisse alors pas de pays */
				if (adresseCourrierUnireg.getNoOfsPays() == ServiceInfrastructureService.noOfsSuisse) {
					adresse.setPays(null);
				}
				else {

					/** RÃ©cupération du pays depuis Infrastructure. */
					try {
						adresse.setPays(serviceInfrastructure.getPays(adresseCourrierUnireg.getNoOfsPays()));
					}
					catch (InfrastructureException e) {
						throw new RegistreException("Problème durant la récupération du pays", e);
					}
					catch (RemoteException e) {
						throw new RegistreException("Problème d'accès distant durant la récupération du pays", e);
					}
					//JIRA UNIREG-1099
					AdresseEtrangere adresseEtrangere = (AdresseEtrangere) contribuableUnireg.getAdresseActive(TypeAdresseTiers.COURRIER, RegDate.get());
					if (adresseEtrangere!=null) {
						adresse.setLocaliteAbregeMinuscule(adresseEtrangere.getNumeroPostalLocalite());
						adresse.setLocaliteCompletMinuscule(adresseEtrangere.getNumeroPostalLocalite());
					}


				}
				result.setAdresse(adresse);
			}
			// on vérifie la cohérence de la période

			if (dernierAssujettissement != null && dernierAssujettissement.getDateFin() != null) {
				if (dernierAssujettissement.getDateDebut().before(dernierAssujettissement.getDateFin())) {

					result.setDernierAssujettissement(dernierAssujettissement);
				}
			}
			else {
				result.setDernierAssujettissement(dernierAssujettissement);
			}

			result.setDernierFor(dernierFor);
			if (contribuableUnireg.getBlocageRemboursementAutomatique() != null) {
				result.setCodeBlocageRmbtAuto(contribuableUnireg.getBlocageRemboursementAutomatique());
			}
			result.setTypeContribuable(ContribuableUniregHelper.getTypeContribuable(contribuableUnireg, periodeRecherche.getDateFin()));

		}

		return result;
	}

	/**
	 * @see ServiceFiscalBean#getNoContribuableFoyer(long, int, int)
	 *
	 * @param noContribuableIndividu
	 *            le numéro de contribuable individu.
	 * @param anneeDebut
	 *            l'année début de recherche. Si la valeur de ce paramètre est égale à <code>0</code>, aucune filtre n'est appliquée sur
	 *            l'année de début de recherche.
	 * @param anneeFin
	 *            l'année fin de recherche.Si la valeur de ce paramètre est égale à <code>0</code>, aucune filtre n'est appliquée sur
	 *            l'année de fin de recherche.
	 * @return la liste des numéros de contribuable foyer auxquels le contribuable individu à participé.
	 * @throws RegistreException
	 *             si un problème métier survient lors de l'invocation du service.
	 */
	public Collection getNoContribuableFoyer(long noContribuableIndividu, int anneeDebut, int anneeFin) throws RegistreException {

		RegDate debutAnnee = null;
		RegDate finAnnee = null;

		if (anneeDebut == 0) {
			debutAnnee = RegDate.getEarlyDate();
		}
		else {
			debutAnnee = RegDate.get(anneeDebut, 1, 1);
		}

		if (anneeFin == 0) {
			finAnnee = RegDate.getLateDate();

		}
		else {
			finAnnee = RegDate.get(anneeFin, 12, 31);
		}

		final long numero = noContribuableIndividu;
		final int debut = debutAnnee.index();
		final int fin = finAnnee.index();

		final List<Long> result = (List<Long>) hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.createQuery(menageFoyer);
				queryObject.setParameter("numeroContribuable", numero);
				queryObject.setParameter("debut", debut);
				queryObject.setParameter("fin", fin);
				return queryObject.list();
			}
		});

		return result;

	}

	/**
	 * @see ServiceFiscalBean#getCoordonneesFinancieres(long)
	 *
	 * @param noContribuable
	 *            le numéro du contribuable.
	 * @return les coordonnées financières, valides durant l'année, du contribuable.
	 * @throws RegistreException
	 *             si aucun contribuable, pour l'année donnée, n'est identifié par le numéro en paramètre ou si un problème métier survient
	 *             lors de l'invocation du service.
	 */
	@SuppressWarnings("unchecked")
	public CoordonneesFinancieres getCoordonneesFinancieres(long noContribuable,boolean retrieveI107) throws RegistreException {

		CoordonneesFinancieresImpl coordonneesFinancieres = null;
		//Si l'on est en presence d'un iban malformée, on n'est pas en mesure de rechercherun
		//numero de clearing et une institution financiere
		boolean ibanCorrect=true;

		/*
		 * Récupération, si existant, des coordonnées financiÃ¨res du contribuable.
		 */
		ch.vd.uniregctb.tiers.Contribuable contribuableUnireg = getTiersDAO().getContribuableByNumero(noContribuable);
		if (contribuableUnireg != null) {
			if (!retrieveI107 && contribuableUnireg.isDebiteurInactif()) {
				throw new RegistreException("Ce contribuable est considéré comme inexistant car il est inactif");
			}
			if(contribuableUnireg.getNumeroCompteBancaire()!=null){
				coordonneesFinancieres = new CoordonneesFinancieresImpl();
				coordonneesFinancieres.setDesignationTitulaireCompte(contribuableUnireg.getTitulaireCompteBancaire());
				//coordonneesFinancieres.setNoCompte(contribuableUnireg.getNumeroCompteBancaire());
				try {
					ibanValidator.validate(contribuableUnireg.getNumeroCompteBancaire());
				} catch (Exception e) {
					ibanCorrect=false;
				}
				// par définition, on ne stocke que le format IBAN dans Unireg
				coordonneesFinancieres.setIBAN(true);
				coordonneesFinancieres.setNoIBAN(contribuableUnireg.getNumeroCompteBancaire());

				coordonneesFinancieres.setNoBIC(contribuableUnireg.getAdresseBicSwift());
				if (ibanCorrect) {
					coordonneesFinancieres.setNoClearing(ibanValidator.getClearing(contribuableUnireg.getNumeroCompteBancaire()));

					try {
						List<InstitutionFinanciere> list = serviceInfrastructure.getInstitutionsFinancieres(coordonneesFinancieres.getNoClearing());
						if (list.isEmpty()) {
							//on recherche le numero en enlevant les 00 en debut de noClearing
							String clearing = StringUtils.stripStart(coordonneesFinancieres.getNoClearing(), "0");
							list = serviceInfrastructure.getInstitutionsFinancieres(clearing);
						}

						if (!list.isEmpty()) {

							// on peut trouver plusieurs institutions, mais laquelle choisir ?
							// la première ne semble pas un choix plus bête qu'un autre...
							final InstitutionFinanciere institution = list.get(0);
							coordonneesFinancieres.setInstitutionFinanciere(institution);
						}
					}
					catch (InfrastructureException ignored) {
						throw new RegistreException("Problème dans le service infrastructure distant  ", ignored);
					}
					catch (RemoteException e) {

						throw new RegistreException("Problème d'accés au service infrastructure distant  ", e);
					}

				}

			}
			else {
				LOGGER.info("pas de numero de compte pour le ctb "+noContribuable);
			}
		}
		else {
			LOGGER_ERROR.error("RECHERCHE DE COORDONNEES FINANCIERES: Le contribuable "+noContribuable+" n'est pas dans unireg");
		}
		return coordonneesFinancieres;
	}

	/**
	 * @see ServiceFiscalBean#getFors(long, int)
	 *
	 * @param noContribuable
	 *            le numéro du contribuable.
	 * @param annee
	 *            l'année de validité.
	 * @return la liste des fors, valides durant l'année, du contribuable. *
	 * @throws RegistreException
	 *             si aucun contribuable, pour l'année donnée, n'est identifié par le numéro en paramètre ou si un problème métier survient
	 *             lors de l'invocation du service.
	 */
	@Transactional(readOnly = true)
	public Collection getFors(long noContribuable, int annee) throws RegistreException {
		ch.vd.uniregctb.tiers.Contribuable contribuableUnireg = getTiersDAO().getContribuableByNumero(noContribuable);

		Range periodeRecherche = setUpPeriodeRecherche(annee);
		/** recherche du menageCommun s'il existe */
		// contribuableUnireg=ContribuableUniregHelper.resolveContribuable(contribuableUnireg, periodeRecherche);
		/** Collection de fors, rÃ©sultat de la fonction. */
		Collection fors = null;
		if (contribuableUnireg == null) {
			throw new RegistreException("Problème durant la récupération des fors du contribuable identifié par le numéro "
					+ noContribuable + " pour l'année " + annee + ". Aucun contribuable ne correspond à  cet identifiant.");
		}
		else {

			/** Collection de fors, résultat de la fonction. */
			fors = new ArrayList();

			/** Récupération des fors. */

			for (ForFiscal f : contribuableUnireg.getForsFiscauxSorted()) {
				// For non annulé ayant été valide sur la période de recherche
				if (!f.isAnnule() && DateRangeHelper.intersect(f, periodeRecherche) == true) {
					if (f instanceof ForFiscalRevenuFortune) {
						ForFiscalRevenuFortune forRevenuFortune = (ForFiscalRevenuFortune) f;
						ForImpl unFor = ContribuableUniregHelper.convertFor(forRevenuFortune, contribuableUnireg, serviceInfrastructure,
								tiersService);
						fors.add(unFor);
					}
					else if (f instanceof ForFiscalAutreImpot) {
						ForFiscalAutreImpot forAutreImpot = (ForFiscalAutreImpot) f;
						ForImpl unFor = ContribuableUniregHelper.convertForAutreImpot(forAutreImpot, contribuableUnireg,
								serviceInfrastructure, tiersService);
						fors.add(unFor);
					}

				}
			}
		}

		return fors;

	}

	public boolean isContribuableI107(long noContribuable) throws RemoteException, RegistreException {

		ch.vd.uniregctb.tiers.Contribuable contribuableUnireg = getTiersDAO().getContribuableByNumero(noContribuable);
		if (contribuableUnireg != null) {
			return contribuableUnireg.isDebiteurInactif();
		}
		else {
			throw new RegistreException("le contribuable " + noContribuable + " est inconnue dans le registe fiscale");
		}

	}

	/**
	 * @see ServiceFiscalBean#rechercherContribuables(HashMap, int)
	 *
	 * @param criteresRecherche
	 *            les critères de recherche de contribuables. Cet objet contient un ensemble de couples clé et valeur, les différentes clés
	 *            étant définies par la classe {@link EnumCritereRechercheContribuable}.
	 * @param nbResultat
	 *            le nombre maximal de contribuables retournés par cette recherche. La valeur de ce paramètre ne peut excéder 49. Si la
	 *            recherche renvoie plus de resultats que ce paramètre, le nombre de résultat sera limité à <code>nbResultat</code> + 1.
	 * @return la liste des contribuables, valides durant l'année en paramètre, correspondant aux critères passés en paramètre.
	 * @throws RegistreException
	 *             si un problème métier survient lors de l'invocation du service.
	 */
	@Transactional(readOnly = true)
	public Collection rechercherContribuables(HashMap criteresRecherche, int nbResultat) throws RegistreException {
		if (nbResultat > 49) {
			throw new IllegalArgumentException(
					"Le paramètre représentant le nombre maximal de contribuables retournés par la recherche excède 49.");
		}



		/** Résultat de la fonction. */
		Collection<ResultatRechercheContribuable> result = new ArrayList<ResultatRechercheContribuable>();
		TiersInfoArray resultsUnireg = new TiersInfoArray();

		ch.vd.uniregctb.tiers.Contribuable contribuableUnireg = null;

		/** Paramètre de recherche. */
		String nom = (String) criteresRecherche.get(EnumCritereRechercheContribuable.NOM);
		String prenom = (String) criteresRecherche.get(EnumCritereRechercheContribuable.PRENOM);
		String localite = (String) criteresRecherche.get(EnumCritereRechercheContribuable.LOCALITE_POSTALE);
		String pays = (String) criteresRecherche.get(EnumCritereRechercheContribuable.PAYS);
		String noAvs = (String) criteresRecherche.get(EnumCritereRechercheContribuable.NO_AVS);
		Integer noContribuable = (Integer) criteresRecherche.get(EnumCritereRechercheContribuable.NO_CONTRIBUABLE);
		Integer noIndividu = (Integer) criteresRecherche.get(EnumCritereRechercheContribuable.NO_INDIVIDU);
		Date dateNaissance = (Date) criteresRecherche.get(EnumCritereRechercheContribuable.DATE_NAISSANCE);
		Integer npa = (Integer) criteresRecherche.get(EnumCritereRechercheContribuable.NO_NPA);

		if (noContribuable != null) {

			contribuableUnireg = getTiersDAO().getContribuableByNumero(new Long(noContribuable));
			if (contribuableUnireg == null) {
				throw new RegistreException("Ce numéro n'est attribué à aucun contribuable");
			}
		}
		//
		else if (noIndividu != null) {
			contribuableUnireg = getTiersDAO().getHabitantByNumeroIndividu(new Long(noIndividu));
			if (contribuableUnireg == null) {
				throw new RegistreException("Ce numéro d'individu n'est associé à aucun contribuable dans unireg");
			}
		}
		else {// Recherche selon les critères rentrés au début


			SearchTiers searchTiers = new SearchTiers();

			searchTiers.setLogin(login);
			if (localite != null) {
				searchTiers.setLocaliteOuPays(localite);
			}else if (pays!=null) {
				searchTiers.setLocaliteOuPays(pays);
			}
			if (dateNaissance != null) {
				ch.vd.uniregctb.webservices.tiers.Date dateWSNaissance = new ch.vd.uniregctb.webservices.tiers.Date();
				dateWSNaissance.setDay(RegDate.get(dateNaissance).day());
				dateWSNaissance.setMonth(RegDate.get(dateNaissance).month());
				dateWSNaissance.setYear(RegDate.get(dateNaissance).year());
				searchTiers.setDateNaissance(dateWSNaissance);
			}


			if (noAvs != null) {

				if (noAvs.length() == 11) {
					noAvs = FormatNumeroHelper.formatAncienNumAVS(noAvs);
				}
				if (noAvs.length() == 13) {
					noAvs = FormatNumeroHelper.formatNumAVS(noAvs);

				}
				searchTiers.setNumeroAVS(noAvs);
			}
			if (nom != null && prenom != null) {
				searchTiers.setNomCourrier(prenom + " " + nom);


			}
			else{
				String critere = null;
				if (nom != null) {
					critere = nom;
				} else {
					critere = prenom;
				}
				searchTiers.setNomCourrier(critere);

			}

			try {
				searchTiers.setTypeRecherche(ch.vd.uniregctb.webservices.tiers.TypeRecherche.EST_EXACTEMENT);
				resultsUnireg = serviceTiersPort.searchTiers(searchTiers);
			} catch (Exception e) {
				LOGGER_ERROR.info("Le service web unireg a renvoye une erreur " +e.getMessage());
				throw new RegistreException("Le service web unireg a renvoye une erreur:"+e.getMessage());
			}



		}



		if (contribuableUnireg != null) {
			ResultatRechercheContribuable resultat = setUpResultatRecherche(contribuableUnireg.getNumero(),npa,true);
			if (resultat!=null) {
				result.add(resultat);
			}


		}
		else if (resultsUnireg != null) {
			int i=0;
			for (TiersInfo t : resultsUnireg.getItem()) {
				ResultatRechercheContribuable resultat =setUpResultatRecherche(t.getNumero(),npa,false);
				if (resultat!=null) {
					result.add(resultat);
				}
				i++;
				//on limite le nombre d'element à 49
				if (i==49) {
					break;
				}

			}
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @param declarationQuittances
	 * @return
	 * @throws RegistreException
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
	public List<DeclarationQuittance> quittanceDeclarations(List<DeclarationQuittance> declarationQuittances) throws RegistreException {

		final String NO_DECLARATION_REGEXP = "(\\d{8,9})(\\d{4})(\\d{2})(\\d{2})";

		final Pattern NO_DECLARATION_PATTERN = Pattern.compile(NO_DECLARATION_REGEXP);
		long noContribuable;
		int periodeFiscale;
		int noParAnnee;
		RegDate dateRetour = RegDate.get();
		Date timeStampDate = dateRetour.asJavaDate();
		int nombreDeclarationRegistre = 0;

		if (declarationQuittances == null || declarationQuittances.isEmpty())
			return declarationQuittances;

		Iterator<DeclarationQuittance> iterDeclaration = declarationQuittances.iterator();
		// On parcourt toutes les declaration a quittancer
		//Verification de la presence dans unireg des DI à quittancer

		while (iterDeclaration.hasNext()) {
			String codeRetour = null;

			DeclarationQuittance declarationAquittancer = iterDeclaration.next();
			String noDeclaration = Long.toString(declarationAquittancer.getNoDeclaration());
			Matcher matcher = NO_DECLARATION_PATTERN.matcher(noDeclaration);

			if (!matcher.matches()){
					throw new RegistreException("quittanceDeclarations: no de declaration " + noDeclaration + " ne respecte pas format " + NO_DECLARATION_REGEXP);
			}
			// Extraction du numero de contribuable, de la periode fiscale et du numero de sequence dans l'annee
			noContribuable = Long.parseLong(matcher.group(1));
			periodeFiscale = Integer.parseInt(matcher.group(2));
			noParAnnee = Integer.parseInt(matcher.group(3));
			if (declarationAquittancer.getCodeRetour().getNameAsInt() == -1 && noContribuable != 0) {
				LOGGER.info("Quittancement de la DI " + noDeclaration + " pour le contribuable numero " + noContribuable);
				ch.vd.uniregctb.tiers.Contribuable contribuableUnireg = getTiersDAO().getContribuableByNumero(noContribuable);
				// Si le contribuable n’existe pas, elle renvoie le message « Ce contribuable n’existe pas », avec le code de retour 2, la
				// date et l’heure.
				if (contribuableUnireg == null) {
					LOGGER_ERROR.error("quittanceDeclarations: le contribuable " + noContribuable  + " n'existe pas.");
					codeRetour = "2";

				}
				else {
						// Si le contribuable ne possède pas de for, elle renvoie le message « Erreur système : veuillez contacter le
						// responsable de
						// l’application », avec le code de retour 2, la date et l’heure.
						if (contribuableUnireg.getDernierForFiscalPrincipal() == null) {
							LOGGER_ERROR.error("quittanceDeclarations: Erreur système : veuillez contacter le responsable de l’application (pas de for Principal pour "
									+ noContribuable + ").");
							codeRetour = "2";
						}
						// Si le contribuable est un débiteur archivé (i107), elle renvoie le message « Mise à jour impossible sur un débiteur
						// archivé ». Avec quel code de retour ?
						else if (contribuableUnireg.isDebiteurInactif()) {
							LOGGER_ERROR.error("quittanceDeclarations: Mise à jour impossible sur un débiteur archivé numero "+ noContribuable );
							codeRetour = "2";
						}
						else{
								//Début de test sur la déclaration si elle est trouvée

								DeclarationImpotOrdinaire declarationUnireg = ContribuableUniregHelper.findDeclaration(contribuableUnireg,
										periodeFiscale, noParAnnee);
								if (declarationUnireg != null) {

									// Si la date de retour est manquante et ou est située dans le futur. Cas non prévu
									// Si la date de retour est antérieure ou égale à la date d’émission, elle renvoie le code de retour -1 avec la date
									// et
									// l’heure dans l’élément de la liste renvoyée en retour.
									if (RegDate.get().isBeforeOrEqual(declarationUnireg.getDateExpedition())) {
										codeRetour = "-1";
										LOGGER_ERROR.error("quittanceDeclarations: contribuable" +noContribuable+" Date de quittancement avant date expedition"+" CodeRetour:"+codeRetour);

									}

									// Si la déclaration est annulée, elle renvoie le code de retour 3 avec la date et l’heure dans l’élément de la
									// liste
									// renvoyée en retour.
									else if (declarationUnireg.isAnnule()) {
										codeRetour = "3";
										LOGGER_ERROR.error("quittanceDeclarations: contribuable" +noContribuable+" DI annulee"+" CodeRetour:"+codeRetour);
									}


									else {
										// Si la déclaration est sommée à une date située dans le futur, On annule cette état de sommation dans le futur
										//pour permettre le quittancement.
										if (declarationUnireg.getDernierEtat().getEtat().equals(TypeEtatDeclaration.SOMMEE)
												&& RegDate.get().isBeforeOrEqual(declarationUnireg.getDernierEtat().getDateObtention())) {
											declarationUnireg.getEtatDeclarationActif(TypeEtatDeclaration.SOMMEE).setAnnule(true);
										}

											//La declaration est correcte, on la quittance
											try {
												diService.retourDI(contribuableUnireg, declarationUnireg, dateRetour);
												Assert.isEqual(TypeEtatDeclaration.RETOURNEE, declarationUnireg.getDernierEtat().getEtat());
												//pour chaque DI quittancée on initialise le code retour à 0
												codeRetour = "0";
												LOGGER.info(" DI " + noDeclaration + " quittancee pour le contribuable numero " + noContribuable+" CodeRetour:"+codeRetour);
											}
											catch (Exception e) {
												LOGGER_ERROR.info("quittanceDeclarations:Erreur lors de la mis a jour de la DI du contribuable "+noContribuable+" " +e.getMessage());
												LOGGER_ERROR.error("quittanceDeclarations: erreur lors de la mis a jour de la DI :" + e.getMessage());
												codeRetour = "-1";
											}
									}
								}
								// Si la déclaration d’impôt n’existe pas, elle renvoie le message « La déclaration d’impôt n’existe pas », avec
								// le code de retour 2, la date et l’heure.
								else {
									LOGGER_ERROR.error("quittanceDeclarations: La declaration d’impot numero "+noParAnnee+" pour la période fiscale "+periodeFiscale+" concernant le contribuable "+noContribuable+" n’existe pas");
									codeRetour = "2";
								}

						}
			  }

		}


		declarationAquittancer.setQuittanceTime(timeStampDate);
		declarationAquittancer.setCodeRetour(EnumCodeRetourDeclarationQuittance.getEnum(codeRetour));

	}
	// Suite à Discussion avec Bernard Gaberell LE requittancement doit être possible !!!!
	// Si la déclaration est retournée, elle renvoie le code de retour 2 avec la date et l’heure dans l’élément de la
	// liste
	// renvoyée en retour.
	/*if (declarationUnireg.getDernierEtat().equals(TypeEtatDeclaration.RETOURNEE)) {
		// code retour en accord avec la retro specification de REG_PP_SOC_GET_QUITTANCE_DI
		codeRetour = "1";
		LOGGER_ERROR.error("quittanceDeclarations: contribuable" +noContribuable+" DI deja quittancee le :"+declarationUnireg.getDateRetour()+" CodeRetour:"+codeRetour);
	}*/
		return declarationQuittances;
}

	private void verifierNombreDIaQuittancer(List<DeclarationQuittance> listeDI){
		for(DeclarationQuittance declarationAQuittancer :listeDI){


		}
	}

	/**
	 *
	 * @param noContribuable
	 * @param code
	 * @param user
	 * @throws RegistreException
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
	public void modifierCodeBlocageRmbtAuto(long noContribuable, boolean code, String user,boolean retrieveI107) throws RegistreException {

		ch.vd.uniregctb.tiers.Contribuable contribuableUnireg = getTiersDAO().getContribuableByNumero(noContribuable);
		if (contribuableUnireg != null) {
			if (!retrieveI107 && contribuableUnireg.isDebiteurInactif()) {
				throw new RegistreException("Ce contribuable est considéré comme inexistant car il est inactif");
			}

			contribuableUnireg.setBlocageRemboursementAutomatique(code);

		}
		else {
			throw new RegistreException("le contribuable " + noContribuable + " est inconnue dans le registe fiscale");
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.registre.fiscal.service.impl.IServiceFiscal#modifierInformationsPersonnelles(ch.vd.registre.fiscal.model.ContribuableRetourInfoDi)
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
	public void modifierInformationsPersonnelles(ContribuableRetourInfoDi ctbRetourInfoDi, boolean retrieveI107) throws RegistreException {

		ContribuableRetourInfoDiImpl contribuableRetourInfoDi = (ContribuableRetourInfoDiImpl) ctbRetourInfoDi;

		long noContribuable = contribuableRetourInfoDi.getNoContribuable();

		ch.vd.uniregctb.tiers.Contribuable contribuableUnireg = getTiersDAO().getContribuableByNumero(noContribuable);
		if (contribuableUnireg == null) {

			throw new RegistreException("modifierInformationsPersonnelles: le contribuable " + noContribuable + " est inconnue dans le registe fiscale");

		}
		else{
			if (!retrieveI107 && contribuableUnireg.isDebiteurInactif()) {
				throw new RegistreException("modifierInformationsPersonnelles: Ce contribuable est considéré comme inexistant car il est inactif");
			}
		}
		List<Declaration> declarations = contribuableUnireg.getDeclarationForPeriode(contribuableRetourInfoDi.getAnneeFiscale());

		if (declarations == null || declarations.isEmpty()) {
			throw new RegistreException("modifierInformationsPersonnelles: Pas de declaration trouvée pour l'année fiscale " + contribuableRetourInfoDi.getAnneeFiscale());
		}
		DeclarationImpotOrdinaire declarationAModifier = null;
		// on recherche la DI sur une année differente de l'année fiscale courante
		if (contribuableRetourInfoDi.getAnneeFiscale() != 0) {
			declarationAModifier = ContribuableUniregHelper.findDeclaration(contribuableUnireg, contribuableRetourInfoDi.getAnneeFiscale(),
					contribuableRetourInfoDi.getNoImpotAnnee());
		}
		// l'année fiscale n'est pas renseignée, on met une exception selon la spec
		else {
			 throw new RegistreException("modifierInformationsPersonnelles: Contribuable numero:"+noContribuable+" La declaration numero "+contribuableRetourInfoDi.getNoImpotAnnee()+" non trouvée pour l'année fiscale " + contribuableRetourInfoDi.getAnneeFiscale());
		}

		if (contribuableRetourInfoDi.getTitulaireCompte() != null) {
			contribuableUnireg.setTitulaireCompteBancaire(contribuableRetourInfoDi.getTitulaireCompte());
		}
		if (contribuableRetourInfoDi.getIban() != null) {
			contribuableUnireg.setNumeroCompteBancaire(contribuableRetourInfoDi.getIban());
		}
		if (contribuableRetourInfoDi.getNoTelephone() != null) {
			contribuableUnireg.setNumeroTelephonePrive(contribuableRetourInfoDi.getNoTelephone());
		}
		if (contribuableRetourInfoDi.getNoMobile() != null) {
			contribuableUnireg.setNumeroTelephonePortable(contribuableRetourInfoDi.getNoMobile());
		}
		if (contribuableRetourInfoDi.getEmail() != null) {
			contribuableUnireg.setAdresseCourrierElectronique(contribuableRetourInfoDi.getEmail());
		}

		final int annee = contribuableRetourInfoDi.getAnneeFiscale();
		final PeriodeFiscale periode = periodeFiscaleDAO.getPeriodeFiscaleByYear(annee);
		final ModeleDocument vaudTax = modeleDocumentDAO.getModelePourDeclarationImpotOrdinaire(periode, TypeDocument.DECLARATION_IMPOT_VAUDTAX);
		final ModeleDocument complete = modeleDocumentDAO.getModelePourDeclarationImpotOrdinaire(periode, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
		Assert.notNull(vaudTax);
		Assert.notNull(complete);
		final EnumTypeImposition typeImposition = contribuableRetourInfoDi.getTypeImposition();
		if (typeImposition != null) {
			if (declarationAModifier != null) {
				final TypeDocument typeDocument = declarationAModifier.getModeleDocument().getTypeDocument();
				if (typeImposition.equals(EnumTypeImposition.ELECTRONIQUE)
						&& (typeDocument.equals(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL) || typeDocument
								.equals(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH))) {
					declarationAModifier.setModeleDocument(vaudTax);
				}
				else if (typeImposition.equals(EnumTypeImposition.MANUELLE) && typeDocument.equals(TypeDocument.DECLARATION_IMPOT_VAUDTAX)) {
					declarationAModifier.setModeleDocument(complete);
				}
			}
		}

	}



	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}



	public ResultatRechercherNoContribuable RechercherNoContribuable(RechercherNoContribuable rechercherNoContribuable)
			throws RegistreException {

		ResultatRechercherNoContribuable reponse = new ResultatRechercherNoContribuableImpl();

		List<TiersIndexedData> resultsUnireg = new ArrayList<TiersIndexedData>();
		TiersIndexedData tiersTrouve = null;

		final long numeroAvs = rechercherNoContribuable.getNoAVS();
		final int annee = rechercherNoContribuable.getAnnee();
		TiersCriteria tiersCriteria = new TiersCriteria();
		ch.vd.uniregctb.tiers.Contribuable contribuableUnireg = null;

		tiersCriteria.setNumeroAVS(String.valueOf(numeroAvs));

		if (rechercherNoContribuable.getNom() != null || rechercherNoContribuable.getPrenom() != null) {
			tiersCriteria.setNomRaison(rechercherNoContribuable.getNom() + " " + rechercherNoContribuable.getPrenom());
			tiersCriteria.setTypeRechercheDuNom(TypeRecherche.CONTIENT);

		}

		try {
			resultsUnireg = tiersService.search(tiersCriteria);

		}
		catch (TooManyResultsIndexerException ee) {
			LOGGER_ERROR.error("Exception dans l'indexer: " + ee.getMessage(), ee);
			throw new RegistreException("Trop de resultats trouvés", ee);

		}
		catch (IndexerException e) {
			LOGGER_ERROR.error("Exception dans l'indexer: " + e.getMessage(), e);
			throw new RegistreException("Exception dans l'indexer", e);

		}

		if (resultsUnireg != null && !resultsUnireg.isEmpty()) {
			tiersTrouve = resultsUnireg.get(0);
			contribuableUnireg = getTiersDAO().getContribuableByNumero(tiersTrouve.getNumero());

			if (contribuableUnireg != null) {
				reponse.setNoTechnique(rechercherNoContribuable.getNoTechnique());
				if (contribuableUnireg instanceof PersonnePhysique) {
					reponse.setNoContribuableSeul(contribuableUnireg.getNumero().intValue());
				}
				else if (contribuableUnireg instanceof MenageCommun) {
					reponse.setNpContribuableCouple(contribuableUnireg.getNumero().intValue());
				}

				reponse.setNbrOccurence(resultsUnireg.size());
				ForFiscalPrincipal forPrincipal = contribuableUnireg.getForFiscalPrincipalAt(RegDate.get(annee, 12, 31));

				if (forPrincipal != null && forPrincipal.getModeImposition().equals(ModeImposition.SOURCE)) {
					reponse.setSourcierPur(true);
				}
				else {
					reponse.setSourcierPur(false);
				}

			}

		}
		return reponse;
	}

	public ResultatRechercherNoContribuable RechercherNoContribuable(String numeroAvs, String nom, String prenom, int annee)
			throws RegistreException {

		ResultatRechercherNoContribuable reponse = new ResultatRechercherNoContribuableImpl();

		List<TiersIndexedData> resultsUnireg = new ArrayList<TiersIndexedData>();
		TiersIndexedData tiersTrouve = null;

		TiersCriteria tiersCriteria = new TiersCriteria();
		ch.vd.uniregctb.tiers.Contribuable contribuableUnireg = null;

		tiersCriteria.setNumeroAVS(numeroAvs);

		if (nom != null || prenom != null) {
			tiersCriteria.setNomRaison(prenom + " " + nom);
			tiersCriteria.setTypeRechercheDuNom(TypeRecherche.CONTIENT);

		}

		try {
			resultsUnireg = tiersService.search(tiersCriteria);

		}
		catch (TooManyResultsIndexerException ee) {
			LOGGER_ERROR.error("Exception dans l'indexer: " + ee.getMessage(), ee);
			throw new RegistreException("Trop de resultats trouvés:"+ee.getMessage());

		}
		catch (IndexerException e) {
			LOGGER_ERROR.error("Exception dans l'indexer: " + e.getMessage(), e);
			throw new RegistreException("Exception dans l'indexer:"+ e.getMessage());

		}

		if (resultsUnireg != null && !resultsUnireg.isEmpty()) {
			tiersTrouve = resultsUnireg.get(0);
			contribuableUnireg = getTiersDAO().getContribuableByNumero(tiersTrouve.getNumero());

			if (contribuableUnireg != null) {
				reponse.setNoTechnique(12);
				if (contribuableUnireg instanceof PersonnePhysique) {
					reponse.setNoContribuableSeul(contribuableUnireg.getNumero().intValue());
				}
				else if (contribuableUnireg instanceof MenageCommun) {
					reponse.setNpContribuableCouple(contribuableUnireg.getNumero().intValue());
				}

				reponse.setNbrOccurence(resultsUnireg.size());
				ForFiscalPrincipal forPrincipal = contribuableUnireg.getForFiscalPrincipalAt(RegDate.get(annee, 12, 31));

				if (forPrincipal != null && forPrincipal.getModeImposition().equals(ModeImposition.SOURCE)) {
					reponse.setSourcierPur(true);
				}
				else {
					reponse.setSourcierPur(false);
				}

			}

		}
		return reponse;
	}
	private Range setUpPeriodeRecherche(Integer annee){
		Range periodeRecherche = null;

		RegDate dateDebut = null;
		RegDate dateFin = null;
		// Calcul de la date de recherche
		if (annee >= RegDate.get().year() || annee == 0 || annee ==null) {
			dateDebut = RegDate.get(RegDate.get().year(), RegDate.JANVIER, 1);
			dateFin = RegDate.get();

		}
		else {
			dateDebut = RegDate.get(annee.intValue(), RegDate.JANVIER, 1);
			dateFin = RegDate.get(annee.intValue(), RegDate.DECEMBRE, 31);

		}
		periodeRecherche = new Range(dateDebut, dateFin);
		return periodeRecherche;
	}
	private IndividuImpl setUpIndividu(Individu individuCivil, PersonnePhysique contribuableUnireg, Range periodeRecherche) {
		IndividuImpl myIndividu = new IndividuImpl();
		EtatCivilImpl etatCivil = new EtatCivilImpl();
		Collection<EtatCivilImpl> lesEtatsCivils = null;


		VueSituationFamille situationFamille = situationFamilleService.getVue(
				contribuableUnireg, periodeRecherche.getDateFin());

		etatCivil = ContribuableUniregHelper.getEtatCivil(situationFamille);

		if (individuCivil.getDateDeces()!=null) {

			RegDate dateAvantDeces = RegDate.get(individuCivil.getDateDeces()).getOneDayBefore();
			VueSituationFamille derniereSituationFamille=situationFamilleService.getVue(contribuableUnireg, dateAvantDeces);
			etatCivil = ContribuableUniregHelper.getEtatCivil(derniereSituationFamille);

		}

		lesEtatsCivils = new ArrayList<EtatCivilImpl>();
		lesEtatsCivils.add(etatCivil);

		/** Récupération du dernier historique de l'individu principal. */
		HistoriqueIndividuImpl dernierHistoriqueIndividu = new HistoriqueIndividuImpl();
		dernierHistoriqueIndividu.setAutresPrenoms(ContribuableUniregHelper.convertNullToEmpty(individuCivil.getDernierHistoriqueIndividu()
				.getAutresPrenoms()));
		dernierHistoriqueIndividu.setNom(individuCivil.getDernierHistoriqueIndividu().getNom());
		dernierHistoriqueIndividu.setNomCourrier1(ContribuableUniregHelper.convertNullToEmpty(individuCivil.getDernierHistoriqueIndividu()
				.getNomCourrier1()));
		dernierHistoriqueIndividu.setNomCourrier2(ContribuableUniregHelper.convertNullToEmpty(individuCivil.getDernierHistoriqueIndividu()
				.getNomCourrier2()));
		if (individuCivil.getDernierHistoriqueIndividu().getNomNaissance() == null) {
			dernierHistoriqueIndividu.setNomNaissance("");
		}
		else {
			dernierHistoriqueIndividu.setNomNaissance(individuCivil.getDernierHistoriqueIndividu().getNomNaissance());
		}

		dernierHistoriqueIndividu.setPrenom(individuCivil.getDernierHistoriqueIndividu().getPrenom());

		// initialisation des informations sur l'individu
		myIndividu.setNoTechnique(individuCivil.getNoTechnique());


		myIndividu.setSexeMasculin(individuCivil.isSexeMasculin());

		myIndividu.setDateDeces(individuCivil.getDateDeces());
		myIndividu.setDateNaissance(individuCivil.getDateNaissance());
		myIndividu.setDernierHistoriqueIndividu(dernierHistoriqueIndividu);
		myIndividu.setEtatsCivils(lesEtatsCivils);

		return myIndividu;
	}

	public Collection getListeCtbModifies(Date dateDebutRech, Date dateFinRech, int numeroCtbDepart) throws RegistreException {

		HashSet resultatSet = new HashSet() ;
		List resultat=null;

		final String contribuableValides = // --------------------------------
			"SELECT DISTINCT forFiscal.tiers.numero                                                 "
			+ "FROM                                                                         "
			+ "     ForFiscal as forFiscal                                                  "
			+ "INNER JOIN                                                                   "
		    + "    forFiscal.tiers as tiers                                                 "
			+ "WHERE                                                                        "
			+ "     tiers.numero >= :contribuableStart AND                                  "
			+ "     tiers.class !='DebiteurPrestationImposable' AND                         "
			+ "     forFiscal.annulationDate IS null                                        ";



		final String contribuableModifies = // --------------------------------
		"SELECT DISTINCT tiers.numero                                                           "
				+ "FROM                                                                         "
				+ "     Tiers as tiers                                                          "
				+ "WHERE                                                                        "
				+ "     tiers.numero in (:listeValide)     AND                                  "
				+ "     tiers.numero >= :contribuableStart AND                                  "
				+ "     tiers.logModifDate >= :debut AND                                        "
				+ "     tiers.logModifDate <= :fin AND                                          "
				+ "     tiers.annulationDate IS null                                            ";

		final String forModifies = // --------------------------------
		"SELECT DISTINCT forFiscal.tiers.numero                                                 "
				+ "FROM                                                                         "
				+ "     ForFiscal as forFiscal                                                  "
				+ "WHERE                                                                        "
				+ "     forFiscal.tiers.numero in (:listeValide)     AND                        "
				+ "     forFiscal.tiers.numero >= :contribuableStart AND                        "
				+ "     forFiscal.logModifDate >= :debut AND                                    "
				+ "     forFiscal.logModifDate <= :fin AND                                      "
				+ "     forFiscal.annulationDate IS null                                        ";

		final String diModifies = // --------------------------------
		"SELECT DISTINCT declaration.tiers.numero                                               "
				+ "FROM                                                                         "
				+ "    Declaration as declaration                                               "
				+ "INNER JOIN                                                                   "
			    + "    declaration.etats as etats                                               "
				+ "WHERE                                                                        "
				+ "     declaration.tiers.numero in (:listeValide)     AND                      "
				+ "     declaration.tiers.numero >= :contribuableStart AND                      "
				+ "     (etats.etat = 'EMISE' OR  etats.etat = 'ECHUE') AND                    "
				+ "     etats.dateObtention >= :dateDebut AND                              "
				+ "     etats.dateObtention <= :dateFin   AND                              "
				+ "     etats.annulationDate IS null                                       ";

		final RegDate dateDebut = RegDate.get(dateDebutRech);
		final RegDate dateFin = RegDate.get(dateFinRech);
		final Timestamp timestampDebut = new Timestamp(dateDebutRech.getTime());
		final Timestamp timestampFin = new Timestamp(dateFinRech.getTime());

		final int contribuableStart = numeroCtbDepart;

		final List<Integer> listeContribuablesValides = (List<Integer>) hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.createQuery(contribuableValides);
				queryObject.setParameter("contribuableStart", contribuableStart);
				queryObject.setMaxResults(1000);

				return queryObject.list();
			}
		});
		LOGGER.info("Nombre de ctb valides "+ listeContribuablesValides.size());

		final List<Integer> listeContribuablesModifies = (List<Integer>) hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.createQuery(contribuableModifies);
				queryObject.setParameterList("listeValide", listeContribuablesValides);

				queryObject.setParameter("contribuableStart", contribuableStart);
				queryObject.setParameter("debut", timestampDebut);
				queryObject.setParameter("fin", timestampFin);
				queryObject.setMaxResults(300);

				return queryObject.list();
			}
		});
		LOGGER.info("Nombre de ctb modifiés "+ listeContribuablesModifies.size());
		resultatSet.addAll(listeContribuablesModifies);
		final List<Integer> listeForModifies = (List<Integer>) hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.createQuery(forModifies);
				queryObject.setParameterList("listeValide", listeContribuablesValides);
				queryObject.setParameter("contribuableStart", contribuableStart);
				queryObject.setParameter("debut", timestampDebut);
				queryObject.setParameter("fin", timestampFin);
				queryObject.setMaxResults(300);

				return queryObject.list();
			}
		});
		LOGGER.info("Nombre de ctb avec for modifiés "+ listeForModifies.size());
		resultatSet.addAll(listeForModifies);

		final List<Integer> listeDiModifies = (List<Integer>) hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.createQuery(diModifies);
				queryObject.setParameterList("listeValide", listeContribuablesValides);
				queryObject.setParameter("contribuableStart", contribuableStart);
				 queryObject.setParameter("dateDebut", dateDebut.index());
				 queryObject.setParameter("dateFin", dateFin.index());
				queryObject.setMaxResults(300);

				return queryObject.list();
			}
		});
		LOGGER.info("Nombre de ctb avec declaration émises modifiés "+ listeDiModifies.size());
		resultatSet.addAll(listeDiModifies);

		if (resultatSet.size()>300){
			//sublist ne marche pas car un pb de serialisation
			List resultatTemporaire= new ArrayList<Integer>();
			for(int i=0;i<300;i++){
				resultatTemporaire.add(Arrays.asList(resultatSet.toArray()).get(i));
			}
			resultat= resultatTemporaire;
		}else{
			resultat= Arrays.asList(resultatSet.toArray());
		}



		return resultat;
	}

	public Collection getListeCtbSansDIPeriode(int periodeFiscale, int numeroCtbDepart) throws RegistreException {
		final RegDate dateRecherche = RegDate.get(periodeFiscale, RegDate.get().DECEMBRE, 31);
		final int ctbStart = numeroCtbDepart;

		if (periodeFiscale < 1995 || periodeFiscale > RegDate.get().year()) {
			throw new RegistreException("Année fiscale erronée : doit être > ou = à 1995 ");

		}

		final List<Integer> result = (List<Integer>) hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.createQuery(contribuablesSansDI);
				queryObject.setParameter("dateRecherche", dateRecherche.index());
				queryObject.setParameter("annee", dateRecherche.year());
				queryObject.setParameter("contribuableStart", ctbStart);
				queryObject.setMaxResults(1000);
				return queryObject.list();
			}
		});

		return result;
	}
	@Transactional(readOnly = true)
	public ContribuableSDI getCtrlContribuable(Date dateRef, int numeroCtb) throws BusinessException {

		long numeroContribuable = numeroCtb;

		LOGGER.debug("Entree getCtrlContribuable pour " + numeroCtb);

		ch.vd.uniregctb.tiers.Contribuable contribuableUnireg = getTiersDAO().getContribuableByNumero(numeroContribuable);
		if (contribuableUnireg == null) {
			throw new BusinessException("Contribuable inexistant dans Unireg");
		}

		if (dateRef == null) {
			throw new BusinessException("Année fiscale erronée : doit être > ou = à 1995");
		}
		RegDate dateParam = RegDate.get(dateRef);
		ContribuableSDI contribuable = new ContribuableSDI();

		/** on recupere le numero de contribuable depuis registre */
		contribuable.setNumeroCtb(contribuableUnireg.getNumero().intValue());

		/** on recupere l'adresse courrier depuis registre */
		// Recuperations des informations courrier du contribuables
		AdresseEnvoiDetaillee adresseEnvoi = null;
		try {
			adresseEnvoi = adresseService.getAdresseEnvoi(contribuableUnireg, RegDate.get(dateRef));
		}
		catch (AdressesResolutionException e) {
			LOGGER_ERROR.info("Erreur lors du chargement de l'adresse du contribuable "+numeroCtb+" " +e.getMessage());
			throw new BusinessException("Erreur lors du chargement de l'adresse du contribuable  ", e);
		}

		AdresseGenerique adresseCourrierUnireg = null;
		try {

			adresseCourrierUnireg = adresseService.getAdressesFiscales(contribuableUnireg, RegDate.get(dateRef)).courrier;
		}
		catch (AdressesResolutionException e) {
			LOGGER_ERROR.info("Problème durant la resolution de l'adresse contribuable dans Unireg  "+numeroCtb+" " +e.getMessage());
			throw new BusinessException("Problème durant la resolution de l'adresse contribuable dans Unireg ", e);
		}

		AdresseCourrierSDI adresse = new AdresseCourrierSDI();
		if (adresseEnvoi!=null) {

			adresse.setFormulePolitesse(adresseEnvoi.getSalutations());
			adresse.setNom1(adresseEnvoi.getNomPrenom().get(0));
			if (adresseEnvoi.getNomPrenom().size() > 1) {

				adresse.setNom2(adresseEnvoi.getNomPrenom().get(1));

			}
			if (adresseCourrierUnireg!=null) {


				adresse.setChez(adresseCourrierUnireg.getComplement());


				adresse.setRue(adresseCourrierUnireg.getRue());


				// adresse.setLieu(adresseCourrierUnireg.getCommuneAdresse().getNomMinuscule());

				adresse.setLocalite(adresseCourrierUnireg.getLocalite());
				if (adresseCourrierUnireg.getNumeroPostal() != null) {
					adresse.setNpa(adresseCourrierUnireg.getNumeroPostal());
				}
				else {
					adresse.setNpa(null);
				}
			}
		}
		contribuable.setAdresse(adresse);

		/** on recupere la liste des declarations d'impots depuis registre */

		List lstDI = new ArrayList();

		DeclarationSDI declarationTemp = null;

		for (Declaration d : contribuableUnireg.getDeclarationForPeriode(dateParam.year())) {
			DeclarationImpotOrdinaire declarationUnireg = (DeclarationImpotOrdinaire)d;
			declarationTemp = new DeclarationSDI();
			declarationTemp.setAnneeFiscale(declarationUnireg.getPeriode().getAnnee());
			declarationTemp.setNoParAnnee(declarationUnireg.getNumero());

			declarationTemp.setCodeDernierEtat(Integer.parseInt(ContribuableUniregHelper.convertEtatDeclaration(
					declarationUnireg.getDernierEtat().getEtat()).getName()));
			if (declarationUnireg.getDernierEtat().getDateDebut() != null) {
				declarationTemp.setDateDernierEtat(declarationUnireg.getDernierEtat().getDateDebut().asJavaDate());
			}
			else {
				declarationTemp.setDateDernierEtat(null);
			}



			lstDI.add(declarationTemp);
		}
		contribuable.setListeDI(lstDI);

		LOGGER.debug("Contribuable retourne :" + contribuable);

		return contribuable;

	}
	/**
	 * @param noContribuable: numero de contribuable a retourner
	 * @param npa :critère de restriction sur ke contribuable a retourner son adresse doit correspondre à ce NPA
	 * @param sansRestriction :dans le cas ou la reccherche est faite par numero, pas de verification sur les fors
	 * @return
	 * @throws RegistreException
	 */
	private ResultatRechercheContribuableImpl setUpResultatRecherche(long noContribuable,Integer npa,boolean sansRestriction) throws RegistreException {


		Contribuable contribuableHostTrouve = getContribuable(noContribuable, RegDate.get().year(), false, false,true);
		/** Récupération des informations sur l'individu . */
		if (contribuableHostTrouve!=null) {
			//restriction sur le dernier for actif
			For dernierFor = contribuableHostTrouve.getDernierFor();
			if ((dernierFor!=null && dernierFor.getDateFinValidite()==null) || sansRestriction) {

			//Restriction sur le npa
				if (isNpaCompatible(contribuableHostTrouve, npa)) {

					ResultatRechercheContribuableImpl resultatRechercheContribuable = new ResultatRechercheContribuableImpl();

					/** Récupération des informations sur le contribuable. */
					resultatRechercheContribuable.setNoContribuable(noContribuable);

					Individu individuPrincipal = (Individu) contribuableHostTrouve.getEntiteCivile();

					if (individuPrincipal != null) {
						resultatRechercheContribuable.setNoTechniqueIndividuPrincipal(individuPrincipal.getNoTechnique());
						resultatRechercheContribuable.setPrenomIndividuPrincipal(individuPrincipal.getDernierHistoriqueIndividu().getPrenom());
						resultatRechercheContribuable.setNomIndividuPrincipal(individuPrincipal.getDernierHistoriqueIndividu().getNom());
						resultatRechercheContribuable.setDateNaissanceIndividuPrincipal(individuPrincipal.getDateNaissance());
						/** Récupération du sexe de l'individu principal. */

						resultatRechercheContribuable.setSexeMasculinIndividuPrincipal(individuPrincipal.isSexeMasculin());
						// ** Récupération des informations sur l'état civil. */
						if (individuPrincipal.getEtatsCivils() != null && !individuPrincipal.getEtatsCivils().isEmpty()) {
							EtatCivil etatCivil = (EtatCivil) Arrays.asList(individuPrincipal.getEtatsCivils().toArray()).get(0);

							resultatRechercheContribuable.setDateValiditeEtatCivil(etatCivil.getDateDebutValidite());
							resultatRechercheContribuable.setTypeEtatCivil(etatCivil.getTypeEtatCivil());

						}

						/**
						 * Récupération des information sur l'adresse de l'individu principal.
						 */
						if (contribuableHostTrouve.getAdresse() != null) {
							resultatRechercheContribuable.setTitreAdresse(contribuableHostTrouve.getAdresse().getTitre());
							resultatRechercheContribuable.setRueAdresse(contribuableHostTrouve.getAdresse().getRue());
							resultatRechercheContribuable.setNumeroAdresse(contribuableHostTrouve.getAdresse().getNumero());

							resultatRechercheContribuable.setLocaliteAdresse(contribuableHostTrouve.getAdresse().getLocalite());
							resultatRechercheContribuable.setNumeroPostalAdresse(contribuableHostTrouve.getAdresse().getNumeroPostal());
							resultatRechercheContribuable.setNumeroPostalComplementaireAdresse(contribuableHostTrouve.getAdresse()
									.getNumeroPostalComplementaire());
							resultatRechercheContribuable.setNumeroOrdrePostalAdresse(contribuableHostTrouve.getAdresse().getNumeroOrdrePostal());

						}

						/** Récupération des information sur l'individu conjoint. */
						if (individuPrincipal.getConjoint() != null) {
							Individu individuConjoint = individuPrincipal.getConjoint();

							resultatRechercheContribuable.setNoTechniqueIndividuConjoint(individuConjoint.getNoTechnique());
							resultatRechercheContribuable.setPrenomIndividuConjoint(individuConjoint.getDernierHistoriqueIndividu().getPrenom());
							resultatRechercheContribuable.setNomIndividuConjoint(individuConjoint.getDernierHistoriqueIndividu().getNom());
							resultatRechercheContribuable.setDateNaissanceIndividuConjoint(individuConjoint.getDateNaissance());
							resultatRechercheContribuable.setSexeMasculinIndividuConjoint(individuConjoint.isSexeMasculin());


						}
					}
					return resultatRechercheContribuable;
				}
	     }
	}
	return null;

	}
	/**
	 * Permet de tester si l'adresse du contribuable en parametre correspond au critères de recherches
	 * localité et npa
	 * @return
	 * @throws Exception
	 */
	private boolean isNpaCompatible(Contribuable contribuable, Integer npa ) {

		if(npa==null){
			return true;
		}
		Adresse adresse = contribuable.getAdresse();
		if (adresse!=null) {
			if (npa.intValue() > 0 && npa.toString().equalsIgnoreCase(adresse.getNumeroPostal())) {
				return true;
			}
		}

		return false;
	}


	private boolean isCriteresVides(SearchTiers searchTiers){
		if (searchTiers.getDateNaissance()==null &&
			searchTiers.getLocaliteOuPays()==null &&
			searchTiers.getNomCourrier()==null &&
			searchTiers.getNumeroAVS()==null) {
			return true;
		}
		return  false;
	}
	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setModeleDocumentDAO(ModeleDocumentDAO modeleDAO) {
		this.modeleDocumentDAO = modeleDAO;
	}

	public void setServiceInfrastructure(ServiceInfrastructure serviceInfrastructure) {
		this.serviceInfrastructure = serviceInfrastructure;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	/*
	 * public TiersService getTiersService() { return tiersService; }
	 *
	 * public void setTiersService(TiersService tiersService) { this.tiersService = tiersService; }
	 */

	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	public ServiceInfrastructure getServiceInfrastructure() {
		return serviceInfrastructure;
	}

	public AdresseService getAdresseService() {
		return adresseService;
	}

	public ServiceCivil getServiceCivil() {
		return serviceCivil;
	}

	public void setServiceCivil(ServiceCivil serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public SituationFamilleService getSituationFamilleService() {
		return situationFamilleService;
	}

	public void setSituationFamilleService(SituationFamilleService situationFamilleService) {
		this.situationFamilleService = situationFamilleService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public DeclarationImpotService getDiService() {
		return diService;
	}

	public void setDiService(DeclarationImpotService diSevice) {
		this.diService = diSevice;
	}





	public void setIbanValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	public void setServiceTiersPort(TiersPort serviceTiersPort) {
		this.serviceTiersPort = serviceTiersPort;
	}

	public void setLogin(UserLogin login) {
		this.login = login;
	}

}
