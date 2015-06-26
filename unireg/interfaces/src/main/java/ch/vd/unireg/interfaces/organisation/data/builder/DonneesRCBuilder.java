package ch.vd.unireg.interfaces.organisation.data.builder;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.Adresse;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;

public class DonneesRCBuilder implements DataBuilder<DonneesRC> {

	@NotNull
	private final List<DateRanged<StatusRC>> status;
	@NotNull
	private final List<DateRanged<String>> nom;
	private List<DateRanged<StatusInscriptionRC>> statusInscription;
	private List<DateRanged<Capital>> capital;
	private List<DateRanged<Adresse>> adresseLegale;

	public DonneesRCBuilder(@NotNull List<DateRanged<StatusRC>> status, @NotNull List<DateRanged<String>> nom) {
		this.status = status;
		this.nom = nom;
	}

	@Override
	public DonneesRC build() {
		return new DonneesRC(adresseLegale, status, nom, statusInscription, capital);
	}

	public void setAdresseLegale(List<DateRanged<Adresse>> adresseLegale) {
		this.adresseLegale = adresseLegale;
	}

	public void setCapital(List<DateRanged<Capital>> capital) {
		this.capital = capital;
	}

	public void setStatusInscription(List<DateRanged<StatusInscriptionRC>> statusInscription) {
		this.statusInscription = statusInscription;
	}
}
