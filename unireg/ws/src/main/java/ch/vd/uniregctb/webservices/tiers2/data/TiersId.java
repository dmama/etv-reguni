package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Contient l'identifiant d'un tiers
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TiersId")
public class TiersId {
	@XmlElement(required = true)
	public long id;

	public TiersId(){

	}

	public TiersId(long id) {
		this.id = id;
	}
}
