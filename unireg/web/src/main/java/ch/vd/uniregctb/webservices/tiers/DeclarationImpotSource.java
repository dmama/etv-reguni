package ch.vd.uniregctb.webservices.tiers;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers.impl.EnumHelper;

/**
 * Contient les informations relative à une liste récapitulative (= LR) émise sur un débiteur.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DeclarationImpotSource", propOrder = {
		"periodicite", "modeCommunication"
})
public class DeclarationImpotSource extends Declaration {

	@XmlElement(required = true)
	public PeriodiciteDecompte periodicite;

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
