package ch.vd.uniregctb.declaration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

@Entity
@DiscriminatorValue("EMISE")
public class EtatDeclarationEmise extends EtatDeclaration {
	public EtatDeclarationEmise() {
		super();
	}

	public EtatDeclarationEmise(RegDate dateObtention) {
		super(dateObtention);
	}

	@Override
	@Transient
	public TypeEtatDeclaration getEtat() {
		return TypeEtatDeclaration.EMISE;
	}
}
