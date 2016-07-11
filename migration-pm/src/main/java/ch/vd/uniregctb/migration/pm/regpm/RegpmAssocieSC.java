package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Set;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.BooleanYesNoUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "ASSOCIE_SC")
@TypeDefs({
		@TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class),
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class)
})
public class RegpmAssocieSC extends RegpmEntity {

	/**
	 * Ils ont fait une clé primaire avec le numéro de l'entreprise et un numéro de séquence
	 */
	@Embeddable
	public static class PK implements Serializable {

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

		@Column(name = "NO_SEQUENCE")
		public Integer getSeqNo() {
			return seqNo;
		}

		public void setSeqNo(Integer seqNo) {
			this.seqNo = seqNo;
		}

		@Column(name = "FK_A_ENTPRNO")
		public Long getIdEntreprise() {
			return idEntreprise;
		}

		public void setIdEntreprise(Long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}
	}

	private PK id;
	private RegDate dateValidite;
	private Long montantCommandite;
	private boolean commandite;
	private boolean rectifiee;
	private RegpmEntreprise entreprise;
	private RegpmEtablissement etablissement;
	private RegpmIndividu individu;
	private Set<RegpmFinAssocieSC> finsAssocieSC;

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

	@Column(name = "MT_COMMANDITE")
	public Long getMontantCommandite() {
		return montantCommandite;
	}

	public void setMontantCommandite(Long montantCommandite) {
		this.montantCommandite = montantCommandite;
	}

	@Column(name = "COMMANDITE")
	@Type(type = "BooleanYesNo", parameters = @Parameter(name = "default", value = "false"))
	public boolean isCommandite() {
		return commandite;
	}

	public void setCommandite(boolean commandite) {
		this.commandite = commandite;
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
	@JoinColumn(name = "FK_E_ENTPRNO")
	public RegpmEntreprise getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(RegpmEntreprise entreprise) {
		this.entreprise = entreprise;
	}

	@ManyToOne
	@JoinColumn(name = "FK_ETABNO")
	public RegpmEtablissement getEtablissement() {
		return etablissement;
	}

	public void setEtablissement(RegpmEtablissement etablissement) {
		this.etablissement = etablissement;
	}

	@ManyToOne
	@JoinColumn(name = "FK_INDNO")
	public RegpmIndividu getIndividu() {
		return individu;
	}

	public void setIndividu(RegpmIndividu individu) {
		this.individu = individu;
	}

	@OneToMany(fetch = FetchType.EAGER)
	@JoinColumns({
			             @JoinColumn(name = "FK_ASSOCSCNO", referencedColumnName = "NO_SEQUENCE"),
			             @JoinColumn(name = "FK_ASSOCSCFK_ENT", referencedColumnName = "FK_A_ENTPRNO")

	             })
	public Set<RegpmFinAssocieSC> getFinsAssocieSC() {
		return finsAssocieSC;
	}

	public void setFinsAssocieSC(Set<RegpmFinAssocieSC> finsAssocieSC) {
		this.finsAssocieSC = finsAssocieSC;
	}
}
