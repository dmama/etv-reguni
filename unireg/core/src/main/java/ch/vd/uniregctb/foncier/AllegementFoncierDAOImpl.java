package ch.vd.uniregctb.foncier;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Query;
import org.hibernate.Session;

import ch.vd.uniregctb.common.BaseDAOImpl;

public class AllegementFoncierDAOImpl extends BaseDAOImpl<AllegementFoncier, Long> implements AllegementFoncierDAO {

	public AllegementFoncierDAOImpl() {
		super(AllegementFoncier.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends AllegementFoncier> List<T> getAllegementsFonciers(long idContribuable, long idImmeuble, Class<T> clazz) {
		final Session session = getCurrentSession();
		final String hql = "FROM AllegementFoncier AS af WHERE af.contribuable.id=:idContribuable AND af.immeuble.id=:idImmeuble";
		final Query query = session.createQuery(hql);
		query.setParameter("idContribuable", idContribuable);
		query.setParameter("idImmeuble", idImmeuble);

		final List<AllegementFoncier> all = query.list();
		return all.stream()
				.filter(af -> clazz.isAssignableFrom(af.getClass()))
				.map(af -> (T) af)
				.collect(Collectors.toList());
	}
}
