package ch.vd.uniregctb.evenement.fiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.declaration.Declaration;

@Entity
@DiscriminatorValue(value = "DECLARATION")
public class EvenementFiscalDeclaration extends EvenementFiscal {

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

	private Declaration declaration;
	private TypeAction typeAction;

	public EvenementFiscalDeclaration() {
	}

	public EvenementFiscalDeclaration(RegDate dateValeur, Declaration declaration, TypeAction typeAction) {
		super(declaration.getTiers(), dateValeur);
		this.declaration = declaration;
		this.typeAction = typeAction;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "DECLARATION_ID")
	@ForeignKey(name = "FK_EVTFISC_DECL_ID")
	public Declaration getDeclaration() {
		return declaration;
	}

	public void setDeclaration(Declaration declaration) {
		this.declaration = declaration;
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
