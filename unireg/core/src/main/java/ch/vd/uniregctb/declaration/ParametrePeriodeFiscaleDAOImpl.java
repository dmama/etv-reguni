package ch.vd.uniregctb.declaration;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.type.TypeContribuable;

public class ParametrePeriodeFiscaleDAOImpl extends BaseDAOImpl<ParametrePeriodeFiscale, Long> implements ParametrePeriodeFiscaleDAO {

	public ParametrePeriodeFiscaleDAOImpl() {
		super(ParametrePeriodeFiscale.class);
	}

	@Override
	public List<ParametrePeriodeFiscale> getByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return find("FROM PeriodeParametreFiscale p WHERE p.periodeFiscale = :pf", buildNamedParameters(Pair.of("pf", periodeFiscale)), null);
	}

	@SuppressWarnings("unchecked")
	private ParametrePeriodeFiscale getByPeriodeFiscaleAndTypeContribuable(PeriodeFiscale periodeFiscale, TypeContribuable typeCtb) {
		final List<ParametrePeriodeFiscale> list = find("FROM ParametrePeriodeFiscale p WHERE p.periodefiscale = :pf and p.typeContribuable = :typeCtb",
		                                                buildNamedParameters(Pair.<String, Object>of("pf", periodeFiscale),
		                                                                     Pair.<String, Object>of("typeCtb", typeCtb)),
		                                                null);
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	@Override
	public ParametrePeriodeFiscale getDepenseByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getByPeriodeFiscaleAndTypeContribuable(periodeFiscale, TypeContribuable.VAUDOIS_DEPENSE);
	}

	@Override
	public ParametrePeriodeFiscale getHorsCantonByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getByPeriodeFiscaleAndTypeContribuable(periodeFiscale, TypeContribuable.HORS_CANTON);
	}

	@Override
	public ParametrePeriodeFiscale getHorsSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getByPeriodeFiscaleAndTypeContribuable(periodeFiscale, TypeContribuable.HORS_SUISSE);
	}

	@Override
	public ParametrePeriodeFiscale getDiplomateSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getByPeriodeFiscaleAndTypeContribuable(periodeFiscale, TypeContribuable.DIPLOMATE_SUISSE);
	}
	
	@Override
	public ParametrePeriodeFiscale getVaudByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getByPeriodeFiscaleAndTypeContribuable(periodeFiscale, TypeContribuable.VAUDOIS_ORDINAIRE);
	}

}
