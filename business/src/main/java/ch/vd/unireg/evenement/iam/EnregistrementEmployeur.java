package ch.vd.uniregctb.evenement.iam;

import java.util.Date;
import java.util.List;

public class EnregistrementEmployeur extends EvenementIAM {

	private String businessId;

	private Date dateTraitement;

	private List<InfoEmployeur> employeursAMettreAJour;

	public String getBusinessId() {
		return businessId;
	}

	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}

	public Date getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(Date dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	public List<InfoEmployeur> getEmployeursAMettreAJour() {
		return employeursAMettreAJour;
	}

	public void setEmployeursAMettreAJour(List<InfoEmployeur> employeursAMettreAJour) {
		this.employeursAMettreAJour = employeursAMettreAJour;
	}

	@Override
	public String toString() {
		return "EnregistrementEmployeur{" +
				"businessId='" + businessId + '\'' +
				",dateTraitement='" + dateTraitement + '\'' +
				",Employeurs='" + employeursAMettreAJour + '\'' +
				'}';
	}
}
