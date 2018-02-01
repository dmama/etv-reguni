package ch.vd.unireg.mandataire;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.tiers.Tiers;

public class MockConfigurationMandataire implements ConfigurationMandataire {

	@Override
	public Acces getAffichageMandatGeneral(@NotNull Tiers tiers) {
		return Acces.AUCUN;
	}

	@Override
	public Acces getAffichageMandatTiers(@NotNull Tiers tiers) {
		return Acces.AUCUN;
	}

	@Override
	public Acces getAffichageMandatSpecial(@NotNull Tiers tiers, @NotNull GenreImpotMandataire genreImpotMandataire) {
		return Acces.AUCUN;
	}

	@Override
	public boolean isCreationRapportEntreTiersAutoriseePourMandatsCourrier() {
		return false;
	}
}
