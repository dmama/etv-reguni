package ch.vd.uniregctb.indexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class GenericIndexable extends AbstractIndexable {
	
	//private Logger LOGGER = Logger.getLogger(GenericIndexable.class);
	
	private String subType;
	private String type;
	private long id = -1;
	
	private List<String> fields = new ArrayList<String>();
	private List<String> values = new ArrayList<String>();

	public GenericIndexable(long id, String type, List<String> fields, List<String> values) {
		this.type = type;
		this.subType = type;
		this.id = id;
		this.fields = fields;
		this.values = values;
	}
	
	public GenericIndexable(long id, String type, String subType, List<String> fields, List<String> values) {
		this.type = type;
		this.subType = subType;
		this.id = id;
		this.fields = fields;
		this.values = values;
	}
	public Long getID() {
		return id;
	}

	public String getType() {
		return type;
	}

	public String getSubType() {
		return subType;
	}

	@Override
	public HashMap<String, String> getKeyValues() throws IndexerException {
		return listsToMap(fields, values);
	}

}
