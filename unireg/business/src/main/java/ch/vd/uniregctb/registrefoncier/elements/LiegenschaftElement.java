package ch.vd.uniregctb.registrefoncier.elements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import ch.vd.capitastra.grundstueck.Liegenschaft;

/**
 * Elément XML local qui permet de unmarshaller qu'une partie de l'export du registre foncier
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Liegenschaft", namespace = "http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck")
public class LiegenschaftElement extends Liegenschaft {
	private static final long serialVersionUID = 1L;
}
