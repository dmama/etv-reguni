package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.BooleanYesNoUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "DOMICILE_ETA")
@TypeDefs({
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		@TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class)
})
public class RegpmDomicileEtablissement extends RegpmEntity implements Comparable<RegpmDomicileEtablissement> {

	/**
	 * Ils ont fait une clé primaire avec le numéro de l'établissement et un numéro de séquence
	 */
	@Embeddable
	public static class PK implements Serializable, Comparable<PK> {

		private Integer seqNo;
		private Long idEtablissement;

		public PK() {
		}

		public PK(Integer seqNo, Long idEtablissement) {
			this.seqNo = seqNo;
			this.idEtablissement = idEtablissement;
		}

		@Override
		public int compareTo(@NotNull PK o) {
			int comparison = Long.compare(idEtablissement, o.idEtablissement);
			if (comparison == 0) {
				comparison = seqNo - o.seqNo;
			}
			return comparison;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PK pk = (PK) o;
			return !(idEtablissement != null ? !idEtablissement.equals(pk.idEtablissement) : pk.idEtablissement != null) && !(seqNo != null ? !seqNo.equals(pk.seqNo) : pk.seqNo != null);
		}

		@Override
		public int hashCode() {
			int result = seqNo != null ? seqNo.hashCode() : 0;
			result = 31 * result + (idEtablissement != null ? idEtablissement.hashCode() : 0);
			return result;
		}

		@Column(name = "NO_SEQUENCE")
		public Integer getSeqNo() {
			return seqNo;
		}

		public void setSeqNo(Integer seqNo) {
			this.seqNo = seqNo;
		}

		@Column(name = "FK_ETABNO")
		public Long getIdEtablissement() {
			return idEtablissement;
		}

		public void setIdEtablissement(Long idEtablissement) {
			this.idEtablissement = idEtablissement;
		}
	}

	private PK id;
	private RegDate dateValidite;
	private boolean rectifiee;
	private RegpmCommune commune;

	@Override
	public int compareTo(@NotNull RegpmDomicileEtablissement o) {
		int comparison = NullDateBehavior.EARLIEST.compare(dateValidite, o.dateValidite);
		if (comparison == 0) {
			comparison = id.compareTo(o.id);
		}
		return comparison;
	}

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Column(name = "DA_VALIDITE")
	@Type(type = "RegDate")
	public RegDate getDateValidite() {
		return dateValidite;
	}

	public void setDateValidite(RegDate dateValidite) {
		this.dateValidite = dateValidite;
	}

	@Column(name = "RECTIFIEE")
	@Type(type = "BooleanYesNo", parameters = @Parameter(name = "default", value = "false"))
	public boolean isRectifiee() {
		return rectifiee;
	}

	public void setRectifiee(boolean rectifiee) {
		this.rectifiee = rectifiee;
	}

	@ManyToOne
	@JoinColumn(name = "FK_COMMUNENO")
	public RegpmCommune getCommune() {
		return commune;
	}

	public void setCommune(RegpmCommune commune) {
		this.commune = commune;
	}
}
