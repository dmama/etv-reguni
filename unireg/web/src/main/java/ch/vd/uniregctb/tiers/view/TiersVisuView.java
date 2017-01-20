package ch.vd.uniregctb.tiers.view;

/**
 * Structure model pour l'ecran de visualisation des Tiers
 *
 * @author xcikce
 *
 */
public class TiersVisuView extends TiersView {

	private boolean adressesHisto;

	private boolean adressesHistoCiviles;

	private boolean adressesHistoCivilesConjoint;

	private boolean raisonsSocialesCivileHistoParam;
	private boolean siegesCivilHistoParam;
	private boolean formesJuridiquesCivileHistoParam;
	private boolean capitauxCivileHistoParam;

	public boolean isAdressesHisto() {
		return adressesHisto;
	}

	public void setAdressesHisto(boolean adressesHisto) {
		this.adressesHisto = adressesHisto;
	}

	public void setAdressesHistoCiviles(boolean adressesHistoCiviles) {
		this.adressesHistoCiviles = adressesHistoCiviles;
	}
	public boolean isAdressesHistoCiviles() {
		return adressesHistoCiviles;
	}

	public void setAdressesHistoCivilesConjoint(boolean adressesHistoCivilesConjoint) {
		this.adressesHistoCivilesConjoint = adressesHistoCivilesConjoint;
	}

	public boolean isAdressesHistoCivilesConjoint() {
		return adressesHistoCivilesConjoint;
	}

	public boolean isRaisonsSocialesCivileHistoParam() {
		return raisonsSocialesCivileHistoParam;
	}

	public void setRaisonsSocialesCivileHistoParam(boolean raisonsSocialesCivileHistoParam) {
		this.raisonsSocialesCivileHistoParam = raisonsSocialesCivileHistoParam;
	}

	public boolean isSiegesCivilHistoParam() {
		return siegesCivilHistoParam;
	}

	public void setSiegesCivilHistoParam(boolean siegesCivilHistoParam) {
		this.siegesCivilHistoParam = siegesCivilHistoParam;
	}

	public boolean isFormesJuridiquesCivileHistoParam() {
		return formesJuridiquesCivileHistoParam;
	}

	public void setFormesJuridiquesCivileHistoParam(boolean formesJuridiquesCivileHistoParam) {
		this.formesJuridiquesCivileHistoParam = formesJuridiquesCivileHistoParam;
	}

	public boolean isCapitauxCivileHistoParam() {
		return capitauxCivileHistoParam;
	}

	public void setCapitauxCivileHistoParam(boolean capitauxCivileHistoParam) {
		this.capitauxCivileHistoParam = capitauxCivileHistoParam;
	}
}
