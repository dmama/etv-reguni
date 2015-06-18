package ch.vd.unireg.interfaces.organisation.rcent.converters;

public abstract class BaseEnumConverter <T extends Enum<T>, R extends Enum<R>> extends BaseConverter<T, R> {

	protected static String genericUnsupportedValueMessage(String name, String className) {
		return "La valeur [" + name + "] de l'énumération" +
				" [" + className + "] n'est pas supportée.";
	}
}
