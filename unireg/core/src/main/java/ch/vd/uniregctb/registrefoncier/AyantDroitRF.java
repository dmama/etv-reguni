package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;

/**
 * Ayant-droit sur un ou plusieurs immeubles. Un ayant-droit est soit un tiers (personne physique ou morale), soit une communauté (regroupement de personnes physiques ou morales).
 */
@Entity
@Table(name = "RF_AYANT_DROIT", uniqueConstraints = @UniqueConstraint(name = "IDX_AYANTDROIT_ID_RF", columnNames = "ID_RF"))
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

	@Column(name = "ID_RF", nullable = false, length = LengthConstants.RF_ID_RF)
	public String getIdRF() {
		return idRF;
	}

	public void setIdRF(String idRF) {
		this.idRF = idRF;
	}

	// configuration hibernate : l'ayant-droit ne possède pas les droits
	@OneToMany(mappedBy = "ayantDroit")
	public Set<DroitRF> getDroits() {
		return droits;
	}

	public void setDroits(Set<DroitRF> droits) {
		this.droits = droits;
	}

	public void addDroit(@NotNull DroitRF droit) {
		if (this.droits == null) {
			this.droits = new HashSet<>();
		}
		this.droits.add(droit);
	}

	public void copyDataTo(AyantDroitRF right) {
		right.idRF = this.idRF;
	}
}
