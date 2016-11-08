package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.List;

import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.BaseDAOImpl;

public class EvenementRFMutationDAOImpl extends BaseDAOImpl<EvenementRFMutation, Long> implements EvenementRFMutationDAO {
	protected EvenementRFMutationDAOImpl() {
		super(EvenementRFMutation.class);
	}

	@NotNull
	@Override
	public List<Long> findIds(long importId, @NotNull EvenementRFMutation.TypeEntite typeEntite, @NotNull EtatEvenementRF... etats) {
		final Query query = getCurrentSession().createQuery("select id from EvenementRFMutation where typeEntite = :typeEntite and parentImport.id = :importId and etat in (:etats)");
		query.setParameter("importId", importId);
		query.setParameter("typeEntite", typeEntite);
		query.setParameterList("etats", etats);
		//noinspection unchecked
		return query.list();
	}
}
