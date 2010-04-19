package ch.vd.uniregctb.rattrapage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.evenement.common.EnsembleTiersCouple;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.rattrapage.rapport.RattrapageDoublonResults;
import ch.vd.uniregctb.rattrapage.rapport.RattrapageMarieSeul;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeActivite;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class ContribuableManager {

	private final int BATCH_SIZE = 100;

	private PlatformTransactionManager transactionManager;

	private HibernateTemplate hibernateTemplate;

	private TiersDAO tiersDAO;

	private AdresseService adresseService;

	private ServiceInfrastructureService serviceInfra;
	private TiersService tiersService;
	private ServiceCivilService serviceCivilService;

	private GlobalTiersIndexer globalTiersIndexer;
	private RattrapageDoublonResults rapport;
	private RattrapageMarieSeul rapportMarieCourant;

	private static final Logger LOGGER = Logger.getLogger(ContribuableManager.class);
	private static final Logger RAPPORT = Logger.getLogger(ContribuableManager.class.getName() + ".Rapport");
	private static final Logger RAPPORTFORDOUBLON = Logger.getLogger(ContribuableManager.class.getName() + ".For");
	private static final Logger ERROR = Logger.getLogger(ContribuableManager.class.getName() + ".Error");
	private static final Logger DOUBLONHORSMIGRATION = Logger.getLogger(ContribuableManager.class.getName() + ".HorsMigration");
	private static final Logger CONJOINTDOUBLON = Logger.getLogger(ContribuableManager.class.getName() + ".conjoint");


	private static final Logger RAPPORTFORSUPPRIME = Logger.getLogger(ContribuableManager.class.getName() + ".ForSupprime");


	@Transactional
	public void rattraperDoublont(final LoggingStatusManager statutManager) {

		globalTiersIndexer.setOnTheFlyIndexation(false);

		final List<Long> listATraiter = getDoublon();
		//final List<Long> listATraiter = new ArrayList<Long>();
		// listATraiter.add(10687809L);
		final int nombreDoublon = listATraiter.size();
		final RattrapageDoublonResults rapportFinal = new RattrapageDoublonResults(nombreDoublon);
		LOGGER.info("Chargement terminée: " + nombreDoublon + " doublon chargés");

		final BatchTransactionTemplate<Long> template = new BatchTransactionTemplate<Long>(listATraiter, BATCH_SIZE,
				Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, hibernateTemplate);

		template.execute(new BatchCallback<Long>() {

			private Long idCtb = null;
			private List<Long> batchEnCours;

			@Override
			public void beforeTransaction() {
				rapport = new RattrapageDoublonResults();
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
				statutManager.setMessage("Traitement des doublons:", percent);
			}
		});

		ecrireRapport(rapportFinal);

	}



	public void rattraperMarieSeul(final LoggingStatusManager statutManager) {

		globalTiersIndexer.setOnTheFlyIndexation(false);

		final List<Long> listATraiter = getMarieSeul();
		//final List<Long> listATraiter = new ArrayList<Long>();
		// listATraiter.add(10687809L);
		final int nombreMarieSeul = listATraiter.size();
		final RattrapageMarieSeul rapportFinal = new RattrapageMarieSeul(nombreMarieSeul);
		LOGGER.info("Chargement terminée: " + nombreMarieSeul + " mariés seuls chargés");

		final BatchTransactionTemplate<Long> template = new BatchTransactionTemplate<Long>(listATraiter, BATCH_SIZE,
				Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, hibernateTemplate);

		template.execute(new BatchCallback<Long>() {

			private Long idCtb = null;
			private List<Long> batchEnCours;

			@Override
			public void beforeTransaction() {
				rapportMarieCourant = new RattrapageMarieSeul();
				idCtb = null;
			}

			@Override
			public boolean doInTransaction(List<Long> batch) throws Exception {
				batchEnCours = batch;
				LOGGER.info("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...");

				if (batch.size() == 1) {
					idCtb = batch.get(0);
				}
				traiterBatchMarieSeul(batch);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				String message = "===> Rollback du batch [" + batchEnCours.get(0) + "-" + batchEnCours.get(batchEnCours.size() - 1)
						+ "] willRetry=" + willRetry;


				if (willRetry) {
					// le batch va être rejoué -> on peut ignorer le rapport
					rapportMarieCourant = null;
				}
				else {
					// on ajoute l'exception directement dans le rapport final
					rapportFinal.addError(idCtb + ";" + e.getMessage());
					rapportMarieCourant = null;
				}
			}

			@Override
			public void afterTransactionCommit() {
				rapportFinal.add(rapportMarieCourant);
				statutManager.setMessage("Traitement des mariés seuls posant problème:", percent);
			}
		});

		ecrireRapportMarieSeul(rapportFinal);

	}


	private void ecrireRapport(RattrapageDoublonResults rapportFinal) {

		LOGGER.info("Nombre de doublons à traiter :" + rapportFinal.nombreCtbCharges);
		LOGGER.info("Nombre de doublons traités :" + rapportFinal.nbCtbsTotal);
		LOGGER.info("Nombre de for sur les doublons :" + rapportFinal.nbCtbFors);
		LOGGER.info("Nombre de doublons hors migrations IS :" + rapportFinal.nbHorsMigration);
		LOGGER.info("Nombre d'erreurs:" + rapportFinal.nbErrors);
		LOGGER.info("Nombre de conjoint:" + rapportFinal.nbConjoint);

		RAPPORT.info("numéro du CTB doublon; numéro du CTB correct");
		RAPPORTFORDOUBLON.info("Numéro ctb;Menage Commun;Date Debut;Date Fin;modeImposition;Numero OFS Autorite fiscale");
		DOUBLONHORSMIGRATION.info("Numéro ctb;visa de creation");
		ERROR.info("numéro du CTB doublon; message erreur");
		CONJOINTDOUBLON.info("numéro du conjoint");

		List<String> listeMessage = rapportFinal.listeResultats;
		for (String message : listeMessage) {
			RAPPORT.info(message);
		}
		listeMessage = rapportFinal.listeFor;
		for (String message : listeMessage) {
			RAPPORTFORDOUBLON.info(message);
		}
		listeMessage = rapportFinal.listeHorsMigrationIs;
		for (String message : listeMessage) {
			DOUBLONHORSMIGRATION.info(message);
		}
		listeMessage = rapportFinal.listeError;
		for (String message : listeMessage) {
			ERROR.info(message);
		}
		listeMessage = rapportFinal.listeConjoint;
		for (String message : listeMessage) {
			CONJOINTDOUBLON.info(message);
		}

	}

	private void ecrireRapportMarieSeul(RattrapageMarieSeul rapportFinal) {

		LOGGER.info("Nombre de Maries Seuls créés depuis le 15 Mars 2010 :" + rapportFinal.nombreCtbCharges);
		LOGGER.info("Nombre de Maries seuls posant problème et corrigés :" + rapportFinal.nbCtbsTotal);
		LOGGER.info("Nombre de fors fermés :" + rapportFinal.nbCtbFors);
		LOGGER.info("Nombre d'erreurs:" + rapportFinal.nbErrors);
		LOGGER.info("Nombre de couples reformés:" + rapportFinal.nbConjoint);

		RAPPORT.info("numéro du marie seul; numéro du ménage;numéro du conjoint;Numéro OFS autorité fiscal(null si pas de for)");
		RAPPORTFORSUPPRIME.info("Numéro ctb;Date Debut;Date Fin;modeImposition;Numero OFS Autorite fiscale;Type");

		ERROR.error("numéro du marié seul; message erreur");


		List<String> listeMessage = rapportFinal.listeResultats;
		for (String message : listeMessage) {
			RAPPORT.info(message);
		}
		listeMessage = rapportFinal.listeFor;
		for (String message : listeMessage) {
			RAPPORTFORSUPPRIME.info(message);
		}

		listeMessage = rapportFinal.listeError;
		for (String message : listeMessage) {
			ERROR.error(message);
		}


	}

	private void traiterBatch(List<Long> batch) {
		List<Tiers> listeTiers = getAllTiers(batch);

		for (Tiers tiers : listeTiers) {
			// Traitement des doublons créées par migration
			if ("[HostImpotSourceMigrator thread]".equals(tiers.getLogCreationUser())) {

				PersonnePhysique personneDoublon = (PersonnePhysique) tiers;
				PersonnePhysique personneCorrecte = getPersonneCorrecte(personneDoublon);
				traiterDoublon(personneDoublon, personneCorrecte);
				LOGGER.debug(tiers.getNumero() + " annulé");
			}
			else {
				rapport.addHorsMigrationIs(tiers.getNumero() + ";" + tiers.getLogCreationUser());

			}

		}

	}

	private void traiterBatchMarieSeul(List<Long> batch) {
		List<Tiers> listeTiers = getAllTiers(batch);

		for (Tiers tiers : listeTiers) {
			// Traitement des maries Seuls
			PersonnePhysique personneMarieSeul = (PersonnePhysique) tiers;
			if (personneMarieSeul.isHabitant()) {
				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(personneMarieSeul, RegDate.get());
				final MenageCommun menage = ensemble.getMenage();
				final RapportEntreTiers rapport = personneMarieSeul.getDernierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				final Long numeroIndividuCourant = personneMarieSeul.getNumeroIndividu();
				final Individu individuConjoint = serviceCivilService.getConjoint(numeroIndividuCourant, RegDate.get());
				if (individuConjoint != null) {
					PersonnePhysique conjoint = tiersService.getPersonnePhysiqueByNumeroIndividu(individuConjoint.getNoTechnique());
					if (conjoint != null) {
						//fermeture des fors fiscaux
						final RegDate dateMariage = rapport.getDateDebut();
						RegDate dateFermeture = dateMariage.getOneDayBefore();
						fermerForFiscaux(conjoint, dateFermeture, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

						//tiersService.addTiersToCouple(menage, conjoint, dateMariage, null);
						Integer numeroAutorite = null;
						if(menage.getDernierForFiscalPrincipal()!=null){
							numeroAutorite = menage.getDernierForFiscalPrincipal().getNumeroOfsAutoriteFiscale();
						}
						rapportMarieCourant.addResultat(personneMarieSeul.getNumero() + ";" + menage.getNumero() + ";" + conjoint.getNumero()+";"+numeroAutorite);
						rapportMarieCourant.addConjoint(conjoint.getNumero()+";"+numeroAutorite);
					}
				}
			
			}

			LOGGER.debug(tiers.getNumero());


		}

	}

	private void fermerForFiscaux(PersonnePhysique conjoint, RegDate dateFermeture, MotifFor motifFermeture) {
		final List<ForFiscal> fors = conjoint.getForsFiscauxValidAt(dateFermeture);
		for (ForFiscal forFiscal : fors) {
			if (forFiscal.getDateFin() == null) {
				if (forFiscal instanceof ForFiscalPrincipal) {
					// voir commentaire plus bas
					//closeForFiscalPrincipal(contribuable, (ForFiscalPrincipal)forFiscal, dateFermeture, motifFermeture);
				}
				else if (forFiscal instanceof ForFiscalSecondaire) {
					closeForFiscalSecondaire((ForFiscalSecondaire) forFiscal, dateFermeture, motifFermeture, conjoint);
				}
				else if (forFiscal instanceof ForFiscalAutreElementImposable) {
					closeForFiscalAutreElementImposable((ForFiscalAutreElementImposable) forFiscal, dateFermeture, motifFermeture, conjoint);
				}
				else if (forFiscal instanceof ForFiscalAutreImpot) {
					closeForAutreImpot((ForFiscalAutreImpot) forFiscal, dateFermeture, conjoint);
				}
				//else if (forFiscal instanceof ForDebiteurPrestationImposable) {//impossible
			}
		}
		/*
		 * La fermeture est faite en 2 étapes pour suivre la logique métier:
		 *  - aucun for secondaire ne doit exister s'il nya pas de for principal
		 * Pour cette raison les fors secondaires, autre élément imposable et
		 * autre impot doivent être fermés avant le principal.
		 */
		for (ForFiscal forFiscal : fors) {
			if (forFiscal.getDateFin() == null) {
				if (forFiscal instanceof ForFiscalPrincipal) {
					closeForFiscalPrincipal((ForFiscalPrincipal) forFiscal, dateFermeture, motifFermeture, conjoint);
				}
			}
		}
	}

	private void closeForFiscalPrincipal(ForFiscalPrincipal forFiscalPrincipal, RegDate dateFermeture, MotifFor motifFermeture, PersonnePhysique conjoint) {
		if (forFiscalPrincipal.getDateDebut().isAfter(dateFermeture)) {
			throw new ValidationException(forFiscalPrincipal, "La date de fermeture (" + RegDateHelper.dateToDisplayString(dateFermeture) + ") est avant la date de début (" +
					RegDateHelper.dateToDisplayString(forFiscalPrincipal.getDateDebut())
					+ ") du for fiscal actif");
		}

	//	forFiscalPrincipal.setDateFin(dateFermeture);
	//	forFiscalPrincipal.setMotifFermeture(motifFermeture);
		rapportMarieCourant.addFor(conjoint.getNumero()+ ";"
				+ forFiscalPrincipal.getDateDebut()+ ";"
				+ forFiscalPrincipal.getDateFin()+ ";"
				+ forFiscalPrincipal.getModeImposition()+ ";"
				+ forFiscalPrincipal.getNumeroOfsAutoriteFiscale()+	";"
				+ forFiscalPrincipal.getTypeAutoriteFiscale());
	}

	private void closeForFiscalSecondaire(ForFiscalSecondaire forFiscalSecondaire, RegDate dateFermeture, MotifFor motifFermeture, PersonnePhysique conjoint) {
		if (forFiscalSecondaire != null) {
			if (forFiscalSecondaire.getDateDebut().isAfter(dateFermeture)) {
				throw new ValidationException(forFiscalSecondaire, "La date de fermeture ("
						+ RegDateHelper.dateToDisplayString(dateFermeture) + ") est avant la date de début ("
						+ RegDateHelper.dateToDisplayString(forFiscalSecondaire.getDateDebut()) + ") du for fiscal actif");
			}
		//	forFiscalSecondaire.setDateFin(dateFermeture);
		//	forFiscalSecondaire.setMotifFermeture(motifFermeture);
			rapportMarieCourant.addFor(conjoint.getNumero()+ ";"
				+ forFiscalSecondaire.getDateDebut()+ ";"
				+ forFiscalSecondaire.getDateFin()+ ";"
				+ "Secondaire;"
				+ forFiscalSecondaire.getNumeroOfsAutoriteFiscale()+	";"
				+ forFiscalSecondaire.getTypeAutoriteFiscale());
		}
	}

	private void closeForAutreImpot(ForFiscalAutreImpot forFiscalAutreImpot, RegDate dateFermeture, PersonnePhysique conjoint) {
		//Toif (autre != null) {
		if (forFiscalAutreImpot.getDateDebut().isAfter(dateFermeture)) {
			throw new ValidationException(forFiscalAutreImpot, "La date de fermeture ("
					+ RegDateHelper.dateToDisplayString(dateFermeture) + ") est avant la date de début ("
					+ RegDateHelper.dateToDisplayString(forFiscalAutreImpot.getDateDebut()) + ") du for fiscal actif");
		}
	//	forFiscalAutreImpot.setDateFin(dateFermeture);
		rapportMarieCourant.addFor(conjoint.getNumero()+ ";"
				+ forFiscalAutreImpot.getDateDebut()+ ";"
				+ forFiscalAutreImpot.getDateFin()+ ";"
				+ "autrImpot;"
				+ forFiscalAutreImpot.getNumeroOfsAutoriteFiscale()+	";"
				+ forFiscalAutreImpot.getTypeAutoriteFiscale());
	}

	private void closeForFiscalAutreElementImposable(ForFiscalAutreElementImposable forFiscalAutreElementImposable, RegDate dateFermeture, MotifFor motifFermeture, PersonnePhysique conjoint) {
		if (forFiscalAutreElementImposable != null) {
			if (forFiscalAutreElementImposable.getDateDebut().isAfter(dateFermeture)) {
				throw new ValidationException(forFiscalAutreElementImposable, "La date de fermeture ("
						+ RegDateHelper.dateToDisplayString(dateFermeture) + ") est avant la date de début ("
						+ RegDateHelper.dateToDisplayString(forFiscalAutreElementImposable.getDateDebut()) + ") du for fiscal actif");
			}
			//forFiscalAutreElementImposable.setDateFin(dateFermeture);
		//	forFiscalAutreElementImposable.setMotifFermeture(motifFermeture);
			rapportMarieCourant.addFor(conjoint.getNumero()+ ";"
				+ forFiscalAutreElementImposable.getDateDebut()+ ";"
				+ forFiscalAutreElementImposable.getDateFin()+ ";"
					+ "autrElementImposable;"
				+ forFiscalAutreElementImposable.getNumeroOfsAutoriteFiscale()+	";"
				+ forFiscalAutreElementImposable.getTypeAutoriteFiscale());
		}
	}

	private List<Tiers> getAllTiers(List<Long> batch) {
		List<Tiers> listeTiers = new ArrayList<Tiers>();
		for (Long numero : batch) {
			listeTiers.add(tiersService.getTiers(numero));
		}


		return listeTiers;
	}

	private void traiterDoublon(PersonnePhysique personneDoublon, PersonnePhysique personneCorrecte) {
		personneCorrecte.setAncienNumeroSourcier(personneDoublon.getAncienNumeroSourcier());

		String remarque = personneCorrecte.getRemarque();

		if (remarque == null || "".equals(remarque)) {
			remarque = personneDoublon.getRemarque();
		}
		else {
			remarque = remarque + " " + personneDoublon.getRemarque();
		}

		personneCorrecte.setRemarque(personneDoublon.getRemarque());

		traiterRapport(personneDoublon, personneCorrecte);
		traiterFor(personneDoublon, personneCorrecte);
		traiterContribuables(personneDoublon, personneCorrecte);

		rapport.addResultat(personneDoublon.getNumero() + ";" + personneCorrecte.getNumero());


	}

	private void traiterRapport(PersonnePhysique personneDoublon, PersonnePhysique personneCorrecte) {
		List<RapportPrestationImposable> listRapport = getRapportPrestationImposable(personneDoublon);

		for (RapportPrestationImposable rapportPrestationImposable : listRapport) {
			DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) rapportPrestationImposable.getObjet();
			if (!isRapportExist(personneCorrecte, debiteur, rapportPrestationImposable.getDateDebut())) {
				final RegDate dateDebut = rapportPrestationImposable.getDateDebut();
				final RegDate dateFin = rapportPrestationImposable.getDateFin();
				final TypeActivite typeActivite = rapportPrestationImposable.getTypeActivite();
				final Integer tauxActivite = rapportPrestationImposable.getTauxActivite();
				tiersService.addRapportPrestationImposable(personneCorrecte, debiteur, dateDebut, dateFin, typeActivite, tauxActivite);


			}
			rapportPrestationImposable.setAnnule(true);


		}
	}

	private void traiterContribuables(PersonnePhysique personneDoublon, PersonnePhysique personneCorrecte) {

		EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(personneDoublon, null);
		if (ensemble != null) {
			PersonnePhysique principal = ensemble.getPrincipal();
			PersonnePhysique conjoint = ensemble.getConjoint();
			MenageCommun menage = ensemble.getMenage();
			if (principal != null) {
				tiersService.annuleTiers(principal);
			}
			if (conjoint != null && "[HostImpotSourceMigrator thread]".equals(conjoint.getLogCreationUser())) {

				tiersService.annuleTiers(conjoint);
				rapport.addConjoint((conjoint.getNumero().toString()));
			}

			if (menage != null) {
				tiersService.annuleTiers(menage);
			}


		}
		else {
			tiersService.annuleTiers(personneDoublon);
		}


	}

	private void traiterFor(PersonnePhysique personneDoublon, PersonnePhysique personneCorrecte) {
		Contribuable contribuableDoublon = getContribuable(personneDoublon);

		Contribuable contribuableCorrecte = getContribuable(personneCorrecte);

		List<ForFiscal> listeFor = contribuableDoublon.getForsFiscauxSorted();
		for (ForFiscal forFiscalCourant : listeFor) {
			ForFiscalPrincipal forFiscal = (ForFiscalPrincipal) forFiscalCourant;
			String message = null;
			final RegDate dateDebut = forFiscal.getDateDebut();
			final RegDate dateFin = forFiscal.getDateFin();
			final String modeImposition = forFiscal.getModeImposition().name();
			final Integer numeroOfsAutoriteFiscale = forFiscal.getNumeroOfsAutoriteFiscale();
			if (contribuableDoublon instanceof PersonnePhysique) {

				message = contribuableDoublon.getNumero() + ";" + "0" + ";" + dateDebut + ";" + dateFin + ";" + modeImposition + ";"
						+ numeroOfsAutoriteFiscale;
			}
			else if (contribuableDoublon instanceof MenageCommun) {

				message = contribuableDoublon.getNumero() + ";" + "1" + ";" + dateDebut + ";" + dateFin + ";" + modeImposition + ";"
						+ numeroOfsAutoriteFiscale;
			}

			rapport.addFor(message);


		}

	}

	private Contribuable getContribuable(PersonnePhysique personne) {
		Contribuable contribuable = null;
		if (tiersService.isInMenageCommun(personne, RegDate.get())) {

			final EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(personne, RegDate.get());
			contribuable = ensembleTiersCouple.getMenage();
		}
		else {
			contribuable = personne;
		}
		return contribuable;
	}

	private List<RapportPrestationImposable> getRapportPrestationImposable(PersonnePhysique personneDoublon) {
		String queryWhere = " and rapport.sujet.numero = " + personneDoublon.getNumero();
		String annulationDate = " and rapport.annulationDate is null";

		final String query = " select rapport from RapportPrestationImposable rapport where 1=1 " + queryWhere + annulationDate;
		return hibernateTemplate.find(query);

	}

	public List<Long> getDoublon() {
		final String query = // --------------------------------
				"SELECT DISTINCT                                                                         "
						+ "   personne.id                                                                "
						+ "FROM                                                                          "
						+ "    PersonnePhysique AS personne                                              "
						+ "INNER JOIN                                                                    "
						+ "    personne.rapportsSujet AS rapportPrestation                               "
						+ "WHERE                                                                         "
						+ "    rapportPrestation.class = RapportPrestationImposable                      "
						+ "    AND rapportPrestation.annulationDate IS null                              "
						+ "    AND rapportPrestation.dateFin IS null                                     "
						+ "    AND  personne.annulationDate IS null                                      "
						+ "    AND  personne.ancienNumeroSourcier IS NOT null                            "
						+ "    AND  personne.ancienNumeroSourcier IS NOT null                            "
						+ "    AND  personne.numeroIndividu in (                                         "
						+ "                SELECT                                                        "
						+ "                     pp.numeroIndividu                                        "
						+ "                FROM                                                          "
						+ "                    PersonnePhysique As pp                                    "
						+ "                WHERE                                                         "
						+ "                    pp.numeroIndividu is not null                             "
						+ "                    AND  pp.annulationDate IS null                            "
						+ "                    group by pp.numeroIndividu                                "
						+ "                    having count(pp.numeroIndividu) > 1                       "
						+ "              )                                                               "
						+ "ORDER BY personne.id ASC                                                      ";

		List<Long> resultat = hibernateTemplate.find(query);
		return resultat;
	}


	public List<Long> getMarieSeul() {
		final String query = // --------------------------------
				"SELECT  DISTINCT  personne.id                                                    "
						+ "FROM                                                                          "
						+ "    PersonnePhysique AS personne                                              "
						+ "INNER JOIN                                                                    "
						+ "    personne.rapportsSujet AS rapportMenage                                   "
						+ "WHERE                                                                         "
						+ "    rapportMenage.class = AppartenanceMenage                                  "
						+ "    AND  rapportMenage.annulationDate IS null                                 "
						+ "    AND  rapportMenage.dateFin IS null                                        "
						+ "    AND  rapportMenage.logCreationDate >=  ?                                  "
						+ "    AND  personne.annulationDate IS null                                      "
						+ "    AND  NOT EXISTS (                                                         "
						+ "                SELECT                                                        "
						+ "                    rapportMenageSupp.id                                      "
						+ "                FROM                                                          "
						+ "                    AppartenanceMenage As rapportMenageSupp                   "
						+ "                WHERE                                                         "
						+ "                     rapportMenageSupp.objet.id = rapportMenage.objet.id      "
						+ "                AND  rapportMenageSupp.sujet.id != personne.id                "
						+ "                AND  rapportMenageSupp.annulationDate IS null                 "
						+ "                AND  rapportMenageSupp.dateFin IS null                        "
						+ "              )                                                               ";

		List<Long> resultat = hibernateTemplate.find(query, RegDate.get(2010, 3, 15).asJavaDate());
		return resultat;
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


	private PersonnePhysique getPersonneCorrecte(PersonnePhysique personneDoublon) {
		Object[] criteria = {
				personneDoublon.getNumeroIndividu(), personneDoublon.getNumero()
		};
		String query = "from PersonnePhysique habitant where habitant.numeroIndividu = ? and habitant.numero <> ? and habitant.annulationDate IS null";

		final List<?> list = hibernateTemplate.find(query, criteria);
		if (list.size() > 0) {
			return (PersonnePhysique) list.get(0);
		}
		else {
			return null;
		}
	}

	private boolean isRapportExist(PersonnePhysique sourcier, DebiteurPrestationImposable dpi, RegDate dateDebut) {

		Set<RapportEntreTiers> listeRapport = sourcier.getRapportsSujet();
		if (listeRapport != null) {
			for (Iterator iterator = listeRapport.iterator(); iterator.hasNext();) {
				RapportEntreTiers rapportEntreTiers = (RapportEntreTiers) iterator.next();
				if (rapportEntreTiers instanceof RapportPrestationImposable && rapportEntreTiers.getObjet() == dpi
						&& rapportEntreTiers.getDateDebut() == dateDebut) {
					return true;

				}
			}
		}
		return false;

	}

	public ServiceCivilService getServiceCivilService() {
		return serviceCivilService;
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}
}
