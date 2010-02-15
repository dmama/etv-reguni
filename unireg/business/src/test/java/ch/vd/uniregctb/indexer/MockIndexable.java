package ch.vd.uniregctb.indexer;

import java.util.ArrayList;
import java.util.HashMap;

public class MockIndexable extends AbstractIndexable {
	
	private ArrayList<String> fields;
	private ArrayList<String> values;
	private Long id;
	
	public MockIndexable() {

		id = 12L;
		init();
	}

	public MockIndexable(long id) {

		this.id = id;
		init();
	}
	
	private void init() {

		fields = new ArrayList<String>();
		fields.add("Nom");
		fields.add("Prenom");
		fields.add("NomCourier");
		fields.add("Champ1");
		values = new ArrayList<String>();
		values.add("U");
		values.add("a good man du");
		values.add("dardare");
		values.add("essuies");
	}
	
	public Long getID() {
		return id;
	}

	public String getSubType() {
		return "TheSubType";
	}

	public String getType() {
		return "TheType";
	}

	@Override
	public HashMap<String, String> getKeyValues() throws IndexerException {
		
		return listsToMap(fields, values);
	}

}
