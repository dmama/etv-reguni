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

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "EX_COMMERCIAL")
@TypeDefs({
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class)
})
public class RegpmExerciceCommercial extends RegpmEntity implements Comparable<RegpmExerciceCommercial> {

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

		@Column(name = "FK_ENTPRNO")
		public Long getIdEntreprise() {
			return idEntreprise;
		}

		public void setIdEntreprise(Long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}
	}

	private PK id;
	private RegDate dateDebut;
	private RegDate dateFin;
	private Integer nbJours;
	private RegpmDossierFiscal dossierFiscal;

	@Override
	public int compareTo(@NotNull RegpmExerciceCommercial o) {
		return NullDateBehavior.EARLIEST.compare(dateDebut, o.dateDebut);
	}

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Column(name = "DAD")
	@Type(type = "RegDate")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Column(name = "DAF")
	@Type(type = "RegDate")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Column(name = "NB_JOURS")
	public Integer getNbJours() {
		return nbJours;
	}

	public void setNbJours(Integer nbJours) {
		this.nbJours = nbJours;
	}

	@ManyToOne
	@JoinColumns({
			@JoinColumn(name = "FK_DOF_FKASSUJNO", referencedColumnName = "FK_ASSUJNO"),
			@JoinColumn(name = "FK_DOFNOSEQ", referencedColumnName = "NO_SEQUENCE")
	})
	public RegpmDossierFiscal getDossierFiscal() {
		return dossierFiscal;
	}

	public void setDossierFiscal(RegpmDossierFiscal dossierFiscal) {
		this.dossierFiscal = dossierFiscal;
	}
}
