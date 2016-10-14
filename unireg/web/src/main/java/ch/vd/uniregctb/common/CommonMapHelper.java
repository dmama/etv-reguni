package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;

import ch.vd.uniregctb.type.RestrictedAccess;

public class CommonMapHelper {

	private MessageSourceAccessor messageSourceAccessor;

	public void setMessageSourceAccessor(MessageSource messageSource) {
		this.messageSourceAccessor = new MessageSourceAccessor(messageSource);
	}

	public void setMessageSourceAccessor(MessageSourceAccessor messageSource) {
		this.messageSourceAccessor = messageSource;
	}

	protected MessageSourceAccessor getMessageSourceAccessor() {
		return messageSourceAccessor;
	}

	/**
	 * Transforme une énumération en map indexée par enum et dont les valeurs sont les descriptions pour l'utilisateur
	 *
	 * @param keyPrefix le préfixe de la ressource utilisée pour récupérer les descriptions des enums
	 * @param clazz la classe de l'énumération
	 * @param ignored une liste optionnelle des enums à ignorer (si l'enum implémente {@link RestrictedAccess}, les éléments qui répondent <code>false</code> à {@link ch.vd.uniregctb.type.RestrictedAccess#isAllowed()} sont de toute façon ignorés)
	 * @return une map non-modifiable
	 */
	@SafeVarargs
	protected final <T extends Enum<T>> Map<T, String> initMapEnum(String keyPrefix, Class<T> clazz, T... ignored) {
		final EnumSet<T> ignoredSet = EnumSet.noneOf(clazz);
		if (ignored != null && ignored.length > 0) {
			ignoredSet.addAll(Arrays.asList(ignored));
		}
		final Set<T> values = EnumSet.complementOf(ignoredSet);
		return getSpecificMapEnum(keyPrefix, values);
	}

	/**
	 * Transforme une énumération en map indexée par enum et dont les valeurs sont les descriptions pour l'utilisateur
	 *
	 * @param keyPrefix le préfixe de la ressource utilisée pour récupérer les descriptions des enums
	 * @param values modalités de l'enum à utiliser
	 * @return une map non-modifiable
	 */
	protected final <T extends Enum<T>> Map<T, String> getSpecificMapEnum(String keyPrefix, Set<T> values) {
		final List<Pair<T, String>> tmp = new ArrayList<>(values.size());
		for (T c : values) {
			if (!(c instanceof RestrictedAccess) || ((RestrictedAccess) c).isAllowed()) {
				final String nom = this.getMessageSourceAccessor().getMessage(keyPrefix + c);
				tmp.add(Pair.of(c, nom));
			}
		}
    	Collections.sort(tmp, Comparator.comparing(Pair::getRight));

		final Map<T, String> map = new LinkedHashMap<>();
		for (Pair<T, String> pair : tmp) {
			map.put(pair.getLeft(), pair.getRight());
		}
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Transforme une énumération en map indexée par enum et dont les valeurs sont les descriptions pour l'utilisateur
	 *
	 * @param keyPrefix le préfixe de la ressource utilisée pour récupérer les descriptions des enums
	 * @param constants la liste des enums à inclure
	 * @return une map
	 */
	@SafeVarargs
	protected final <T extends Enum<T>> Map<T, String> initMapEnum(String keyPrefix, T... constants) {
		final List<Pair<T, String>> tmp = new ArrayList<>(constants.length);
		for (T c : constants) {
			final String nom = this.getMessageSourceAccessor().getMessage(keyPrefix + c);
			tmp.add(Pair.of(c, nom));
		}
		Collections.sort(tmp, Comparator.comparing(Pair::getRight));

		final Map<T, String> map = new LinkedHashMap<>();
		for (Pair<T, String> pair : tmp) {
			map.put(pair.getLeft(), pair.getRight());
		}
		return Collections.unmodifiableMap(map);
	}

}
