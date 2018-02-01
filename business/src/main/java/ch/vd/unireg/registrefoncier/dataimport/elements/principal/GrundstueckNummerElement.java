package ch.vd.unireg.registrefoncier.dataimport.elements.principal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import ch.vd.capitastra.grundstueck.GrundstueckNummer;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "GrundstueckNummer", namespace = "http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck")
public class GrundstueckNummerElement extends GrundstueckNummer {

	public GrundstueckNummerElement() {
	}

	public GrundstueckNummerElement(Integer noRf, String nom) {
		super(noRf, nom, null, 0, null, null, null, null);
	}
}
