package ch.vd.unireg.hibernate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.StringParser;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.etiquette.ActionAutoEtiquette;
import ch.vd.unireg.etiquette.CorrectionSurDate;
import ch.vd.unireg.etiquette.Decalage;
import ch.vd.unireg.etiquette.DecalageAvecCorrection;
import ch.vd.unireg.etiquette.UniteDecalageDate;

public class ActionAutoEtiquetteUserType extends GenericUserType implements UserType {

	private static final Logger LOGGER = LoggerFactory.getLogger(ActionAutoEtiquetteUserType.class);

	private static final int[] SQL_TYPES = {Types.VARCHAR};

	public ActionAutoEtiquetteUserType() {
	}

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class<?> returnedClass() {
		return ActionAutoEtiquette.class;
	}

	/*
	 * Mappings entre nom et classe
	 */
	private static final Map<String, Class<? extends Function<RegDate, RegDate>>> functionsByName;
	private static final Map<Class<? extends Function<RegDate, RegDate>>, String> namesByFunction;

	static {
		final Map<String, Class<? extends Function<RegDate, RegDate>>> byName = new HashMap<>();
		final Map<Class<? extends Function<RegDate, RegDate>>, String> byFunction = new HashMap<>();

		registerDateFunctionNameMapping(byName, byFunction, "Decalage", Decalage.class);
		registerDateFunctionNameMapping(byName, byFunction, "DecalageAvecCorrection", DecalageAvecCorrection.class);

		functionsByName = byName;
		namesByFunction = byFunction;
	}

	private static void registerDateFunctionNameMapping(Map<String, Class<? extends Function<RegDate, RegDate>>> byName,
	                                                    Map<Class<? extends Function<RegDate, RegDate>>, String> byFunction,
	                                                    String name,
	                                                    Class<? extends Function<RegDate, RegDate>> clazz) {
		if (byName.containsKey(name)) {
			throw new IllegalArgumentException("Function name '" + name + "' used more than once!");
		}
		if (byFunction.containsKey(clazz)) {
			throw new IllegalArgumentException("Function class '" + clazz.getName() + "' used more than once!");
		}
		byName.put(name, clazz);
		byFunction.put(clazz, name);
	}

	private static String getNameForFunctionClass(Class<? extends Function<RegDate, RegDate>> clazz) {
		return namesByFunction.get(clazz);
	}

	private static Class<? extends Function<RegDate, RegDate>> getFunctionClassForName(String name) {
		return functionsByName.get(name);
	}

	/*
	 * Quelques renderers (Object -> String)
	 */

	private static final Map<Class<? extends Function<RegDate, RegDate>>, StringRenderer<?>> renderers = buildRenderers();

	private static <T extends Function<RegDate, RegDate>> void registerRenderer(Map<Class<? extends Function<RegDate, RegDate>>, StringRenderer<?>> map,
	                                                                            Class<T> clazz,
	                                                                            StringRenderer<T> renderer) {
		map.put(clazz, renderer);
	}

	private static Map<Class<? extends Function<RegDate, RegDate>>, StringRenderer<?>> buildRenderers() {
		final Map<Class<? extends Function<RegDate, RegDate>>, StringRenderer<?>> map = new HashMap<>();
		registerRenderer(map, Decalage.class, ActionAutoEtiquetteUserType::renderDecalageSimple);
		registerRenderer(map, DecalageAvecCorrection.class, ActionAutoEtiquetteUserType::renderDecalageAvecCorrection);
		return map;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Function<RegDate, RegDate>> StringRenderer<Object> findRenderer(Class<T> clazz) {
		return (StringRenderer<Object>) renderers.get(clazz);
	}

	private static String renderDecalageSimple(Decalage decalage) {
		return String.format("%+d%s",
		                     decalage.getDecalage(),
		                     decalage.getUniteDecalage().getCode());
	}

	private static String renderDecalageAvecCorrection(DecalageAvecCorrection decalage) {
		return String.format("%s/%s",
		                     renderDecalageSimple(decalage),
		                     decalage.getCorrection().getCode());
	}

	/*
	 * Quelques parsers (String -> Object)
	 */

	private static final Map<Class<? extends Function<RegDate, RegDate>>, StringParser<?>> parsers = buildParsers();

	private static <T extends Function<RegDate, RegDate>> void registerParser(Map<Class<? extends Function<RegDate, RegDate>>, StringParser<?>> map,
	                                                                          Class<T> clazz,
	                                                                          StringParser<T> parser) {
		map.put(clazz, parser);
	}

	private static Map<Class<? extends Function<RegDate, RegDate>>, StringParser<?>> buildParsers() {
		final Map<Class<? extends Function<RegDate, RegDate>>, StringParser<?>> map = new HashMap<>();
		registerParser(map, Decalage.class, ActionAutoEtiquetteUserType::parseDecalageSimple);
		registerParser(map, DecalageAvecCorrection.class, ActionAutoEtiquetteUserType::parseDecalageAvecCorrection);
		return map;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Function<RegDate, RegDate>> StringParser<Function<RegDate, RegDate>> findParser(Class<T> clazz) {
		return (StringParser<Function<RegDate, RegDate>>) parsers.get(clazz);
	}

	/**
	 * Un pattern qui matche ce qui est pondu par le renderer de {@link #renderDecalageAvecCorrection(DecalageAvecCorrection)}
	 */
	private static final Pattern DECALAGE_AVEC_CORRECTION_PATTERN = buildDecalageAvecCorrectionPattern();
	private static final Pattern DECALAGE_SIMPLE_PATTERN = buildDecalageSimplePattern();

	private static String buildDecalageAvecCorrectionRegexp() {
		final String correctionPattern = Stream.of(CorrectionSurDate.values())
				.map(CorrectionSurDate::getCode)
				.map(Pattern::quote)
				.collect(Collectors.joining("|"));

		return String.format("%s/(%s)", buildDecalageSimpleRegexp(), correctionPattern);
	}

	private static Pattern buildDecalageAvecCorrectionPattern() {
		return Pattern.compile(buildDecalageAvecCorrectionRegexp());
	}

	private static String buildDecalageSimpleRegexp() {
		final String unitPattern = Stream.of(UniteDecalageDate.values())
				.map(UniteDecalageDate::getCode)
				.map(Pattern::quote)
				.collect(Collectors.joining("|"));

		return String.format("([+-]?[0-9]{1,9})(%s)", unitPattern);
	}

	private static Pattern buildDecalageSimplePattern() {
		return Pattern.compile(buildDecalageSimpleRegexp());
	}

	private static DecalageAvecCorrection parseDecalageAvecCorrection(String string) {
		final Matcher matcher = DECALAGE_AVEC_CORRECTION_PATTERN.matcher(string);
		if (matcher.matches()) {
			final int decalage = Integer.parseInt(matcher.group(1));
			final UniteDecalageDate unite = UniteDecalageDate.valueOfCode(matcher.group(2));
			final CorrectionSurDate correction = CorrectionSurDate.valueOfCode(matcher.group(3));
			return new DecalageAvecCorrection(decalage, unite, correction);
		}
		else {
			LOGGER.warn("Valeur ignorée : '" + string + "'");
			return null;
		}
	}

	private static Decalage parseDecalageSimple(String string) {
		final Matcher matcher = DECALAGE_SIMPLE_PATTERN.matcher(string);
		if (matcher.matches()) {
			final int decalage = Integer.parseInt(matcher.group(1));
			final UniteDecalageDate unite = UniteDecalageDate.valueOfCode(matcher.group(2));
			return new Decalage(decalage, unite);
		}
		else {
			LOGGER.warn("Valeur ignorée : '" + string + "'");
			return null;
		}
	}

	public static final StringRenderer<ActionAutoEtiquette> ACTION_RENDERER = new StringRenderer<ActionAutoEtiquette>() {
		@Override
		public String toString(ActionAutoEtiquette object) {
			final StringBuilder b = new StringBuilder();

			// d'abord la date de début, puis la date de fin
			b.append("BD:");
			renderDateFunction(b, object.getDateDebut());
			b.append(";ED:");
			renderDateFunction(b, object.getDateFin());

			return b.toString();
		}

		private StringBuilder renderDateFunction(StringBuilder builder, Function<RegDate, RegDate> function) {
			if (function != null) {
				//noinspection unchecked
				final String name = getNameForFunctionClass((Class<? extends Function<RegDate, RegDate>>) function.getClass());
				if (name == null) {
					throw new IllegalArgumentException("Aucune dénomination pour la fonction de classe " + function.getClass());
				}

				builder.append(name);
				builder.append("{");
				final StringRenderer<Object> renderer = findRenderer(function.getClass());
				builder.append(renderer.toString(function));
				builder.append("}");
			}
			return builder;
		}
	};

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		if (value == null) {
			st.setNull(index, Types.VARCHAR);
		}
		else {
			final ActionAutoEtiquette aae = (ActionAutoEtiquette) value;
			final String renderedValue = ACTION_RENDERER.toString(aae);
			st.setObject(index, renderedValue, Types.VARCHAR);
		}
	}

	private static final Pattern GLOBAL_PATTERN = Pattern.compile("BD:(?:([a-zA-Z0-9.]+)\\{(.*)\\})?;ED:(?:([a-zA-Z0-9.]+)\\{(.*)\\})?");

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
		final String value = rs.getString(names[0]);
		if (StringUtils.isBlank(value)) {
			return null;
		}

		final Matcher matcher = GLOBAL_PATTERN.matcher(value);
		if (!matcher.matches()) {
			LOGGER.warn("Valeur ignorée : '" + value + "'");
			return null;
		}

		try {
			final String nomClasseDateDebut = matcher.group(1);
			final Function<RegDate, RegDate> dateDebut;
			if (StringUtils.isNotBlank(nomClasseDateDebut)) {
				final Class<? extends Function<RegDate, RegDate>> clazz = getFunctionClassForName(nomClasseDateDebut);
				if (clazz == null) {
					throw new IllegalArgumentException("Pas de classe liée au nom '" + nomClasseDateDebut + "'");
				}
				final StringParser<Function<RegDate, RegDate>> parser = findParser(clazz);
				dateDebut = parser.parse(matcher.group(2));
			}
			else {
				dateDebut = null;
			}

			final String nomClasseDateFin = matcher.group(3);
			final Function<RegDate, RegDate> dateFin;
			if (StringUtils.isNotBlank(nomClasseDateFin)) {
				final Class<? extends Function<RegDate, RegDate>> clazz = getFunctionClassForName(nomClasseDateFin);
				if (clazz == null) {
					throw new IllegalArgumentException("Pas de classe liée au nom '" + nomClasseDateFin + "'");
				}
				final StringParser<Function<RegDate, RegDate>> parser = findParser(clazz);
				dateFin = parser.parse(matcher.group(4));
			}
			else {
				dateFin = null;
			}

			if (dateDebut != null || dateFin != null) {
				return new ActionAutoEtiquette(dateDebut, dateFin);
			}
		}
		catch (ClassCastException | IllegalArgumentException e) {
			LOGGER.warn("Valeur ignorée : '" + value + "' (" + e.getMessage() + ")");
		}

		return null;
	}
}
