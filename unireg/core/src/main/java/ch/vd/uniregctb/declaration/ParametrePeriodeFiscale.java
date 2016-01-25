package ch.vd.uniregctb.declaration;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeContribuable;

@Entity
@Table(name = "PARAMETRE_PERIODE_FISCALE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "PPF_TYPE", length = LengthConstants.PARAMETRE_PF_TYPE, discriminatorType = DiscriminatorType.STRING)
public abstract class ParametrePeriodeFiscale extends HibernateEntity {

	private Long id;
	private PeriodeFiscale periodefiscale;
	private TypeContribuable typeContribuable;

	// nécessaire pour Hibernate
	protected ParametrePeriodeFiscale() {
	}

	public ParametrePeriodeFiscale(ParametrePeriodeFiscale right) {
		this.periodefiscale = right.periodefiscale;
		this.typeContribuable = right.typeContribuable;
	}

	public ParametrePeriodeFiscale(PeriodeFiscale periodefiscale, TypeContribuable typeContribuable) {
		this.periodefiscale = periodefiscale;
		this.typeContribuable = typeContribuable;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "PERIODE_ID", insertable = false, updatable = false)
	public PeriodeFiscale getPeriodefiscale() {
		return periodefiscale;
	}

	public void setPeriodefiscale(PeriodeFiscale thePeriodefiscale) {
		periodefiscale = thePeriodefiscale;
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