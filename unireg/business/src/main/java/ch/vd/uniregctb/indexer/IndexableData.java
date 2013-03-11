package ch.vd.uniregctb.indexer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;

import ch.vd.registre.simpleindexer.LuceneData;
import ch.vd.uniregctb.indexer.lucene.LuceneHelper;

/**
 * Classe représentant les données brutes extraites d'un indexable. Utilisée de manière interne par l'indexeur.
 * <p>
 * Cette classe est immutable.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class IndexableData implements LuceneData {

	protected final Long id;
	protected final String type;
	protected final String subType;

	protected IndexableData(Long id, String type, String subType) {
		this.id = id;
		this.type = type;
		this.subType = subType;
	}

	public long getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public String getSubType() {
		return subType;
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}

	public Document asDoc() {
		Document d = new Document();

		d.add(new StringField(LuceneHelper.F_ENTITYID, id.toString(), Field.Store.YES));
		d.add(new StringField(LuceneHelper.F_DOCID, type.toLowerCase() + '-' + id, Field.Store.YES));
		d.add(new StringField(LuceneHelper.F_DOCTYPE, type.toLowerCase(), Field.Store.YES));
		d.add(new StringField(LuceneHelper.F_DOCSUBTYPE, (subType == null ? "" : subType.toLowerCase()), Field.Store.YES));

		return d;
	}
}
