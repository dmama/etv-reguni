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
import java.util.SortedSet;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.BooleanYesNoUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.TypeEtatEntrepriseUserType;

@Entity
@Table(name = "ETAT_ENTREPRISE")
@TypeDefs({
		          @TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		          @TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		          @TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class),
		          @TypeDef(name = "TypeEtatEntreprise", typeClass = TypeEtatEntrepriseUserType.class)
          })
public class RegpmEtatEntreprise extends RegpmEntity implements Comparable<RegpmEtatEntreprise> {

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
		public int compareTo(@NotNull PK o) {
			int comparison = Long.compare(idEntreprise, o.idEntreprise);
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
			return !(idEntreprise != null ? !idEntreprise.equals(pk.idEntreprise) : pk.idEntreprise != null) && !(seqNo != null ? !seqNo.equals(pk.seqNo) : pk.seqNo != null);
		}

		@Override
		public int hashCode() {
			int result = seqNo != null ? seqNo.hashCode() : 0;
			result = 31 * result + (idEntreprise != null ? idEntreprise.hashCode() : 0);
			return result;
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
	private RegDate dateValidite;
	private boolean rectifie;
	private RegpmTypeEtatEntreprise typeEtat;
	private SortedSet<RegpmPrononceFaillite> prononcesFaillite;
	private SortedSet<RegpmLiquidation> liquidations;

	@Override
	public int compareTo(@NotNull RegpmEtatEntreprise o) {
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

	@Column(name = "RECTIFIE")
	@Type(type = "BooleanYesNo", parameters = @Parameter(name = "default", value = "false"))
	public boolean isRectifie() {
		return rectifie;
	}

	public void setRectifie(boolean rectifie) {
		this.rectifie = rectifie;
	}

	@Column(name = "FK_ETATNO")
	@Type(type = "TypeEtatEntreprise")
	public RegpmTypeEtatEntreprise getTypeEtat() {
		return typeEtat;
	}

	public void setTypeEtat(RegpmTypeEtatEntreprise typeEtat) {
		this.typeEtat = typeEtat;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumns({
			             @JoinColumn(name = "FK_EENTPRNO", referencedColumnName = "NO_SEQUENCE"),
			             @JoinColumn(name = "FK_EENTPRFKENT", referencedColumnName = "FK_ENTPRNO")
	             })
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmPrononceFaillite> getPrononcesFaillite() {
		return prononcesFaillite;
	}

	public void setPrononcesFaillite(SortedSet<RegpmPrononceFaillite> prononcesFaillite) {
		this.prononcesFaillite = prononcesFaillite;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumns({
			             @JoinColumn(name = "FK_ETENTNO", referencedColumnName = "NO_SEQUENCE"),
			             @JoinColumn(name = "FK_ETENTFKENT", referencedColumnName = "FK_ENTPRNO")
	             })
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmLiquidation> getLiquidations() {
		return liquidations;
	}

	public void setLiquidations(SortedSet<RegpmLiquidation> liquidations) {
		this.liquidations = liquidations;
	}
}
