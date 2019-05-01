package ch.vd.unireg.ehcache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.utils.WebServiceV5Helper;
import ch.vd.unireg.ws.parties.v1.Entry;
import ch.vd.unireg.ws.parties.v1.Parties;
import ch.vd.unireg.xml.party.v3.Party;

public class StresserCache {
	private static final Logger LOGGER = LoggerFactory.getLogger(StresserCache.class);
	private static final int NB_THREADS = 30;
	private static final int NB_THREADS_EVICTION = 20;
	private static CacheUtils cacheUtils;
	private static final String urlWebService = "http://unireg-in.etat-de-vaud.ch/fiscalite/int-unireg/ws/v5";
	private static final String userWebService = "unireg";
	private static final String pwdWebService = "unireg_1014";

	// PRE-PRODUCTION
//	private static final String urlWebService = "http://unireg-pp.etat-de-vaud.ch/fiscalite/unireg/ws/v5";
//	private static final String userWebService = "web-it";
//	private static final String pwdWebService = "unireg_1014";

	// PRODUCTION
//	private static final String urlWebService = "http://unireg-pr.etat-de-vaud.ch/fiscalite/unireg/ws/v5";
//	private static final String userWebService = "se renseigner...";
//	private static final String pwdWebService = "se renseigner...";

	private static final String userId = "usrreg06";
	private static final int oid = 22;

	private static final String COMMA = ";";
	private static final int TAILLE_LOT = 100;
	private static final String nomFichier1 = "tiers.csv";
	private static final String nomFichier2 = "tiers2.csv";

	public static void main(String[] args) throws IOException {

		LOGGER.info("Création du cache");
		cacheUtils = new CacheUtils();

		if (cacheUtils.isEmpty()) {
			try {
				// on lit le contenu du fichier
				final List<Integer> tiers = lireFichierTiers(nomFichier1);
				LOGGER.info("Découpage en lot");
				final List<List<Integer>> lots = creerLots(tiers);
				chargerCache(lots);
				triturerCache(tiers);

			}
			catch (ExecutionException e) {
				LOGGER.error("Erreur d'ecexution: " + e.getMessage());
			}
			catch (InterruptedException e) {
				LOGGER.error("Erreur d'Interuption: " + e.getMessage());
			}
		}


		LOGGER.info("Fin du traitment");
	}

	private static void triturerCache( List<Integer> tiers) throws ExecutionException, InterruptedException, IOException {
		final List<Integer> idLotALire = new ArrayList<>(tiers);
		Collections.shuffle(idLotALire);
		final List<Integer> idLotAEvicter = new ArrayList<>(idLotALire);
		Collections.shuffle(idLotAEvicter);
		final List<Integer> idLotACharger = new ArrayList<>(idLotAEvicter);
		Collections.shuffle(idLotACharger);

		final List<Integer> idTiers = lireFichierTiers(nomFichier2);
		LOGGER.info("Découpage en lot");
		final List<List<Integer>> lots = creerLots(idTiers);

		ExecutorService executor =Executors.newFixedThreadPool(3);
		Future futureCharge = executor.submit(new Callable<Object>() {
			@Override
			public Object call() throws Exception {

				executerInsertion(idLotACharger);
				return null;
			}
		});

		Future futureLire = executor.submit(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				executerLecture(idLotALire);
				return null;
			}
		});

	//	Future futureChargeSupplementaire = executor.submit(new ChargerCacheLot(lots));

		Future futureEvict = executor.submit(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				executerEviction(idLotAEvicter);
				return null;
			}
		});

		executor.shutdown();
		futureCharge.get();
		LOGGER.info("Fin traitement Charge");
		futureLire.get();
		LOGGER.info("Fin traitement Lecture");
		futureEvict.get();
		LOGGER.info("Fin traitement Eviction");
		//futureChargeSupplementaire.get();
		LOGGER.info("Fin traitement Charge supplementaire");


	}

	private static void executerInsertion( List<Integer> ids) throws IOException, ExecutionException, InterruptedException {

		// et on boucle sur les lots
		final ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);
		final List<Future<Long>> futures = new ArrayList<>(ids.size());
		for (Integer id : ids) {

			try {
				futures.add(executor.submit(new ChargerUnPartyDansCache(id)));
			}
			catch (Exception e) {
				LOGGER.error(e.getMessage());
			}
		}
		executor.shutdown();
		for (Future<Long> future : futures) {
			if (future.get() > 1000 ) {
				LOGGER.info("Insertion: "+future.get()+" ms");
			}
		}
		LOGGER.info("Fin Chargement cache");
	}


	private static void executerLecture( List<Integer> ids) throws IOException, ExecutionException, InterruptedException {

		// et on boucle sur les lots
		final ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);
		final List<Future<Long>> futures = new ArrayList<>(ids.size());
		for (Integer id : ids) {

			try {
				futures.add(executor.submit(new LireUnPartyDepuisCache(id)));
			}
			catch (Exception e) {
				LOGGER.error(e.getMessage());
			}
		}
		executor.shutdown();
		for (Future<Long> future : futures) {
			if (future.get() > 1000 ) {
				LOGGER.info("Lecture: "+future.get()+" ms");
			}
		}
		LOGGER.info("Fin Lecture cache");
	}

	private static void executerEviction( List<Integer> ids) throws IOException, ExecutionException, InterruptedException {

		// et on boucle sur les lots
		final ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS_EVICTION);
		final List<Future<Long>> futures = new ArrayList<>(ids.size());
		for (Integer id : ids) {

			try {
				futures.add(executor.submit(new EvictUnPartyDepuisCache(id)));
			}
			catch (Exception e) {
				LOGGER.error(e.getMessage());
			}
		}
		executor.shutdown();
		for (Future<Long> future : futures) {
			if (future.get() > 1000 ) {
				LOGGER.info("Eviction: "+future.get()+" ms");
			}

		}
		LOGGER.info("Fin eviction cache");
	}


	private static void chargerCache( List<List<Integer>> lots) throws IOException, ExecutionException, InterruptedException {

		// et on boucle sur les lots
		final ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);
		final List<Future<?>> futures = new ArrayList<>(lots.size());
		for (List<Integer> lot : lots) {

		LOGGER.info("Chargement du lot de ["+lot.get(0)+"-"+lot.get(lot.size()-1)+"]");
			try {
				final Parties parties = WebServiceV5Helper.getParties(urlWebService, userWebService, pwdWebService, userId, oid, lot, null);
				futures.add(executor.submit(new ChargerCache(parties)));
			}
			catch (Exception e) {
				LOGGER.error(e.getMessage());
			}
		}
		executor.shutdown();
		for (Future<?> future : futures) {
			future.get();
		}
		LOGGER.info("Fin du chargement initial du cache");
	}

	private static List<List<Integer>> creerLots(List<Integer> tiers) {
		// ensuite, on fait des groupes de TAILLE_LOT
		final int nbLots = tiers.size() / TAILLE_LOT + 1;
		final List<List<Integer>> lots = new ArrayList<>(nbLots);
		for (int i = 0 ; i < nbLots ; ++ i) {
			final List<Integer> lot = tiers.subList(i * TAILLE_LOT, Math.min((i + 1) * TAILLE_LOT, tiers.size()));
			if (!lot.isEmpty()) {
				lots.add(lot);
			}
		}
		return lots;
	}

	private static List<Integer> lireFichierTiers(String nom) throws IOException {
		final List<Integer> tiers = new ArrayList<>();
		LOGGER.info("Chargement des numéro depuis le fichier");
		try (InputStream in = StresserCache.class.getResourceAsStream(nom);
		     InputStreamReader fis = new InputStreamReader(in);
		     BufferedReader reader = new BufferedReader(fis)) {

			String line = reader.readLine();
			while (line != null) {
				final Integer ctb = Integer.valueOf(line);
				tiers.add(ctb);
				line = reader.readLine();
			}
		}
		return tiers;
	}

	static final class ChargerCache implements Runnable {
		private final Parties parties;

		private ChargerCache(Parties p) {
			this.parties = p;
		}

		@Override
		public void run() {
				for (Entry entry : parties.getEntries()) {
					final Party party = entry.getParty();
					cacheUtils.addTiers(party);
				}
		}
	}



	static final class ChargerUnPartyDansCache implements Callable {
		private final int idParty;

		private ChargerUnPartyDansCache(int id) {
			this.idParty = id;
		}


		@Override
		public Object call() throws Exception {
			final Party tiersFromCache = cacheUtils.getTiersFromCache(idParty);
			if (tiersFromCache == null) {
				final Party party = WebServiceV5Helper.getParty(urlWebService, userWebService, pwdWebService, userId, oid, idParty, null);
				final long start = System.currentTimeMillis();
				cacheUtils.addTiers(party);
				return System.currentTimeMillis() - start;
			}
			return 0L;

		}
	}


	static final class LireUnPartyDepuisCache implements Callable {
		private final int idParty;

		private LireUnPartyDepuisCache(int id) {
			this.idParty = id;
		}

		@Override
		public Object call() throws Exception {
			final long start = System.currentTimeMillis();
			final Party tiersFromCache = cacheUtils.getTiersFromCache(idParty);
			return System.currentTimeMillis() - start;

		}
	}


	static final class EvictUnPartyDepuisCache implements Callable {
		private final int idParty;

		private EvictUnPartyDepuisCache(int id) {
			this.idParty = id;
		}

		@Override
		public Object call() throws Exception {
			final long start = System.currentTimeMillis();
			cacheUtils.evictTiers(idParty);
			return System.currentTimeMillis() - start;
		}
	}

	private static class ChargerCacheLot implements Runnable {
		private final List<List<Integer>> lots;

		private ChargerCacheLot(List<List<Integer>> lots) {
			this.lots = lots;
		}

		@Override
		public void run()  {
			final ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);
			final List<Future<?>> futures = new ArrayList<>(lots.size());
			for (List<Integer> lot : lots) {

				LOGGER.info("Chargement du lot de ["+lot.get(0)+"-"+lot.get(lot.size()-1)+"]");
				try {
					final Parties parties = WebServiceV5Helper.getParties(urlWebService, userWebService, pwdWebService, userId, oid, lot, null);
					futures.add(executor.submit(new ChargerCache(parties)));
				}
				catch (Exception e) {
					LOGGER.error(e.getMessage());
				}
			}
			executor.shutdown();
			for (Future<?> future : futures) {
				try {
					future.get();
				}
				catch (InterruptedException e) {
					LOGGER.info(e.getMessage());
				}
				catch (ExecutionException e) {
					LOGGER.info(e.getMessage());
				}
			}
			LOGGER.info("Fin du chargement initial du cache");
		}

		// et on boucle sur les lots

	}
}
