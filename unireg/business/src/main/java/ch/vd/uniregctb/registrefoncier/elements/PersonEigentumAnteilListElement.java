package ch.vd.uniregctb.registrefoncier.elements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.unireg.UniregToStringStyle;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
		name = "",
		propOrder = {"personEigentumAnteil"}
)
public class PersonEigentumAnteilListElement implements Serializable {
	private static final long serialVersionUID = 1L;
	@XmlElement(
			name = "PersonEigentumAnteil",
			required = true
	)
	protected List<PersonEigentumAnteil> personEigentumAnteil;

	public PersonEigentumAnteilListElement() {
	}

	public PersonEigentumAnteilListElement(List<PersonEigentumAnteil> PersonEigentumAnteil) {
		this.personEigentumAnteil = PersonEigentumAnteil;
	}

	public List<PersonEigentumAnteil> getPersonEigentumAnteil() {
		if (this.personEigentumAnteil == null) {
			this.personEigentumAnteil = new ArrayList<>();
		}

		return this.personEigentumAnteil;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this, UniregToStringStyle.STYLE);
	}
}
