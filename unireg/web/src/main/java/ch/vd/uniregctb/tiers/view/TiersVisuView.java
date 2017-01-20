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

	private boolean raisonsSocialesHisto;
	private boolean nomsAdditionnelsHisto;
	private boolean siegesHisto;
	private boolean formesJuridiquesHisto;
	private boolean capitauxHisto;
	private boolean domicilesHisto;

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

	public boolean isRaisonsSocialesHisto() {
		return raisonsSocialesHisto;
	}

	public void setRaisonsSocialesHisto(boolean raisonsSocialesHisto) {
		this.raisonsSocialesHisto = raisonsSocialesHisto;
	}

	public boolean isNomsAdditionnelsHisto() {
		return nomsAdditionnelsHisto;
	}

	public void setNomsAdditionnelsHisto(boolean nomsAdditionnelsHisto) {
		this.nomsAdditionnelsHisto = nomsAdditionnelsHisto;
	}

	public boolean isSiegesHisto() {
		return siegesHisto;
	}

	public void setSiegesHisto(boolean siegesHisto) {
		this.siegesHisto = siegesHisto;
	}

	public boolean isFormesJuridiquesHisto() {
		return formesJuridiquesHisto;
	}

	public void setFormesJuridiquesHisto(boolean formesJuridiquesHisto) {
		this.formesJuridiquesHisto = formesJuridiquesHisto;
	}

	public boolean isCapitauxHisto() {
		return capitauxHisto;
	}

	public void setCapitauxHisto(boolean capitauxHisto) {
		this.capitauxHisto = capitauxHisto;
	}

	public boolean isDomicilesHisto() {
		return domicilesHisto;
	}

	public void setDomicilesHisto(boolean domicilesHisto) {
		this.domicilesHisto = domicilesHisto;
	}
}
