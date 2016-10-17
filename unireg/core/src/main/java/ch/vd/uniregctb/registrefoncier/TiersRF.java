package ch.vd.uniregctb.registrefoncier;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.HibernateEntity;

/**
 * Tiers au registre foncier. Un tiers est soit une personne physique, soit une personne morale.
 */
@Entity
@Table(name = "RF_TIERS")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TIERS_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class TiersRF extends HibernateEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Identifiant technique du tiers au registre foncier.
	 */
	private String idRF;

	/**
	 * Numéro public du tiers au registre foncier.
	 */
	private long noRF;

	/**
	 * Numéro de contribuable selon le registre foncier.
	 */
	@Nullable
	private Long noContribuable;

	/**
	 * Les droits sur des immeubles dont jouit le tiers.
	 */
	private Set<DroitRF> droits;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "ID_RF", nullable = false)
	public String getIdRF() {
		return idRF;
	}

	public void setIdRF(String idRF) {
		this.idRF = idRF;
	}

	@Column(name = "NO_RF")
	public long getNoRF() {
		return noRF;
	}

	public void setNoRF(long noRF) {
		this.noRF = noRF;
	}

	@Column(name = "NO_CTB")
	@Nullable
	public Long getNoContribuable() {
		return noContribuable;
	}

	public void setNoContribuable(@Nullable Long noContribuable) {
		this.noContribuable = noContribuable;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "TIERS_ID", nullable = false)
	@ForeignKey(name = "FK_DROIT_RF_TIERS_ID")
	public Set<DroitRF> getDroits() {
		return droits;
	}

	public void setDroits(Set<DroitRF> droits) {
		this.droits = droits;
	}
}
