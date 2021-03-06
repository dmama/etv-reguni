package ch.vd.unireg.entreprise;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.tiers.DegreAssociationRegistreCivil;
import ch.vd.unireg.tiers.view.EtatEntrepriseView;

public class EntrepriseView {

	private Long id;
	private Long noCantonal;

	private List<ShowRaisonSocialeView> raisonsSociales;
	private List<DateRanged<String>> nomsAdditionnels;

	private List<ShowSiegeView> sieges;
	private List<ShowFormeJuridiqueView> formesJuridiques;
	private List<ShowCapitalView> capitaux;
	private List<EtatEntrepriseView> etats;
	private String numerosIDE;
	private String secteurActivite;

	private RegDate dateInscriptionRC;
	private RegDate dateInscriptionRCVD;
	private StatusInscriptionRC statusRC;
	private RegDate dateRadiationRC;
	private RegDate dateRadiationRCVD;

	private RegDate dateInscriptionIde;
	private StatusRegistreIDE statusIde;

	private DegreAssociationRegistreCivil degreAssocCivil;

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

	public List<ShowRaisonSocialeView> getRaisonsSociales() {
		return raisonsSociales;
	}

	public void setRaisonsSociales(List<ShowRaisonSocialeView> raisonsSociales) {
		this.raisonsSociales = raisonsSociales;
	}

	public List<DateRanged<String>> getNomsAdditionnels() {
		return nomsAdditionnels;
	}

	public void setNomsAdditionnels(List<DateRanged<String>> nomsAdditionnels) {
		this.nomsAdditionnels = nomsAdditionnels;
	}

	public List<ShowSiegeView> getSieges() {
		return sieges;
	}

	public void setSieges(List<ShowSiegeView> sieges) {
		this.sieges = sieges;
	}

	public List<ShowFormeJuridiqueView> getFormesJuridiques() {
		return formesJuridiques;
	}

	public void setFormesJuridiques(List<ShowFormeJuridiqueView> formesJuridiques) {
		this.formesJuridiques = formesJuridiques;
	}

	public boolean getIsOrWasSocieteDePersonnes() {
		if (formesJuridiques != null) {
			for (ShowFormeJuridiqueView view : formesJuridiques) {
				final FormeLegale formeLegale = view.getType();
				if (formeLegale == FormeLegale.N_0104_SOCIETE_EN_COMMANDITE || formeLegale == FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF) {
					return true;
				}
			}
		}
		return false;
	}

	public List<ShowCapitalView> getCapitaux() {
		return capitaux;
	}

	public void setCapitaux(List<ShowCapitalView> capitaux) {
		this.capitaux = capitaux;
	}

	public List<EtatEntrepriseView> getEtats() {
		return etats;
	}

	public void setEtats(List<EtatEntrepriseView> etats) {
		this.etats = etats;
	}

	public String getNumerosIDE() {
		return numerosIDE;
	}

	public void setNumerosIDE(String numerosIDE) {
		this.numerosIDE = numerosIDE;
	}

	public String getSecteurActivite() {
		return secteurActivite;
	}

	public void setSecteurActivite(String secteurActivite) {
		this.secteurActivite = secteurActivite;
	}

	public StatusInscriptionRC getStatusRC() {
		return statusRC;
	}

	public void setStatusRC(StatusInscriptionRC statusRC) {
		this.statusRC = statusRC;
	}

	public RegDate getDateInscriptionRC() {
		return dateInscriptionRC;
	}

	public void setDateInscriptionRC(RegDate dateInscriptionRC) {
		this.dateInscriptionRC = dateInscriptionRC;
	}

	public RegDate getDateInscriptionRCVD() {
		return dateInscriptionRCVD;
	}

	public void setDateInscriptionRCVD(RegDate dateInscriptionRCVD) {
		this.dateInscriptionRCVD = dateInscriptionRCVD;
	}

	public RegDate getDateRadiationRC() {
		return dateRadiationRC;
	}

	public void setDateRadiationRC(RegDate dateRadiationRC) {
		this.dateRadiationRC = dateRadiationRC;
	}

	public RegDate getDateRadiationRCVD() {
		return dateRadiationRCVD;
	}

	public void setDateRadiationRCVD(RegDate dateRadiationRCVD) {
		this.dateRadiationRCVD = dateRadiationRCVD;
	}

	public RegDate getDateInscriptionIde() {
		return dateInscriptionIde;
	}

	public void setDateInscriptionIde(RegDate dateInscriptionIde) {
		this.dateInscriptionIde = dateInscriptionIde;
	}

	public StatusRegistreIDE getStatusIde() {
		return statusIde;
	}

	public void setStatusIde(StatusRegistreIDE statusIde) {
		this.statusIde = statusIde;
	}

	public DegreAssociationRegistreCivil getDegreAssocCivil() {
		return degreAssocCivil;
	}

	public void setDegreAssocCivil(DegreAssociationRegistreCivil degreAssocCivil) {
		this.degreAssocCivil = degreAssocCivil;
	}
}
