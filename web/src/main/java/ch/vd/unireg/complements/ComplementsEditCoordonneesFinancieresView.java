package ch.vd.unireg.complements;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.iban.IbanHelper;
import ch.vd.unireg.tiers.CompteBancaire;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.tiers.Tiers;

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

	public ComplementsEditCoordonneesFinancieresView(@NotNull Tiers tiers) {
		initReadOnlyData(tiers);

		final CoordonneesFinancieres coords = tiers.getCoordonneesFinancieresCourantes();
		if (coords != null) {
			this.titulaireCompteBancaire = coords.getTitulaire();

			final CompteBancaire compteBancaire = coords.getCompteBancaire();
			if (compteBancaire != null) {
				this.oldIban = IbanHelper.normalize(compteBancaire.getIban());
				this.iban = IbanHelper.normalize(compteBancaire.getIban());
				this.adresseBicSwift = compteBancaire.getBicSwift();
			}
		}
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
