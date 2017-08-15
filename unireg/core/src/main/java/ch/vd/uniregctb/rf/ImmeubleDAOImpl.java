package ch.vd.uniregctb.rf;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.common.pagination.ParamPagination;

public class ImmeubleDAOImpl extends BaseDAOImpl<Immeuble, Long> implements ImmeubleDAO {

	public ImmeubleDAOImpl() {
		super(Immeuble.class);
	}

	@Override
	public int count(final long proprietaireId) {
		final Session session = getCurrentSession();
		final Query query = session.createQuery("select count(*) from Immeuble as i where i.contribuable.id = :propId");
		query.setParameter("propId", proprietaireId);
		return DataAccessUtils.intResult(query.list());
	}

	@SuppressWarnings({"unchecked"})
	@Override
	public List<Immeuble> find(final long proprietaireId) {
		final Session session = getCurrentSession();
		final Query query = session.createQuery("from Immeuble as i where i.contribuable.id = :propId order by i.nomCommune asc, i.numero asc, i.dateDerniereMutation desc");
		query.setParameter("propId", proprietaireId);
		return query.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Immeuble> find(final long proprietaireId, final ParamPagination pagination) {
		final Session session = getCurrentSession();

		// [SIFISC-4216] affichage par nom de commune puis numéro d'immeuble croissants
		// [SIFISC-6316] ajouté le tri par date de dernière mutation décroissante
		final Query query = session.createQuery("from Immeuble as i where i.contribuable.id = :propId order by i.nomCommune asc, i.numero asc, i.dateDerniereMutation desc");
		query.setParameter("propId", proprietaireId);

		final int firstResult = pagination.getSqlFirstResult();
		final int maxResult = pagination.getSqlMaxResults();
		query.setFirstResult(firstResult);
		query.setMaxResults(maxResult);

		return query.list();
	}

	@Override
	public void removeAll() {
		final Session session = getCurrentSession();
		final Query query = session.createSQLQuery("delete from Immeuble");
		query.executeUpdate();
	}
}
