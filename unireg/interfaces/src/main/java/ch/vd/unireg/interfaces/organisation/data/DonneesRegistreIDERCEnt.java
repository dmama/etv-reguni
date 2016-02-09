package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.List;

import ch.vd.registre.base.date.RegDate;

/**
 *   Utilisez les méthodes des helpers pour produire les données des accesseurs.
 *
 *   OrganisationHelper fournit les méthodes nécessaires à l'accès par date:
 *   valuesForDate(), valueForDate() et dateRangeForDate(), à utiliser en priorité.
 */
public class DonneesRegistreIDERCEnt implements DonneesRegistreIDE, Serializable {

	private static final long serialVersionUID = -6444917918127810542L;

	private final List<DateRanged<StatusRegistreIDE>> status;
	private final List<DateRanged<TypeOrganisationRegistreIDE>> typeOrganisation;
	private final List<AdresseRCEnt> adresseEffective;
	private final List<AdresseRCEnt> adresseBoitePostale;
	private final List<DateRanged<RaisonLiquidationRegistreIDE>> raisonDeLiquidation;

	public DonneesRegistreIDERCEnt(List<AdresseRCEnt> adresseBoitePostale,
	                          List<DateRanged<StatusRegistreIDE>> status,
	                          List<DateRanged<TypeOrganisationRegistreIDE>> typeOrganisation,
	                          List<AdresseRCEnt> adresseEffective,
	                          List<DateRanged<RaisonLiquidationRegistreIDE>> raisonDeLiquidation) {
		this.adresseBoitePostale = adresseBoitePostale;
		this.status = status;
		this.typeOrganisation = typeOrganisation;
		this.adresseEffective = adresseEffective;
		this.raisonDeLiquidation = raisonDeLiquidation;
	}

	@Override
	public List<AdresseRCEnt> getAdresseBoitePostale() {
		return adresseBoitePostale;
	}

	@Override
	public List<AdresseRCEnt> getAdresseEffective() {
		return adresseEffective;
	}

	@Override
	public List<DateRanged<RaisonLiquidationRegistreIDE>> getRaisonDeLiquidation() {
		return raisonDeLiquidation;
	}

	@Override
	public List<DateRanged<StatusRegistreIDE>> getStatus() {
		return status;
	}

	@Override
	public StatusRegistreIDE getStatus(RegDate date) {
		return OrganisationHelper.valueForDate(status, date);
	}

	@Override
	public List<DateRanged<TypeOrganisationRegistreIDE>> getTypeOrganisation() {
		return typeOrganisation;
	}
}
