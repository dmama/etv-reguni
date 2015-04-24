package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;

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
@Table(name = "CAPITAL")
@TypeDefs({
		          @TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		          @TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class)
          })
public class RegpmCapital extends RegpmEntity implements Comparable<RegpmCapital> {

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

		@Column(name = "FK_ENTPRNO")
		public Long getIdEntreprise() {
			return idEntreprise;
		}

		public void setIdEntreprise(Long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}
	}

	private PK id;
	private RegDate dateEvolutionCapital;
	private BigDecimal capitalAction;
	private BigDecimal capitalLibere;
	private Integer anneePublicationFosc;
	private Integer noPublicationFosc;
	private Integer anneeRectificationFosc;
	private Integer noRectificationFosc;
	private boolean rectifiee;

	@Override
	public int compareTo(@NotNull RegpmCapital o) {
		int comparison = NullDateBehavior.EARLIEST.compare(dateEvolutionCapital, o.dateEvolutionCapital);
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

	@Column(name = "DA_EVOLUTION_CAP")
	@Type(type = "RegDate")
	public RegDate getDateEvolutionCapital() {
		return dateEvolutionCapital;
	}

	public void setDateEvolutionCapital(RegDate dateEvolutionCapital) {
		this.dateEvolutionCapital = dateEvolutionCapital;
	}

	@Column(name = "CAPITAL_ACTION")
	public BigDecimal getCapitalAction() {
		return capitalAction;
	}

	public void setCapitalAction(BigDecimal capitalAction) {
		this.capitalAction = capitalAction;
	}

	@Column(name = "CAPITAL_LIBERE")
	public BigDecimal getCapitalLibere() {
		return capitalLibere;
	}

	public void setCapitalLibere(BigDecimal capitalLibere) {
		this.capitalLibere = capitalLibere;
	}

	@Column(name = "FK_P_FOSCANNEE")
	public Integer getAnneePublicationFosc() {
		return anneePublicationFosc;
	}

	public void setAnneePublicationFosc(Integer anneePublicationFosc) {
		this.anneePublicationFosc = anneePublicationFosc;
	}

	@Column(name = "FK_P_FOSCNO_ANN")
	public Integer getNoPublicationFosc() {
		return noPublicationFosc;
	}

	public void setNoPublicationFosc(Integer noPublicationFosc) {
		this.noPublicationFosc = noPublicationFosc;
	}

	@Column(name = "FK_R_FOSCANNEE")
	public Integer getAnneeRectificationFosc() {
		return anneeRectificationFosc;
	}

	public void setAnneeRectificationFosc(Integer anneeRectificationFosc) {
		this.anneeRectificationFosc = anneeRectificationFosc;
	}

	@Column(name = "FK_R_FOSCNO_ANN")
	public Integer getNoRectificationFosc() {
		return noRectificationFosc;
	}

	public void setNoRectificationFosc(Integer noRectificationFosc) {
		this.noRectificationFosc = noRectificationFosc;
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
