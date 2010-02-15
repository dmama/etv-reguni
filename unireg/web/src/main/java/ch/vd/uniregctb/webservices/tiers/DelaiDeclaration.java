package ch.vd.uniregctb.webservices.tiers;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers.impl.DataHelper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DelaiDeclaration", propOrder = {
		"dateDemande", "dateTraitement", "dateAnnulation", "delaiAccordeAu", "confirmationEcrite", "annule"
})
public class DelaiDeclaration {

	@XmlElement(required = true)
	public Date dateDemande;

	@XmlElement(required = true)
	public Date dateTraitement;

	@XmlElement(required = true)
	public Date delaiAccordeAu;

	/** Date à laquelle le délai a été annulé, ou <b>null</b> s'il n'est pas annulé. */
	@XmlElement(required = false)
	public Date dateAnnulation;

	@XmlElement(required = true)
	public boolean confirmationEcrite;

	/**
	 * <b>vrai</b> si l'état est annulé, <b>faux</b> autrement.
	 *
	 * @deprecated gardé pour des raisons de compatibilité. Remplacé par {@link #dateAnnulation}.
	 * @see #dateAnnulation
	 */
	@Deprecated
	@XmlElement(required = true)
	public boolean annule;

	public DelaiDeclaration() {
	}

	public DelaiDeclaration(ch.vd.uniregctb.declaration.DelaiDeclaration delai) {
		this.dateDemande = DataHelper.coreToWeb(delai.getDateDemande());
		this.dateTraitement = DataHelper.coreToWeb(delai.getDateTraitement());
		this.dateAnnulation = DataHelper.coreToWeb(delai.getAnnulationDate());
		this.delaiAccordeAu = DataHelper.coreToWeb(delai.getDelaiAccordeAu());
		this.confirmationEcrite = DataHelper.coreToWeb(delai.getConfirmationEcrite());
		this.annule = delai.isAnnule();
	}
}
