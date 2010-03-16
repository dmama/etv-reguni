package ch.vd.uniregctb.rattrapage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesFiscalesHisto;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.evenement.common.EnsembleTiersCouple;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.rattrapage.rapport.RattrapageForResults;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.TiersDAO.Parts;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class ForManager {

	private final int BATCH_SIZE = 100;

	private PlatformTransactionManager transactionManager;

	private HibernateTemplate hibernateTemplate;

	private TiersDAO tiersDAO;

	private AdresseService adresseService;

	private ServiceInfrastructureService serviceInfra;
	private TiersService tiersService;

	private GlobalTiersIndexer globalTiersIndexer;

	private RattrapageForResults rapport;

	private static final Logger LOGGER = Logger.getLogger(ForManager.class);
	private static final Logger RAPPORT = Logger.getLogger(ForManager.class.getName() + ".Rapport");
	private static final Logger ERROR = Logger.getLogger(ForManager.class.getName() + ".Error");
	private static final Logger HORSSUISSE = Logger.getLogger(ForManager.class.getName() + ".HorsSuisse");
	private static final Logger SANSADRESSE = Logger.getLogger(ForManager.class.getName() + ".SansAdresse");
	private static final Logger SUISSEPERMISC = Logger.getLogger(ForManager.class.getName() + ".estSuisse");
	private static final Logger DECEDE = Logger.getLogger(ForManager.class.getName() + ".Decede");

	public void rattraperForSourcier(final LoggingStatusManager statutManager) {

		globalTiersIndexer.setOnTheFlyIndexation(false);

		final List<Long> listATraiter = new ArrayList<Long>();
		LOGGER.info("Chargement de la liste des ménages commun sans for");
		final List<Long> listeMenageCommun = getListeMenageCommun();
		LOGGER.info("Chargement terminée: " + listeMenageCommun.size() + " menages communs chargés");
		listATraiter.addAll(listeMenageCommun);
		LOGGER.info("Chargement de la liste des Personnes Physiques sans for");
		final List<Long> listPersonnePhysique = getListPersonnePhysique();
		LOGGER.info("Chargement terminée: " + listPersonnePhysique.size() + " personnes physiques chargées");
		listATraiter.addAll(listPersonnePhysique);

		 //final List<Long> listATraiter = new ArrayList<Long>();
		// listATraiter.add(10713548L);

		final int nombreContribuables = listATraiter.size();
		LOGGER.info("Chargement terminée: " + nombreContribuables + " contribuables chargés");


		final RattrapageForResults rapportFinal = new RattrapageForResults(nombreContribuables);

		final BatchTransactionTemplate<Long> template = new BatchTransactionTemplate<Long>(listATraiter, BATCH_SIZE,
				Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, hibernateTemplate);

		template.execute(new BatchCallback<Long>() {

			private Long idCtb = null;
			private List<Long> batchEnCours;

			@Override
			public void beforeTransaction() {
				rapport = new RattrapageForResults();
				idCtb = null;
			}

			@Override
			public boolean doInTransaction(List<Long> batch) throws Exception {
				batchEnCours = batch;
				LOGGER.info("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...");

				if (batch.size() == 1) {
					idCtb = batch.get(0);
				}
				traiterBatch(batch);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				String message = "===> Rollback du batch [" + batchEnCours.get(0) + "-" + batchEnCours.get(batchEnCours.size() - 1)
						+ "] willRetry=" + willRetry;

				if (willRetry) {
					// le batch va être rejoué -> on peut ignorer le rapport
					rapport = null;
				}
				else {
					// on ajoute l'exception directement dans le rapport final
					rapportFinal.addError(idCtb + ";" + e.getMessage());
					rapport = null;
				}
			}

			@Override
			public void afterTransactionCommit() {
				rapportFinal.add(rapport);
				statutManager.setMessage("Traitement des contribuables:", percent);

			}
		});
		ecrireRapport( rapportFinal);
	}

	@SuppressWarnings("unchecked")
	private List<Long> getListeMenageCommun() {
		final String query = // --------------------------------
		"SELECT DISTINCT                                                                         "
				+ "    rapportMenage.objet.id                                                    "
				+ "FROM                                                                          "
				+ "    Contribuable AS cont                                                      "
				+ "INNER JOIN                                                                    "
				+ "    cont.rapportsSujet AS rapportPrestation                                   "
				+ "INNER JOIN                                                                    "
				+ "    cont.rapportsSujet AS rapportMenage                                       "
				+ "WHERE                                                                         "
				+ "    rapportPrestation.class = RapportPrestationImposable                      "
				+ "    AND rapportPrestation.annulationDate IS null                              "
				+ "    AND rapportPrestation.dateFin IS null                                     "
				+ "    AND rapportMenage.class = AppartenanceMenage                              "
				+ "    AND rapportMenage.annulationDate IS null                                  "
				+ "    AND rapportMenage.dateFin IS null                                         "
				+ "    AND  NOT EXISTS (                                                         "
				+ "                SELECT                                                        "
				+ "                  fors                                                        "
				+ "                FROM                                                          "
				+ "                    Contribuable As menage                                    "
				+ "                INNER JOIN                                                    "
				+ "                    menage.forsFiscaux as fors                                "
				+ "                WHERE                                                         "
				+ "                    fors.annulationDate IS null                               "
				+ "                    AND  menage.id = rapportMenage.objet.id                   "
				+ "              )                                                               "
				+ "ORDER BY rapportMenage.objet.id ASC                                           ";

		List<Long> resultat = hibernateTemplate.find(query);
		return resultat;
	}

	private List<Long> getListPersonnePhysique() {

		final String query = // --------------------------------
		"SELECT DISTINCT                                                                         "
				+ "   cont.id                                                                    "
				+ "FROM                                                                          "
				+ "    Contribuable AS cont                                                      "
				+ "INNER JOIN                                                                    "
				+ "    cont.rapportsSujet AS rapportPrestation                                   "
				+ "WHERE                                                                         "
				+ "    rapportPrestation.class = RapportPrestationImposable                      "
				+ "    AND rapportPrestation.annulationDate IS null                              "
				+ "    AND rapportPrestation.dateFin IS null                                     "
				+ "    AND  NOT EXISTS (                                                         "
				+ "                SELECT                                                        "
				+ "                  personne                                                    "
				+ "                FROM                                                          "
				+ "                    Contribuable As personne                                  "
				+ "                INNER JOIN                                                    "
				+ "                    personne.rapportsSujet AS rapportMenage                   "
				+ "                WHERE                                                         "
				+ "                    rapportMenage.class = AppartenanceMenage                  "
				+ "                    AND rapportMenage.annulationDate IS null                  "
				+ "                    AND rapportMenage.dateFin IS null                         "
				+ "                    AND cont.id = personne.id                                 "
				+ "              )                                                               "
				+ "    AND  NOT EXISTS (                                                         "
				+ "                SELECT                                                        "
				+ "                  fors                                                        "
				+ "                FROM                                                          "
				+ "                    Contribuable As personne                                  "
				+ "                INNER JOIN                                                    "
				+ "                    personne.forsFiscaux as fors                              "
				+ "                WHERE                                                         "
				+ "                    fors.annulationDate IS null                               "
				+ "                    AND cont.id = personne.id                                 "
				+ "              )                                                               "
				+ "ORDER BY cont.id ASC                                                          ";

		List<Long> resultat = hibernateTemplate.find(query);
		return resultat;
	}

	private void traiterBatch(List<Long> batch) throws AdressesResolutionException, InfrastructureException {
		Canton vaud = serviceInfra.getVaud();
		List<Tiers> listeTiers = getAllTiers(batch);

		for (Tiers tiers : listeTiers) {

			boolean aTraiter = true;
			if (tiers instanceof PersonnePhysique) {
				PersonnePhysique personne = (PersonnePhysique) tiers;
				if (personne.isHabitant()) {
					try {
						if (tiersService.isSuisse(personne, RegDate.get())
								|| tiersService.isHabitantEtrangerAvecPermisC(personne, RegDate.get())) {
							aTraiter = false;
							rapport.addSuissePermisC(tiers.getNumero().toString());

						}
					}
					catch (TiersException e) {
						// Impossible de determiner la nationalité ouo le permis c: on traite quand même
						aTraiter = true;
					}

					if (tiersService.isDecede(personne)) {
						aTraiter = false;
						rapport.addDecede(tiers.getNumero().toString());

					}
				}

			}

			if (aTraiter) {
				traiterTiers(tiers, vaud);
			}

		}

	}

	private void traiterTiers(Tiers tiers, Canton vaud) throws InfrastructureException, AdressesResolutionException {
		int noCommune = 0;
		AdresseGenerique adresse = adresseService.getAdresseFiscale(tiers, TypeAdresseTiers.DOMICILE, null);
		if (adresse != null) {
			Commune commune = serviceInfra.getCommuneByAdresse(adresse);
			if (commune != null) {
				noCommune = commune.getNoOFSEtendu();
				creerForSource(tiers, noCommune, vaud);

			}
			else {

				//traiterLesHorsSuisses(tiers);
				rapport.addHorsSuisse(tiers.getNumero().toString());


			}

		}
		else {
			noCommune = getCommuneOfDebiteur(tiers);
			Commune communeDebiteur = serviceInfra.getCommuneByNumeroOfsEtendu(noCommune);
			if (communeDebiteur == null || !serviceInfra.estDansLeCanton(communeDebiteur)) {
				rapport.addSansAdresse(tiers.getNumero().toString());
			}
			else{
				creerForSource(tiers, noCommune, vaud);
			}

		}


	}

	private void traiterLesHorsSuisses(Tiers tiers) throws AdressesResolutionException, InfrastructureException {

		AdresseGenerique avantDerniereAdresse=null;
		AdresseGenerique derniereAdresse=null;
		AdressesFiscalesHisto adresses =  adresseService.getAdressesFiscalHisto(tiers);
		List<AdresseGenerique> adressesDomicile = ordonnerAdresse(adresses.domicile);
		if (adressesDomicile==null || adressesDomicile.size()==0) {
			rapport.addHorsSuisse(tiers.getNumero().toString());

		}
		else if (adressesDomicile.size()==1){
			derniereAdresse =  adressesDomicile.get(0);

		}
		else if (adressesDomicile.size()>1) {
			int nombreAdresse = adressesDomicile.size();
			avantDerniereAdresse =  adressesDomicile.get(nombreAdresse-2);
			derniereAdresse =  adressesDomicile.get(nombreAdresse-1);
		}

		if (avantDerniereAdresse!=null) {
			Commune commune= serviceInfra.getCommuneByAdresse(avantDerniereAdresse);
			if (commune !=null) {

			}
			else {

			}


		}
		else {

		}


	}

	private List<AdresseGenerique> ordonnerAdresse(List<AdresseGenerique> domicile) {

		Collections.sort(domicile, new Comparator<AdresseGenerique>() {

			public int compare(AdresseGenerique o1, AdresseGenerique o2) {
				return o1.getDateDebut().compareTo(o2.getDateDebut());
			}
		});
		return domicile;
	}

	private int getCommuneOfDebiteur(Tiers tiers) {
		DebiteurPrestationImposable debiteur = null;
		if (tiers instanceof PersonnePhysique) {
			PersonnePhysique personne = (PersonnePhysique) tiers;
			debiteur = getDernierDPI(personne);
		}
		else if (tiers instanceof MenageCommun) {
			MenageCommun menage = (MenageCommun) tiers;
			PersonnePhysique principal = tiersService.getPrincipal(menage);
			debiteur = getDernierDPI(principal);

		}
		ForDebiteurPrestationImposable forDebiteur = debiteur.getDernierForDebiteur();

		if (forDebiteur == null) {
			return 0;
		}

		return forDebiteur.getNumeroOfsAutoriteFiscale();
	}

	private DebiteurPrestationImposable getDernierDPI(PersonnePhysique personne) {
		List<RapportPrestationImposable> listeRapport = getRapportPrestationImposable(personne);
		DebiteurPrestationImposable debiteur = null;
		RegDate dateDebut = RegDate.getEarlyDate();
		for (RapportPrestationImposable rapportPrestationImposable : listeRapport) {
			if (rapportPrestationImposable.getDateDebut().isAfter(dateDebut)) {
				dateDebut = rapportPrestationImposable.getDateDebut();
				debiteur = (DebiteurPrestationImposable) rapportPrestationImposable.getObjet();
			}
		}
		return debiteur;
	}



	private void creerForSource(Tiers tiers, Commune commune,AdresseGenerique adresseCourante,AdresseGenerique adresseSuivante, Canton vaud) throws InfrastructureException {

		final int noOFSCommune = commune.getNoOFSEtendu();

		RegDate dateOuverture = adresseCourante.getDateDebut();



		ForFiscalPrincipal ffp = new ForFiscalPrincipal();
		ffp.setMotifRattachement(MotifRattachement.DOMICILE);

		ffp.setDateDebut(dateOuverture);
		ffp.setDateFin(null);
		if (adresseSuivante!=null) {
			ffp.setMotifOuverture(MotifFor.ARRIVEE_HS);
			ffp.setTypeAutoriteFiscale(determineAutoriteFiscal(vaud, noOFSCommune));
		}
		else {
			ffp.setMotifOuverture(MotifFor.DEPART_HS);
		//	serviceInfra.getPays(codePays)
			ffp.setTypeAutoriteFiscale(determineAutoriteFiscal(vaud, noOFSCommune));
		}

		ffp.setModeImposition(ModeImposition.SOURCE);
		ffp.setGenreImpot(GenreImpot.REVENU_FORTUNE);


		ffp.setNumeroOfsAutoriteFiscale(noOFSCommune);
		tiersService.addAndSave(tiers, ffp);
		rapport.addResultat(tiers.getNumero() + ";" + noOFSCommune);

	}


	private void creerForSource(Tiers tiers, int noCommune, Canton vaud) throws InfrastructureException {

		final int noOFSCommune = noCommune;

		RegDate dateOuverture = RegDate.get(2009, 1, 1);

		// Calcul de la date d'ouverture du for
		if (tiers instanceof MenageCommun) {
			MenageCommun menage = (MenageCommun) tiers;
			RapportEntreTiers rapport = menage.getRapportObjetValidAt(RegDate.get(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
			if (dateOuverture.isBefore(rapport.getDateDebut())) {
				dateOuverture = rapport.getDateDebut();
			}
		}
		else if (tiers instanceof PersonnePhysique) {
			// Recherche d'un ménage commun à la date d'ouverture du for
			PersonnePhysique personne = (PersonnePhysique) tiers;
			EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(personne, dateOuverture);
			if (ensemble != null) {
				MenageCommun menage = ensemble.getMenage();
				RapportEntreTiers rapport = menage.getRapportObjetValidAt(dateOuverture, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				if (rapport.getDateFin() != null) {
					dateOuverture = rapport.getDateFin().getOneDayAfter();
				}
			}

		}

		ForFiscalPrincipal ffp = new ForFiscalPrincipal();
		ffp.setMotifRattachement(MotifRattachement.DOMICILE);

		ffp.setDateDebut(dateOuverture);
		ffp.setDateFin(null);
		ffp.setMotifOuverture(MotifFor.ARRIVEE_HS);
		ffp.setModeImposition(ModeImposition.SOURCE);
		ffp.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp.setTypeAutoriteFiscale(determineAutoriteFiscal(vaud, noOFSCommune));
		ffp.setNumeroOfsAutoriteFiscale(noOFSCommune);
		tiersService.addAndSave(tiers, ffp);
		rapport.addResultat(tiers.getNumero() + ";" + noOFSCommune);

	}

	public List<Tiers> getListTiers(List<Long> batch) {
		final Set<Parts> parts = new HashSet<Parts>();
		parts.add(Parts.ADRESSES);
		final List<Tiers> tiers = tiersDAO.getBatch(batch, parts);

		return tiers;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setGlobalTiersIndexer(GlobalTiersIndexer globalTiersIndexer) {
		this.globalTiersIndexer = globalTiersIndexer;
	}

	private TypeAutoriteFiscale determineAutoriteFiscal(Canton vaud, int numeroCommuneResidence) throws InfrastructureException {
		Canton cantonByCommune = serviceInfra.getCantonByCommune(numeroCommuneResidence);

		if (cantonByCommune != null && vaud.getSigleOFS().equals(cantonByCommune.getSigleOFS())) {
			return TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
		}
		else if (cantonByCommune != null && !vaud.getSigleOFS().equals(cantonByCommune.getSigleOFS())) {
			return TypeAutoriteFiscale.COMMUNE_HC;
		}

		return null;
	}

	private List<Tiers> getAllTiers(List<Long> batch) {
		List<Tiers> listeTiers = new ArrayList<Tiers>();
		if (batch.size() > 1) {
			listeTiers = getListTiers(batch);
			// On repasse la session en automatique, elle a été mise en MANUAL dans getBatch
			Session session = hibernateTemplate.getSessionFactory().getCurrentSession();
			session.setFlushMode(FlushMode.AUTO);
		}
		else {
			listeTiers.add(tiersService.getTiers(batch.get(0)));
		}
		return listeTiers;
	}

	private List<RapportPrestationImposable> getRapportPrestationImposable(PersonnePhysique personneDoublon) {
		String queryWhere = " and rapport.sujet.numero = " + personneDoublon.getNumero();
		String annulationDate = " and rapport.annulationDate is null";

		final String query = " select rapport from RapportPrestationImposable rapport where 1=1 " + queryWhere + annulationDate;
		return hibernateTemplate.find(query);

	}

	private void ecrireRapport(RattrapageForResults rapportFinal) {

		LOGGER.info("Nombre de contribuables à traiter :" + rapportFinal.nombreCtbCharges);
		LOGGER.info("Nombre de contribuables traités avec ajout de for :" + rapportFinal.nbCtbsTotal);
		LOGGER.info("Nombre de Suisse ou Permis C :" + rapportFinal.nbSuissePermisC);
		LOGGER.info("Nombre Hors suisse :" + rapportFinal.nbHorsSuisse);
		LOGGER.info("Nombre de sans adresse :" + rapportFinal.nbSansAdresse);
		LOGGER.info("Nombre de Décédés:" + rapportFinal.nbDecede);
		LOGGER.info("Nombre d'erreurs:" + rapportFinal.nbErrors);

		List<String> listeMessage = rapportFinal.listeResultats;
		for (String message : listeMessage) {
			RAPPORT.info(message);
		}
		listeMessage = rapportFinal.listeSansAdresse;
		for (String message : listeMessage) {
			SANSADRESSE.info(message);
		}
		listeMessage = rapportFinal.listeSuissePermisC;
		for (String message : listeMessage) {
			SUISSEPERMISC.info(message);
		}
		listeMessage = rapportFinal.listeError;
		for (String message : listeMessage) {
			ERROR.info(message);
		}
		listeMessage = rapportFinal.listeHorsSuisse;
		for (String message : listeMessage) {
			HORSSUISSE.info(message);
		}

		listeMessage = rapportFinal.listeDecede;
		for (String message : listeMessage) {
			DECEDE.info(message);
		}

	}

}
