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
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.TypeMandatUserType;

@Entity
@Table(name = "MANDAT")
@TypeDefs({
		          @TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		          @TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		          @TypeDef(name = "TypeMandat", typeClass = TypeMandatUserType.class)
          })
public class RegpmMandat extends RegpmEntity {

	@Embeddable
	private static class PK implements Serializable {

		private Integer noSequence;
		private Long idEntreprise;

		@Column(name = "NO_SEQUENCE")
		public Integer getNoSequence() {
			return noSequence;
		}

		public void setNoSequence(Integer noSequence) {
			this.noSequence = noSequence;
		}

		@Column(name = "FK_P_ENTPRNO")
		public Long getIdEntreprise() {
			return idEntreprise;
		}

		public void setIdEntreprise(Long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PK pk = (PK) o;
			return !(idEntreprise != null ? !idEntreprise.equals(pk.idEntreprise) : pk.idEntreprise != null) && !(noSequence != null ? !noSequence.equals(pk.noSequence) : pk.noSequence != null);
		}

		@Override
		public int hashCode() {
			int result = noSequence != null ? noSequence.hashCode() : 0;
			result = 31 * result + (idEntreprise != null ? idEntreprise.hashCode() : 0);
			return result;
		}
	}

	private PK id;
	private RegpmTypeMandat type;
	private RegDate dateAttribution;
	private RegDate dateResiliation;
	private String motif;
	private String nomContact;
	private String prenomContact;
	private String noTelContact;
	private String noFaxContact;
	private String noCCP;
	private String noCompteBancaire;
	private String iban;
	private String bicSwift;
	private String nomInstitutionFinanciere;
	private RegpmEntreprise mandataireEntreprise;
	private RegpmEtablissement mandataireEtablissement;
	private RegpmIndividu mandataireIndividu;
	private RegpmInstitutionFinanciere institutionFinanciere;

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Column(name = "CO_MANDAT")
	@Type(type = "TypeMandat")
	public RegpmTypeMandat getType() {
		return type;
	}

	public void setType(RegpmTypeMandat type) {
		this.type = type;
	}

	@Column(name = "DA_ATTRIBUTION")
	@Type(type = "RegDate")
	public RegDate getDateAttribution() {
		return dateAttribution;
	}

	public void setDateAttribution(RegDate dateAttribution) {
		this.dateAttribution = dateAttribution;
	}

	@Column(name = "DA_RESILIATION")
	@Type(type = "RegDate")
	public RegDate getDateResiliation() {
		return dateResiliation;
	}

	public void setDateResiliation(RegDate dateResiliation) {
		this.dateResiliation = dateResiliation;
	}

	@Column(name = "MOTIF")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "40"))
	public String getMotif() {
		return motif;
	}

	public void setMotif(String motif) {
		this.motif = motif;
	}

	@Column(name = "NOM_CONTACT")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getNomContact() {
		return nomContact;
	}

	public void setNomContact(String nomContact) {
		this.nomContact = nomContact;
	}

	@Column(name = "PRENOM_CONTACT")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "20"))
	public String getPrenomContact() {
		return prenomContact;
	}

	public void setPrenomContact(String prenomContact) {
		this.prenomContact = prenomContact;
	}

	@Column(name = "NO_TEL_CONTACT")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "16"))
	public String getNoTelContact() {
		return noTelContact;
	}

	public void setNoTelContact(String noTelContact) {
		this.noTelContact = noTelContact;
	}

	@Column(name = "NO_FAX_CONTACT")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "16"))
	public String getNoFaxContact() {
		return noFaxContact;
	}

	public void setNoFaxContact(String noFaxContact) {
		this.noFaxContact = noFaxContact;
	}

	@Column(name = "NO_CCP")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "15"))
	public String getNoCCP() {
		return noCCP;
	}

	public void setNoCCP(String noCCP) {
		this.noCCP = noCCP;
	}

	@Column(name = "NO_COMPTE_BANCAIRE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "20"))
	public String getNoCompteBancaire() {
		return noCompteBancaire;
	}

	public void setNoCompteBancaire(String noCompteBancaire) {
		this.noCompteBancaire = noCompteBancaire;
	}

	@Column(name = "IBAN")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "40"))
	public String getIban() {
		return iban;
	}

	public void setIban(String iban) {
		this.iban = iban;
	}

	@Column(name = "BIC_SWIFT")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "12"))
	public String getBicSwift() {
		return bicSwift;
	}

	public void setBicSwift(String bicSwift) {
		this.bicSwift = bicSwift;
	}

	@Column(name = "NOM_INSTIT_FIN")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "50"))
	public String getNomInstitutionFinanciere() {
		return nomInstitutionFinanciere;
	}

	public void setNomInstitutionFinanciere(String nomInstitutionFinanciere) {
		this.nomInstitutionFinanciere = nomInstitutionFinanciere;
	}

	@ManyToOne
	@JoinColumn(name = "FK_A_ENTPRNO")
	public RegpmEntreprise getMandataireEntreprise() {
		return mandataireEntreprise;
	}

	public void setMandataireEntreprise(RegpmEntreprise mandataireEntreprise) {
		this.mandataireEntreprise = mandataireEntreprise;
	}

	@ManyToOne
	@JoinColumn(name = "FK_ETABNO")
	public RegpmEtablissement getMandataireEtablissement() {
		return mandataireEtablissement;
	}

	public void setMandataireEtablissement(RegpmEtablissement mandataireEtablissement) {
		this.mandataireEtablissement = mandataireEtablissement;
	}

	@ManyToOne
	@JoinColumn(name = "FK_INDNO")
	public RegpmIndividu getMandataireIndividu() {
		return mandataireIndividu;
	}

	public void setMandataireIndividu(RegpmIndividu mandataireIndividu) {
		this.mandataireIndividu = mandataireIndividu;
	}

	@ManyToOne
	@JoinColumn(name = "FK_INSTIT_FINNO")
	public RegpmInstitutionFinanciere getInstitutionFinanciere() {
		return institutionFinanciere;
	}

	public void setInstitutionFinanciere(RegpmInstitutionFinanciere institutionFinanciere) {
		this.institutionFinanciere = institutionFinanciere;
	}
}
