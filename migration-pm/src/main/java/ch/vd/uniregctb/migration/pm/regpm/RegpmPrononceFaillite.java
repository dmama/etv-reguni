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
@Table(name = "PRONONCE_FAILLITE")
@TypeDefs({
		          @TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class),
		          @TypeDef(name = "RegDate", typeClass = RegDateUserType.class)
          })
public class RegpmPrononceFaillite extends RegpmEntity implements Comparable<RegpmPrononceFaillite> {

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

		@Column(name = "FK_EENTPRFKENT")
		public Long getIdEntreprise() {
			return idEntreprise;
		}

		public void setIdEntreprise(Long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}

		@Column(name = "FK_EENTPRNO")
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
	private RegDate dateFinAvanceFrais;
	private Long avanceFrais;
	private RegDate datePrononceFaillite;
	private RegDate dateLimiteProd;
	private boolean rectifiee;
	private Set<RegpmFinFaillite> finsFaillite;
	private Set<RegpmLiquidateur> liquidateurs;

	@Override
	public int compareTo(@NotNull RegpmPrononceFaillite o) {
		return NullDateBehavior.EARLIEST.compare(datePrononceFaillite, o.datePrononceFaillite);
	}

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Column(name = "DAF_AVANCE_FRAIS")
	@Type(type = "RegDate")
	public RegDate getDateFinAvanceFrais() {
		return dateFinAvanceFrais;
	}

	public void setDateFinAvanceFrais(RegDate dateFinAvanceFrais) {
		this.dateFinAvanceFrais = dateFinAvanceFrais;
	}

	@Column(name = "AVANCE_FRAIS")
	public Long getAvanceFrais() {
		return avanceFrais;
	}

	public void setAvanceFrais(Long avanceFrais) {
		this.avanceFrais = avanceFrais;
	}

	@Column(name = "DA_PRON_FAILLITE")
	@Type(type = "RegDate")
	public RegDate getDatePrononceFaillite() {
		return datePrononceFaillite;
	}

	public void setDatePrononceFaillite(RegDate datePrononceFaillite) {
		this.datePrononceFaillite = datePrononceFaillite;
	}

	@Column(name = "DA_LIMITE_PROD")
	@Type(type = "RegDate")
	public RegDate getDateLimiteProd() {
		return dateLimiteProd;
	}

	public void setDateLimiteProd(RegDate dateLimiteProd) {
		this.dateLimiteProd = dateLimiteProd;
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
			             @JoinColumn(name = "FK_PROFAIFKEENTFK", referencedColumnName = "FK_EENTPRFKENT"),
			             @JoinColumn(name = "FK_PROFAIFKEENTNO", referencedColumnName = "FK_EENTPRNO"),
			             @JoinColumn(name = "FK_PROFAINO", referencedColumnName = "NO_SEQUENCE")
	             })
	public Set<RegpmFinFaillite> getFinsFaillite() {
		return finsFaillite;
	}

	public void setFinsFaillite(Set<RegpmFinFaillite> finsFaillite) {
		this.finsFaillite = finsFaillite;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumns({
			             @JoinColumn(name = "FK_PROFAINO", referencedColumnName = "NO_SEQUENCE"),
			             @JoinColumn(name = "FK_PROFAIFKEENTNO", referencedColumnName = "FK_EENTPRNO"),
			             @JoinColumn(name = "FK_PROFAIFKEENTFK", referencedColumnName = "FK_EENTPRFKENT")
	             })
	public Set<RegpmLiquidateur> getLiquidateurs() {
		return liquidateurs;
	}

	public void setLiquidateurs(Set<RegpmLiquidateur> liquidateurs) {
		this.liquidateurs = liquidateurs;
	}
}
