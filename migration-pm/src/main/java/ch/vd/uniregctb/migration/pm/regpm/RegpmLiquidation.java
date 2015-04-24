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
import java.io.Serializable;
import java.util.Set;

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
@Table(name = "LIQUIDATION")
@TypeDefs({
		          @TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		          @TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class)
          })
public class RegpmLiquidation extends RegpmEntity implements Comparable<RegpmLiquidation> {

	@Embeddable
	public static class PK implements Serializable {

		private Integer noSeq;
		private Long idEntreprise;
		private Integer noSequenceEtatEntreprise;

		@Column(name = "NO_SEQUENCE")
		public Integer getNoSeq() {
			return noSeq;
		}

		public void setNoSeq(Integer noSeq) {
			this.noSeq = noSeq;
		}

		@Column(name = "FK_ETENTFKENT")
		public Long getIdEntreprise() {
			return idEntreprise;
		}

		public void setIdEntreprise(Long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}

		@Column(name = "FK_ETENTNO")
		public Integer getNoSequenceEtatEntreprise() {
			return noSequenceEtatEntreprise;
		}

		public void setNoSequenceEtatEntreprise(Integer noSequenceEtatEntreprise) {
			this.noSequenceEtatEntreprise = noSequenceEtatEntreprise;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PK pk = (PK) o;
			return !(idEntreprise != null ? !idEntreprise.equals(pk.idEntreprise) : pk.idEntreprise != null) && !(noSeq != null ? !noSeq.equals(pk.noSeq) : pk.noSeq != null) &&
					!(noSequenceEtatEntreprise != null ? !noSequenceEtatEntreprise.equals(pk.noSequenceEtatEntreprise) : pk.noSequenceEtatEntreprise != null);
		}

		@Override
		public int hashCode() {
			int result = noSeq != null ? noSeq.hashCode() : 0;
			result = 31 * result + (idEntreprise != null ? idEntreprise.hashCode() : 0);
			result = 31 * result + (noSequenceEtatEntreprise != null ? noSequenceEtatEntreprise.hashCode() : 0);
			return result;
		}
	}

	private PK id;
	private RegDate dateDebutLiquidation;
	private boolean rectifiee;
	private Set<RegpmFinLiquidation> finsLiquidation;
	private Set<RegpmLiquidateur> liquidateurs;

	@Override
	public int compareTo(@NotNull RegpmLiquidation o) {
		return NullDateBehavior.EARLIEST.compare(dateDebutLiquidation, o.dateDebutLiquidation);
	}

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Column(name = "DAD_LIQUIDATION")
	@Type(type = "RegDate")
	public RegDate getDateDebutLiquidation() {
		return dateDebutLiquidation;
	}

	public void setDateDebutLiquidation(RegDate dateDebutLiquidation) {
		this.dateDebutLiquidation = dateDebutLiquidation;
	}

	@Column(name = "RECTIFIEE")
	@Type(type = "BooleanYesNo", parameters = @Parameter(name = "default", value = "false"))
	public boolean isRectifiee() {
		return rectifiee;
	}

	public void setRectifiee(boolean rectifiee) {
		this.rectifiee = rectifiee;
	}

	@OneToMany(fetch = FetchType.EAGER)
	@JoinColumns({
			@JoinColumn(name = "FK_LIQIONNO", referencedColumnName = "NO_SEQUENCE"),
			@JoinColumn(name = "FK_LIQIONFKEENTNO", referencedColumnName = "FK_ETENTNO"),
			@JoinColumn(name = "FK_LIQIONFKEENTFK", referencedColumnName = "FK_ETENTFKENT")
	             })
	public Set<RegpmFinLiquidation> getFinsLiquidation() {
		return finsLiquidation;
	}

	public void setFinsLiquidation(Set<RegpmFinLiquidation> finsLiquidation) {
		this.finsLiquidation = finsLiquidation;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumns({
			             @JoinColumn(name = "FK_LIQIONNO", referencedColumnName = "NO_SEQUENCE"),
			             @JoinColumn(name = "FK_LIQIONFKEENTNO", referencedColumnName = "FK_ETENTNO"),
			             @JoinColumn(name = "FK_LIQIONFKEENTFK", referencedColumnName = "FK_ETENTFKENT")
	             })
	public Set<RegpmLiquidateur> getLiquidateurs() {
		return liquidateurs;
	}

	public void setLiquidateurs(Set<RegpmLiquidateur> liquidateurs) {
		this.liquidateurs = liquidateurs;
	}
}
