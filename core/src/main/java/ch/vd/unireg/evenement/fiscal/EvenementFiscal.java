package ch.vd.unireg.evenement.fiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;

@Entity
@Table(name = "EVENEMENT_FISCAL")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "EVT_TYPE", discriminatorType = DiscriminatorType.STRING, length = LengthConstants.EVTFISCAL_TYPE)
public abstract class EvenementFiscal extends HibernateEntity {

	private Long id;
	@Nullable
	private RegDate dateValeur;

	public EvenementFiscal() {
	}

	public EvenementFiscal(@Nullable RegDate dateValeur) {
		this.dateValeur = dateValeur;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	@Column(name = "ID")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Nullable
	@Column(name = "DATE_VALEUR")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateValeur() {
		return dateValeur;
	}

	public void setDateValeur(@Nullable RegDate dateValeur) {
		this.dateValeur = dateValeur;
	}
}
