package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForPrincipal;

public class ForPrincipalOuvertApresFinAssujettissementLoggedElement implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Arrays.asList(LoggedElementAttribute.ENTREPRISE_ID,
	                                                                                                     LoggedElementAttribute.RAISON_SOCIALE,
	                                                                                                     LoggedElementAttribute.NOM_COMMUNE,
	                                                                                                     LoggedElementAttribute.DATE_DEBUT_FOR,
	                                                                                                     LoggedElementAttribute.DATE_FIN_ASSUJETTISSEMENT));

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	private final Map<LoggedElementAttribute, Object> values;

	public ForPrincipalOuvertApresFinAssujettissementLoggedElement(RegpmEntreprise entreprise, RegpmForPrincipal forPrincipal, RegDate finAssujettissement) {
		this.values = buildItemValues(entreprise, forPrincipal, finAssujettissement);
	}

	@NotNull
	private static Map<LoggedElementAttribute, Object> buildItemValues(RegpmEntreprise entreprise, RegpmForPrincipal forPrincipal, RegDate finAssujettissement) {
		// on prend la dernière raison sociale officielle de l'entreprise
		final String raisonSociale = Stream.of(entreprise.getRaisonSociale1(), entreprise.getRaisonSociale2(), entreprise.getRaisonSociale3())
				.filter(Objects::nonNull)
				.collect(Collectors.joining(" "));

		final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ENTREPRISE_ID, entreprise.getId());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RAISON_SOCIALE, raisonSociale);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.NOM_COMMUNE, forPrincipal.getCommune().getNom());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.DATE_DEBUT_FOR, forPrincipal.getDateValidite());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.DATE_FIN_ASSUJETTISSEMENT, finAssujettissement);
		return Collections.unmodifiableMap(map);
	}

	@NotNull
	@Override
	public List<LoggedElementAttribute> getItems() {
		return NAMES;
	}

	@NotNull
	@Override
	public Map<LoggedElementAttribute, Object> getItemValues() {
		return values;
	}
}
