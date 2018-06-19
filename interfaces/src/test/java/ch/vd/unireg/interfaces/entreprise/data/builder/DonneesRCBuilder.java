package ch.vd.unireg.interfaces.entreprise.data.builder;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.entreprise.data.AdresseLegaleRCEnt;
import ch.vd.unireg.interfaces.entreprise.data.Capital;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.DonneesRC;
import ch.vd.unireg.interfaces.entreprise.data.DonneesRCRCEnt;
import ch.vd.unireg.interfaces.entreprise.data.EntreeJournalRC;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;

public class DonneesRCBuilder implements DataBuilder<DonneesRC> {

	private List<DateRanged<InscriptionRC>> inscription;
	private List<Capital> capital;
	private List<AdresseLegaleRCEnt> adresseLegale;
	private List<DateRanged<String>> buts;
	private List<DateRanged<RegDate>> dateStatus;
	private List<EntreeJournalRC> entreesJournalRC;

	public DonneesRCBuilder() {}

	public DonneesRCBuilder(@NotNull List<DateRanged<InscriptionRC>> inscription, List<DateRanged<String>> buts, List<DateRanged<RegDate>> dateStatus) {
		this.inscription = inscription;
		this.buts = buts;
		this.dateStatus = dateStatus;
	}

	@Override
	public DonneesRCRCEnt build() {
		return new DonneesRCRCEnt(adresseLegale, inscription, capital, buts, dateStatus, entreesJournalRC);
	}

	public DonneesRCBuilder addInscription(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull InscriptionRC valeur) {
		inscription = BuilderHelper.addValueToList(inscription, new DateRanged<>(dateDebut, dateDeFin, valeur));
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

	public DonneesRCBuilder addEntreeJournalRC(@NotNull EntreeJournalRC entree) {
		this.entreesJournalRC = BuilderHelper.addValueToList(this.entreesJournalRC, entree);
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

	public DonneesRCBuilder entreesJournalRC(List<EntreeJournalRC> entreesJournalRC) {
		this.entreesJournalRC = entreesJournalRC;
		return this;
	}

}