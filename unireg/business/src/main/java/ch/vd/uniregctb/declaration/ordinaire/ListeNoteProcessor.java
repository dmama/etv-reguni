package ch.vd.uniregctb.declaration.ordinaire;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.ListeNoteResults;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParTypeAt;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;

public class ListeNoteProcessor {

	private static final int BATCH_SIZE = 100;

	final Logger LOGGER = Logger.getLogger(ListeNoteProcessor.class);

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final AdresseService adresseService;
	private final ServiceInfrastructureService infraService;
	private final int batchSize = BATCH_SIZE;
	private final ThreadLocal<ListeNoteResults> rapport = new ThreadLocal<ListeNoteResults>();
	private Map<Long, List<ForFiscalSecondaire>> mapInfo;

	public ListeNoteProcessor(HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager, TiersService tiersService, AdresseService adresseService,
	                          ServiceInfrastructureService infraService) {

		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
		this.infraService = infraService;
	}


	public ListeNoteResults run(final RegDate dateTraitement, final int annee, int nbThreads, final StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final ListeNoteResults rapportFinal = new ListeNoteResults(dateTraitement, annee);
		status.setMessage("Récupération des contribuables ...");
		mapInfo = recupererContribuables(annee);
		List<Long> ids = new ArrayList(mapInfo.keySet());

		// Reussi les messages par lots
		final ParallelBatchTransactionTemplate<Long, ListeNoteResults>
				template = new ParallelBatchTransactionTemplate<Long, ListeNoteResults>(ids, batchSize, nbThreads, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE,
				transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, ListeNoteResults>() {

			@Override
			public ListeNoteResults createSubRapport() {
				return new ListeNoteResults(dateTraitement);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, ListeNoteResults r) throws Exception {

				rapport.set(r);
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);

				traiterBatch(batch, annee);
				return true;
			}
		});

		final int count = rapportFinal.listeContribuableAvecNote.size();

		if (status.interrupted()) {
			status.setMessage("La production de la liste de contribuable avec note a été interrompue."
					+ " Nombre de fors secondaires déclenchant une note au moment de l'interruption = " + count);
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("La production de la liste de contribuable avec note est terminée."
					+ "Nombre de contribuable parcouru = " + rapportFinal.nbContribuable + ". Nombre de fors secondaires déclenchant une note = " + count + ". Nombre d'erreurs = " + rapportFinal.erreurs.size());
		}

		rapportFinal.end();

		return rapportFinal;

	}


	private void traiterBatch(final List<Long> batch, final int annee) throws Exception {

		// On charge tous les contribuables en vrac
		final List<Contribuable> list = hibernateTemplate.executeWithNativeSession(new HibernateCallback<List<Contribuable>>() {
			@Override
			public List<Contribuable> doInHibernate(Session session) throws HibernateException {
				final Criteria crit = session.createCriteria(Contribuable.class);
				crit.add(Restrictions.in("id", batch));
				crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
				//noinspection unchecked
				return crit.list();
			}
		});

		for (Contribuable contribuable : list) {
			rapport.get().nbContribuable++;
			List<ForFiscalSecondaire> forsSecondaires = mapInfo.get(contribuable.getNumero());
			Collections.sort(forsSecondaires, new DateRangeComparator<ForFiscalSecondaire>());
			for (ForFiscalSecondaire forsSecondaire : forsSecondaires) {
				if (!isForSecondaireRecouvert(contribuable, forsSecondaire)) {
					RegDate dateFinSecondaire = forsSecondaire.getDateFin();
					if (tiersService.isHorsCanton(contribuable, dateFinSecondaire)) {
						rapport.get().addContribuableAvecNote(new ListeNoteResults.InfoContribuableAvecNote(
								contribuable, dateFinSecondaire, adresseService, tiersService, infraService));
					}

				}
			}


		}


	}


	/**
	 * Permet de vérifier qu 'il n'existe pas de for secondaire #MotifFor.ACHAT_IMMOBILIER ou #MotifFor.DEBUT_EXPLOITATION qui recouvre le for analysé
	 *
	 * @param contribuable         à analyser
	 * @param forFiscaleSecondaire le for à analyser
	 * @return true si le for n'est pas recouvert par un autre for secondaire #MotifFor.ACHAT_IMMOBILIER ou #MotifFor.DEBUT_EXPLOITATION false sinon
	 */
	protected Boolean isForSecondaireRecouvert(Contribuable contribuable, ForFiscalSecondaire forFiscaleSecondaire) {

		final RegDate dateFin = forFiscaleSecondaire.getDateFin();
		RegDate oneDayAfter = dateFin.getOneDayAfter();
		//Recherche des fors valide un jour après la date de fin du for étudié
		ForsParTypeAt forParTypeAt = contribuable.getForsParTypeAt(oneDayAfter, true);

		List<ForFiscalSecondaire> forsSecondaires = forParTypeAt.secondaires;
		if (forsSecondaires != null && !forsSecondaires.isEmpty()) {
			for (ForFiscalSecondaire forSecondaire : forsSecondaires) {
				//Si on trouve un for immeuble ou début d'activité, on regarde si ce for recouvre
				// le for secondaire étudié, c'est à dire qu'il est valide à la date de fin du for étudié 
				if (forSecondaire.getMotifOuverture() == MotifFor.ACHAT_IMMOBILIER ||
						forSecondaire.getMotifOuverture() == MotifFor.DEBUT_EXPLOITATION) {
					if (forSecondaire.isValidAt(dateFin)) {
						return true;

					}

				}

			}
		}


		return false;
	}


	private Map<Long, List<ForFiscalSecondaire>> recupererContribuables(final int annee) {
		final RegDate debutAnnee = RegDate.get(annee, 1, 1);

		//Identifier les contribuables ayant vendu un immeuble ou cessé une activité indépendante dans une PF donnée
		final String queryIdsCtbForsSecondaire = // --------------------------------
				"SELECT DISTINCT                                                                         "
						+ "    cont.id, fors                                                             "
						+ "FROM                                                                          "
						+ "    Contribuable AS cont                                                      "
						+ "INNER JOIN                                                                    "
						+ "    cont.forsFiscaux AS fors                                                  "
						+ "WHERE                                                                         "
						+ "        cont.annulationDate IS null                                           "
						+ "    AND fors.annulationDate IS null                                           "
						+ "    AND fors.class = ForFiscalSecondaire                                      "
						+ "    AND fors.motifFermeture IN ('VENTE_IMMOBILIER', 'FIN_EXPLOITATION')       "
						+ "    AND fors.dateFin IS NOT null                                              "
						+ "    AND fors.dateFin BETWEEN :debutAnnee AND :finAnnee                        "
						+ "ORDER BY cont.id ASC                                                          ";


		final RegDate finAnnee = RegDate.get(annee, 12, 31);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final Map<Long, List<ForFiscalSecondaire>> mapInfo = template.execute(new TransactionCallback<Map<Long, List<ForFiscalSecondaire>>>() {
			@Override
			public Map<Long, List<ForFiscalSecondaire>> doInTransaction(TransactionStatus status) {

				final List<Object[]> listeFors = hibernateTemplate.executeWithNewSession(new HibernateCallback<List<Object[]>>() {
					@Override
					public List<Object[]> doInHibernate(Session session) throws HibernateException {
						Query queryObject = session.createQuery(queryIdsCtbForsSecondaire);
						queryObject.setParameter("debutAnnee", debutAnnee.index());
						queryObject.setParameter("finAnnee", finAnnee.index());
						//noinspection unchecked
						return queryObject.list();
					}
				});


				//Construction de la map <numeroctb;Liste des fors secondaires fermés>
				final Map<Long, List<ForFiscalSecondaire>> mapInfo = new HashMap<Long, List<ForFiscalSecondaire>>();
				for (Object[] listeId : listeFors) {
					final long numeroCtb = ((Number) listeId[0]).longValue();
					final ForFiscalSecondaire forSecondaire = (ForFiscalSecondaire) listeId[1];
					List<ForFiscalSecondaire> listeForSecondaire = mapInfo.get(numeroCtb);
					if (listeForSecondaire == null) {
						listeForSecondaire = new ArrayList<ForFiscalSecondaire>();

					}
					listeForSecondaire.add(forSecondaire);
					mapInfo.put(numeroCtb, listeForSecondaire);

				}

				return mapInfo;
			}


		});


		return mapInfo;
	}

	private List<Long> getIdCtb(List<InfoForFerme> listInfos) {
		List<Long> result = new ArrayList<Long>();
		for (InfoForFerme listInfo : listInfos) {
			result.add(listInfo.numeroCtb);
		}
		return result;

	}

	private static class InfoForFerme implements DateRange {
		public final long numeroCtb;
		public final long idFor;
		public RegDate dateDebut;
		public final RegDate dateFin;

		private InfoForFerme(long numeroCtb, long idFor, RegDate dateFin) {
			this.numeroCtb = numeroCtb;
			this.idFor = idFor;
			this.dateFin = dateFin;
		}


		@Override
		public boolean isValidAt(RegDate regDate) {
			return false;
		}

		@Override
		public RegDate getDateDebut() {
			return null;
		}

		@Override
		public RegDate getDateFin() {
			return null;
		}
	}
}
