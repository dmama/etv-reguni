package ch.vd.uniregctb.registrefoncier.elements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.EigentumAnteil;
import ch.vd.capitastra.grundstueck.GrundstueckExport;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PersonEigentumAnteilList", namespace = "http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck")
public class PersonEigentumAnteilListElement extends GrundstueckExport.EigentumList {

	public PersonEigentumAnteilListElement() {
	}

	public PersonEigentumAnteilListElement(@NotNull List<PersonEigentumAnteil> eigentumAnteils) {
		this.personEigentumAnteilOrGrundstueckEigentumAnteilOrHerrenlosEigentum = eigentumAnteils.stream()
				.map(e -> (EigentumAnteil) e)
				.collect(Collectors.toList());
	}
}
