package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

@Entity
@Table(name = "ETAT_ENTREPRISE")
public class EtatEntreprise extends HibernateDateRangeEntity {

	private Long id;
	private TypeEtatEntreprise type;
	private Entreprise entreprise;

	@Transient
	@Override
	public Long getKey() {
		return id;
	}

	@Id
	@Column(name = "ID", nullable = false, updatable = false)
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "TYPE_ETAT", length = LengthConstants.ETATENT_ETAT, nullable = false)
	@Enumerated(value = EnumType.STRING)
	public TypeEtatEntreprise getType() {
		return type;
	}

	public void setType(TypeEtatEntreprise type) {
		this.type = type;
	}

	@ManyToOne
	@JoinColumn(name = "ENTREPRISE_ID")
	public Entreprise getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(Entreprise entreprise) {
		this.entreprise = entreprise;
	}
}
