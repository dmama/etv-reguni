package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

public abstract class BaseEnumConverter<T extends Enum<T>, R extends Enum<R>> extends BaseConverter<T, R> {

	@NotNull
	public static <E extends Enum<E>> String genericUnsupportedValueMessage(@NotNull E value) {
		return String.format("La valeur [%s] de l'énumération [%s] n'est pas supportée.", value.name(), value.getClass().getSimpleName());
	}
}
