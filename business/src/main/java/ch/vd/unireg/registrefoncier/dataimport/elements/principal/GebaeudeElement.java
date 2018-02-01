package ch.vd.unireg.registrefoncier.dataimport.elements.principal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import ch.vd.capitastra.grundstueck.Gebaeude;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Gebaeude", namespace = "http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck")
public class GebaeudeElement extends Gebaeude {
}
