package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;

/**
 * [UNIREG-1059] Une remarque associée à un tiers. Un tiers peut posséder 0 ou n remarques.
 */
@Entity
@Table(name = "REMARQUE")
public class Remarque extends HibernateEntity {

	private Long id;
	private String texte;
	private Tiers tiers;

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

	@Column(name = "TEXTE", length = LengthConstants.TIERS_REMARQUE)
	public String getTexte() {
		return texte;
	}

	public void setTexte(String texte) {
		this.texte = texte;
	}

	@ManyToOne
	@JoinColumn(name = "TIERS_ID", nullable = false)
	@Index(name = "IDX_REMARQUE_TIERS_ID", columnNames = "TIERS_ID")
	@ForeignKey(name = "FK_REMARQUE_TRS_ID")
	public Tiers getTiers() {
		return tiers;
	}

	public void setTiers(Tiers tiers) {
		this.tiers = tiers;
	}
}
