package ch.vd.uniregctb.persistentcache;

import java.io.File;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class BerkeleyPersistentCache<T extends Serializable> implements PersistentCache<T>, InitializingBean, DisposableBean {

	private static final Logger LOGGER = Logger.getLogger(BerkeleyPersistentCache.class);

	private Class<T> clazz;
	private Environment env;
	private String homeDirectory;
	private StoredMap<ObjectKey, T> map;
	private Database catalogDb;
	private Database mainDb;

	public void setHomeDirectory(String homeDirectory) {
		this.homeDirectory = homeDirectory;
	}

	public void setClazz(Class<T> clazz) {
		this.clazz = clazz;
	}

	public void afterPropertiesSet() throws Exception {

		final EnvironmentConfig envConfig = new EnvironmentConfig();
		envConfig.setTransactional(true);
		envConfig.setAllowCreate(true);

		final File dir = new File(homeDirectory);
		//noinspection ResultOfMethodCallIgnored
		dir.mkdirs();

		env = new Environment(dir, envConfig);

		// Crée la base de donnée principale
		Transaction txn = env.beginTransaction(null, null);
		DatabaseConfig dbConfig = new DatabaseConfig();
		dbConfig.setTransactional(true);
		dbConfig.setAllowCreate(true);
		dbConfig.setSortedDuplicates(true);
		mainDb = env.openDatabase(txn, "dataDb", dbConfig);

		// Crée une seconde base de données pour stocker les bindings (bizarre, mais apparemment obligatoire)
		DatabaseConfig catalogConfig = new DatabaseConfig();
		catalogConfig.setTransactional(true);
		catalogConfig.setAllowCreate(true);
		catalogDb = env.openDatabase(txn, "catalogDb", catalogConfig);

		// Create a serial binding for key and value objects. Serial bindings can be used to store any Serializable object.
		StoredClassCatalog catalog = new StoredClassCatalog(catalogDb);
		EntryBinding<ObjectKey> keyBinding = new SerialBinding<ObjectKey>(catalog, ObjectKey.class);
		EntryBinding<T> valueBinding = new SerialBinding<T>(catalog, clazz);
		txn.commit();

		map = new StoredMap<ObjectKey, T>(mainDb, keyBinding, valueBinding, true);
	}

	public void destroy() throws Exception {
		catalogDb.close();
		mainDb.close();
		env.close();
	}

	@SuppressWarnings({"unchecked"})
	public T get(ObjectKey key) {
		final long start = System.nanoTime();
		final T t = map.get(key);
		LOGGER.warn("time to get = " + ((System.nanoTime() - start) / 1000000L) + " ms");
		return t;
	}

	public void put(ObjectKey key, T object) {
		map.put(key, object);
	}

	public void removeAll(long id) {

		final long start = System.nanoTime();

		final Iterator<Map.Entry<ObjectKey, T>> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			final Map.Entry<ObjectKey, T> entry = iterator.next();
			final ObjectKey key = entry.getKey();
			if (key.getId() == id) {
				iterator.remove();
			}
		}

		LOGGER.warn("time to remove id " + id + " = " + ((System.nanoTime() - start) / 1000000L) + " ms");
	}

	public void clear() {
		map.clear();
	}
}
