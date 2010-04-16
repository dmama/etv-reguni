package ch.vd.uniregctb.indexer;

import java.util.HashMap;

/**
 * Classe représentant les données brutes extraites d'un indexable. Utilisée de manière interne par l'indexeur.
 * <p>
 * Cette classe est immutable.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public final class IndexableData {

	private final Long id;
	private final String type;
	private final String subType;
	private final HashMap<String, String> keyValues;

	public IndexableData(Indexable indexable) {
		id = indexable.getID();
		type = indexable.getType();
		subType = indexable.getSubType();
		keyValues = indexable.getKeyValues();
	}

	public Long getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public String getSubType() {
		return subType;
	}

	public HashMap<String, String> getKeyValues() {
		return keyValues;
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}
}
