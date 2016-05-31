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

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.BooleanYesNoUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FormeFusionUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "FUSION")
@TypeDefs({
		          @TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		          @TypeDef(name = "FormeFusion", typeClass = FormeFusionUserType.class),
		          @TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class)
          })
public class RegpmFusion extends RegpmEntity {

	/**
	 * Ils ont fait une clé primaire avec le numéro de l'entreprise destination de la fusion et un numéro de séquence
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

		@Column(name = "FK_D_ENTPRNO")
		public Long getIdEntreprise() {
			return idEntreprise;
		}

		public void setIdEntreprise(Long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}
	}

	private PK id;
	private RegpmFormeFusion formeFusion;
	private RegDate dateInscription;
	private RegDate dateContrat;
	private RegDate dateBilan;
	private boolean rectifiee;
	private RegpmEntreprise entrepriseAvant;
	private RegpmEntreprise entrepriseApres;

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Column(name = "FORME_FUSION")
	@Type(type = "FormeFusion")
	public RegpmFormeFusion getFormeFusion() {
		return formeFusion;
	}

	public void setFormeFusion(RegpmFormeFusion formeFusion) {
		this.formeFusion = formeFusion;
	}

	@Column(name = "DA_INSC_FUSION")
	@Type(type = "RegDate")
	public RegDate getDateInscription() {
		return dateInscription;
	}

	public void setDateInscription(RegDate dateInscription) {
		this.dateInscription = dateInscription;
	}

	@Column(name = "DA_CONTRAT_FUSION")
	@Type(type = "RegDate")
	public RegDate getDateContrat() {
		return dateContrat;
	}

	public void setDateContrat(RegDate dateContrat) {
		this.dateContrat = dateContrat;
	}

	@Column(name = "DA_BILAN_FUSION")
	@Type(type = "RegDate")
	public RegDate getDateBilan() {
		return dateBilan;
	}

	public void setDateBilan(RegDate dateBilan) {
		this.dateBilan = dateBilan;
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
	@JoinColumn(name = "FK_P_ENTPRNO")
	public RegpmEntreprise getEntrepriseAvant() {
		return entrepriseAvant;
	}

	public void setEntrepriseAvant(RegpmEntreprise entrepriseAvant) {
		this.entrepriseAvant = entrepriseAvant;
	}

	@ManyToOne
	@JoinColumn(name = "FK_D_ENTPRNO", updatable = false, insertable = false)
	public RegpmEntreprise getEntrepriseApres() {
		return entrepriseApres;
	}

	public void setEntrepriseApres(RegpmEntreprise entrepriseApres) {
		this.entrepriseApres = entrepriseApres;
	}
}
