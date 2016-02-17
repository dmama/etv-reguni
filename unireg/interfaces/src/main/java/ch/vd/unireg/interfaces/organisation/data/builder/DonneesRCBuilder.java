package ch.vd.unireg.interfaces.organisation.data.builder;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.AdresseLegaleRCEnt;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRCRCEnt;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;

public class DonneesRCBuilder implements DataBuilder<DonneesRC> {

	private List<DateRanged<StatusInscriptionRC>> statusInscription;
	private List<DateRanged<RegDate>> dateInscription;
	private List<Capital> capital;
	private List<AdresseLegaleRCEnt> adresseLegale;
	private List<DateRanged<String>> buts;
	private List<DateRanged<RegDate>> dateStatus;
	private List<DateRanged<RegDate>> dateRadiation;

	public DonneesRCBuilder() {}

	public DonneesRCBuilder(@NotNull List<DateRanged<StatusInscriptionRC>> statusInscription, List<DateRanged<String>> buts, List<DateRanged<RegDate>> dateStatus) {
		this.statusInscription = statusInscription;
		this.buts = buts;
		this.dateStatus = dateStatus;
	}

	@Override
	public DonneesRCRCEnt build() {
		return new DonneesRCRCEnt(adresseLegale, statusInscription, dateInscription, capital, buts, dateStatus, dateRadiation);
	}

	public DonneesRCBuilder addStatusInscription(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull StatusInscriptionRC valeur) {
		statusInscription = BuilderHelper.addValueToList(statusInscription, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRCBuilder addDateInscription(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull RegDate valeur) {
		dateInscription = BuilderHelper.addValueToList(dateInscription, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRCBuilder addCapital(@NotNull Capital valeur) {
		capital = BuilderHelper.addValueToList(capital, valeur);
		return this;
	}

	public DonneesRCBuilder addAdresseLegale(@NotNull AdresseLegaleRCEnt valeur) {
		adresseLegale = BuilderHelper.addValueToList(adresseLegale, valeur);
		return this;
	}

	public DonneesRCBuilder addButs(@NotNull DateRanged<String> buts) {
		this.buts = BuilderHelper.addValueToList(this.buts, buts);
		return this;
	}

	public DonneesRCBuilder addDateRadiation(@NotNull DateRanged<RegDate> dateRadiation) {
		this.dateRadiation = BuilderHelper.addValueToList(this.dateRadiation, dateRadiation);
		return this;
	}

	public DonneesRCBuilder withAdresseLegale(List<AdresseLegaleRCEnt> adresseLegale) {
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

	public DonneesRCBuilder withDateInscription(List<DateRanged<RegDate>> dateInscription) {
		this.dateInscription = dateInscription;
		return this;
	}
}