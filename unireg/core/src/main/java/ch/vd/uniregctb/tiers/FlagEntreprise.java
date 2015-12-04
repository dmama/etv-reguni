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
import java.util.Collections;
import java.util.List;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.type.TypeFlagEntreprise;

/**
 * Une durée de validité (depuis une année jusqu'à une autre, cette dernière étant optionnelle)
 * d'un flag/état/attribut associé à une entreprise
 */
@Entity
@Table(name = "FLAG_ENTREPRISE")
public class FlagEntreprise extends HibernateEntity implements LinkedEntity {

	private Long id;
	private TypeFlagEntreprise type;
	private Integer anneeDebutValidite;
	private Integer anneeFinValidite;
	private Entreprise entreprise;

	public FlagEntreprise() {
	}

	public FlagEntreprise(TypeFlagEntreprise type, int anneeDebutValidite, Integer anneeFinValidite) {
		this.anneeDebutValidite = anneeDebutValidite;
		this.anneeFinValidite = anneeFinValidite;
		this.type = type;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, updatable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "ANNEE_DEBUT", nullable = false)
	public Integer getAnneeDebutValidite() {
		return anneeDebutValidite;
	}

	public void setAnneeDebutValidite(Integer anneeDebutValidite) {
		this.anneeDebutValidite = anneeDebutValidite;
	}

	@Column(name = "ANNEE_FIN", nullable = true)
	public Integer getAnneeFinValidite() {
		return anneeFinValidite;
	}

	public void setAnneeFinValidite(Integer anneeFinValidite) {
		this.anneeFinValidite = anneeFinValidite;
	}

	@Column(name = "FLAG", length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	public TypeFlagEntreprise getType() {
		return type;
	}

	public void setType(TypeFlagEntreprise type) {
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

	@Transient
	@Override
	public List<?> getLinkedEntities(boolean includeAnnuled) {
		return entreprise == null ? null : Collections.singletonList(entreprise);
	}
}
