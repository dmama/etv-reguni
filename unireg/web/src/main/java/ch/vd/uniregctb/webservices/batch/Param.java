package ch.vd.uniregctb.webservices.batch;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "param", propOrder = {
		"name", "type", "enumValues", "isMandatory"
})
public class Param {
	@XmlElement(required = false)
	public String name;

	@XmlElement(required = false)
	public String type;

	/**
	 * Liste des valeurs admises <b>si</b> le {@link #type} est un enum.
	 */
	@XmlElement(required = false)
	public List<String> enumValues;

	@XmlElement(required = false)
	boolean isMandatory;

	public Param(ch.vd.uniregctb.scheduler.JobDefinition job, ch.vd.uniregctb.scheduler.JobParam param) {
		name = param.getName();
		isMandatory = param.isMandatory();

		final Class<?> clazz = param.getType().getConcreteClass();
		if (clazz.isEnum()) {
			// On expose pas les enum tels quels parce que le client ne les connaît pas !
			// On expose plutôt les enums sous forme de string.
			type = "enum";
			enumValues = new ArrayList<>();
			final Enum<?>[] enums = (Enum<?>[]) clazz.getEnumConstants();
			for (Enum<?> e : enums) {
				enumValues.add(e.name());
			}
		}
		else {
			type = clazz.getSimpleName().toLowerCase();
		}
	}

	public Param() {
	}
}
