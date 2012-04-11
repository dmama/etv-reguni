package ch.vd.uniregctb.declaration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

@Entity
@DiscriminatorValue("ECHUE")
public class EtatDeclarationEchue extends EtatDeclaration {
	public EtatDeclarationEchue() {
		super();
	}

	public EtatDeclarationEchue(RegDate dateObtention) {
		super(dateObtention);
	}

	@Override
	@Transient
	public TypeEtatDeclaration getEtat() {
		return TypeEtatDeclaration.ECHUE;
	}
}
