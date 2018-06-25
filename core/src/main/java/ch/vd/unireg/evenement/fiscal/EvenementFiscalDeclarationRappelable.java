package ch.vd.unireg.evenement.fiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.declaration.QuestionnaireSNC;

@Entity
@DiscriminatorValue("DECLARATION_RAPPELABLE")
public class EvenementFiscalDeclarationRappelable extends EvenementFiscalDeclaration {

	/**
	 * Le type d'action sur la déclaration que décrit cet événement
	 */
	public enum TypeAction {
		EMISSION,
		ANNULATION,
		RAPPEL,
		ECHEANCE,
		QUITTANCEMENT
	}

	private TypeAction typeAction;

	public EvenementFiscalDeclarationRappelable() {
	}

	public EvenementFiscalDeclarationRappelable(RegDate dateValeur, QuestionnaireSNC declaration, TypeAction typeAction) {
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
