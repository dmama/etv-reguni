package ch.vd.unireg.ubr;

import java.util.Collection;

/**
 * Description d'un paramètre de job tel qu'exposé dans le WS batch
 */
public class JobParamDescription {

	private String name;
	private boolean mandatory;
	private String type;
	private boolean multiValues;
	private String[] enumValues;

	public JobParamDescription() {
	}

	public JobParamDescription(String name, boolean mandatory, Class<?> type, boolean multiValues, Collection<String> allowedValues) {
		this.name = name;
		this.mandatory = mandatory;
		this.type = (type.isEnum() ? Enum.class : type).getSimpleName().toLowerCase();
		this.multiValues = multiValues;

		if (allowedValues == null || allowedValues.isEmpty()) {
			this.enumValues = null;
		}
		else {
			this.enumValues = allowedValues.toArray(new String[0]);
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isMultiValues() {
		return multiValues;
	}

	public void setMultiValues(boolean multiValues) {
		this.multiValues = multiValues;
	}

	public String[] getEnumValues() {
		return enumValues;
	}

	public void setEnumValues(String[] enumValues) {
		this.enumValues = enumValues;
	}
}
