package ch.vd.uniregctb.registrefoncier.dataimport.elements.principal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.EigentumAnteil;
import ch.vd.capitastra.grundstueck.GrundstueckExport;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PersonEigentumAnteilList", namespace = "http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck")
public class PersonEigentumAnteilListElement extends GrundstueckExport.EigentumList {

	public PersonEigentumAnteilListElement() {
	}

	public PersonEigentumAnteilListElement(@NotNull List<EigentumAnteil> eigentumAnteils) {
		this.personEigentumAnteilOrGrundstueckEigentumAnteilOrHerrenlosEigentum = eigentumAnteils;
	}
}
