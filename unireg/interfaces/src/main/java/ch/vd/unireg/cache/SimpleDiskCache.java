package ch.vd.unireg.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.ObjectKey;
import ch.vd.uniregctb.cache.PersistentCache;
import ch.vd.uniregctb.cache.SimpleCacheStats;

/**
 * Implémente un cache persistant où chaque objet est stocké sous forme de fichier sur le disque. Les fichiers sont stockés sous une arborescence de répertoire pour éviter d'avoir tous les fichiers en
 * vrac.
 *
 * @param <T> le type d'objet stocké
 */
public class SimpleDiskCache<T extends Serializable> implements PersistentCache<T>, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(SimpleDiskCache.class);

	private String storeDir;
	private static final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
	private final SimpleCacheStats stats = new SimpleCacheStats();

	public void setStoreDir(String storeDir) {
		this.storeDir = storeDir;
		if (!this.storeDir.endsWith("/")) {
			this.storeDir += "/";
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		File dir = new File(storeDir);
		if (!dir.exists() && !dir.mkdirs()) {
			throw new RuntimeException("Impossible de créer le répertoire [" + storeDir + ']');
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public T get(ObjectKey key) {

		final long start = System.nanoTime();

		final String directory = calculateDir(key.getId());
		final String filename = directory + calculateFilename(key);

		// on recherche le fichier
		FileInputStream fis;
		try {
			fis = new FileInputStream(filename);
		}
		catch (FileNotFoundException e) {
			// le fichier n'existe pas -> pas d'objet
			stats.addMiss();
			return null;
		}

		// on désérialise le fichier
		T object = null;
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(fis);
			object = (T) ois.readObject();
		}
		catch (Throwable e) {
			LOGGER.warn("Impossible de lire l'objet avec la clé [" + key.getId() + ',' + key.getComplement() + ']', e);
			// le fichier est corrumpu ou illisible -> on l'efface
			File file = new File(filename);
			//noinspection ResultOfMethodCallIgnored
			file.delete();
			stats.addMiss();
			return null;
		}
		finally {
			IOUtils.closeQuietly(fis);
			IOUtils.closeQuietly(ois);
		}

		LOGGER.warn("time to get = " + ((System.nanoTime() - start) / 1000000L) + " ms");

		stats.addHit();
		return object;
	}

	private static String calculateFilename(ObjectKey key) {
		return String.format("%d_%s.dmp", key.getId(), key.getComplement());
	}

	private static String buildTempFilename(ObjectKey key) {
		final long threadId = Thread.currentThread().getId();
		return String.format("%d_%s.%s-%d.tmp", key.getId(), key.getComplement(), jvmName, threadId);
	}

	/**
	 * A l'heure actuelle, le plus grand numéro d'individu est 1'000'000.
	 * <p/>
	 * <pre>
	 *           1 => ${baseDir}0/00/00/
	 *         123 => ${baseDir}0/00/12/
	 *     123'456 => ${baseDir}0/12/34/
	 *   1'234'567 => ${baseDir}1/23/45/
	 * 123'456'789 => ${baseDir}123/45/67/
	 * </pre>
	 *
	 * @param id l'id de l'objet stocké
	 * @return le chemin absolu du répertoire où est stocké le fichier
	 */
	protected String calculateDir(long id) {

		final String idAsString = String.format("%07d", id);
		final int idLength = idAsString.length();

		final StringBuilder dir = new StringBuilder(idLength + 3 + storeDir.length());

		dir.append(storeDir);
		dir.append(idAsString.subSequence(0, idLength - 6));
		dir.append('/');
		dir.append(idAsString.subSequence(idLength - 6, idLength - 4));
		dir.append('/');
		dir.append(idAsString.subSequence(idLength - 4, idLength - 2));
		dir.append('/');

		return dir.toString();
	}

	@Override
	public void put(ObjectKey key, T object) {

		// on crée le répertoire où sera stocké le fichier
		final String directory = calculateDir(key.getId());
		mkpath(directory);

		final String tempname = directory + buildTempFilename(key);
		final File tempfile = new File(tempname);

		// on sérialise l'objet dans un fichier temporaire
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(tempfile);
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(fos);
			oos.writeObject(object);
		}
		catch (Throwable e) {
			LOGGER.warn("Impossible d'écrire l'objet avec la clé [" + key.getId() + ',' + key.getComplement() + ']', e);
			//noinspection ResultOfMethodCallIgnored
			tempfile.delete();
			return;
		}
		finally {
			IOUtils.closeQuietly(fos);
			IOUtils.closeQuietly(oos);
		}

		// on renomme le fichier temporaire dans son appellation définitive (de manière aussi atomique que possible)
		final String filename = directory + calculateFilename(key);
		moveFileTo(tempfile, filename);
	}

	@Override
	public void putAll(Map<? extends ObjectKey, T> objectKeyTMap) {
		for (Map.Entry<? extends ObjectKey, T> entry : objectKeyTMap.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Crée les répertoires représentés par le chemin spécifié. Les répertoires sont créées à la volée si nécessaire. Cette méthode ne fait si les répertoires existent déjà.
	 *
	 * @param path un chemin qui doit être créé sur le disque.
	 */
	private static void mkpath(String path) {
		final File dir = new File(path);
		int i = 0;
		while (!dir.isDirectory()) {
			if (!dir.mkdirs()) {
				// il est possible qu'un autre thread/process soit entrain de créer le même répertoire, on attend la moindre et on essaie encore
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
					// ignored
				}
				if (++i > 10) {
					// après 10 essais, on laisse tomber
					throw new RuntimeException("Impossible de créer le répertoire [" + path + ']');
				}
			}
		}
	}

	private static void moveFileTo(File tempfile, String filename) {

		final File file = new File(filename);

		int i = 0;
		while (!tempfile.renameTo(file)) {
			if (!file.delete()) {
				//noinspection ResultOfMethodCallIgnored
				tempfile.delete();
				throw new RuntimeException("Impossible d'effacer le fichier [" + filename + ']');
			}
			if (++i > 100) {
				//noinspection ResultOfMethodCallIgnored
				tempfile.delete();
				throw new RuntimeException("Impossible de renommer le fichier [" + tempfile + "] en [" + filename + ']');
			}
		}
	}

	@Override
	public void removeAll(final long id) {

		final String directory = calculateDir(id);
		final File dir = new File(directory);
		if (!dir.isDirectory()) {
			// le répertoire n'existe pas => rien à faire
		}

		final File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(String.valueOf(id) + '_');
			}
		});
		if (files != null) {
			for (File f : files) {
				if (!f.delete()) {
					throw new RuntimeException("Impossible de supprimer le fichier [" + f + ']');
				}
			}
		}
	}

	@Override
	public void clear() {
		final File dir = new File(storeDir);
		try {
			FileUtils.cleanDirectory(dir);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public CacheStats buildStats() {
		return new SimpleCacheStats(stats);
	}
}
