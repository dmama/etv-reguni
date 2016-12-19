package ch.vd.uniregctb.registrefoncier.dao;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.NonUniqueResultException;
import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

public class ImmeubleRFDAOImpl extends BaseDAOImpl<ImmeubleRF, Long> implements ImmeubleRFDAO {
	protected ImmeubleRFDAOImpl() {
		super(ImmeubleRF.class);
	}

	@Nullable
	@Override
	public ImmeubleRF find(@NotNull ImmeubleRFKey key) {
		final Query query = getCurrentSession().createQuery("from ImmeubleRF where idRF = :idRF");
		query.setParameter("idRF", key.getIdRF());
		return (ImmeubleRF) query.uniqueResult();
	}

	@NotNull
	@Override
	public Set<String> findWithActiveSurfacesAuSol() {
		final Query query = getCurrentSession().createQuery("select i.idRF from ImmeubleRF i left join i.surfacesAuSol s where s.dateFin is null and i.surfacesAuSol is not empty");
		//noinspection unchecked
		return new HashSet<>(query.list());
	}

	@NotNull
	@Override
	public Set<String> findImmeublesActifs() {
		final Query query = getCurrentSession().createQuery("select idRF from ImmeubleRF where dateRadiation is null");
		//noinspection unchecked
		return new HashSet<>(query.list());
	}

	@Nullable
	@Override
	public ImmeubleRF findImmeubleActif(int noOfsCommune, int noParcelle, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) throws NonUniqueResultException {

		String queryString = "select s.immeuble from SituationRF s, CommuneRF c " +
				"where c.noOfs = :noOfsCommune " +
				"and s.commune.id = c.id " +
				"and s.noParcelle = :noParcelle ";
		if (index1 == null) {
			queryString += 	"and s.index1 is null ";
		}
		else {
			queryString += 	"and s.index1 = :index1 ";
		}
		if (index2 == null) {
			queryString += 	"and s.index2 is null ";
		}
		else {
			queryString += 	"and s.index2 = :index2 ";
		}
		if (index3 == null) {
			queryString += 	"and s.index3 is null ";
		}
		else {
			queryString += 	"and s.index3 = :index3 ";
		}
		
		final Query query = getCurrentSession().createQuery(queryString);
		query.setParameter("noOfsCommune", noOfsCommune);
		query.setParameter("noParcelle", noParcelle);
		if (index1 != null) {
			query.setParameter("index1", index1);
		}
		if (index2 != null) {
			query.setParameter("index2", index2);
		}
		if (index3 != null) {
			query.setParameter("index3", index3);
		}
		return (ImmeubleRF) query.uniqueResult();
	}
}
