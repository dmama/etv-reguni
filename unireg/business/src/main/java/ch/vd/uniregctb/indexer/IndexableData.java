package ch.vd.uniregctb.indexer;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

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
		final Document d = new Document();

		d.add(new StringField(LuceneHelper.F_ENTITYID, id.toString(), Field.Store.YES));
		d.add(new StringField(LuceneHelper.F_DOCID, type.toLowerCase() + '-' + id, Field.Store.YES));
		d.add(new StringField(LuceneHelper.F_DOCTYPE, type.toLowerCase(), Field.Store.YES));
		d.add(new StringField(LuceneHelper.F_DOCSUBTYPE, (subType == null ? StringUtils.EMPTY : subType.toLowerCase()), Field.Store.YES));

		return d;
	}

	protected static void addStoredValue(Document d, String name, String value) {
		d.add(new StoredField(name, toString(value)));
	}

	protected static void addNumber(Document d, String name, Integer number) {
		final Field field;
		if (number != null) {
			field = new IntField(name, number, Field.Store.YES);
		}
		else {
			field = new StringField(name, StringUtils.EMPTY, Field.Store.YES);
		}
		d.add(field);
	}

	protected static void addNumber(Document d, String name, Long number) {
		final Field field;
		if (number != null) {
			field = new LongField(name, number, Field.Store.YES);
		}
		else {
			field = new StringField(name, StringUtils.EMPTY, Field.Store.YES);
		}
		d.add(field);
	}

	protected static void addAnalyzedValue(Document d, String name, String value) {
		d.add(new TextField(name, toString(value), Field.Store.YES));
	}

	protected static void addNotAnalyzedValue(Document d, String name, String value) {
		d.add(new StringField(name, toString(value), Field.Store.YES));
	}

	protected static String toString(String value) {
		return value == null ? StringUtils.EMPTY : value;
	}
}
