package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmGenreContribution;

public class GenreContributionUserType extends EnumCharMappingUserType<RegpmGenreContribution> {

	private static final Map<String, RegpmGenreContribution> MAPPING = buildMapping();

	private static Map<String, RegpmGenreContribution> buildMapping() {
		final Map<String, RegpmGenreContribution> map = new HashMap<>();
		map.put("1", RegpmGenreContribution.PP);
		map.put("2", RegpmGenreContribution.PM);
		map.put("3", RegpmGenreContribution.PP_PM);
		return map;
	}

	public GenreContributionUserType() {
		super(RegpmGenreContribution.class, MAPPING);
	}
}
