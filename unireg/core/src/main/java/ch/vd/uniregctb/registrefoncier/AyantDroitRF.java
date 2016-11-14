package ch.vd.uniregctb.registrefoncier;

import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import ch.vd.uniregctb.common.HibernateEntity;

/**
 * Ayant-droit sur un ou plusieurs immeubles. Un ayant-droit est soit un tiers (personne physique ou morale), soit une communauté (regroupement de personnes physiques ou morales).
 */
@Entity
@Table(name = "RF_AYANT_DROIT")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class AyantDroitRF extends HibernateEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Identifiant technique de l'ayant-droit au registre foncier.
	 */
	private String idRF;

	/**
	 * Les droits sur des immeubles dont jouit l'ayant-droit.
	 */
	private Set<DroitRF> droits;

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

	@Index(name = "IDX_AYANTDROIT_ID_RF")
	@Column(name = "ID_RF", nullable = false, length = 33)
	public String getIdRF() {
		return idRF;
	}

	public void setIdRF(String idRF) {
		this.idRF = idRF;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "AYANT_DROIT_ID", nullable = false)
	@ForeignKey(name = "FK_DROIT_RF_AYANT_DROIT_ID")
	public Set<DroitRF> getDroits() {
		return droits;
	}

	public void setDroits(Set<DroitRF> droits) {
		this.droits = droits;
	}

	public void copyDataTo(AyantDroitRF right) {
		right.idRF = this.idRF;
	}
}
