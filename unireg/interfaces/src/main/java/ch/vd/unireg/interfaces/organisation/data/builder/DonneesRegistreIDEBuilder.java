package ch.vd.unireg.interfaces.organisation.data.builder;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.Adresse;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.RaisonLiquidationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;

public class DonneesRegistreIDEBuilder implements DataBuilder<DonneesRegistreIDE> {

	@NotNull
	private final List<DateRanged<StatusRegistreIDE>> status;
	private List<DateRanged<TypeOrganisationRegistreIDE>> typeOrganisation;
	private List<DateRanged<Adresse>> adresseEffective;
	private List<DateRanged<Adresse>> adresseBoitePostale;
	private List<DateRanged<RaisonLiquidationRegistreIDE>> raisonDeLiquidation;

	public DonneesRegistreIDEBuilder(@NotNull List<DateRanged<StatusRegistreIDE>> status) {
		this.status = status;
	}

	@Override
	public DonneesRegistreIDE build() {
		return new DonneesRegistreIDE(adresseBoitePostale, status, typeOrganisation, adresseEffective, raisonDeLiquidation);
	}

	public void setAdresseBoitePostale(List<DateRanged<Adresse>> adresseBoitePostale) {
		this.adresseBoitePostale = adresseBoitePostale;
	}

	public void setAdresseEffective(List<DateRanged<Adresse>> adresseEffective) {
		this.adresseEffective = adresseEffective;
	}

	public void setRaisonDeLiquidation(List<DateRanged<RaisonLiquidationRegistreIDE>> raisonDeLiquidation) {
		this.raisonDeLiquidation = raisonDeLiquidation;
	}

	public void setTypeOrganisation(List<DateRanged<TypeOrganisationRegistreIDE>> typeOrganisation) {
		this.typeOrganisation = typeOrganisation;
	}
}
