package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;

/**
 * Adresse spécialisée utilisée dans la cas de poursuite pour signifier qu'un autre tiers doit recevoir la réquisition.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>addressOtherPartyType</i> (xml) / <i>AddressOtherParty</i> (client java)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AdresseAutreTiers", propOrder = {
		"type"
})
public class AdresseAutreTiers extends Adresse {

	/**
	 * le type de l'adresse
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>type</i>
	 */
	@XmlElement(required = true)
	public TypeAdresseAutreTiers type;

	public AdresseAutreTiers() {
	}

	public AdresseAutreTiers(AdresseGenerique adresse, ServiceInfrastructureService serviceInfra) throws
			BusinessException {
		super(adresse, serviceInfra);
		this.type = DataHelper.source2type(adresse.getSource().getType());
	}
}
