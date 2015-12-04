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

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.BooleanYesNoUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "FIN_LIQUIDATION")
@TypeDefs({
		          @TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		          @TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class)
          })
public class RegpmFinLiquidation extends RegpmEntity {

	@Embeddable
	public static class PK implements Serializable {

		private Integer noSeq;
		private Long idEntreprise;
		private Integer noSequenceEtatEntreprise;
		private Integer noSequenceLiquidation;

		@Column(name = "NO_SEQUENCE")
		public Integer getNoSeq() {
			return noSeq;
		}

		public void setNoSeq(Integer noSeq) {
			this.noSeq = noSeq;
		}

		@Column(name = "FK_LIQIONFKEENTFK")
		public Long getIdEntreprise() {
			return idEntreprise;
		}

		public void setIdEntreprise(Long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}

		@Column(name = "FK_LIQIONFKEENTNO")
		public Integer getNoSequenceEtatEntreprise() {
			return noSequenceEtatEntreprise;
		}

		public void setNoSequenceEtatEntreprise(Integer noSequenceEtatEntreprise) {
			this.noSequenceEtatEntreprise = noSequenceEtatEntreprise;
		}

		@Column(name = "FK_LIQIONNO")
		public Integer getNoSequenceLiquidation() {
			return noSequenceLiquidation;
		}

		public void setNoSequenceLiquidation(Integer noSequenceLiquidation) {
			this.noSequenceLiquidation = noSequenceLiquidation;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PK pk = (PK) o;

			if (idEntreprise != null ? !idEntreprise.equals(pk.idEntreprise) : pk.idEntreprise != null) return false;
			if (noSeq != null ? !noSeq.equals(pk.noSeq) : pk.noSeq != null) return false;
			if (noSequenceEtatEntreprise != null ? !noSequenceEtatEntreprise.equals(pk.noSequenceEtatEntreprise) : pk.noSequenceEtatEntreprise != null) return false;
			if (noSequenceLiquidation != null ? !noSequenceLiquidation.equals(pk.noSequenceLiquidation) : pk.noSequenceLiquidation != null) return false;
			return true;
		}

		@Override
		public int hashCode() {
			int result = noSeq != null ? noSeq.hashCode() : 0;
			result = 31 * result + (idEntreprise != null ? idEntreprise.hashCode() : 0);
			result = 31 * result + (noSequenceEtatEntreprise != null ? noSequenceEtatEntreprise.hashCode() : 0);
			result = 31 * result + (noSequenceLiquidation != null ? noSequenceLiquidation.hashCode() : 0);
			return result;
		}
	}

	private PK id;
	private RegDate dateCloture;
	private boolean rectifiee;

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Column(name = "DA_CLOTURE")
	@Type(type = "RegDate")
	public RegDate getDateCloture() {
		return dateCloture;
	}

	public void setDateCloture(RegDate dateCloture) {
		this.dateCloture = dateCloture;
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
