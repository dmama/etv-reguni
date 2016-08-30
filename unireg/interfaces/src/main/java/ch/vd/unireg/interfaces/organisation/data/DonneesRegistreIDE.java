package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public interface DonneesRegistreIDE {

	List<AdresseBoitePostaleRCEnt> getAdresseBoitePostale();

	List<AdresseEffectiveRCEnt> getAdresseEffective();

	AdresseEffectiveRCEnt getAdresseEffective(RegDate date);

	List<DateRanged<RaisonDeRadiationRegistreIDE>> getRaisonDeLiquidation();

	List<DateRanged<StatusRegistreIDE>> getStatus();

	StatusRegistreIDE getStatus(RegDate date);

	List<DateRanged<TypeOrganisationRegistreIDE>> getTypeOrganisation();
}
