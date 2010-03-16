package ch.vd.uniregctb.indexer;

import java.util.HashMap;

import org.apache.log4j.Logger;

public abstract class AbstractSubIndexable implements SubIndexable {

	//private final Logger LOGGER = Logger.getLogger(AbstractSubIndexable.class);

	public AbstractSubIndexable() {
	}

	/**
	 * Map spécialisée pour le stockage des données d'indexation.
	 */
	public class IndexMap extends HashMap<String, String> {

		private static final long serialVersionUID = 6022209107781842679L;

		/**
		 * Formate et ajoute une valeur brute. A utiliser de préférence sur 'put'.
		 */
		public String putRawValue(String key, Object value) {
			String formatted = formatValueAsString(value);
			return put(key, formatted);
		}

		private String formatValueAsString(Object value) throws IndexerException {
			String str = null; // We should never return null
			try {
				str = IndexerFormatHelper.objectToString(value);
			}
			catch (Exception e) {
				throw new IndexerException(e);
			}
			return str;
		}
	}

	private IndexMap map = null;

	/**
	 * Remplis la map avec les données d'indexation de l'indexable courant.
	 */
	protected abstract void fillKeyValues(IndexMap map) throws IndexerException;

	public final HashMap<String, String> getKeyValues() throws IndexerException {
		if (map == null) {
			// on initialise à la demande et une seule fois pour optimiser les appels (car l'objet associé ne change pas).
			map = new IndexMap();
			fillKeyValues(map);
		}
		return map;
	}

	public static void debugDumpValues(Logger log, HashMap<String, String> values) {

		for (String k : values.keySet()) {
			String v = values.get(k);
			log.error(k + " => " + v);
		}
		values = null;
	}

}
