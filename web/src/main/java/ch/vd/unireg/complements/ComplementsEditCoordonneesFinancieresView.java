package ch.vd.uniregctb.complements;

import ch.vd.uniregctb.iban.IbanHelper;
import ch.vd.uniregctb.tiers.Tiers;

@SuppressWarnings("UnusedDeclaration")
public class ComplementsEditCoordonneesFinancieresView {

	private long id;

	// coordonnées financières
	private String oldIban;
	private String iban;
	private String titulaireCompteBancaire;
	private String adresseBicSwift;

	public ComplementsEditCoordonneesFinancieresView() {
	}

	public ComplementsEditCoordonneesFinancieresView(Tiers tiers) {
		initReadOnlyData(tiers);

		this.oldIban = IbanHelper.normalize(tiers.getNumeroCompteBancaire());
		this.iban = IbanHelper.normalize(tiers.getNumeroCompteBancaire());
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
		return IbanHelper.toDisplayString(iban);
	}

	public void setIban(String iban) {
		this.iban = IbanHelper.normalize(iban);
	}

	public String getOldIban() {
		return oldIban;
	}

	public void setOldIban(String oldIban) {
		this.oldIban = oldIban;
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
