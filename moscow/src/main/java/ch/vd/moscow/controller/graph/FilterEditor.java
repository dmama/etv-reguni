package ch.vd.moscow.controller.graph;

import java.beans.PropertyEditorSupport;

import org.apache.commons.lang.StringUtils;

public class FilterEditor extends PropertyEditorSupport {

	@Override
	public String getAsText() {
		if (getValue() instanceof Filter) {
			final Filter filter = (Filter) getValue();
			return filter.toString();
		}
		else if (getValue() instanceof Filter[]) {
			Filter[] filters = (Filter[]) getValue();
			StringBuilder s = new StringBuilder();
			boolean first = true;
			for (Filter filter : filters) {
				if (!first) {
					s.append(",");
				}
				s.append(filter.toString());
				first = false;
			}
			return s.toString();
		}
		return super.getAsText();
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.isBlank(text)) {
			setValue(null);
		}
		else {
			if (text.contains(",")) {
				final String[] filterTexts = text.split(",");
				final Filter[] filters = new Filter[filterTexts.length];
				for (int i = 0, filterTextsLength = filterTexts.length; i < filterTextsLength; i++) {
					filters[i] = parseFilter(filterTexts[i]);
				}
				setValue(filters);
			}
			else {
				setValue(parseFilter(text));
			}
		}
	}

	private static Filter parseFilter(String text) {
		final String[] tokens = text.split(":");
		if (tokens.length == 0) {
			throw new IllegalArgumentException("Could not parse filter [" + text + "]: missing ':'");
		}
		else if (tokens.length > 2) {
			throw new IllegalArgumentException("Could not parse filter [" + text + "]: expecting only one ':'");
		}
		try {
			final CallDimension dimension = CallDimension.valueOf(tokens[0]);
			return new Filter(dimension, tokens[1]);
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Could not parse filter [" + text + "]: " + ex.getMessage(), ex);
		}
	}
}
