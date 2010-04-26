package ch.vd.uniregctb.indexer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * Classe représentant les données brutes extraites d'un indexable. Utilisée de manière interne par l'indexeur.
 * <p>
 * Cette classe est immutable.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class IndexableData {

	protected Long id;
	protected String type;
	protected String subType;

	protected IndexableData() {
	}

	protected IndexableData(Long id, String type, String subType) {
		this.id = id;
		this.type = type;
		this.subType = subType;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSubType() {
		return subType;
	}

	public void setSubType(String subType) {
		this.subType = subType;
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}

	public Document asDoc() {
		Document d = new Document();

		d.add(new Field(LuceneEngine.F_ENTITYID, id.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		d.add(new Field(LuceneEngine.F_DOCID, type.toLowerCase() + "-" + id, Field.Store.YES, Field.Index.NOT_ANALYZED));
		d.add(new Field(LuceneEngine.F_DOCTYPE, type.toLowerCase(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		d.add(new Field(LuceneEngine.F_DOCSUBTYPE, (subType == null ? "" : subType.toLowerCase()), Field.Store.YES, Field.Index.NOT_ANALYZED));

		return d;
	}
}
