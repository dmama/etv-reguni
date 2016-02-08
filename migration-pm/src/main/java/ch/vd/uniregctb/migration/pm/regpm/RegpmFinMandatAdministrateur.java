package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.BooleanYesNoUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "FIN_MANDAT_ADM")
@TypeDefs({
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		@TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class)
})
public class RegpmFinMandatAdministrateur extends RegpmEntity {

	/**
	 * Ils ont fait une clé primaire avec le numéro de l'administrateur (= numéro d'individu et numéro de séquence) et un numéro de séquence
	 */
	@Embeddable
	public static class PK implements Serializable, Comparable<PK> {

		private Integer seqNo;
		private Long admIdIndividu;
		private Integer admSeqNo;

		public PK() {
		}

		public PK(Integer seqNo, Long admIdIndividu, Integer admSeqNo) {
			this.seqNo = seqNo;
			this.admIdIndividu = admIdIndividu;
			this.admSeqNo = admSeqNo;
		}

		@Override
		public int compareTo(@NotNull PK o) {
			int comparison = Long.compare(admIdIndividu, o.admIdIndividu);
			if (comparison == 0) {
				comparison = admSeqNo - o.admSeqNo;
			}
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

			if (seqNo != null ? !seqNo.equals(pk.seqNo) : pk.seqNo != null) return false;
			if (admIdIndividu != null ? !admIdIndividu.equals(pk.admIdIndividu) : pk.admIdIndividu != null) return false;
			return admSeqNo != null ? admSeqNo.equals(pk.admSeqNo) : pk.admSeqNo == null;
		}

		@Override
		public int hashCode() {
			int result = seqNo != null ? seqNo.hashCode() : 0;
			result = 31 * result + (admIdIndividu != null ? admIdIndividu.hashCode() : 0);
			result = 31 * result + (admSeqNo != null ? admSeqNo.hashCode() : 0);
			return result;
		}

		@Column(name = "NO_SEQUENCE")
		public Integer getSeqNo() {
			return seqNo;
		}

		public void setSeqNo(Integer seqNo) {
			this.seqNo = seqNo;
		}

		@Column(name = "FK_ADMEURFKIND")
		public Long getAdmIdIndividu() {
			return admIdIndividu;
		}

		public void setAdmIdIndividu(Long admIdIndividu) {
			this.admIdIndividu = admIdIndividu;
		}

		@Column(name = "FK_ADMEURNO")
		public Integer getAdmSeqNo() {
			return admSeqNo;
		}

		public void setAdmSeqNo(Integer admSeqNo) {
			this.admSeqNo = admSeqNo;
		}
	}

	private PK id;
	private RegDate dateFinMandat;
	private boolean rectifiee;

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Column(name = "DAF_FONCTION")
	@Type(type = "RegDate")
	public RegDate getDateFinMandat() {
		return dateFinMandat;
	}

	public void setDateFinMandat(RegDate dateFinMandat) {
		this.dateFinMandat = dateFinMandat;
	}

	@Column(name = "RECTIFIEE")
	@Type(type = "BooleanYesNo", parameters = @Parameter(name = "default", value = "false"))
	public boolean isRectifiee() {
		return rectifiee;
	}

	public void setRectifiee(boolean rectifiee) {
		this.rectifiee = rectifiee;
	}
}
