package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

public abstract class BaseEnumConverter <T extends Enum<T>, R extends Enum<R>> extends BaseConverter<T, R> {

	@NotNull
	protected String genericUnsupportedValueMessage(@NotNull T value) {
		return "La valeur [" + value.name() + "] de l'énumération" +
				" [" + value.getClass().getSimpleName() + "] n'est pas supportée.";
	}
}
