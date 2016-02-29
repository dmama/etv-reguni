package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
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
@Table(name = "FIN_ASSOCIE_SC")
@TypeDefs({
		          @TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class),
		          @TypeDef(name = "RegDate", typeClass = RegDateUserType.class)
          })
public class RegpmFinAssocieSC extends RegpmEntity {

	/**
	 * Clé primaire avec une clé primaire de RegpmAssocieSC et un numéro de séquence
	 */
	public static class PK implements Serializable {

		private Integer noSeq;
		private Integer noSeqAssocieSC;
		private Long idEntreprise;

		@Column(name = "NO_SEQUENCE")
		public Integer getNoSeq() {
			return noSeq;
		}

		public void setNoSeq(Integer noSeq) {
			this.noSeq = noSeq;
		}

		@Column(name = "FK_ASSOCSCNO")
		public Integer getNoSeqAssocieSC() {
			return noSeqAssocieSC;
		}

		public void setNoSeqAssocieSC(Integer noSeqAssocieSC) {
			this.noSeqAssocieSC = noSeqAssocieSC;
		}

		@Column(name = "FK_ASSOCSCFK_ENT")
		public Long getIdEntreprise() {
			return idEntreprise;
		}

		public void setIdEntreprise(Long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PK pk = (PK) o;

			if (idEntreprise != null ? !idEntreprise.equals(pk.idEntreprise) : pk.idEntreprise != null) return false;
			if (noSeq != null ? !noSeq.equals(pk.noSeq) : pk.noSeq != null) return false;
			if (noSeqAssocieSC != null ? !noSeqAssocieSC.equals(pk.noSeqAssocieSC) : pk.noSeqAssocieSC != null) return false;
			return true;
		}

		@Override
		public int hashCode() {
			int result = noSeq != null ? noSeq.hashCode() : 0;
			result = 31 * result + (noSeqAssocieSC != null ? noSeqAssocieSC.hashCode() : 0);
			result = 31 * result + (idEntreprise != null ? idEntreprise.hashCode() : 0);
			return result;
		}
	}

	private PK id;
	private RegDate dateFinFonction;
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
	public RegDate getDateFinFonction() {
		return dateFinFonction;
	}

	public void setDateFinFonction(RegDate dateFinFonction) {
		this.dateFinFonction = dateFinFonction;
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
