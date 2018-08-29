package ch.vd.unireg.message;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Configuration permettant d'exposer un bean {@link MessageSource} et d'accéder à ses clés de manière statique.
 */
public class MessageHelper {
	/**
	 * L'unique instance de {@link MessageHelper}.
	 */
	private static final MessageHelper INSTANCE = new MessageHelper();

	private MessageSource messageSource;

	private Locale localeDefinie = Locale.forLanguageTag("fr_CH");

	private static Locale getLocaleDefinie() {
		if (INSTANCE.localeDefinie == null) {
			return LocaleContextHolder.getLocale();
		}
		return INSTANCE.localeDefinie;
	}

	/**
	 * Retourne le <code>code</code> correspondant dans la {@link Locale} definie par défaut.
	 *
	 * @param code
	 * @return
	 */
	public static String getMessage(String code) {
		return INSTANCE.messageSource.getMessage(code, null, getLocaleDefinie());
	}

	/**
	 * Retourne le <code>code</code> correspondant dans la {@link Locale} retournée par la locale
	 * <code>locale</code>.
	 *
	 * @param code
	 * @param locale
	 * @return
	 */
	public static String getMessage(String code, Locale locale) {
		if (code == null) {
			return null;
		}
		return INSTANCE.messageSource.getMessage(code, null, locale);
	}

	/**
	 * Retourne le <code>code</code> correspondant dans la {@link Locale} retournée par la locale
	 * <code>locale</code>. <br>
	 * Si le message n'existe pas, le retour est <code>defaultMessage</code>.
	 *
	 * @param code
	 * @param defaultMessage
	 * @param locale
	 * @return
	 */
	public static String getMessageWithDefault(String code, String defaultMessage, Locale locale) {
		return INSTANCE.messageSource.getMessage(code, null, defaultMessage, locale);
	}

	/**
	 * Retourne le <code>code</code> correspondant dans la {@link Locale} retournée par la locale
	 * <code>locale</code>. <br>
	 * Si le message n'existe pas, le retour est <code>defaultMessage</code>.
	 *
	 * @param code
	 * @param defaultMessage
	 * @return
	 */
	public static String getMessageWithDefault(String code, String defaultMessage) {
		return INSTANCE.messageSource.getMessage(code, null, defaultMessage, getLocaleDefinie());
	}


	/**
	 * Retourne le <code>code</code> correspondant dans la {@link Locale} retournée par la locale
	 * <code>locale</code>, formatée avec les <code>args</code> donnés.
	 *
	 * @param code
	 * @param locale
	 * @param args   Arguments
	 * @return
	 */
	public static String getMessage(String code, Locale locale, Object... args) {
		return INSTANCE.messageSource.getMessage(code, args, locale);
	}

	/**
	 * Retourne le <code>code</code> correspondant dans la {@link Locale} retournée par la locale
	 * <code>locale</code>, formatée avec les <code>args</code> donnés.
	 *
	 * @param code
	 * @param args Arguments
	 * @return
	 */
	public static String getMessage(String code, Object... args) {
		return INSTANCE.messageSource.getMessage(code, args, getLocaleDefinie());
	}

	/**
	 * Retourne le <code>code</code> correspondant dans la {@link Locale} retournée par la locale
	 * <code>locale</code>, formatée avec les <code>args</code> donnés. <br>
	 * Si le message n'existe pas, le retour est <code>defaultMessage</code>.
	 *
	 * @param code
	 * @param defaultMessage
	 * @param locale
	 * @param args
	 * @return
	 */
	public static String getMessageWithDefault(String code, String defaultMessage, Locale locale, Object... args) {
		return INSTANCE.messageSource.getMessage(code, args, defaultMessage, locale);
	}

	/**
	 * Retourne le <code>code</code> correspondant dans la {@link Locale} retournée par la locale
	 * <code>locale</code>, formatée avec les <code>args</code> donnés. <br>
	 * Si le message n'existe pas, le retour est <code>defaultMessage</code>.
	 *
	 * @param code
	 * @param defaultMessage
	 * @param args
	 * @return
	 */
	public static String getMessageWithDefault(String code, String defaultMessage, Object... args) {
		return INSTANCE.messageSource.getMessage(code, args, defaultMessage, getLocaleDefinie());
	}

	public static MessageSource getMessageSource() {
		return INSTANCE.messageSource;
	}


	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public static MessageHelper messageHelper() {
		return INSTANCE;
	}
}
