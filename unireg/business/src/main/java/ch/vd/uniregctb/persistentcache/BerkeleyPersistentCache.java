package ch.vd.uniregctb.persistentcache;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryCursor;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;
import com.sleepycat.je.Transaction;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.SimpleCacheStats;

public class BerkeleyPersistentCache<T extends Serializable> implements PersistentCache<T>, InitializingBean, DisposableBean {

//	private static final Logger LOGGER = Logger.getLogger(BerkeleyPersistentCache.class);

	private Class<T> clazz;
	private Environment env;
	private String homeDirectory;
	private StoredMap<ObjectKey, T> map;
	private Database catalogDb;
	private Database mainDb;
	private SecondaryDatabase secDb;
	private TupleBinding<Long> secKeyBinding;
	private SerialBinding<ObjectKey> keyBinding;
	private int cachePercent = 5;
	private final SimpleCacheStats stats = new SimpleCacheStats();

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHomeDirectory(String homeDirectory) {
		this.homeDirectory = homeDirectory;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setClazz(Class<T> clazz) {
		this.clazz = clazz;
	}

	/**
	 * Spécifie le pourcentage maximum d'utilisation mémoire pour le cache (par rapport à -Xmx)
	 *
	 * @param cachePercent un pourcentage
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public void setCachePercent(int cachePercent) {
		this.cachePercent = cachePercent;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		final EnvironmentConfig envConfig = new EnvironmentConfig();
		envConfig.setTransactional(true);
		envConfig.setAllowCreate(true);
		envConfig.setCachePercent(cachePercent);

		final File dir = new File(homeDirectory);
		//noinspection ResultOfMethodCallIgnored
		dir.mkdirs();

		env = new Environment(dir, envConfig);

		// Crée la base de donnée principale
		Transaction txn = env.beginTransaction(null, null);
		DatabaseConfig dbConfig = new DatabaseConfig();
		dbConfig.setTransactional(true);
		dbConfig.setAllowCreate(true);
		mainDb = env.openDatabase(txn, "dataDb", dbConfig);

		// Crée une seconde base de données pour stocker les bindings (bizarre, mais apparemment obligatoire)
		DatabaseConfig catalogConfig = new DatabaseConfig();
		catalogConfig.setTransactional(true);
		catalogConfig.setAllowCreate(true);
		catalogDb = env.openDatabase(txn, "catalogDb", catalogConfig);

		// Create a serial binding for key and value objects. Serial bindings can be used to store any Serializable object.
		StoredClassCatalog catalog = new StoredClassCatalog(catalogDb);
		keyBinding = new SerialBinding<ObjectKey>(catalog, ObjectKey.class);
		final SerialBinding<T> valueBinding = new SerialBinding<T>(catalog, clazz);

		// On crée une seconde base de données qui indexe les clés primaires de la première base de données par id.
		// Cela permet de retrouver rapidemment toutes les données par id (sans tenir compte du complément, donc).
		secKeyBinding = TupleBinding.getPrimitiveBinding(Long.class);
		SecondaryConfig secConfig = new SecondaryConfig();
		secConfig.setTransactional(true);
		secConfig.setAllowCreate(true);
		secConfig.setSortedDuplicates(true);
		secConfig.setKeyCreator(new KeyIdKeyCreator(secKeyBinding, keyBinding));
		secDb = env.openSecondaryDatabase(txn, "bindingsSecDb", mainDb, secConfig);

		txn.commit();

		map = new StoredMap<ObjectKey, T>(mainDb, keyBinding, valueBinding, true);
	}

	@Override
	public void destroy() throws Exception {
		secDb.close();
		catalogDb.close();
		mainDb.close();
		env.close();
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public T get(ObjectKey key) {
		final T o = map.get(key);
		if (o == null) {
			stats.addMiss();
		}
		else {
			stats.addHit();
		}
		return o;
	}

	@Override
	public void put(ObjectKey key, T object) {
		map.put(key, object);
	}

	@Override
	public void putAll(Map<? extends ObjectKey, T> m) {
		map.putAll(m);
	}

	@Override
	public void removeAll(long id) {

		//final long start = System.nanoTime();

		// on efface toutes les données en relation avec l'id spécifié
		final DatabaseEntry key = new DatabaseEntry();
		secKeyBinding.objectToEntry(id, key);
		secDb.delete(null, key);

		//LOGGER.warn("time to remove id " + id + " = " + ((System.nanoTime() - start) / 1000000L) + " ms");
	}

	@SuppressWarnings({"UnusedDeclaration"})
	private void dumpEntries(long id) {

		final DatabaseEntry key = new DatabaseEntry();
		secKeyBinding.objectToEntry(id, key);

		final DatabaseEntry primaryKey = new DatabaseEntry();
		final DatabaseEntry dataEntry = new DatabaseEntry();
		SecondaryCursor cursor = secDb.openCursor(null, null);
		try {
			if (cursor.getSearchKey(key, primaryKey, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
				do {
					Long k = secKeyBinding.entryToObject(key);
					if (k != id) {
						break;
					}
					ObjectKey pk = keyBinding.entryToObject(primaryKey);
					System.out.println("primary key id=" + pk.getId() + " complement=" + pk.getComplement());
				}
				while (cursor.getNext(key, primaryKey, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS);
			}
		}
		finally {
			cursor.close();
		}
	}

	@Override
	public void clear() {
		map.clear();
	}

	/**
	 * Cette classe permet de construire une clé secondaire (id) à partir d'une clé primaire (id + complement).
	 */
	private class KeyIdKeyCreator implements SecondaryKeyCreator {

		private final EntryBinding<Long> secKeyBinding;
		private final EntryBinding<ObjectKey> primareyKeyBinding;

		public KeyIdKeyCreator(EntryBinding<Long> secKeyBinding, EntryBinding<ObjectKey> primareyKeyBinding) {
			this.secKeyBinding = secKeyBinding;
			this.primareyKeyBinding = primareyKeyBinding;
		}

		@Override
		public boolean createSecondaryKey(SecondaryDatabase secondary, DatabaseEntry key, DatabaseEntry data, DatabaseEntry result) {
			final ObjectKey k = primareyKeyBinding.entryToObject(key);
			final long id = k.getId();
			secKeyBinding.objectToEntry(id, result);
			return true;
		}
	}

	@Override
	public CacheStats buildStats() {
		return new SimpleCacheStats(stats);
	}
}
