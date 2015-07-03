package ch.vd.unireg.interfaces.organisation.data.builder;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Adresse;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.RaisonLiquidationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;

public class DonneesRegistreIDEBuilder implements DataBuilder<DonneesRegistreIDE> {

	private List<DateRanged<StatusRegistreIDE>> status;
	private List<DateRanged<TypeOrganisationRegistreIDE>> typeOrganisation;
	private List<DateRanged<Adresse>> adresseEffective;
	private List<DateRanged<Adresse>> adresseBoitePostale;
	private List<DateRanged<RaisonLiquidationRegistreIDE>> raisonDeLiquidation;

	public DonneesRegistreIDEBuilder() {}

	public DonneesRegistreIDEBuilder(@NotNull List<DateRanged<StatusRegistreIDE>> status) {
		this.status = status;
	}

	@Override
	public DonneesRegistreIDE build() {
		return new DonneesRegistreIDE(adresseBoitePostale, status, typeOrganisation, adresseEffective, raisonDeLiquidation);
	}

	public DonneesRegistreIDEBuilder addStatus(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull StatusRegistreIDE valeur) {
		BuilderHelper.addValueToList(status, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRegistreIDEBuilder addTypeOrganisation(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull TypeOrganisationRegistreIDE valeur) {
		BuilderHelper.addValueToList(typeOrganisation, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRegistreIDEBuilder addAdresseEffective(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Adresse valeur) {
		BuilderHelper.addValueToList(adresseEffective, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRegistreIDEBuilder addAdresseBoitePostale(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Adresse valeur) {
		BuilderHelper.addValueToList(adresseBoitePostale, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRegistreIDEBuilder addRaisonDeLiquidation(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull RaisonLiquidationRegistreIDE valeur) {
		BuilderHelper.addValueToList(raisonDeLiquidation, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRegistreIDEBuilder withAdresseBoitePostale(List<DateRanged<Adresse>> adresseBoitePostale) {
		this.adresseBoitePostale = adresseBoitePostale;
		return this;
	}

	public DonneesRegistreIDEBuilder withAdresseEffective(List<DateRanged<Adresse>> adresseEffective) {
		this.adresseEffective = adresseEffective;
		return this;
	}

	public DonneesRegistreIDEBuilder withRaisonDeLiquidation(List<DateRanged<RaisonLiquidationRegistreIDE>> raisonDeLiquidation) {
		this.raisonDeLiquidation = raisonDeLiquidation;
		return this;
	}

	public DonneesRegistreIDEBuilder withTypeOrganisation(List<DateRanged<TypeOrganisationRegistreIDE>> typeOrganisation) {
		this.typeOrganisation = typeOrganisation;
		return this;
	}
}
