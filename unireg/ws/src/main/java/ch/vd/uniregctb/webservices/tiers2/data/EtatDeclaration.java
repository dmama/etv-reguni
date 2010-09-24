package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EtatDeclaration", propOrder = {
		"etat", "dateObtention", "dateAnnulation"
})
public class EtatDeclaration {

	@XmlType(name = "TypeEtatDeclaration")
	@XmlEnum(String.class)
	public static enum Type {
		EMISE,
		SOMMEE,
		ECHUE,
		RETOURNEE;

		public static Type fromValue(String v) {
			return valueOf(v);
		}
	}

	@XmlElement(required = true)
	public Type etat;

	@XmlElement(required = true)
	public Date dateObtention;

	/** Date à laquelle le délai a été annulé, ou <b>null</b> s'il n'est pas annulé. */
	@XmlElement(required = false)
	public Date dateAnnulation;

	public EtatDeclaration() {
	}

	public EtatDeclaration(ch.vd.uniregctb.declaration.EtatDeclaration etatDeclaration) {
		this.etat = EnumHelper.coreToWeb(etatDeclaration.getEtat());
		this.dateObtention = DataHelper.coreToWeb(etatDeclaration.getDateObtention());
		this.dateAnnulation = DataHelper.coreToWeb(etatDeclaration.getAnnulationDate());
	}
}
