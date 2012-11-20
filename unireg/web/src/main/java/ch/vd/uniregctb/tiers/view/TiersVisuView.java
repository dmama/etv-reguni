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
}
