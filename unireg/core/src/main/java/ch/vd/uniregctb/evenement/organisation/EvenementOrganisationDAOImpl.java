package ch.vd.uniregctb.evenement.organisation;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

public class EvenementOrganisationDAOImpl extends AbstractEvenementOrganisationDAOImpl<EvenementOrganisation, TypeEvenementOrganisation> implements EvenementOrganisationDAO {

	private static final Set<EtatEvenementOrganisation> ETATS_NON_TRAITES;

	static {
		final Set<EtatEvenementOrganisation> etats = EnumSet.noneOf(EtatEvenementOrganisation.class);
		for (EtatEvenementOrganisation etat : EtatEvenementOrganisation.values()) {
			if (!etat.isTraite()) {
				etats.add(etat);
			}
		}
		ETATS_NON_TRAITES = Collections.unmodifiableSet(etats);
	}

	public EvenementOrganisationDAOImpl() {
		super(EvenementOrganisation.class);
	}

	@Override
	public List<EvenementOrganisation> getEvenementsOrganisationNonTraites(Collection<Long> nosOrganisation) {
		return getEvenementsOrganisationNonTraites(nosOrganisation, true);

	}

	@Override
	public List<EvenementOrganisation> getEvenementsPourOrganisation(long noOrganisation) {
		return getEvenementsOrganisationNonTraites(Collections.singletonList(noOrganisation));
	}

	@SuppressWarnings("unchecked")
	private List<EvenementOrganisation> getEvenementsOrganisationNonTraites(Collection<Long> nosOrganisation, boolean nonTraitesSeulement) {
		//final String hql = "from EvenementOrganisation as ec where ec.annulationDate is null and ec.noOrganisation in (:nosOrganisation)" + (nonTraitesSeulement ? " and ec.etat in (:etats)" : StringUtils.EMPTY);
		Criteria query = getCurrentSession().createCriteria(EvenementOrganisation.class, "eo");
		query.add(Restrictions.isNull("annulationDate"));
		query.add(Restrictions.in("nosOrganisation", nosOrganisation));
		if (nonTraitesSeulement) {
			query.add(Restrictions.in("etat", ETATS_NON_TRAITES));
		}
		return query.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<EvenementOrganisation> getEvenementsOrganisationARelancer() {
		// final String hql = "from EvenementOrganisation as ec where ec.annulationDate is null and ec.etat = :etat";
		Criteria query = getCurrentSession().createCriteria(EvenementOrganisation.class, "eo");
		query.add(Restrictions.isNull("annulationDate"));
		query.add(Restrictions.eq("etat", EtatEvenementOrganisation.A_TRAITER));
		return query.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<Long> getOrganisationsConcerneesParEvenementsPourRetry() {
		//final String hql = "select distinct ec.noOrganisation from EvenementOrganisation ec where ec.annulationDate is null and ec.noOrganisation is not null and ec.etat in (:etats)";
		Criteria query = getCurrentSession().createCriteria(EvenementOrganisation.class, "eo");
		query.add(Restrictions.isNull("annulationDate"));
		query.add(Restrictions.isNotNull("noOrganisation"));
		query.add(Restrictions.in("etat", ETATS_NON_TRAITES));
		query.setProjection(Projections.property("noOrganisation"));
		query.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return new HashSet<>(query.list());
	}

	@Override
	public List<EvenementOrganisation> find(final EvenementOrganisationCriteria<TypeEvenementOrganisation> criterion, @Nullable final ParamPagination paramPagination) {
		return genericFind(criterion, paramPagination);
	}

	@Override
	public int count(EvenementOrganisationCriteria<TypeEvenementOrganisation> criterion) {
		return genericCount(criterion);
	}


	@Override
	protected Class getEvenementOrganisationClass() {
		return EvenementOrganisation.class;
	}
}
