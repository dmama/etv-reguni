package ch.vd.unireg.registrefoncier.dataimport.elements.principal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import ch.vd.capitastra.grundstueck.Gemeinschaft;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Gemeinschaft", namespace = "http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck")
public class GemeinschaftElement extends Gemeinschaft {
}
