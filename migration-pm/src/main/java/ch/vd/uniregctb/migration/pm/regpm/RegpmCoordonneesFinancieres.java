package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;

@Embeddable
@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class)
public class RegpmCoordonneesFinancieres implements Serializable {

	private String noCCP;
	private String noCompteBancaire;
	private String iban;
	private String bicSwift;
	private String nomInstitutionFinanciere;
	private RegpmInstitutionFinanciere institutionFinanciere;

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
	@JoinColumn(name = "FK_INSTIT_FINNO")
	public RegpmInstitutionFinanciere getInstitutionFinanciere() {
		return institutionFinanciere;
	}

	public void setInstitutionFinanciere(RegpmInstitutionFinanciere institutionFinanciere) {
		this.institutionFinanciere = institutionFinanciere;
	}
}
