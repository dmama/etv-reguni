package ch.vd.uniregctb.evenement.fiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;

@Entity
@DiscriminatorValue("DECLARATION_SOMMABLE")
public class EvenementFiscalDeclarationSommable extends EvenementFiscalDeclaration {

	/**
	 * Le type d'action sur la déclaration que décrit cet événement
	 */
	public enum TypeAction {
		EMISSION,
		ANNULATION,
		SOMMATION,
		ECHEANCE,
		QUITTANCEMENT
	}

	private TypeAction typeAction;

	public EvenementFiscalDeclarationSommable() {
	}

	public EvenementFiscalDeclarationSommable(RegDate dateValeur, DeclarationImpotSource declaration, TypeAction typeAction) {
		super(dateValeur, declaration);
		this.typeAction = typeAction;
	}

	public EvenementFiscalDeclarationSommable(RegDate dateValeur, DeclarationImpotOrdinaire declaration, TypeAction typeAction) {
		super(dateValeur, declaration);
		this.typeAction = typeAction;
	}

	@Column(name = "TYPE_EVT_DECLARATION", length = LengthConstants.EVTFISCAL_TYPE_EVT_DECLARATION)
	@Enumerated(EnumType.STRING)
	public TypeAction getTypeAction() {
		return typeAction;
	}

	public void setTypeAction(TypeAction typeAction) {
		this.typeAction = typeAction;
	}
}
