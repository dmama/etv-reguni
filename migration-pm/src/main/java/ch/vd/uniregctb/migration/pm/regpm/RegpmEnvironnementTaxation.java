package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Set;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "ENV_TAXATION")
@TypeDef(name = "RegDate", typeClass = RegDateUserType.class)
public class RegpmEnvironnementTaxation extends RegpmEntity implements Comparable<RegpmEnvironnementTaxation> {

	/**
	 * La clé primaire est composée de la clé vers le dossier fiscal et d'un numéro de séquence
	 */
	@Embeddable
	public static class PK implements Serializable, Comparable<PK> {

		private Integer seqNo;
		private Long idEntreprise;
		private Integer anneeFiscale;

		public PK() {
		}

		public PK(Integer seqNo, Long idEntreprise, Integer anneeFiscale) {
			this.seqNo = seqNo;
			this.idEntreprise = idEntreprise;
			this.anneeFiscale = anneeFiscale;
		}

		@Override
		public int compareTo(@NotNull PK o) {
			int comparison = Long.compare(idEntreprise, o.idEntreprise);
			if (comparison == 0) {
				comparison = anneeFiscale - o.anneeFiscale;
			}
			if (comparison == 0) {
				comparison = seqNo - o.seqNo;
			}
			return comparison;
		}

		@Column(name = "NO_SEQUENCE")
		public Integer getSeqNo() {
			return seqNo;
		}

		public void setSeqNo(Integer seqNo) {
			this.seqNo = seqNo;
		}

		@Column(name = "FK_ENTPRNO")
		public Long getIdEntreprise() {
			return idEntreprise;
		}

		public void setIdEntreprise(Long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}

		@Column(name = "ANNEE_FISCALE")
		public Integer getAnneeFiscale() {
			return anneeFiscale;
		}

		public void setAnneeFiscale(Integer anneeFiscale) {
			this.anneeFiscale = anneeFiscale;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PK pk = (PK) o;

			if (seqNo != null ? !seqNo.equals(pk.seqNo) : pk.seqNo != null) return false;
			if (idEntreprise != null ? !idEntreprise.equals(pk.idEntreprise) : pk.idEntreprise != null) return false;
			return !(anneeFiscale != null ? !anneeFiscale.equals(pk.anneeFiscale) : pk.anneeFiscale != null);

		}

		@Override
		public int hashCode() {
			int result = seqNo != null ? seqNo.hashCode() : 0;
			result = 31 * result + (idEntreprise != null ? idEntreprise.hashCode() : 0);
			result = 31 * result + (anneeFiscale != null ? anneeFiscale.hashCode() : 0);
			return result;
		}
	}

	private PK id;
	private RegDate dateCreation;
	private Set<RegpmDecisionTaxation> decisionsTaxation;

	@Override
	public int compareTo(RegpmEnvironnementTaxation o) {
		return id.compareTo(o.id);
	}

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Transient
	public Integer getAnneeFiscale() {
		return id.getAnneeFiscale();
	}

	@Column(name = "DA_CREATION")
	@Type(type = "RegDate")
	public RegDate getDateCreation() {
		return dateCreation;
	}

	public void setDateCreation(RegDate dateCreation) {
		this.dateCreation = dateCreation;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumns({
			@JoinColumn(name = "FK_ETXNOSEQ", referencedColumnName = "NO_SEQUENCE"),
			@JoinColumn(name = "FK_ETXANFIS", referencedColumnName = "ANNEE_FISCALE"),
			@JoinColumn(name = "FK_ETX_FKENTPRNO", referencedColumnName = "FK_ENTPRNO")
	})
	public Set<RegpmDecisionTaxation> getDecisionsTaxation() {
		return decisionsTaxation;
	}

	public void setDecisionsTaxation(Set<RegpmDecisionTaxation> decisionsTaxation) {
		this.decisionsTaxation = decisionsTaxation;
	}
}
