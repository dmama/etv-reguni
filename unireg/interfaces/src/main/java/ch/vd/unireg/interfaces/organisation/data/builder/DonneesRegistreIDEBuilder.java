package ch.vd.unireg.interfaces.organisation.data.builder;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.AdresseBoitePostaleRCEnt;
import ch.vd.unireg.interfaces.organisation.data.AdresseEffectiveRCEnt;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDERCEnt;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeRadiationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;

public class DonneesRegistreIDEBuilder implements DataBuilder<DonneesRegistreIDE> {

	private List<DateRanged<StatusRegistreIDE>> status;
	private List<DateRanged<TypeOrganisationRegistreIDE>> typeOrganisation;
	private List<AdresseEffectiveRCEnt> adresseEffective;
	private List<AdresseBoitePostaleRCEnt> adresseBoitePostale;
	private List<DateRanged<RaisonDeRadiationRegistreIDE>> raisonDeLiquidation;

	public DonneesRegistreIDEBuilder() {}

	public DonneesRegistreIDEBuilder(List<DateRanged<StatusRegistreIDE>> status) {
		this.status = status;
	}

	@Override
	public DonneesRegistreIDE build() {
		return new DonneesRegistreIDERCEnt(adresseBoitePostale, status, typeOrganisation, adresseEffective, raisonDeLiquidation);
	}

	public DonneesRegistreIDEBuilder addStatus(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull StatusRegistreIDE valeur) {
		status = BuilderHelper.addValueToList(status, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRegistreIDEBuilder addTypeOrganisation(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull TypeOrganisationRegistreIDE valeur) {
		typeOrganisation = BuilderHelper.addValueToList(typeOrganisation, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRegistreIDEBuilder addAdresseEffective(@NotNull AdresseEffectiveRCEnt valeur) {
		adresseEffective = BuilderHelper.addValueToList(adresseEffective, valeur);
		return this;
	}

	public DonneesRegistreIDEBuilder addAdresseBoitePostale(@NotNull AdresseBoitePostaleRCEnt valeur) {
		adresseBoitePostale = BuilderHelper.addValueToList(adresseBoitePostale, valeur);
		return this;
	}

	public DonneesRegistreIDEBuilder addRaisonDeLiquidation(@NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull RaisonDeRadiationRegistreIDE valeur) {
		raisonDeLiquidation = BuilderHelper.addValueToList(raisonDeLiquidation, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public DonneesRegistreIDEBuilder withAdresseBoitePostale(List<AdresseBoitePostaleRCEnt> adresseBoitePostale) {
		this.adresseBoitePostale = adresseBoitePostale;
		return this;
	}

	public DonneesRegistreIDEBuilder withAdresseEffective(List<AdresseEffectiveRCEnt> adresseEffective) {
		this.adresseEffective = adresseEffective;
		return this;
	}

	public DonneesRegistreIDEBuilder withRaisonDeLiquidation(List<DateRanged<RaisonDeRadiationRegistreIDE>> raisonDeLiquidation) {
		this.raisonDeLiquidation = raisonDeLiquidation;
		return this;
	}

	public DonneesRegistreIDEBuilder withTypeOrganisation(List<DateRanged<TypeOrganisationRegistreIDE>> typeOrganisation) {
		this.typeOrganisation = typeOrganisation;
		return this;
	}
}
