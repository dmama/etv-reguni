package ch.vd.uniregctb.utils;

import javax.servlet.jsp.PageContext;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.displaytag.decorator.DisplaytagColumnDecorator;
import org.displaytag.exception.DecoratorException;
import org.displaytag.properties.MediaTypeEnum;

public class SwissCurrencyColumnDecorator implements DisplaytagColumnDecorator {

	private static final DecimalFormat FORMAT;

	static {
		final DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setGroupingSeparator('\'');
		FORMAT = new DecimalFormat("#,###", symbols);
	}

	@Override
	public Object decorate(Object o, PageContext pageContext, MediaTypeEnum mediaTypeEnum) throws DecoratorException {
		try {
			if (o instanceof Number) {
				return FORMAT.format(o);
			}
			else if (o instanceof String) {
				return FORMAT.format(Double.parseDouble((String) o));
			}
			else {
				// pas de décoration
				return o;
			}
		}
		catch (Exception e) {
			// pas de décoration
			return o;
		}
	}
}
