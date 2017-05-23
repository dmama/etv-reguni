package ch.vd.uniregctb.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JobParamDynamicEnum<T> extends JobParamType {

	private final Supplier<Collection<T>> allValuesSupplier;
	private final Function<? super T, String> codeProvider;

	public JobParamDynamicEnum(Class<T> clazz,
	                           Supplier<Collection<T>> allValuesSupplier,
	                           Function<? super T, String> codeProvider) {
		super(clazz);
		this.allValuesSupplier = allValuesSupplier;
		this.codeProvider = codeProvider;
	}

	private Map<String, T> buildValueMap() {
		return allValuesSupplier.get().stream()
				.collect(Collectors.toMap(codeProvider,
				                          Function.identity(),
				                          (t1, t2) -> { throw new IllegalArgumentException("Le code '" + codeProvider.apply(t1) + "' est utilisé plus d'une fois..."); }));
	}

	@Override
	public T stringToValue(String s) throws IllegalArgumentException {
		if (s == null) {
			return null;
		}
		final Map<String, T> map = buildValueMap();
		final T value = map.get(s);
		if (value == null) {
			throw new IllegalArgumentException("Valeur non-supportée : '" + s + "'");
		}
		return value;
	}

	@Override
	public String valueToString(Object o) throws IllegalArgumentException {
		if (o == null) {
			return null;
		}
		if (!getConcreteClass().isInstance(o)) {
			throw new ClassCastException("Valeur non-supportée : " + o);
		}
		//noinspection unchecked
		return codeProvider.apply((T) o);
	}

	/**
	 * @return Liste des valeurs autorisées, triées par ordre alphabétique
	 */
	public List<T> getAllowedValues() {
		final Map<String, T> map = buildValueMap();
		final List<T> liste = new ArrayList<>(map.values());
		liste.sort(Comparator.comparing(codeProvider));
		return liste;
	}
}
