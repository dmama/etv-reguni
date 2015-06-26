package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;

import org.jetbrains.annotations.NotNull;

public class DonneesRegistreIDE {
	@NotNull
	private final List<DateRanged<StatusRegistreIDE>> status;
	private final List<DateRanged<TypeOrganisationRegistreIDE>> typeOrganisation;
	private final List<DateRanged<Adresse>> adresseEffective;
	private final List<DateRanged<Adresse>> adresseBoitePostale;
	private final List<DateRanged<RaisonLiquidationRegistreIDE>> raisonDeLiquidation;

	public DonneesRegistreIDE(List<DateRanged<Adresse>> adresseBoitePostale,
	                          @NotNull List<DateRanged<StatusRegistreIDE>> status,
	                          List<DateRanged<TypeOrganisationRegistreIDE>> typeOrganisation,
	                          List<DateRanged<Adresse>> adresseEffective,
	                          List<DateRanged<RaisonLiquidationRegistreIDE>> raisonDeLiquidation) {
		this.adresseBoitePostale = adresseBoitePostale;
		this.status = status;
		this.typeOrganisation = typeOrganisation;
		this.adresseEffective = adresseEffective;
		this.raisonDeLiquidation = raisonDeLiquidation;
	}

	public List<DateRanged<Adresse>> getAdresseBoitePostale() {
		return adresseBoitePostale;
	}

	public List<DateRanged<Adresse>> getAdresseEffective() {
		return adresseEffective;
	}

	public List<DateRanged<RaisonLiquidationRegistreIDE>> getRaisonDeLiquidation() {
		return raisonDeLiquidation;
	}

	@NotNull
	public List<DateRanged<StatusRegistreIDE>> getStatus() {
		return status;
	}

	public List<DateRanged<TypeOrganisationRegistreIDE>> getTypeOrganisation() {
		return typeOrganisation;
	}
}
