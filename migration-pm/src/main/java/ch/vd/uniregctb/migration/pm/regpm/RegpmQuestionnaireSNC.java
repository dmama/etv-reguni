package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.TypeEtatQuestionnaireSNCUserType;

@Entity
@Table(name = "QUESTIONNAIRE_SNC")
@TypeDefs({
		          @TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		          @TypeDef(name = "TypeEtatQuestionnaireSNC", typeClass = TypeEtatQuestionnaireSNCUserType.class)
          })
public class RegpmQuestionnaireSNC extends RegpmEntity implements Comparable<RegpmQuestionnaireSNC> {

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

		@Column(name = "FK_ENTRNO_ENTR")
		public Long getIdEntreprise() {
			return idEntreprise;
		}

		public void setIdEntreprise(Long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}
	}

	private PK id;
	private int anneeFiscale;
	private int noParAnnee;
	private RegpmTypeEtatQuestionnaireSNC etat;
	private RegDate dateEnvoi;
	private RegDate delaiRetour;
	private RegDate dateRetour;
	private RegDate dateDecision;
	private RegDate dateRappel;
	private RegDate dateAnnulation;

	@Override
	public int compareTo(@NotNull RegpmQuestionnaireSNC o) {
		return anneeFiscale == o.anneeFiscale ? noParAnnee - o.noParAnnee : anneeFiscale - o.anneeFiscale;
	}

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Column(name = "ANNEE_FISCALE")
	public int getAnneeFiscale() {
		return anneeFiscale;
	}

	public void setAnneeFiscale(int anneeFiscale) {
		this.anneeFiscale = anneeFiscale;
	}

	@Column(name = "NO_PAR_ANNEE")
	public int getNoParAnnee() {
		return noParAnnee;
	}

	public void setNoParAnnee(int noParAnnee) {
		this.noParAnnee = noParAnnee;
	}

	@Column(name = "CODE_ETAT")
	@Type(type = "TypeEtatQuestionnaireSNC")
	public RegpmTypeEtatQuestionnaireSNC getEtat() {
		return etat;
	}

	public void setEtat(RegpmTypeEtatQuestionnaireSNC etat) {
		this.etat = etat;
	}

	@Column(name = "DATE_ENVOI")
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

	@Column(name = "DATE_RETOUR")
	@Type(type = "RegDate")
	public RegDate getDateRetour() {
		return dateRetour;
	}

	public void setDateRetour(RegDate dateRetour) {
		this.dateRetour = dateRetour;
	}

	@Column(name = "DATE_DECISION")
	@Type(type = "RegDate")
	public RegDate getDateDecision() {
		return dateDecision;
	}

	public void setDateDecision(RegDate dateDecision) {
		this.dateDecision = dateDecision;
	}

	@Column(name = "DATE_RAPPEL")
	@Type(type = "RegDate")
	public RegDate getDateRappel() {
		return dateRappel;
	}

	public void setDateRappel(RegDate dateRappel) {
		this.dateRappel = dateRappel;
	}

	@Column(name = "DATE_ANNULATION")
	@Type(type = "RegDate")
	public RegDate getDateAnnulation() {
		return dateAnnulation;
	}

	public void setDateAnnulation(RegDate dateAnnulation) {
		this.dateAnnulation = dateAnnulation;
	}
}
