package ch.vd.uniregctb.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

import ch.vd.uniregctb.common.HibernateDateRangeEntity;

/**
 * Situation d'un immeuble inscrit au registre foncier.
 */
@Entity
@Table(name = "RF_SITUATION")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = false)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN"))
})
public class SituationRF extends HibernateDateRangeEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Le numéro de la parcelle sur laquelle est construit l'immeuble.
	 */
	private int noParcelle;

	/**
	 * Numéro d'index de premier niveau.
	 */
	private Integer index1;

	/**
	 * Numéro d'index de deuxième niveau.
	 */
	private Integer index2;

	/**
	 * Numéro d'index de troisième niveau.
	 */
	private Integer index3;

	/**
	 * Le numéro RF de la commune de la parcelle (il s'agit en fait de l'ancien numéro cantonal de commune, mais la situation n'est pas claire concernant les nouvelles communes fusionnées).
	 */
	private int noRfCommune;

	/**
	 * Le numéro Ofs de la commune de la parcelle.
	 */
	private int noOfsCommune;

	/**
	 * L'immeuble concerné par la situation.
	 */
	private ImmeubleRF immeuble;

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

	@Column(name = "NO_PARCELLE", nullable = false)
	public int getNoParcelle() {
		return noParcelle;
	}

	public void setNoParcelle(int noParcelle) {
		this.noParcelle = noParcelle;
	}

	@Column(name = "INDEX1")
	public Integer getIndex1() {
		return index1;
	}

	public void setIndex1(Integer index1) {
		this.index1 = index1;
	}

	@Column(name = "INDEX2")
	public Integer getIndex2() {
		return index2;
	}

	public void setIndex2(Integer index2) {
		this.index2 = index2;
	}

	@Column(name = "INDEX3")
	public Integer getIndex3() {
		return index3;
	}

	public void setIndex3(Integer index3) {
		this.index3 = index3;
	}

	@Column(name = "NO_RF_COMMUNE", nullable = false)
	public int getNoRfCommune() {
		return noRfCommune;
	}

	public void setNoRfCommune(int noRfCommune) {
		this.noRfCommune = noRfCommune;
	}

	@Column(name = "NO_OFS_COMMUNE", nullable = false)
	public int getNoOfsCommune() {
		return noOfsCommune;
	}

	public void setNoOfsCommune(int noOfsCommune) {
		this.noOfsCommune = noOfsCommune;
	}

	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "IMMEUBLE_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_SIT_RF_IMMEUBLE_ID", columnNames = "IMMEUBLE_ID")
	public ImmeubleRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleRF immeuble) {
		this.immeuble = immeuble;
	}
}
