package ch.vd.uniregctb.webservices.tiers;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Une période fiscale couvre la période du 1er janvier au 31 décembre (compris) de l'année considérée.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PeriodeFiscale", propOrder = {
		"annee"
})
public class PeriodeFiscale {

	@XmlElement(required = true)
	public int annee;

	public PeriodeFiscale() {
	}

	public PeriodeFiscale(ch.vd.uniregctb.declaration.PeriodeFiscale periode) {
		this.annee = periode.getAnnee();
	}
}
