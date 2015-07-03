package ch.vd.unireg.interfaces.organisation.data.builder;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Adresse;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;

public class DonneesRCBuilder implements DataBuilder<DonneesRC> {

	private List<DateRanged<StatusRC>> status;
	private List<DateRanged<String>> nom;
	private List<DateRanged<StatusInscriptionRC>> statusInscription;
	private List<DateRanged<Capital>> capital;
	private List<DateRanged<Adresse>> adresseLegale;

	public DonneesRCBuilder() {}

	public DonneesRCBuilder(@NotNull List<DateRanged<StatusRC>> status, @NotNull List<DateRanged<String>> nom) {
		this.status = status;
		this.nom = nom;
	}

	@Override
	public DonneesRC build() {
		return new DonneesRC(adresseLegale, status, nom, statusInscription, capital);
	}

	public DonneesRCBuilder addStatus(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull StatusRC valeur) {
		BuilderHelper.addValueToList(status, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRCBuilder addNom(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		BuilderHelper.addValueToList(nom, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRCBuilder addStatusInscription(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull StatusInscriptionRC valeur) {
		BuilderHelper.addValueToList(statusInscription, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRCBuilder addCapital(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Capital valeur) {
		BuilderHelper.addValueToList(capital, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRCBuilder addAdresseLegale(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Adresse valeur) {
		BuilderHelper.addValueToList(adresseLegale, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRCBuilder withAdresseLegale(List<DateRanged<Adresse>> adresseLegale) {
		this.adresseLegale = adresseLegale;
		return this;
	}

	public DonneesRCBuilder withCapital(List<DateRanged<Capital>> capital) {
		this.capital = capital;
		return this;
	}

	public DonneesRCBuilder withStatusInscription(List<DateRanged<StatusInscriptionRC>> statusInscription) {
		this.statusInscription = statusInscription;
		return this;
	}
}
