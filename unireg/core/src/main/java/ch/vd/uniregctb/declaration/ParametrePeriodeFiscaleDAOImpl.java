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
	private ParametrePeriodeFiscalePP getPPByPeriodeFiscaleAndTypeContribuable(PeriodeFiscale periodeFiscale, TypeContribuable typeCtb) {
		final List<ParametrePeriodeFiscalePP> list = find("FROM ParametrePeriodeFiscalePP p WHERE p.periodefiscale = :pf and p.typeContribuable = :typeCtb",
		                                                  buildNamedParameters(Pair.<String, Object>of("pf", periodeFiscale),
		                                                                       Pair.<String, Object>of("typeCtb", typeCtb)),
		                                                  null);
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	@SuppressWarnings("unchecked")
	private ParametrePeriodeFiscalePM getPMByPeriodeFiscaleAndTypeContribuable(PeriodeFiscale periodeFiscale, TypeContribuable typeCtb) {
		final List<ParametrePeriodeFiscalePM> list = find("FROM ParametrePeriodeFiscalePM p WHERE p.periodefiscale = :pf and p.typeContribuable = :typeCtb",
		                                                  buildNamedParameters(Pair.<String, Object>of("pf", periodeFiscale),
		                                                                       Pair.<String, Object>of("typeCtb", typeCtb)),
		                                                  null);
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	@Override
	public ParametrePeriodeFiscalePP getPPDepenseByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getPPByPeriodeFiscaleAndTypeContribuable(periodeFiscale, TypeContribuable.VAUDOIS_DEPENSE);
	}

	@Override
	public ParametrePeriodeFiscalePP getPPHorsCantonByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getPPByPeriodeFiscaleAndTypeContribuable(periodeFiscale, TypeContribuable.HORS_CANTON);
	}

	@Override
	public ParametrePeriodeFiscalePP getPPHorsSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getPPByPeriodeFiscaleAndTypeContribuable(periodeFiscale, TypeContribuable.HORS_SUISSE);
	}

	@Override
	public ParametrePeriodeFiscalePP getPPDiplomateSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getPPByPeriodeFiscaleAndTypeContribuable(periodeFiscale, TypeContribuable.DIPLOMATE_SUISSE);
	}
	
	@Override
	public ParametrePeriodeFiscalePP getPPVaudByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getPPByPeriodeFiscaleAndTypeContribuable(periodeFiscale, TypeContribuable.VAUDOIS_ORDINAIRE);
	}

	@Override
	public ParametrePeriodeFiscalePM getPMVaudByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getPMByPeriodeFiscaleAndTypeContribuable(periodeFiscale, TypeContribuable.VAUDOIS_ORDINAIRE);
	}

	@Override
	public ParametrePeriodeFiscalePM getPMHorsCantonByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getPMByPeriodeFiscaleAndTypeContribuable(periodeFiscale, TypeContribuable.HORS_CANTON);
	}

	@Override
	public ParametrePeriodeFiscalePM getPMHorsSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getPMByPeriodeFiscaleAndTypeContribuable(periodeFiscale, TypeContribuable.HORS_SUISSE);
	}

	@Override
	public ParametrePeriodeFiscalePM getPMUtilitePubliqueByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getPMByPeriodeFiscaleAndTypeContribuable(periodeFiscale, TypeContribuable.UTILITE_PUBLIQUE);
	}
}
