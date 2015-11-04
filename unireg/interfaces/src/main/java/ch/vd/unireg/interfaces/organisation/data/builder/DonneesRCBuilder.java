package ch.vd.unireg.interfaces.organisation.data.builder;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.AdresseRCEnt;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRCRCEnt;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;

public class DonneesRCBuilder implements DataBuilder<DonneesRC> {

	private List<DateRanged<StatusRC>> status;
	private List<DateRanged<String>> nom;
	private List<DateRanged<StatusInscriptionRC>> statusInscription;
	private List<Capital> capital;
	private List<AdresseRCEnt> adresseLegale;
	private List<DateRanged<String>> buts;
	private List<DateRanged<RegDate>> dateStatus;

	public DonneesRCBuilder() {}

	public DonneesRCBuilder(@NotNull List<DateRanged<StatusRC>> status, @NotNull List<DateRanged<String>> nom, List<DateRanged<String>> buts, List<DateRanged<RegDate>> dateStatus) {
		this.status = status;
		this.nom = nom;
		this.buts = buts;
		this.dateStatus = dateStatus;
	}

	@Override
	public DonneesRCRCEnt build() {
		return new DonneesRCRCEnt(adresseLegale, status, nom, statusInscription, capital, buts, dateStatus);
	}

	public DonneesRCBuilder addStatus(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull StatusRC valeur) {
		status = BuilderHelper.addValueToList(status, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRCBuilder addNom(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull String valeur) {
		nom = BuilderHelper.addValueToList(nom, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRCBuilder addStatusInscription(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull StatusInscriptionRC valeur) {
		statusInscription = BuilderHelper.addValueToList(statusInscription, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRCBuilder addCapital(@NotNull Capital valeur) {
		capital = BuilderHelper.addValueToList(capital, valeur);
		return this;
	}

	public DonneesRCBuilder addAdresseLegale(@NotNull AdresseRCEnt valeur) {
		adresseLegale = BuilderHelper.addValueToList(adresseLegale, valeur);
		return this;
	}

	public DonneesRCBuilder addButs(@NotNull DateRanged<String> buts) {
		this.buts = BuilderHelper.addValueToList(this.buts, buts);
		return this;
	}

	public DonneesRCBuilder addDateStatus(@NotNull DateRanged<RegDate> dateStatus) {
		this.dateStatus = BuilderHelper.addValueToList(this.dateStatus, dateStatus);
		return this;
	}

	public DonneesRCBuilder withAdresseLegale(List<AdresseRCEnt> adresseLegale) {
		this.adresseLegale = adresseLegale;
		return this;
	}

	public DonneesRCBuilder withCapital(List<Capital> capital) {
		this.capital = capital;
		return this;
	}

	public DonneesRCBuilder withStatusInscription(List<DateRanged<StatusInscriptionRC>> statusInscription) {
		this.statusInscription = statusInscription;
		return this;
	}
}