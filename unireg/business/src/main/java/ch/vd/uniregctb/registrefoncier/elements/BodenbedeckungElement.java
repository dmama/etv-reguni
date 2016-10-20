package ch.vd.uniregctb.registrefoncier.elements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Bodenbedeckung", namespace = "http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck")
public class BodenbedeckungElement extends Bodenbedeckung {
}
