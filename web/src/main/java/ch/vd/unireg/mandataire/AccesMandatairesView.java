package ch.vd.unireg.mandataire;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.Tiers;

/**
 * Bean qui concentre le résultat de la configuration mandataire pour un tiers donné
 */
public class AccesMandatairesView {

	private final ConfigurationMandataire.Acces general;
	private final ConfigurationMandataire.Acces tiersPerception;
	private final Map<String, ConfigurationMandataire.Acces> special;

	public AccesMandatairesView(Tiers tiers, ConfigurationMandataire configuration, ServiceInfrastructureService infraService) {
		final List<GenreImpotMandataire> genresImpot = infraService.getGenresImpotMandataires();
		if (tiers == null) {
			this.general = ConfigurationMandataire.Acces.AUCUN;
			this.tiersPerception = ConfigurationMandataire.Acces.AUCUN;
			this.special = Collections.unmodifiableMap(genresImpot.stream()
					                                           .map(GenreImpotMandataire::getCode)
					                                           .collect(Collectors.toMap(Function.identity(), code -> ConfigurationMandataire.Acces.AUCUN)));
		}
		else {
			this.general = configuration.getAffichageMandatGeneral(tiers);
			this.tiersPerception = configuration.getAffichageMandatTiers(tiers);
			this.special = Collections.unmodifiableMap(genresImpot.stream()
					                                           .map(genre -> Pair.of(genre.getCode(), configuration.getAffichageMandatSpecial(tiers, genre)))
					                                           .collect(Collectors.toMap(Pair::getKey, Pair::getValue)));

		}
	}

	/**
	 * @return <code>true</code> si au moins un des accès n'est pas "AUCUN"
	 */
	public boolean hasAnything() {
		return hasGeneral() || hasTiersPerception() || hasSpecial();
	}

	public boolean hasGeneral() {
		return general != null && general != ConfigurationMandataire.Acces.AUCUN;
	}

	public boolean hasGeneralInEdition() {
		return general == ConfigurationMandataire.Acces.EDITION_POSSIBLE;
	}

	public boolean hasTiersPerception() {
		return tiersPerception != null && tiersPerception != ConfigurationMandataire.Acces.AUCUN;
	}

	public boolean hasTiersPerceptionInEdition() {
		return tiersPerception == ConfigurationMandataire.Acces.EDITION_POSSIBLE;
	}

	public boolean hasSpecial() {
		return special.values().stream().anyMatch(acces -> acces != ConfigurationMandataire.Acces.AUCUN);
	}

	public boolean hasSpecialInEdition() {
		return special.values().stream().anyMatch(acces -> acces == ConfigurationMandataire.Acces.EDITION_POSSIBLE);
	}

	public boolean hasSpecial(String genreImpot) {
		return special.getOrDefault(genreImpot, ConfigurationMandataire.Acces.AUCUN) != ConfigurationMandataire.Acces.AUCUN;
	}

	public boolean hasSpecialInEdition(String genreImpot) {
		return special.getOrDefault(genreImpot, ConfigurationMandataire.Acces.AUCUN) == ConfigurationMandataire.Acces.EDITION_POSSIBLE;
	}
}
