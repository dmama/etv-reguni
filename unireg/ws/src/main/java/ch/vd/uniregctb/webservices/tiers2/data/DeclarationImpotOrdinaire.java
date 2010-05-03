package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers2.impl.Context;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

/**
 * Contient les informations relative à une déclaration d'impôt ordinaire (= DI) émise sur un contribuable.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DeclarationImpotOrdinaire", propOrder = {
		"numero", "typeDocument", "numeroOfsForGestion"
})
public class DeclarationImpotOrdinaire extends Declaration {

	/**
	 * Le numéro de séquence de la déclaration pour la période fiscale.
	 */
	@XmlElement(required = true)
	public Long numero;

	@XmlElement(required = true)
	public TypeDocument typeDocument;

	/**
	 * numéro OFS de la commune du for de gestion à la date de fin de la DI
	 */
	@XmlElement(required = true)
	public Long numeroOfsForGestion;

	public DeclarationImpotOrdinaire() {
	}

	public DeclarationImpotOrdinaire(ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire declaration, Context context) {
		super(declaration);
		this.numero = Long.valueOf(declaration.getNumero());
		this.typeDocument = EnumHelper.coreToWeb(declaration.getTypeDeclaration());
		if (declaration.getNumeroOfsForGestion() != null)
			this.numeroOfsForGestion = (long) context.noOfsTranslator.translateCommune(declaration.getNumeroOfsForGestion());
		else
			this.numeroOfsForGestion = 0L;
	}
}
