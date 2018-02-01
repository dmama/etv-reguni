package ch.vd.uniregctb.indexer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;

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

	@Override
	public String getSubType() {
		return subType;
	}

	@Override
	public Document asDoc() {
		Document d = super.asDoc();

		d.add(new TextField("Nom", nom, Field.Store.YES));
		d.add(new TextField("Prenom", prenom, Field.Store.YES));
		d.add(new TextField("NomCourier", nomCourrier, Field.Store.YES));
		d.add(new TextField("Champ1", champ1, Field.Store.YES));

		return d;
	}
}
