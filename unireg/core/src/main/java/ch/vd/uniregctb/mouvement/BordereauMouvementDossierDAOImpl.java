package ch.vd.uniregctb.mouvement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.uniregctb.common.BaseDAOImpl;

/**
 * Implémentation du DAO pour les bordereaux d'envoi de dossiers
 */
public class BordereauMouvementDossierDAOImpl extends BaseDAOImpl<BordereauMouvementDossier, Long> implements BordereauMouvementDossierDAO {

	public BordereauMouvementDossierDAOImpl() {
		super(BordereauMouvementDossier.class);
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<BordereauMouvementDossier> getBordereauxAReceptionner(Integer noCollAdmReceptrice) {

		final StringBuilder b = new StringBuilder();
		final Map<String, Object> params = new HashMap<>();
		b.append("SELECT bordereau FROM BordereauMouvementDossier bordereau");
		b.append(" WHERE EXISTS (SELECT mvt.id FROM EnvoiDossierVersCollectiviteAdministrative mvt WHERE bordereau = mvt.bordereau AND mvt.etat = :etat and mvt.annulationDate IS NULL");
		params.put("etat", EtatMouvementDossier.TRAITE);
		if (noCollAdmReceptrice != null) {
			b.append(" AND mvt.collectiviteAdministrativeDestinataire.numeroCollectiviteAdministrative = :noCollAdm");
			params.put("noCollAdm", noCollAdmReceptrice);
		}
		b.append(')');
		final String hql = b.toString();
		return find(hql, params, null);
	}
}
