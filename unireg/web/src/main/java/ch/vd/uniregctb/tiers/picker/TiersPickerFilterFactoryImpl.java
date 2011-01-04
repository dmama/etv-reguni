package ch.vd.uniregctb.tiers.picker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class TiersPickerFilterFactoryImpl implements TiersPickerFilterFactory {

	public TiersPickerFilter parse(String paramsString) {
		final Map<String, String> params = mapParams(paramsString);
		return new BasicTiersPickerFilter(params);
	}

	/**
	 * Parse une string qui représente une série de clés-valeurs et remplit une map avec les tokens.
	 *
	 * @param paramsString string représentant des clés-valeurs avec le format <i>key0:val0+key1:val1+...</i>
	 * @return une map des clés-valeurs
	 */
	private Map<String, String> mapParams(String paramsString) {

		if (StringUtils.isBlank(paramsString)) {
			return Collections.emptyMap();
		}

		final Map<String, String> params = new HashMap<String, String>();
		final String[] keyValues = paramsString.split("\\+");
		for (String keyValue : keyValues) {
			final String[] tokens = keyValue.split(":");
			final String key = tokens[0];
			final String value = tokens[1];
			params.put(key, value);
		}

		return params;
	}
}
