package ch.vd.unireg.parametrage;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.declaration.PeriodeFiscale;

@Entity
@Table(name = "PARAMETRE_PERIODE_FISCALE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "PPF_TYPE", length = LengthConstants.PARAMETRE_PF_TYPE, discriminatorType = DiscriminatorType.STRING)
public abstract class ParametrePeriodeFiscale extends HibernateEntity {

	private Long id;
	private PeriodeFiscale periodefiscale;

	// nécessaire pour Hibernate
	protected ParametrePeriodeFiscale() {
	}

	public ParametrePeriodeFiscale(ParametrePeriodeFiscale right) {
		this.periodefiscale = right.periodefiscale;
	}

	public ParametrePeriodeFiscale(PeriodeFiscale periodefiscale) {
		this.periodefiscale = periodefiscale;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	// configuration hibernate : la période fiscale possède les paramètres
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "PERIODE_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_PARAM_PF_PERIODE_ID", columnNames = "PERIODE_ID")
	public PeriodeFiscale getPeriodefiscale() {
		return periodefiscale;
	}

	public void setPeriodefiscale(PeriodeFiscale thePeriodefiscale) {
		periodefiscale = thePeriodefiscale;
	}
}