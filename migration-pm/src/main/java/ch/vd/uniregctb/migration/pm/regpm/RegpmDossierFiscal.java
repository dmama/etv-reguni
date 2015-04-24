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
import java.util.SortedSet;

import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.MotifEnvoiUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.TypeEtatDossierFiscalUserType;

@Entity
@Table(name = "DOSSIER_FISCAL")
@TypeDefs({
		@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		@TypeDef(name = "TypeEtatDossierFiscal", typeClass = TypeEtatDossierFiscalUserType.class),
		@TypeDef(name = "MotifEnvoi", typeClass = MotifEnvoiUserType.class)
})
public class RegpmDossierFiscal extends RegpmEntity implements Comparable<RegpmDossierFiscal> {

	/**
	 * Ils ont fait une clé primaire avec le numéro de l'assujetissement et un numéro de séquence
	 */
	@Embeddable
	public static class PK implements Serializable, Comparable<PK> {

		private Integer seqNo;
		private Long idAssujettissement;

		public PK() {
		}

		public PK(Integer seqNo, Long idAssujettissement) {
			this.seqNo = seqNo;
			this.idAssujettissement = idAssujettissement;
		}

		@Override
		public int compareTo(@NotNull PK o) {
			int comparison = Long.compare(idAssujettissement, o.idAssujettissement);
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
			return !(idAssujettissement != null ? !idAssujettissement.equals(pk.idAssujettissement) : pk.idAssujettissement != null) && !(seqNo != null ? !seqNo.equals(pk.seqNo) : pk.seqNo != null);
		}

		@Override
		public int hashCode() {
			int result = seqNo != null ? seqNo.hashCode() : 0;
			result = 31 * result + (idAssujettissement != null ? idAssujettissement.hashCode() : 0);
			return result;
		}

		@Column(name = "NO_SEQUENCE")
		public Integer getSeqNo() {
			return seqNo;
		}

		public void setSeqNo(Integer seqNo) {
			this.seqNo = seqNo;
		}

		@Column(name = "FK_ASSUJNO")
		public Long getIdAssujettissement() {
			return idAssujettissement;
		}

		public void setIdAssujettissement(Long idAssujettissement) {
			this.idAssujettissement = idAssujettissement;
		}
	}

	private PK id;
	private RegDate dateEnvoi;
	private RegDate delaiRetour;
	private RegDate dateRetour;
	private RegDate dateEnvoiSommation;
	private RegDate delaiSommation;
	private RegpmTypeEtatDossierFiscal etat;
	private Integer pf;
	private Integer noParAnnee;
	private RegpmAssujettissement assujettissement;
	private RegpmMotifEnvoi motifEnvoi;
	private SortedSet<RegpmDemandeDelaiSommation> demandesDelai;

	@Override
	public int compareTo(@NotNull RegpmDossierFiscal o) {
		int comparison = pf - o.pf;
		if (comparison == 0) {
			comparison = noParAnnee - o.noParAnnee;
		}
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

	@Column(name = "DA_ENVOI")
	@Type(type = "RegDate")
	public RegDate getDateEnvoi() {
		return dateEnvoi;
	}

	public void setDateEnvoi(RegDate dateEnvoi) {
		this.dateEnvoi = dateEnvoi;
	}

	@Column(name = "DELAI_RETOUR")
	@Type(type = "RegDate")
	public RegDate getDelaiRetour() {
		return delaiRetour;
	}

	public void setDelaiRetour(RegDate delaiRetour) {
		this.delaiRetour = delaiRetour;
	}

	@Column(name = "DA_RETOUR")
	@Type(type = "RegDate")
	public RegDate getDateRetour() {
		return dateRetour;
	}

	public void setDateRetour(RegDate dateRetour) {
		this.dateRetour = dateRetour;
	}

	@Column(name = "DA_ENVOI_SOMMATION")
	@Type(type = "RegDate")
	public RegDate getDateEnvoiSommation() {
		return dateEnvoiSommation;
	}

	public void setDateEnvoiSommation(RegDate dateEnvoiSommation) {
		this.dateEnvoiSommation = dateEnvoiSommation;
	}

	@Column(name = "DELAI_SOMMATION")
	@Type(type = "RegDate")
	public RegDate getDelaiSommation() {
		return delaiSommation;
	}

	public void setDelaiSommation(RegDate delaiSommation) {
		this.delaiSommation = delaiSommation;
	}

	@Column(name = "CODE_ETAT")
	@Type(type = "TypeEtatDossierFiscal")
	public RegpmTypeEtatDossierFiscal getEtat() {
		return etat;
	}

	public void setEtat(RegpmTypeEtatDossierFiscal etat) {
		this.etat = etat;
	}

	@Column(name = "ANNEE_FISCALE")
	public Integer getPf() {
		return pf;
	}

	public void setPf(Integer pf) {
		this.pf = pf;
	}

	@Column(name = "NO_PAR_ANNEE")
	public Integer getNoParAnnee() {
		return noParAnnee;
	}

	public void setNoParAnnee(Integer noParAnnee) {
		this.noParAnnee = noParAnnee;
	}

	@ManyToOne
	@JoinColumn(name = "FK_ASSUJNO", insertable = false, updatable = false)
	public RegpmAssujettissement getAssujettissement() {
		return assujettissement;
	}

	public void setAssujettissement(RegpmAssujettissement assujettissement) {
		this.assujettissement = assujettissement;
	}

	@Column(name = "FK_MEDNO")
	@Type(type = "MotifEnvoi")
	public RegpmMotifEnvoi getMotifEnvoi() {
		return motifEnvoi;
	}

	public void setMotifEnvoi(RegpmMotifEnvoi motifEnvoi) {
		this.motifEnvoi = motifEnvoi;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumns({
			             @JoinColumn(name = "FK_DOFNOSEQ", referencedColumnName = "NO_SEQUENCE"),
			             @JoinColumn(name = "FK_DOF_FKASSUJNO", referencedColumnName = "FK_ASSUJNO")
	             })
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmDemandeDelaiSommation> getDemandesDelai() {
		return demandesDelai;
	}

	public void setDemandesDelai(SortedSet<RegpmDemandeDelaiSommation> demandesDelai) {
		this.demandesDelai = demandesDelai;
	}
}
