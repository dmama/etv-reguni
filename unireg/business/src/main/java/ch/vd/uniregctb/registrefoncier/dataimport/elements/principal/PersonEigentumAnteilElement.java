package ch.vd.uniregctb.registrefoncier.dataimport.elements.principal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PersonEigentumAnteil", namespace = "http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck")
public class PersonEigentumAnteilElement extends PersonEigentumAnteil {
}
