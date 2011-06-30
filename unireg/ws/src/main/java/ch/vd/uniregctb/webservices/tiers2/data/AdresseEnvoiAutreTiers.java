package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;

/**
 * Adresse formattée spécialisée utilisée dans la cas de poursuite pour signifier qu'un autre tiers doit recevoir la réquisition.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>addressOtherPartyType</i> (xml) / <i>AddressOtherParty</i> (client java)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AdresseEnvoiAutreTiers", propOrder = {
		"type"
})
public class AdresseEnvoiAutreTiers extends AdresseEnvoi {

	/**
	 * le type de l'adresse
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>type</i>
	 */
	@XmlElement(required = true)
	public TypeAdresseAutreTiers type;

	public AdresseEnvoiAutreTiers() {
	}

	public AdresseEnvoiAutreTiers(AdresseEnvoiDetaillee adresse) {
		super(adresse);
		this.type = DataHelper.source2type(adresse.getSource());
	}
}
