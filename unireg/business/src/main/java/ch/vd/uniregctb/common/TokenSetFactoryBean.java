package ch.vd.uniregctb.common;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Bean qui permet d'exporter un ensemble de valeurs lues depuis une chaîne de caractères contenant des éléments séparés par des séparateurs
 * @param <T> le type des éléments dans l'ensemble fourni
 */
public abstract class TokenSetFactoryBean<T> implements FactoryBean<Set<T>>, InitializingBean {

	private final Class<T> elementClass;

	private String separatorRegExp = "[,;]+";
	private String elementString;
	private Set<T> elements;

	public TokenSetFactoryBean(Class<T> elementClass) {
		this.elementClass = elementClass;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setElements(String stringToParse) {
		this.elementString = stringToParse;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setSeparatorRegExp(String separatorRegExp) {
		this.separatorRegExp = separatorRegExp;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (elementClass == null) {
			throw new IllegalArgumentException("elementClass should be given");
		}
		if (separatorRegExp == null) {
			throw new IllegalArgumentException("separatorRegExp should not be null");
		}

		elements = parseString(elementClass, elementString, separatorRegExp);
	}

	protected Set<T> parseString(Class<T> clazz, String toParse, String separatorRegExp) {
		if (StringUtils.isBlank(toParse)) {
			return Collections.emptySet();
		}

		final String[] tokens = toParse.split(separatorRegExp);
		final Set<T> set = new LinkedHashSet<>(tokens.length);      // on conserve l'ordre au cas où il est important (au pire, il ne l'est pas et cela ne fait pas de différence)
		for (String token : tokens) {
			if (StringUtils.isNotBlank(token)) {
				set.add(buildToken(clazz, token));
			}
		}
		return Collections.unmodifiableSet(set);
	}

	protected abstract T buildToken(Class<T> clazz, String token);

	@Override
	public Set<T> getObject() throws Exception {
		return elements;
	}

	@Override
	public Class<?> getObjectType() {
		return Set.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	/**
	 * Classe qui fournit un ensemble de Strings
	 */
	public static final class StringSet extends TokenSetFactoryBean<String> {
		public StringSet() {
			super(String.class);
		}

		@Override
		protected String buildToken(Class<String> clazz, String token) {
			return StringUtils.trimToEmpty(token);
		}
	}

	/**
	 * Classe qui fournit un ensemble de modalités d'un type énuméré
	 * @param <T> le type énuméré en question
	 */
	public static final class EnumSet<T extends Enum<T>> extends TokenSetFactoryBean<T> {
		public EnumSet(Class<T> elementClass) {
			super(elementClass);
		}

		@Override
		protected T buildToken(Class<T> clazz, String token) {
			return Enum.valueOf(clazz, StringUtils.trimToEmpty(token));
		}
	}
}
