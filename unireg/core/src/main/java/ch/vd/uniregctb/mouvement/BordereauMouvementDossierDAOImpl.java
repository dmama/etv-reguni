package ch.vd.uniregctb.mouvement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;

/**
 * Implémentation du DAO pour les bordereaux d'envoi de dossiers
 */
public class BordereauMouvementDossierDAOImpl extends GenericDAOImpl<BordereauMouvementDossier, Long> implements BordereauMouvementDossierDAO {

	public BordereauMouvementDossierDAOImpl() {
		super(BordereauMouvementDossier.class);
	}

	@SuppressWarnings({"unchecked"})
	public List<BordereauMouvementDossier> getBordereauxAReceptionner(Integer noCollAdmReceptrice) {

		final StringBuilder b = new StringBuilder();
		final List<Object> params = new ArrayList();
		b.append("SELECT bordereau FROM BordereauMouvementDossier bordereau");
		b.append(" WHERE EXISTS (SELECT mvt.id FROM EnvoiDossierVersCollectiviteAdministrative mvt WHERE bordereau = mvt.bordereau AND mvt.etat = ? and mvt.annulationDate IS NULL");
		params.add(EtatMouvementDossier.TRAITE.name());
		if (noCollAdmReceptrice != null) {
			b.append(" AND mvt.collectiviteAdministrativeDestinataire.numeroCollectiviteAdministrative = ?");
			params.add(noCollAdmReceptrice);
		}
		b.append(")");
		final String hql = b.toString();

		return (List<BordereauMouvementDossier>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public List<BordereauMouvementDossier> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(hql);
				for (int i = 0 ; i < params.size() ; ++ i) {
					query.setParameter(i, params.get(i));
				}
				return query.list();
			}
		});
	}
}
