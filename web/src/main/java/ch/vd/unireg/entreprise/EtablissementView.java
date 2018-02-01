package ch.vd.unireg.entreprise;

import java.util.List;

import ch.vd.unireg.tiers.DegreAssociationRegistreCivil;
import ch.vd.unireg.tiers.view.DomicileEtablissementView;

public class EtablissementView {

	private Long id;
	private Long noCantonal;

	private String raisonSociale;
	private String enseigne;

	private List<DomicileEtablissementView> domiciles;

	private List<String> numerosIDE;

	private DegreAssociationRegistreCivil degreAssocCivilEntreprise;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public boolean isConnueAuCivil() {
		return noCantonal != null;
	}

	public Long getNoCantonal() {
		return noCantonal;
	}

	public void setNoCantonal(Long noCantonal) {
		this.noCantonal = noCantonal;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	public String getEnseigne() {
		return enseigne;
	}

	public void setEnseigne(String enseigne) {
		this.enseigne = enseigne;
	}

	public List<DomicileEtablissementView> getDomiciles() {
		return domiciles;
	}

	public void setDomiciles(List<DomicileEtablissementView> domiciles) {
		this.domiciles = domiciles;
	}

	public List<String> getNumerosIDE() {
		return numerosIDE;
	}

	public void setNumerosIDE(List<String> numerosIDE) {
		this.numerosIDE = numerosIDE;
	}

	public DegreAssociationRegistreCivil getDegreAssocCivilEntreprise() {
		return degreAssocCivilEntreprise;
	}

	public void setDegreAssocCivilEntreprise(DegreAssociationRegistreCivil degreAssocCivilEntreprise) {
		this.degreAssocCivilEntreprise = degreAssocCivilEntreprise;
	}
}
