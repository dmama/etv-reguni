package ch.vd.uniregctb.declaration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

@Entity
@DiscriminatorValue("RETOURNEE")
public class EtatDeclarationRetournee extends EtatDeclaration {
	public EtatDeclarationRetournee() {
		super();
	}

	public EtatDeclarationRetournee(RegDate dateObtention) {
		super(dateObtention);
	}

	@Override
	@Transient
	public TypeEtatDeclaration getEtat() {
		return TypeEtatDeclaration.RETOURNEE;
	}
}
