package ch.vd.uniregctb.registrefoncier.dataimport.elements.servitude;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import ch.vd.capitastra.rechteregister.Dienstbarkeit;

/**
 * Elément XML local qui permet de unmarshaller qu'une partie de l'export du registre foncier
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Dienstbarkeit", namespace = "http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister")
public class DienstbarkeitElement extends Dienstbarkeit {
}
