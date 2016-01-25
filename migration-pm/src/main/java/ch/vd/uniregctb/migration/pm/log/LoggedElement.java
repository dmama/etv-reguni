package ch.vd.uniregctb.migration.pm.log;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Interface implémentée par tout élément qui veut faire partie d'un log structuré ou d'une liste métier
 */
public interface LoggedElement {

	/**
	 * @return noms des concepts à logguer, dans l'ordre dans lequel ils doivent être loggués
	 */
	@NotNull
	List<LoggedElementAttribute> getItems();

	/**
	 * @return les valeurs des concepts à logguer, associées au nom du concept correspondant
	 */
	@NotNull
	Map<LoggedElementAttribute, Object> getItemValues();

	/**
	 * @return la transformation de cet élément en un {@link LoggedMessage}
	 */
	default LoggedMessage resolve() {
		final Map<LoggedElementAttribute, Object> values = getItemValues();
		final LogLevel logLevel = (LogLevel) values.get(LoggedElementAttribute.NIVEAU);
		return new LoggedMessage(logLevel, LoggedElementRenderer.INSTANCE.toString(this));
	}
}
