package ch.vd.unireg.parametrage;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocumentEmolument;

public class ParametrePeriodeFiscaleDAOImpl extends BaseDAOImpl<ParametrePeriodeFiscale, Long> implements ParametrePeriodeFiscaleDAO {

	public ParametrePeriodeFiscaleDAOImpl() {
		super(ParametrePeriodeFiscale.class);
	}

	@Override
	public List<ParametrePeriodeFiscale> getByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return find("FROM PeriodeParametreFiscale p WHERE p.periodeFiscale = :pf", buildNamedParameters(Pair.of("pf", periodeFiscale)), null);
	}

	@Override
	@Nullable
	public ParametreDemandeDelaisOnline getParamsDemandeDelaisOnline(int periodeFiscale, ParametreDemandeDelaisOnline.@NotNull Type type) {
		final Query query = getCurrentSession().createQuery("from ParametreDemandeDelaisOnline p where annulationDate is null and periodefiscale.annee = :pf and typeTiers = :typeTiers");
		query.setParameter("pf", periodeFiscale);
		query.setParameter("typeTiers", type);
		return (ParametreDemandeDelaisOnline) query.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	private ParametrePeriodeFiscalePP getPPByPeriodeFiscaleAndTypeContribuable(PeriodeFiscale periodeFiscale, TypeContribuable typeCtb) {
		final List<ParametrePeriodeFiscalePP> list = find("FROM ParametrePeriodeFiscalePP p WHERE p.periodefiscale = :pf and p.typeContribuable = :typeCtb",
		                                                  buildNamedParameters(Pair.of("pf", periodeFiscale),
		                                                                       Pair.of("typeCtb", typeCtb)),
		                                                  null);
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	@SuppressWarnings("unchecked")
	private ParametrePeriodeFiscalePM getPMByPeriodeFiscaleAndTypeContribuable(PeriodeFiscale periodeFiscale, TypeContribuable typeCtb) {
		final List<ParametrePeriodeFiscalePM> list = find("FROM ParametrePeriodeFiscalePM p WHERE p.periodefiscale = :pf and p.typeContribuable = :typeCtb",
		                                                  buildNamedParameters(Pair.of("pf", periodeFiscale),
		                                                                       Pair.of("typeCtb", typeCtb)),
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

	@Override
	public ParametrePeriodeFiscaleSNC getSNCByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		final List<ParametrePeriodeFiscaleSNC> list = find("FROM ParametrePeriodeFiscaleSNC p WHERE p.periodefiscale = :pf",
		                                                   buildNamedParameters(Pair.<String, Object>of("pf", periodeFiscale)),
		                                                   null);
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	private ParametrePeriodeFiscaleEmolument getEmolumentByPeriodeFiscaleEtTypeDocument(PeriodeFiscale periodeFiscale, TypeDocumentEmolument typeDocument) {
		final List<ParametrePeriodeFiscaleEmolument> list = find("FROM ParametrePeriodeFiscaleEmolument p WHERE p.periodefiscale = :pf AND p.typeDocument = :typeDoc",
		                                                         buildNamedParameters(Pair.of("pf", periodeFiscale),
		                                                                              Pair.of("typeDoc", typeDocument)),
		                                                         null);
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	@Override
	public ParametrePeriodeFiscaleEmolument getEmolumentSommationDIPPByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		return getEmolumentByPeriodeFiscaleEtTypeDocument(periodeFiscale, TypeDocumentEmolument.SOMMATION_DI_PP);
	}
}
