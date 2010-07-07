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

	@SuppressWarnings("unchecked")
	public List<ParametrePeriodeFiscale> getByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		List list = find(
				"FROM PeriodeParametreFiscale p WHERE p.periodeFiscale = ?", 
				new Object[] {periodeFiscale}, 
				null);
		Collections.sort(
				list,
				new Comparator<ModeleDocument>() {
					public int compare(ModeleDocument o1, ModeleDocument o2) {
						return o1.getTypeDocument().compareTo(o2.getTypeDocument());
					}}
		);
		return (List<ParametrePeriodeFiscale>) list;
			
	}

	@SuppressWarnings("unchecked")
	private ParametrePeriodeFiscale getByPeriodeFiscaleAndTypeContribual(PeriodeFiscale periodeFiscale, TypeContribuable typeCtb) {
		final List<ParametrePeriodeFiscale> list =
				(List<ParametrePeriodeFiscale>) find("FROM ParametrePeriodeFiscale p WHERE p.periodefiscale = ? and p.typeContribuable = ?", new Object[]{periodeFiscale, typeCtb.name()}, null);
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	public ParametrePeriodeFiscale getDepenseByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getByPeriodeFiscaleAndTypeContribual(periodeFiscale, TypeContribuable.VAUDOIS_DEPENSE);
	}

	public ParametrePeriodeFiscale getHorsCantonByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getByPeriodeFiscaleAndTypeContribual(periodeFiscale, TypeContribuable.HORS_CANTON);
	}

	public ParametrePeriodeFiscale getHorsSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getByPeriodeFiscaleAndTypeContribual(periodeFiscale, TypeContribuable.HORS_SUISSE);
	}

	public ParametrePeriodeFiscale getVaudByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getByPeriodeFiscaleAndTypeContribual(periodeFiscale, TypeContribuable.VAUDOIS_ORDINAIRE);
	}

}
