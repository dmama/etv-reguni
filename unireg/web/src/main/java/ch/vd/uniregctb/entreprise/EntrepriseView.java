package ch.vd.uniregctb.entreprise;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.uniregctb.tiers.view.EtatEntrepriseView;

public class EntrepriseView {

	enum SourceCivile {
		RCENT("label.source.donnees.rcent"), UNIREG("label.source.donnees.fiscal");

		private String key;

		SourceCivile(String key) {
			this.key = key;
		}

		public String getKey() {
			return key;
		}
	}

	private List<DateRanged<String>> raisonSociale;
	private List<DateRanged<String>> nomsAdditionnels;

	private List<SiegeView> sieges;
	private List<FormeJuridiqueView> formesJuridiques;
	private List<CapitalView> capitaux;
	private List<EtatEntrepriseView> etats;
	private List<String> numerosIDE;

	private RegDate dateInscriptionRC;
	private StatusRC statusRC;
	private RegDate dateRadiationRC;

	private RegDate dateInscriptionIde;
	private StatusRegistreIDE statusIde;

	private SourceCivile source;

	public List<DateRanged<String>> getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(List<DateRanged<String>> raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	public List<DateRanged<String>> getNomsAdditionnels() {
		return nomsAdditionnels;
	}

	public void setNomsAdditionnels(List<DateRanged<String>> nomsAdditionnels) {
		this.nomsAdditionnels = nomsAdditionnels;
	}

	public List<SiegeView> getSieges() {
		return sieges;
	}

	public void setSieges(List<SiegeView> sieges) {
		this.sieges = sieges;
	}

	public List<FormeJuridiqueView> getFormesJuridiques() {
		return formesJuridiques;
	}

	public void setFormesJuridiques(List<FormeJuridiqueView> formesJuridiques) {
		this.formesJuridiques = formesJuridiques;
	}

	public List<CapitalView> getCapitaux() {
		return capitaux;
	}

	public void setCapitaux(List<CapitalView> capitaux) {
		this.capitaux = capitaux;
	}

	public List<EtatEntrepriseView> getEtats() {
		return etats;
	}

	public void setEtats(List<EtatEntrepriseView> etats) {
		this.etats = etats;
	}

	public List<String> getNumerosIDE() {
		return numerosIDE;
	}

	public void setNumerosIDE(List<String> numerosIDE) {
		this.numerosIDE = numerosIDE;
	}

	public StatusRC getStatusRC() {
		return statusRC;
	}

	public void setStatusRC(StatusRC statusRC) {
		this.statusRC = statusRC;
	}

	public RegDate getDateInscriptionRC() {
		return dateInscriptionRC;
	}

	public void setDateInscriptionRC(RegDate dateInscriptionRC) {
		this.dateInscriptionRC = dateInscriptionRC;
	}

	public RegDate getDateRadiationRC() {
		return dateRadiationRC;
	}

	public void setDateRadiationRC(RegDate dateRadiationRC) {
		this.dateRadiationRC = dateRadiationRC;
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

	public String getSourceKey() {
		return source.getKey();
	}

	public SourceCivile getSource() {
		return source;
	}

	public void setSource(SourceCivile source) {
		this.source = source;
	}
}
