package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

/**
 * Contient les informations relative à une liste récapitulative (= LR) émise sur un débiteur.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>withholdingTaxDeclarationType</i> (xml) / <i>WithholdingTaxDeclaration</i> (client java)
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DeclarationImpotSource", propOrder = {
		"periodicite", "modeCommunication"
})
public class DeclarationImpotSource extends Declaration {

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>periodicity</i>.
	 */
	@XmlElement(required = true)
	public PeriodiciteDecompte periodicite;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>communicationMode</i>.
	 */
	@XmlElement(required = true)
	public ModeCommunication modeCommunication;

	public DeclarationImpotSource() {
	}

	public DeclarationImpotSource(ch.vd.uniregctb.declaration.DeclarationImpotSource declaration) {
		super(declaration);
		this.periodicite = EnumHelper.coreToWeb(declaration.getPeriodicite());
		this.modeCommunication = EnumHelper.coreToWeb(declaration.getModeCommunication());
	}
}
