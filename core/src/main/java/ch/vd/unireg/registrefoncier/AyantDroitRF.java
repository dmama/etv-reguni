package ch.vd.unireg.registrefoncier;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;

/**
 * Ayant-droit sur un ou plusieurs immeubles. Un ayant-droit est soit un tiers (personne physique ou morale), soit une communauté (regroupement de personnes physiques ou morales).
 */
@Entity
@Table(name = "RF_AYANT_DROIT", uniqueConstraints = {
		@UniqueConstraint(name = "IDX_AYANTDROIT_ID_RF", columnNames = "ID_RF"),
		@UniqueConstraint(name = "IDX_AYANTDROIT_IMMEUBLE_ID", columnNames = "IMMEUBLE_ID")
})
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
	 * Les droits de propriété sur des immeubles dont jouit l'ayant-droit.
	 */
	private Set<DroitProprieteRF> droitsPropriete;

	/**
	 * Les servitudes sur des immeubles dont jouit l'ayant-droit.
	 */
	private Set<ServitudeRF> servitudes;

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

	// configuration hibernate : l'ayant-droit ne possède pas les droits de propriété
	@OneToMany(mappedBy = "ayantDroit")
	public Set<DroitProprieteRF> getDroitsPropriete() {
		return droitsPropriete;
	}

	public void setDroitsPropriete(Set<DroitProprieteRF> droitsPropriete) {
		this.droitsPropriete = droitsPropriete;
	}

	public void addDroitPropriete(@NotNull DroitProprieteRF droit) {
		if (this.droitsPropriete == null) {
			this.droitsPropriete = new HashSet<>();
		}
		droit.setAyantDroit(this);
		this.droitsPropriete.add(droit);
	}

	// configuration hibernate : l'ayant-droit ne possède pas les servitudes
	@ManyToMany(cascade = CascadeType.ALL, mappedBy = "ayantDroits")
	public Set<ServitudeRF> getServitudes() {
		return servitudes;
	}

	public void setServitudes(Set<ServitudeRF> servitudes) {
		this.servitudes = servitudes;
	}

	public void addServitude(ServitudeRF servitude) {
		if (this.servitudes == null) {
			this.servitudes = new HashSet<>();
		}
		servitude.addAyantDroit(this);
		this.servitudes.add(servitude);
	}

	@Transient
	public Set<DroitRF> getDroitList() {
		final Set<DroitRF> set = new HashSet<>();
		if (droitsPropriete != null) {
			set.addAll(droitsPropriete);
		}
		if (servitudes != null) {
			set.addAll(servitudes);
		}
		return set;
	}

	public void copyDataTo(AyantDroitRF right) {
		right.idRF = this.idRF;
	}
}
