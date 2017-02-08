package ch.vd.uniregctb.registrefoncier.dataimport.elements.servitude;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import ch.vd.capitastra.rechteregister.NatuerlichePersonstamm;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "NatuerlichePersonstamm", namespace = "http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister")
public class NatuerlichePersonstammElement extends NatuerlichePersonstamm {
}
