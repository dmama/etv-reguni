package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.BooleanYesNoUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.TypeDemandeDelaiUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.TypeEtatDemandeDelaiUserType;

@Entity
@Table(name = "DEM_DELAI_SOMMAT")
@TypeDefs({
		          @TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class),
		          @TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		          @TypeDef(name = "TypeDemandeDelai", typeClass = TypeDemandeDelaiUserType.class),
		          @TypeDef(name = "TypeEtatDemandeDelai", typeClass = TypeEtatDemandeDelaiUserType.class)
          })
public class RegpmDemandeDelaiSommation extends RegpmEntity implements Comparable<RegpmDemandeDelaiSommation> {

	@Embeddable
	public static class PK implements Serializable, Comparable<PK> {

		private Integer noSequence;
		private Integer noSequenceDossierFiscal;
		private Long idAssujettissement;

		@Override
		public int compareTo(@NotNull PK o) {
			int comparison = Long.compare(idAssujettissement, o.idAssujettissement);
			if (comparison == 0) {
				comparison = noSequenceDossierFiscal - o.noSequenceDossierFiscal;
			}
			if (comparison == 0) {
				comparison = noSequence - o.noSequence;
			}
			return comparison;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PK pk = (PK) o;

			if (idAssujettissement != null ? !idAssujettissement.equals(pk.idAssujettissement) : pk.idAssujettissement != null) return false;
			if (noSequence != null ? !noSequence.equals(pk.noSequence) : pk.noSequence != null) return false;
			if (noSequenceDossierFiscal != null ? !noSequenceDossierFiscal.equals(pk.noSequenceDossierFiscal) : pk.noSequenceDossierFiscal != null) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = noSequence != null ? noSequence.hashCode() : 0;
			result = 31 * result + (noSequenceDossierFiscal != null ? noSequenceDossierFiscal.hashCode() : 0);
			result = 31 * result + (idAssujettissement != null ? idAssujettissement.hashCode() : 0);
			return result;
		}

		@Column(name = "NO_SEQUENCE")
		public Integer getNoSequence() {
			return noSequence;
		}

		public void setNoSequence(Integer noSequence) {
			this.noSequence = noSequence;
		}

		@Column(name = "FK_DOFNOSEQ")
		public Integer getNoSequenceDossierFiscal() {
			return noSequenceDossierFiscal;
		}

		public void setNoSequenceDossierFiscal(Integer noSequenceDossierFiscal) {
			this.noSequenceDossierFiscal = noSequenceDossierFiscal;
		}

		@Column(name = "FK_DOF_FKASSUJNO")
		public Long getIdAssujettissement() {
			return idAssujettissement;
		}

		public void setIdAssujettissement(Long idAssujettissement) {
			this.idAssujettissement = idAssujettissement;
		}
	}

	private PK id;
	private RegDate dateDemande;
	private RegDate dateReception;
	private RegDate delaiDemande;
	private RegDate delaiAccorde;
	private RegpmTypeEtatDemandeDelai etat;
	private RegpmTypeDemandeDelai type;
	private RegDate dateEnvoi;
	private boolean impressionLettre;
	private RegpmMandat mandataire;

	@Override
	public int compareTo(@NotNull RegpmDemandeDelaiSommation o) {
		int comparison = NullDateBehavior.EARLIEST.compare(dateDemande, o.dateDemande);
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

	@Column(name = "DA_DEMANDE")
	@Type(type = "RegDate")
	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
	}

	@Column(name = "DA_RECEPTION")
	@Type(type = "RegDate")
	public RegDate getDateReception() {
		return dateReception;
	}

	public void setDateReception(RegDate dateReception) {
		this.dateReception = dateReception;
	}

	@Column(name = "DELAI_DEMANDE")
	@Type(type = "RegDate")
	public RegDate getDelaiDemande() {
		return delaiDemande;
	}

	public void setDelaiDemande(RegDate delaiDemande) {
		this.delaiDemande = delaiDemande;
	}

	@Column(name = "DELAI_ACCORDE")
	@Type(type = "RegDate")
	public RegDate getDelaiAccorde() {
		return delaiAccorde;
	}

	public void setDelaiAccorde(RegDate delaiAccorde) {
		this.delaiAccorde = delaiAccorde;
	}

	@Column(name = "CODE_ETAT")
	@Type(type = "TypeEtatDemandeDelai")
	public RegpmTypeEtatDemandeDelai getEtat() {
		return etat;
	}

	public void setEtat(RegpmTypeEtatDemandeDelai etat) {
		this.etat = etat;
	}

	@Column(name = "TYPE")
	@Type(type = "TypeDemandeDelai")
	public RegpmTypeDemandeDelai getType() {
		return type;
	}

	public void setType(RegpmTypeDemandeDelai type) {
		this.type = type;
	}

	@Column(name = "DA_ENVOI")
	@Type(type = "RegDate")
	public RegDate getDateEnvoi() {
		return dateEnvoi;
	}

	public void setDateEnvoi(RegDate dateEnvoi) {
		this.dateEnvoi = dateEnvoi;
	}

	@Column(name = "IMPRESSION_LETTRE")
	@Type(type = "BooleanYesNo", parameters = @Parameter(name = "default", value = "false"))
	public boolean isImpressionLettre() {
		return impressionLettre;
	}

	public void setImpressionLettre(boolean impressionLettre) {
		this.impressionLettre = impressionLettre;
	}

	@ManyToOne
	@JoinColumns({
			             @JoinColumn(name = "FK_MANDATNOSEQ", referencedColumnName = "NO_SEQUENCE"),
			             @JoinColumn(name = "FK_MANDATFKENTRPNO", referencedColumnName = "FK_P_ENTPRNO")
	             })
	public RegpmMandat getMandataire() {
		return mandataire;
	}

	public void setMandataire(RegpmMandat mandataire) {
		this.mandataire = mandataire;
	}
}
