package ch.vd.unireg.evenement.fiscal;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.Declaration;

@Entity
public abstract class EvenementFiscalDeclaration extends EvenementFiscalTiers {

	private Declaration declaration;

	public EvenementFiscalDeclaration() {
	}

	public EvenementFiscalDeclaration(RegDate dateValeur, Declaration declaration) {
		super(declaration.getTiers(), dateValeur);
		this.declaration = declaration;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "DECLARATION_ID", foreignKey = @ForeignKey(name = "FK_EVTFISC_DECL_ID"))
	public Declaration getDeclaration() {
		return declaration;
	}

	public void setDeclaration(Declaration declaration) {
		this.declaration = declaration;
	}
}
