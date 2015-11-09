package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 *   Utilisez les méthodes des helpers pour produire les données des accesseurs.
 *
 *   OrganisationHelper fournit les méthodes nécessaires à l'accès par date:
 *   valuesForDate(), valueForDate() et dateRangeForDate(), à utiliser en priorité.
 */
public class DonneesRegistreIDE implements Serializable {

	private static final long serialVersionUID = -6444917918127810542L;

	@NotNull
	private final List<DateRanged<StatusRegistreIDE>> status;
	private final List<DateRanged<TypeOrganisationRegistreIDE>> typeOrganisation;
	private final List<AdresseRCEnt> adresseEffective;
	private final List<AdresseRCEnt> adresseBoitePostale;
	private final List<DateRanged<RaisonLiquidationRegistreIDE>> raisonDeLiquidation;

	public DonneesRegistreIDE(List<AdresseRCEnt> adresseBoitePostale,
	                          @NotNull List<DateRanged<StatusRegistreIDE>> status,
	                          List<DateRanged<TypeOrganisationRegistreIDE>> typeOrganisation,
	                          List<AdresseRCEnt> adresseEffective,
	                          List<DateRanged<RaisonLiquidationRegistreIDE>> raisonDeLiquidation) {
		this.adresseBoitePostale = adresseBoitePostale;
		this.status = status;
		this.typeOrganisation = typeOrganisation;
		this.adresseEffective = adresseEffective;
		this.raisonDeLiquidation = raisonDeLiquidation;
	}

	public List<AdresseRCEnt> getAdresseBoitePostale() {
		return adresseBoitePostale;
	}

	public List<AdresseRCEnt> getAdresseEffective() {
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
