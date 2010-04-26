package ch.vd.uniregctb.rattrapage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.evenement.common.EnsembleTiersCouple;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.modeimposition.MariageModeImpositionResolver;
import ch.vd.uniregctb.metier.modeimposition.ModeImpositionResolver;
import ch.vd.uniregctb.metier.modeimposition.ModeImpositionResolverException;
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
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeActivite;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
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
	private static final Logger RAPPORTFORCREES = Logger.getLogger(ContribuableManager.class.getName() + ".ForCrees");


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
		//listATraiter.add(10733709L);
		//listATraiter.add(10601726L);
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
				LOGGER.info("Traitement du batch " + Arrays.toString(batch.toArray()) + " ...");

				if (batch.size() == 1) {
					idCtb = batch.get(0);
				}
				traiterBatchMarieSeul(batch);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				String message = "===> Rollback du batch [" + Arrays.toString(batchEnCours.toArray()) + " ..."
						+ "] willRetry=" + willRetry;
				LOGGER.error(message);

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
		LOGGER.info("Nombre de fors fermés :" + rapportFinal.nbCtbForsSupprimes);
		LOGGER.info("Nombre de fors créés :" + rapportFinal.nbCtbForsCrees);
		LOGGER.info("Nombre d'erreurs:" + rapportFinal.nbErrors);
		LOGGER.info("Nombre de couples reformés:" + rapportFinal.nbConjoint);

		RAPPORT.info("numéro du marie seul; numéro du ménage;numéro du conjoint;Numéro OFS autorité fiscal(null si pas de for)");
		RAPPORTFORSUPPRIME.info("Numéro ctb;Date Debut;Date Fin;modeImposition;Numero OFS Autorite fiscale;Type");

		ERROR.error("numéro du marié seul; message erreur");


		List<String> listeMessage = rapportFinal.listeResultats;
		for (String message : listeMessage) {
			RAPPORT.info(message);
		}
		listeMessage = rapportFinal.listeForSupprimes;
		for (String message : listeMessage) {
			RAPPORTFORSUPPRIME.info(message);
		}

		listeMessage = rapportFinal.listeForCrees;
		for (String message : listeMessage) {
			RAPPORTFORCREES.info(message);
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
						final EnsembleTiersCouple ensembleConjoint = tiersService.getEnsembleTiersCouple(conjoint, RegDate.get());
						if (ensembleConjoint == null || isDateDebutRapportDifferente(conjoint, rapport)) {
							//fermeture des fors fiscaux
							final RegDate dateMariage = rapport.getDateDebut();
							RegDate dateFermeture = dateMariage.getOneDayBefore();
							if (iscoherentAvecDateMariage(conjoint, dateMariage)) {
								LOGGER.debug("la personne " + conjoint.getNumero() + " va être ajoutée dans le ménage " + menage.getNumero());

								tiersService.addTiersToCouple(menage, conjoint, dateMariage, null);

								LOGGER.debug("la personne " + conjoint.getNumero() + " ajoutée dans le ménage " + menage.getNumero());

								doMariageReconciliation(menage, dateMariage, null, EtatCivil.MARIE, 0L, false);

								Integer numeroAutorite = null;
								ModeImposition mode = null;
								if (menage.getDernierForFiscalPrincipal() != null) {
									numeroAutorite = menage.getDernierForFiscalPrincipal().getNumeroOfsAutoriteFiscale();
									mode = menage.getDernierForFiscalPrincipal().getModeImposition();
								}
								rapportMarieCourant.addResultat(personneMarieSeul.getNumero() + ";" + menage.getNumero() + ";" + conjoint.getNumero() + ";" + numeroAutorite + ";" + mode);
								rapportMarieCourant.addConjoint(conjoint.getNumero() + ";" + numeroAutorite);
							}
							else {
								String message = personneMarieSeul.getNumero() + ";" + "le conjoint potentiel " +
										conjoint.getNumero() + " a un ou plusieur fors ouverts après la date du mariage";

								rapportMarieCourant.addError(message);
							}


						}
						else {
							String message = personneMarieSeul.getNumero() + ";" + "le conjoint potentiel " +
									conjoint.getNumero() + " a son propre ménage: " + ensembleConjoint.getMenage().getNumero();

							rapportMarieCourant.addError(message);
						}

					}
				}

			}


		}

	}

	private boolean iscoherentAvecDateMariage(PersonnePhysique conjoint, RegDate dateMariage) {
		final ForFiscalPrincipal forFiscal = conjoint.getDernierForFiscalPrincipal();
		if (forFiscal == null) {
			return true;
		}
		return dateMariage.isAfter(forFiscal.getDateDebut());
	}

	private boolean isDateDebutRapportDifferente(PersonnePhysique conjoint, RapportEntreTiers rapport) {
		final RapportEntreTiers rapportConjoint = conjoint.getDernierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
		if(!rapportConjoint.getDateDebut().equals(rapport.getDateDebut())){
			LOGGER.info("conjoint avec Menage =>"+conjoint.getNumero());
		}


		return !rapportConjoint.getDateDebut().equals(rapport.getDateDebut());
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

		forFiscalPrincipal.setDateFin(dateFermeture);
		forFiscalPrincipal.setMotifFermeture(motifFermeture);
		rapportMarieCourant.addForSupprimes(conjoint.getNumero() + ";"
				+ forFiscalPrincipal.getDateDebut() + ";"
				+ forFiscalPrincipal.getDateFin() + ";"
				+ forFiscalPrincipal.getModeImposition() + ";"
				+ forFiscalPrincipal.getNumeroOfsAutoriteFiscale() + ";"
				+ forFiscalPrincipal.getTypeAutoriteFiscale());
	}

	private void closeForFiscalSecondaire(ForFiscalSecondaire forFiscalSecondaire, RegDate dateFermeture, MotifFor motifFermeture, PersonnePhysique conjoint) {
		if (forFiscalSecondaire != null) {
			if (forFiscalSecondaire.getDateDebut().isAfter(dateFermeture)) {
				throw new ValidationException(forFiscalSecondaire, "La date de fermeture ("
						+ RegDateHelper.dateToDisplayString(dateFermeture) + ") est avant la date de début ("
						+ RegDateHelper.dateToDisplayString(forFiscalSecondaire.getDateDebut()) + ") du for fiscal actif");
			}
			forFiscalSecondaire.setDateFin(dateFermeture);
			forFiscalSecondaire.setMotifFermeture(motifFermeture);
			rapportMarieCourant.addForSupprimes(conjoint.getNumero() + ";"
					+ forFiscalSecondaire.getDateDebut() + ";"
					+ forFiscalSecondaire.getDateFin() + ";"
					+ "Secondaire;"
					+ forFiscalSecondaire.getNumeroOfsAutoriteFiscale() + ";"
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
		forFiscalAutreImpot.setDateFin(dateFermeture);
		rapportMarieCourant.addForSupprimes(conjoint.getNumero() + ";"
				+ forFiscalAutreImpot.getDateDebut() + ";"
				+ forFiscalAutreImpot.getDateFin() + ";"
				+ "autrImpot;"
				+ forFiscalAutreImpot.getNumeroOfsAutoriteFiscale() + ";"
				+ forFiscalAutreImpot.getTypeAutoriteFiscale());
	}

	private void closeForFiscalAutreElementImposable(ForFiscalAutreElementImposable forFiscalAutreElementImposable, RegDate dateFermeture, MotifFor motifFermeture, PersonnePhysique conjoint) {
		if (forFiscalAutreElementImposable != null) {
			if (forFiscalAutreElementImposable.getDateDebut().isAfter(dateFermeture)) {
				throw new ValidationException(forFiscalAutreElementImposable, "La date de fermeture ("
						+ RegDateHelper.dateToDisplayString(dateFermeture) + ") est avant la date de début ("
						+ RegDateHelper.dateToDisplayString(forFiscalAutreElementImposable.getDateDebut()) + ") du for fiscal actif");
			}
			forFiscalAutreElementImposable.setDateFin(dateFermeture);
			forFiscalAutreElementImposable.setMotifFermeture(motifFermeture);
			rapportMarieCourant.addForSupprimes(conjoint.getNumero() + ";"
					+ forFiscalAutreElementImposable.getDateDebut() + ";"
					+ forFiscalAutreElementImposable.getDateFin() + ";"
					+ "autrElementImposable;"
					+ forFiscalAutreElementImposable.getNumeroOfsAutoriteFiscale() + ";"
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

	private MenageCommun doMariageReconciliation(MenageCommun menageCommun, RegDate date, String remarque, ch.vd.uniregctb.type.EtatCivil etatCivilFamille, Long numeroEvenement,
	                                             boolean changeHabitantFlag) {

		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menageCommun, date);

		final PersonnePhysique principal = ensemble.getPrincipal();
		final PersonnePhysique conjoint = ensemble.getConjoint();

		RegDate dateEffective = date;

		final ModeImpositionResolver mariageResolver = new MariageModeImpositionResolver(tiersService, numeroEvenement);
		final ModeImpositionResolver.Imposition imposition;
		try {
			imposition = mariageResolver.resolve(menageCommun, dateEffective, null);
		}
		catch (ModeImpositionResolverException ex) {
			throw new EvenementCivilHandlerException(ex.getMessage(), ex);
		}

		// si le mode d'imposition a pu être déterminé
		if (imposition != null) {

			dateEffective = imposition.getDateDebut();
			ModeImposition modeImposition = imposition.getModeImposition();

			/*
			 * sauvegarde des fors secondaires et autre élément imposable de chaque tiers
			 */
			final List<ForFiscalSecondaire> ffsPrincipal;
			final List<ForFiscalAutreElementImposable> ffaeiPrincipal;
			{
				final Tiers.ForsParType fors = principal.getForsParType(false);
				ffsPrincipal = fors.secondaires;
				ffaeiPrincipal = fors.autreElementImpot;
			}

			final List<ForFiscalSecondaire> ffsConjoint;
			final List<ForFiscalAutreElementImposable> ffaeiConjoint;
			if (conjoint != null) {
				final Tiers.ForsParType fors = conjoint.getForsParType(false);
				ffsConjoint = fors.secondaires;
				ffaeiConjoint = fors.autreElementImpot;
			}
			else {
				ffsConjoint = Collections.emptyList();
				ffaeiConjoint = Collections.emptyList();
			}

			/*
			 * le traitement met fin aux fors de chacun des tiers
			 */
			final RegDate veilleMariage = dateEffective.getOneDayBefore();
			final ForFiscalPrincipal forPrincipal = principal.getForFiscalPrincipalAt(veilleMariage);
			ForFiscalPrincipal forConjoint = null;
			if (conjoint != null) {
				forConjoint = conjoint.getForFiscalPrincipalAt(veilleMariage);
			}

			/*
			 * fermeture des fors des tiers
			 */

			fermerForFiscaux(principal, veilleMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			fermerForFiscaux(conjoint, veilleMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);


			Audit.info(numeroEvenement, "Fermeture des fors fiscaux des membres du ménage");

			/* On récupère la commune de résidence du contribuable principal, ou à défaut celle de son conjoint. */
			Integer noOfsCommune = null;
			TypeAutoriteFiscale typeAutoriteCommune = null;
			MotifFor motifOuverture = MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION;

			// Plusieurs cas se présentent, dans le cas ou l'événement de mariage nous parvient après les événements d'arrivée,
			// alors que le mariage a en fait eu lieu avant l'arrivée d'au moins l'un des deux)
			// 1. le mariage a eu lieu avant l'arrivée des deux protagonistes : il faut alors annuler les fors des deux
			//		membres du couple, créer un nouveau contribuable ménage et lui ouvrir un for à la date d'arrivée
			//		du premier arrivé des deux membres du couple
			// 2. le mariage a eu lieu entre l'arrivée de l'un et l'arrivée de l'autre : il faut alors fermer le for de la personne
			//		déjà là à la veille du mariage (déjà fait plus haut - closeAllForsFiscaux), annuler le for de la personne
			//		effectivement arrivée après le mariage, créer un contribuable couple et lui ouvrir un for à la date du mariage
			if (forPrincipal == null || (conjoint != null && forConjoint == null)) {

				final DonneesOuvertureFor donneesPrincipal = traiterCasEvenementMariageRecuApresEvenementArriveeMaisMariageAnterieur(principal, dateEffective);
				final DonneesOuvertureFor donneesConjoint = conjoint != null ? traiterCasEvenementMariageRecuApresEvenementArriveeMaisMariageAnterieur(conjoint, dateEffective) : null;

				// le mariage a été prononcé avant l'arrivée d'au moins un des conjoints dans le canton,
				// mais on ne l'a su qu'après
				if (donneesPrincipal != null || donneesConjoint != null) {

					// on prend la date d'arrivée du premier (après mariage) des deux, et les données qui vont avec
					final DonneesOuvertureFor source;
					if (donneesPrincipal != null && (donneesConjoint == null || donneesPrincipal.getDateDebut().isBeforeOrEqual(donneesConjoint.getDateDebut()))) {
						source = donneesPrincipal;
					}
					else {
						source = donneesConjoint;
					}

					final boolean conjointInconnuAuFiscal = conjoint == null || conjoint.getForsFiscauxNonAnnules(false).size() == 0;
					final boolean tousLesConjointsConnusSontEnFaitArrivesMaries = donneesPrincipal != null && (conjointInconnuAuFiscal || donneesConjoint != null);

					final boolean unDesConjointEstPartiAvantMariage = (getDernierForFermePourDepart(principal) != null || (conjoint != null && getDernierForFermePourDepart(conjoint) != null));

					if (tousLesConjointsConnusSontEnFaitArrivesMaries || unDesConjointEstPartiAvantMariage) {

						// le motif d'ouverture doit être le mariage dès qu'au moins un des futurs
						// conjoints était déjà présent dans le canton à la date du mariage ;
						// dans le cas où ils étaient tous deux HC/HS à la date du mariage, le
						// motif d'ouverture du for du ménage commun doit être l'arrivée
						motifOuverture = source.getMotifOuverture();

						// la date effective d'ouverture du for du couple doit être la date du mariage
						// dès qu'un au moins des futurs conjoints était déjà présent dans le canton
						// à la date du mariage, et ne prend la valeur de la date d'arrivée du premier
						// des deux conjoints que s'ils étaient déjà mariés en arrivant (mais on ne le
						// savait pas)
						dateEffective = source.getDateDebut();

						// la commune du for du ménage commun, maintenant...
						// si les deux conjoints sont arrivés en fait mariés (mais on ne le savait pas), c'est
						// l'arrivée du premier qui détermine le for du couple ; mais si l'un était déjà là
						// avant le mariage, la commune du for sera la sienne (donc on laisse la variable
						// à null ici, elle sera renseignée plus bas)
						noOfsCommune = source.getNumeroOfsAutoriteFiscale();
						typeAutoriteCommune = source.getTypeAutoriteFiscale();
					}
				}
				else {

					// pas de mariage prononcé-avant-mais-reçu-après une arrivée, et aucun for ouvert à la veille du mariage
					// mais si on a pu trouver un mode d'imposition, c'est qu'il y a un for actif maintenant (on suppose ici
					// que la date du mariage est dans le passé), donc celui-ci a été ouvert après la date du mariage
					if (forPrincipal == null && (conjoint != null && forConjoint == null)) {

						if (forPrincipal == null) {
							throw new EvenementCivilHandlerException(
									"Le contribuable n° " + FormatNumeroHelper.numeroCTBToDisplay(principal.getNumero()) + " possède déjà un for ouvert après la date de mariage");
						}
						else {
							throw new EvenementCivilHandlerException(
									"Le contribuable n° " + FormatNumeroHelper.numeroCTBToDisplay(conjoint.getNumero()) + " possède déjà un for ouvert après la date de mariage");
						}
					}
					else {
						final ForFiscalPrincipal ffpPrincipal = principal.getForFiscalPrincipalAt(dateEffective);
						if (ffpPrincipal != null && dateEffective.equals(ffpPrincipal.getDateDebut()) && MotifFor.MAJORITE.equals(ffpPrincipal.getMotifOuverture())) {
							// mariage d'une personne physique avec un for le même jour du mariage (motif majorité)

							// annulation du for puisqu'il va être "remplacé" par celui du couple
							ffpPrincipal.setAnnule(true);
							noOfsCommune = ffpPrincipal.getNumeroOfsAutoriteFiscale();
							typeAutoriteCommune = ffpPrincipal.getTypeAutoriteFiscale();
						}

						if (conjoint != null) {
							ForFiscalPrincipal ffpConjoint = conjoint.getForFiscalPrincipalAt(dateEffective);
							if (ffpConjoint != null && dateEffective.equals(ffpConjoint.getDateDebut()) && MotifFor.MAJORITE.equals(ffpConjoint.getMotifOuverture())) {
								// mariage d'une personne physique avec un for le même jour du mariage (motif majorité)

								// annulation du for puisqu'il va être "remplacé" par celui du couple
								ffpConjoint.setAnnule(true);
								if (noOfsCommune == null) {
									noOfsCommune = ffpConjoint.getNumeroOfsAutoriteFiscale();
									typeAutoriteCommune = ffpConjoint.getTypeAutoriteFiscale();
								}
							}
						}
					}
				}
			}

			if (noOfsCommune == null) {
				// il n'est apparemment pas possible que les deux fors soient nuls, sinon on n'aurait pas pu trouver de
				// mode d'imposition
				ForFiscalPrincipal forPourMenage = null;

				if ((forPrincipal != null && forConjoint == null) || (forPrincipal == null && forConjoint != null)) {
					// si un seul for existe, on utilise ses données
					if (forPrincipal == null) {
						forPourMenage = forConjoint;
					}
					else {
						forPourMenage = forPrincipal;
					}
				}
				else {
					// sinon il faut determiner selon le principe dans [UNIREG-1462]
					final boolean principalDansCanton = (forPrincipal == null ? false : TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.equals(forPrincipal.getTypeAutoriteFiscale()));
					final boolean conjointDansCanton = (forConjoint == null ? false : TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.equals(forConjoint.getTypeAutoriteFiscale()));
					if (principalDansCanton || !conjointDansCanton) {
						// si le for du principal est dans le canton ou celui de son conjoint ne l'est pas, on utilise le for du principal
						forPourMenage = forPrincipal;
					}
					else if (conjointDansCanton) {
						// si le conjoint est habitant
						forPourMenage = forConjoint;
					}
				}

				noOfsCommune = forPourMenage.getNumeroOfsAutoriteFiscale();
				typeAutoriteCommune = forPourMenage.getTypeAutoriteFiscale();
			}

			final ForFiscalPrincipal forFiscalPrincipalAt = menageCommun.getForFiscalPrincipalAt(dateEffective);
			if (isForFiscalAbsentOuIncoherent(forFiscalPrincipalAt,
					dateEffective, noOfsCommune, typeAutoriteCommune, modeImposition, motifOuverture)) {

				// Si un for existe, à ce stade, c'est qu'il est incoherent, on l'annule
				if (forFiscalPrincipalAt != null) {
					forFiscalPrincipalAt.setAnnule(true);
				}

				openForFiscalPrincipal(menageCommun, dateEffective, MotifRattachement.DOMICILE, noOfsCommune,
						typeAutoriteCommune, modeImposition, motifOuverture, changeHabitantFlag);
			}

			/*
			 * réouverture des autres fors
			 */

			createForsAutreElementImpossable(dateEffective, menageCommun, ffaeiPrincipal, motifOuverture);
			createForsAutreElementImpossable(dateEffective, menageCommun, ffaeiConjoint, motifOuverture);
		}

		/*	if (remarque != null && !"".equals(remarque.trim())) {
			principal.setRemarque((principal.getRemarque() != null ? principal.getRemarque() : "") + remarque);
			if (conjoint != null) {
				conjoint.setRemarque((conjoint.getRemarque() != null ? conjoint.getRemarque() : "") + remarque);
			}
			menageCommun.setRemarque((menageCommun.getRemarque() != null ? menageCommun.getRemarque() : "") + remarque);
		}*/

		//	updateSituationFamilleMariage(menageCommun, dateEffective, etatCivilFamille);

		return menageCommun;
	}

	private boolean isForFiscalAbsentOuIncoherent(ForFiscalPrincipal forFiscal, RegDate dateDebut,
	                                              Integer noOfsCommune, TypeAutoriteFiscale typeAutorite,
	                                              ModeImposition mode, MotifFor motifOuverture) {

		if (forFiscal == null) {
			return true;
		}

		boolean incoherence = !dateDebut.equals(forFiscal.getDateDebut()) ||
				!noOfsCommune.equals(forFiscal.getNumeroOfsAutoriteFiscale()) ||
				!typeAutorite.equals(forFiscal.getTypeAutoriteFiscale()) ||
				!mode.equals(forFiscal.getModeImposition()) ||
				!motifOuverture.equals(forFiscal.getMotifOuverture()) ||
				!MotifRattachement.DOMICILE.equals(forFiscal.getMotifRattachement());
		return incoherence;
	}

	private DonneesOuvertureFor traiterCasEvenementMariageRecuApresEvenementArriveeMaisMariageAnterieur(
			PersonnePhysique membreCouple, RegDate dateMariage) {

		final ForFiscalPrincipal ffp = membreCouple.getDernierForFiscalPrincipal();

		// arrivé en fait après le mariage mais déjà connu avant?
		DonneesOuvertureFor retour = null;
		if (ffp != null && ffp.getDateDebut().isAfter(dateMariage) && isArrivee(ffp.getMotifOuverture())) {

			// il faut annuler les fors individuels créés par l'arrivée
			ffp.setAnnule(true);

			// on garde le motif d'ouverture, la date d'ouverture, ainsi que la commune d'ouverture pour le nouveau for à créer
			retour = new DonneesOuvertureFor(ffp.getMotifOuverture(), ffp.getDateDebut(), ffp.getNumeroOfsAutoriteFiscale(), ffp.getTypeAutoriteFiscale());
		}

		return retour;
	}

	private ForFiscalPrincipal getDernierForFermePourDepart(final PersonnePhysique pp) {
		ForFiscalPrincipal dernierFor = null;
		for (ForFiscal ff : pp.getForsFiscaux()) {
			if (!ff.isAnnule() && ff.isPrincipal()) {
				ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff;
				if (isDepart(ffp.getMotifFermeture()) && (dernierFor == null || RegDateHelper.isAfterOrEqual(dernierFor.getDateFin(), ffp.getDateFin(), NullDateBehavior.EARLIEST))) {
					dernierFor = ffp;
				}
			}
		}
		return dernierFor;
	}

	private boolean isDepart(MotifFor motif) {
		return (MotifFor.DEPART_HC == motif || MotifFor.DEPART_HS == motif);
	}

	private boolean isArrivee(MotifFor motif) {
		return (MotifFor.ARRIVEE_HC == motif || MotifFor.ARRIVEE_HS == motif);
	}

	/**
	 * Ouvre les fors de type autre élément imposable sur le contribuable.
	 *
	 * @param date           la date des nouveaux fors secondaires.
	 * @param contribuable   le contribuable.
	 * @param fors           liste de fors autre élément imposable à créer.
	 * @param motifOuverture motif d'ouverture assigner aux nouveaux fors.
	 */
	private void createForsAutreElementImpossable(RegDate date, Contribuable contribuable, List<ForFiscalAutreElementImposable> fors, MotifFor motifOuverture) {
		for (ForFiscalAutreElementImposable forFiscalAutreElementImposable : fors) {
			if (forFiscalAutreElementImposable.isValidAt(date.getOneDayBefore())) {
				tiersService.openForFiscalAutreElementImposable(contribuable, forFiscalAutreElementImposable.getGenreImpot(),
						date, forFiscalAutreElementImposable.getMotifRattachement(),
						forFiscalAutreElementImposable.getNumeroOfsAutoriteFiscale(),
						forFiscalAutreElementImposable.getTypeAutoriteFiscale(), motifOuverture);
			}
		}
	}


	public ForFiscalPrincipal openForFiscalPrincipal(Contribuable contribuable, final RegDate dateOuverture,
	                                                 MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale,
	                                                 ModeImposition modeImposition, MotifFor motifOuverture, boolean changeHabitantFlag) {

		Assert.isNull(contribuable.getForFiscalPrincipalAt(null), "Le contribuable possède déjà un for principal ouvert");

		// Ouvre un nouveau for à la date d'événement

		ForFiscalPrincipal nouveauForFiscal = new ForFiscalPrincipal();
		nouveauForFiscal.setDateDebut(dateOuverture);
		nouveauForFiscal.setMotifRattachement(motifRattachement);
		nouveauForFiscal.setNumeroOfsAutoriteFiscale(numeroOfsAutoriteFiscale);
		nouveauForFiscal.setTypeAutoriteFiscale(typeAutoriteFiscale);
		nouveauForFiscal.setModeImposition(modeImposition);
		nouveauForFiscal.setMotifOuverture(motifOuverture);
		nouveauForFiscal = (ForFiscalPrincipal) tiersService.addAndSave(contribuable, nouveauForFiscal);


		Assert.notNull(nouveauForFiscal);
		String message = contribuable.getNumero() + ";" + nouveauForFiscal.getNumeroOfsAutoriteFiscale() + ";" + nouveauForFiscal.getDateDebut() + ";" +
				nouveauForFiscal.getDateFin() + ";" + nouveauForFiscal.getModeImposition() + ";" + nouveauForFiscal.getTypeAutoriteFiscale() + ";" +
				nouveauForFiscal.getNumeroOfsAutoriteFiscale();

		rapportMarieCourant.addForCrees(message);
		return nouveauForFiscal;

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

	/**
	 * Classe interne qui permet de conserver les données qui peuvent passer d'un for à l'autre suite à l'annulation du premier en vu de création du deuxième
	 */
	private static final class DonneesOuvertureFor {

		private final MotifFor motifOuverture;
		private final RegDate dateDebut;
		private final Integer numeroOfsAutoriteFiscale;
		private final TypeAutoriteFiscale typeAutoriteFiscale;

		public DonneesOuvertureFor(MotifFor motifOuverture, RegDate dateDebut, Integer numeroOfsAutoriteFiscale,
		                           TypeAutoriteFiscale typeAutoriteFiscale) {
			this.motifOuverture = motifOuverture;
			this.dateDebut = dateDebut;
			this.numeroOfsAutoriteFiscale = numeroOfsAutoriteFiscale;
			this.typeAutoriteFiscale = typeAutoriteFiscale;
		}

		public MotifFor getMotifOuverture() {
			return motifOuverture;
		}

		public RegDate getDateDebut() {
			return dateDebut;
		}

		public Integer getNumeroOfsAutoriteFiscale() {
			return numeroOfsAutoriteFiscale;
		}

		public TypeAutoriteFiscale getTypeAutoriteFiscale() {
			return typeAutoriteFiscale;
		}
	}
}
