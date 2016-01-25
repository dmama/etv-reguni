/**
 *
 */
package ch.vd.uniregctb.utils;

import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;

/**
 * @author xcicfh
 *
 */
public abstract class WebContextUtils {



	public static Locale getDefaultLocale() {
		return  LocaleContextHolder.getLocale();
	}
}
