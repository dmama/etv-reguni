package ch.vd.uniregctb.complements;

import ch.vd.uniregctb.tiers.Tiers;

@SuppressWarnings("UnusedDeclaration")
public class ComplementsEditCoordonneesFinancieresView {

	private long id;

	// coordonnées financières
	private String iban;
	private String titulaireCompteBancaire;
	private String adresseBicSwift;

	public ComplementsEditCoordonneesFinancieresView() {
	}

	public ComplementsEditCoordonneesFinancieresView(Tiers tiers) {
		initReadOnlyData(tiers);

		this.iban = tiers.getNumeroCompteBancaire();
		this.titulaireCompteBancaire = tiers.getTitulaireCompteBancaire();
		this.adresseBicSwift = tiers.getAdresseBicSwift();
	}

	public void initReadOnlyData(Tiers tiers) {
		this.id = tiers.getId();
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getIban() {
		return iban;
	}

	public void setIban(String iban) {
		this.iban = iban;
	}

	public String getTitulaireCompteBancaire() {
		return titulaireCompteBancaire;
	}

	public void setTitulaireCompteBancaire(String titulaireCompteBancaire) {
		this.titulaireCompteBancaire = titulaireCompteBancaire;
	}

	public String getAdresseBicSwift() {
		return adresseBicSwift;
	}

	public void setAdresseBicSwift(String adresseBicSwift) {
		this.adresseBicSwift = adresseBicSwift;
	}
}
