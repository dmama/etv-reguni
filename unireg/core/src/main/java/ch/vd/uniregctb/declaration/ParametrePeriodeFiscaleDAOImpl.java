package ch.vd.uniregctb.declaration;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.uniregctb.type.TypeContribuable;

public class ParametrePeriodeFiscaleDAOImpl extends GenericDAOImpl<ParametrePeriodeFiscale, Long> implements ParametrePeriodeFiscaleDAO {

	public ParametrePeriodeFiscaleDAOImpl() {
		super(ParametrePeriodeFiscale.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ParametrePeriodeFiscale> getByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		List list = find(
				"FROM PeriodeParametreFiscale p WHERE p.periodeFiscale = ?", 
				new Object[] {periodeFiscale}, 
				null);
		Collections.sort(
				list,
				new Comparator<ModeleDocument>() {
					@Override
					public int compare(ModeleDocument o1, ModeleDocument o2) {
						return o1.getTypeDocument().compareTo(o2.getTypeDocument());
					}}
		);
		return (List<ParametrePeriodeFiscale>) list;
			
	}

	@SuppressWarnings("unchecked")
	private ParametrePeriodeFiscale getByPeriodeFiscaleAndTypeContribuable(PeriodeFiscale periodeFiscale, TypeContribuable typeCtb) {
		final List<ParametrePeriodeFiscale> list =
				(List<ParametrePeriodeFiscale>) find("FROM ParametrePeriodeFiscale p WHERE p.periodefiscale = ? and p.typeContribuable = ?", new Object[]{periodeFiscale, typeCtb.name()}, null);
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
