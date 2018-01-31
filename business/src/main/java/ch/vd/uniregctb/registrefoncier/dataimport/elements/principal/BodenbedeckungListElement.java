package ch.vd.uniregctb.registrefoncier.dataimport.elements.principal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import ch.vd.capitastra.grundstueck.GrundstueckExport;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BodenbedeckungList", namespace = "http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck")
public class BodenbedeckungListElement extends GrundstueckExport.BodenbedeckungList {
}
