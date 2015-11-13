package ch.vd.uniregctb.declaration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

@Entity
@DiscriminatorValue("SUSPENDUE")
public class EtatDeclarationSuspendue extends EtatDeclaration {

	public EtatDeclarationSuspendue() {
		super();
	}

	@Override
	@Transient
	public TypeEtatDeclaration getEtat() {
		return TypeEtatDeclaration.SUSPENDUE;
	}

	public EtatDeclarationSuspendue(RegDate dateObtention) {
		super(dateObtention);
	}
}
