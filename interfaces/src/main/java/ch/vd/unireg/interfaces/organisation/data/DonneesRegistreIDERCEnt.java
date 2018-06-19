package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.List;

import ch.vd.registre.base.date.RegDate;

/**
 *   Utilisez les méthodes des helpers pour produire les données des accesseurs.
 *
 *   EntrepriseHelper fournit les méthodes nécessaires à l'accès par date:
 *   valuesForDate(), valueForDate() et dateRangeForDate(), à utiliser en priorité.
 */
public class DonneesRegistreIDERCEnt implements DonneesRegistreIDE, Serializable {

	private static final long serialVersionUID = 8992760212437586681L;

	private final List<DateRanged<StatusRegistreIDE>> status;
	private final List<DateRanged<TypeEntrepriseRegistreIDE>> typeEntreprise;
	private final List<AdresseEffectiveRCEnt> adresseEffective;
	private final List<AdresseBoitePostaleRCEnt> adresseBoitePostale;
	private final List<DateRanged<RaisonDeRadiationRegistreIDE>> raisonDeLiquidation;

	public DonneesRegistreIDERCEnt(List<AdresseBoitePostaleRCEnt> adresseBoitePostale,
	                          List<DateRanged<StatusRegistreIDE>> status,
	                          List<DateRanged<TypeEntrepriseRegistreIDE>> typeEntreprise,
	                          List<AdresseEffectiveRCEnt> adresseEffective,
	                          List<DateRanged<RaisonDeRadiationRegistreIDE>> raisonDeLiquidation) {
		this.adresseBoitePostale = adresseBoitePostale;
		this.status = status;
		this.typeEntreprise = typeEntreprise;
		this.adresseEffective = adresseEffective;
		this.raisonDeLiquidation = raisonDeLiquidation;
	}

	@Override
	public List<AdresseBoitePostaleRCEnt> getAdresseBoitePostale() {
		return adresseBoitePostale;
	}

	@Override
	public List<AdresseEffectiveRCEnt> getAdresseEffective() {
		return adresseEffective;
	}

	@Override
	public AdresseEffectiveRCEnt getAdresseEffective(RegDate date) {
		return EntrepriseHelper.dateRangeForDate(adresseEffective, date);
	}

	@Override
	public List<DateRanged<RaisonDeRadiationRegistreIDE>> getRaisonDeLiquidation() {
		return raisonDeLiquidation;
	}

	@Override
	public List<DateRanged<StatusRegistreIDE>> getStatus() {
		return status;
	}

	@Override
	public StatusRegistreIDE getStatus(RegDate date) {
		return EntrepriseHelper.valueForDate(status, date);
	}

	@Override
	public List<DateRanged<TypeEntrepriseRegistreIDE>> getTypeEntreprise() {
		return typeEntreprise;
	}
}
