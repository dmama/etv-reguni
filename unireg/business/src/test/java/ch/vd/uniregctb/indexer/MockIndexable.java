package ch.vd.uniregctb.indexer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

public class MockIndexable extends IndexableData {

	private String nom;
	private String prenom;
	private String nomCourrier;
	private String champ1;

	public MockIndexable(Long id, String nom, String prenom, String nomCourrier, String champ1) {
		super(id, "TheType", "TheSubType");
		this.nom = nom;
		this.prenom = prenom;
		this.nomCourrier = nomCourrier;
		this.champ1 = champ1;
	}

	public String getSubType() {
		return subType;
	}

	@Override
	public Document asDoc() {
		Document d = super.asDoc();

		d.add(new Field("Nom", nom, Field.Store.YES, Field.Index.ANALYZED));
		d.add(new Field("Prenom", prenom, Field.Store.YES, Field.Index.ANALYZED));
		d.add(new Field("NomCourier", nomCourrier, Field.Store.YES, Field.Index.ANALYZED));
		d.add(new Field("Champ1", champ1, Field.Store.YES, Field.Index.ANALYZED));

		return d;
	}
}
