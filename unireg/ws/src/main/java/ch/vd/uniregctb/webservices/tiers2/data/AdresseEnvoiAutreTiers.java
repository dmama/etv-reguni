package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AdresseEnvoiAutreTiers", propOrder = {
		"type"
})
public class AdresseEnvoiAutreTiers extends AdresseEnvoi {

	/** le type de l'adresse */
	@XmlElement(required = true)
	public TypeAdresseAutreTiers type;

	public AdresseEnvoiAutreTiers() {
	}

	public AdresseEnvoiAutreTiers(AdresseEnvoiDetaillee adresse) {
		super(adresse);
		this.type = DataHelper.source2type(adresse.getSource());
	}
}
