package ch.vd.uniregctb.evenement.fiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.Tiers;

@Entity
@Table(name = "EVENEMENT_FISCAL")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "EVT_TYPE", discriminatorType = DiscriminatorType.STRING, length = LengthConstants.EVTFISCAL_TYPE)
public abstract class EvenementFiscal extends HibernateEntity {

	private Long id;
	private Tiers tiers;
	private RegDate dateValeur;

	public EvenementFiscal() {
	}

	public EvenementFiscal(Tiers tiers, RegDate dateValeur) {
		this.tiers = tiers;
		this.dateValeur = dateValeur;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@JoinColumn(name = "TIERS_ID")
	@ManyToOne(fetch = FetchType.EAGER)
	@ForeignKey(name = "FK_EVTFISC_TIERS_ID")
	public Tiers getTiers() {
		return tiers;
	}

	public void setTiers(Tiers tiers) {
		this.tiers = tiers;
	}

	@Column(name = "DATE_VALEUR")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateValeur() {
		return dateValeur;
	}

	public void setDateValeur(RegDate dateValeur) {
		this.dateValeur = dateValeur;
	}
}
