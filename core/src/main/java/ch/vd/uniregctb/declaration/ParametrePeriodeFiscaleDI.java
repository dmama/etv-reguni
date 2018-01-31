package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeContribuable;

@Entity
public abstract class ParametrePeriodeFiscaleDI extends ParametrePeriodeFiscale {

	private TypeContribuable typeContribuable;

	// nécessaire pour Hibernate
	protected ParametrePeriodeFiscaleDI() {
	}

	public ParametrePeriodeFiscaleDI(ParametrePeriodeFiscaleDI right) {
		super(right);
		this.typeContribuable = right.typeContribuable;
	}

	public ParametrePeriodeFiscaleDI(PeriodeFiscale periodefiscale, TypeContribuable typeContribuable) {
		super(periodefiscale);
		this.typeContribuable = typeContribuable;
	}

	@Column(name = "TYPE_CTB", length = LengthConstants.DI_TYPE_CTB)
	@Enumerated(EnumType.STRING)
	public TypeContribuable getTypeContribuable() {
		return typeContribuable;
	}

	public void setTypeContribuable(TypeContribuable theTypeContribuable) {
		typeContribuable = theTypeContribuable;
	}

}