package ch.vd.uniregctb.tiers.view;

import java.util.List;

/**
 * Structure model pour l'ecran de visualisation des Tiers
 *
 * @author xcikce
 *
 */
public class TiersVisuView extends TiersView{

	public static final String MODIF_FISCAL = "FISCAL";
	public static final String MODIF_CIVIL = "CIVIL";
	public static final String MODIF_ADRESSE = "ADR";
	public static final String MODIF_COMPLEMENT = "CPLT";
	public static final String MODIF_RAPPORT = "RPT";
	public static final String MODIF_DOSSIER = "DOS";
	public static final String MODIF_DEBITEUR = "DBT";
	public static final String MODIF_DI = "DI";
	public static final String MODIF_MOUVEMENT = "MVT";

	private boolean isAllowed;

	private boolean adressesHisto;


	private boolean adressesHistoCiviles;

	private boolean adressesHistoCivilesConjoint;
	@Override
	public boolean isAllowed() {
		return isAllowed;
	}

	@Override
	public void setAllowed(boolean isAllowed) {
		this.isAllowed = isAllowed;
	}

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
