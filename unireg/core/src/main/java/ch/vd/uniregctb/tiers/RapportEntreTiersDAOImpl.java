package ch.vd.uniregctb.tiers;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.uniregctb.common.ParamPagination;

public class RapportEntreTiersDAOImpl extends GenericDAOImpl<RapportEntreTiers, Long> implements RapportEntreTiersDAO {

	public RapportEntreTiersDAOImpl() {
		super(RapportEntreTiers.class);
	}

	public List<RapportEntreTiers> getRepresentationLegaleAvecTuteurEtPupille(Long noTiersTuteur, Long noTiersPupille) {
		return getRepresentationLegaleAvecTuteurEtPupille(noTiersTuteur, noTiersPupille, false);
	}

	@SuppressWarnings("unchecked")
	public List<RapportEntreTiers> getRepresentationLegaleAvecTuteurEtPupille(Long noTiersTuteur, Long noTiersPupille, boolean doNotAutoFlush) {

		Object[] criteria = { noTiersTuteur,noTiersPupille };
		String query = "from RapportEntreTiers ret where ret.objetId = ? and ret.sujetId = ?";
		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);

		List<RapportEntreTiers> list = (List<RapportEntreTiers>) find(query, criteria, mode);
		return list;
	}

	/**
	 * Retourne les rapports prestation imposable d'un débiteur
	 * @param numeroDebiteur
	 * @param paramPagination
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<RapportPrestationImposable> getRapportsPrestationImposable(final Long numeroDebiteur, ParamPagination paramPagination) {

		final StringBuilder b = new StringBuilder();
		b.append("SELECT rapport FROM RapportPrestationImposable rapport WHERE rapport.objetId = :debiteur");
		if (paramPagination.getChamp() == null) {
			b.append(" ORDER BY rapport.logCreationDate");
		}
		else {
			b.append(" ORDER BY rapport.").append(paramPagination.getChamp());
		}
		if (!paramPagination.isSensAscending()) {
			b.append(" DESC");
		}
		final String query = b.toString();

		final int firstResult = (paramPagination.getNumeroPage() - 1) * paramPagination.getTaillePage();
		final int maxResult = paramPagination.getTaillePage();

		return (List<RapportPrestationImposable>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				final Query queryObject = session.createQuery(query);
				queryObject.setParameter("debiteur", numeroDebiteur);

				queryObject.setFirstResult(firstResult);
				queryObject.setMaxResults(maxResult);

				return queryObject.list();
			}
		});
	}


	/**
	 * Compte le nombre de rapports prestation imposable d'un débiteur
	 *
	 * @param numeroDebiteur
	 * @return
	 */
	public int countRapportsPrestationImposable(Long numeroDebiteur){

		String query = "select count(*) from RapportPrestationImposable rapport where rapport.objetId = " + numeroDebiteur ;
		int count = DataAccessUtils.intResult(getHibernateTemplate().find(query));
		return count;
	}

}