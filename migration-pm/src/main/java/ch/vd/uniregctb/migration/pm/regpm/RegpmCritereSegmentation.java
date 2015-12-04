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

import ch.vd.uniregctb.migration.pm.regpm.usertype.BooleanYesNoUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.TypeCritereSegmentationUserType;

@Entity
@Table(name = "CRITERE_SEG_ENT")
@TypeDefs({
		@TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class),
		@TypeDef(name = "TypeCritereSegmentation", typeClass = TypeCritereSegmentationUserType.class)
})
public class RegpmCritereSegmentation extends RegpmEntity {

	/**
	 * Ils ont fait une clé primaire avec le numéro de l'entreprise et un numéro de séquence
	 */
	@Embeddable
	public static class PK implements Serializable, Comparable<PK> {

		private Integer seqNo;
		private Long idEntreprise;

		public PK() {
		}

		public PK(Integer seqNo, Long idEntreprise) {
			this.seqNo = seqNo;
			this.idEntreprise = idEntreprise;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PK pk = (PK) o;
			return !(idEntreprise != null ? !idEntreprise.equals(pk.idEntreprise) : pk.idEntreprise != null) && !(seqNo != null ? !seqNo.equals(pk.seqNo) : pk.seqNo != null);
		}

		@Override
		public int hashCode() {
			int result = seqNo != null ? seqNo.hashCode() : 0;
			result = 31 * result + (idEntreprise != null ? idEntreprise.hashCode() : 0);
			return result;
		}

		@Override
		public int compareTo(@NotNull PK o) {
			int comparison = Long.compare(idEntreprise, o.idEntreprise);
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

		@Column(name = "FK_ENTREPRISE_NO")
		public Long getIdEntreprise() {
			return idEntreprise;
		}

		public void setIdEntreprise(Long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}
	}

	private PK id;
	private int pfDebut;
	private Integer pfFin;
	private boolean annule;
	private RegpmTypeCritereSegmentation type;

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Column(name = "PER_FISCALE_DEBUT", nullable = false)
	public int getPfDebut() {
		return pfDebut;
	}

	public void setPfDebut(int pfDebut) {
		this.pfDebut = pfDebut;
	}

	@Column(name = "PER_FISCALE_FIN")
	public Integer getPfFin() {
		return pfFin;
	}

	public void setPfFin(Integer pfFin) {
		this.pfFin = pfFin;
	}

	@Column(name = "CODE_ANNULATION")
	@Type(type = "BooleanYesNo", parameters = @Parameter(name = "default", value = "false"))
	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	@Column(name = "FK_TY_CRIT_SEG_CO")
	@Type(type = "TypeCritereSegmentation")
	public RegpmTypeCritereSegmentation getType() {
		return type;
	}

	public void setType(RegpmTypeCritereSegmentation type) {
		this.type = type;
	}
}
