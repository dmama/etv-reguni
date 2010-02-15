package ch.vd.uniregctb.indexer;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 *
 *
 */
public abstract class AbstractIndexable implements Indexable {

	//private static final Logger LOGGER = Logger.getLogger(AbstractIndexable.class);

	public abstract HashMap<String, String> getKeyValues() throws IndexerException;

	protected HashMap<String, String> listsToMap(List<String> fields, List<String> values) {
		HashMap<String, String> keyValues = new HashMap<String, String>();
		for (int i=0;i<fields.size();i++) {
			keyValues.put(fields.get(i), values.get(i));
		}
		return keyValues;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
	}

}
