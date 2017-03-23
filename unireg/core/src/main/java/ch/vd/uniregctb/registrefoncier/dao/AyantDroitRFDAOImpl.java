package ch.vd.uniregctb.registrefoncier.dao;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRFMembreInfo;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.registrefoncier.TypeDroit;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;

public class AyantDroitRFDAOImpl extends BaseDAOImpl<AyantDroitRF, Long> implements AyantDroitRFDAO {
	protected AyantDroitRFDAOImpl() {
		super(AyantDroitRF.class);
	}

	@Nullable
	@Override
	public AyantDroitRF find(@NotNull AyantDroitRFKey key) {
		final Query query = getCurrentSession().createQuery("from AyantDroitRF where idRF = :idRF");
		query.setParameter("idRF", key.getIdRF());
		return (AyantDroitRF) query.uniqueResult();
	}

	@Override
	public Set<String> findAvecDroitsActifs(@NotNull TypeDroit typeDroit) {
		final String typeNames = getTypeNamesFor(typeDroit);
		final String queryString = "select a.idRF from AyantDroitRF a left join a.droits d where TYPE(d) in (" + typeNames + ") and d.dateFin is null and a.droits is not empty";
		final Query query = getCurrentSession().createQuery(queryString);
		//noinspection unchecked
		return new HashSet<>(query.list());
	}

	/**
	 * Cette méthode construit la liste des classes concrètes des droits correspondant au type spécifié.
	 *
	 * @param typeDroit un type de droits
	 * @return les noms des classes correspondantes, séparés par des virgules.
	 */
	private String getTypeNamesFor(@NotNull TypeDroit typeDroit) {
		return String.join(",", typeDroit.getEntityClasses().stream()
				.map(Class::getSimpleName)
				.collect(Collectors.toList()));
	}

	@Nullable
	@Override
	public CommunauteRFMembreInfo getCommunauteMembreInfo(long communauteId) {

		final Set<Number> ids = new HashSet<>();

		if (!exists(communauteId)) {
			// la communauté n'existe pas
			return null;
		}

		// on récupère les ids des tiers RF PMs
		final Query query1 = getCurrentSession().createQuery("select d.ayantDroit.id from DroitProprietePersonneRF d where d.annulationDate is null and d.communaute.id = :communauteId");
		query1.setParameter("communauteId", communauteId);
		//noinspection unchecked
		ids.addAll((List<Number>) query1.list());

		if (ids.isEmpty()) {
			// la communauté existe, mais elle est vide
			return new CommunauteRFMembreInfo(0, Collections.emptyList(), Collections.emptyList());
		}

		// conversion ids tiers RF -> ids de tiers Unireg
		final Query query2 = getCurrentSession().createQuery("select r.tiersRF.id, r.contribuable.id  from RapprochementRF r where r.annulationDate is null and r.dateFin is null and r.tiersRF.id in (:ids)");
		query2.setParameterList("ids", ids);

		final Map<Long, Long> rf2ctb = new HashMap<>(ids.size());
		//noinspection unchecked
		query2.list().forEach(o -> {
			final Object row[] = (Object[]) o;
			rf2ctb.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
		});
		final Collection<Long> ctbIds = rf2ctb.values();

		// on détermine les ids des tiers RF non-rapprochés avec des tiers Unireg
		final Set<Long> idsRf = ids.stream()
				.map(Number::longValue)
				.collect(Collectors.toSet());
		idsRf.removeAll(rf2ctb.keySet());

		// on charge les tiers RF non-rapprochés correspondants
		final Collection<TiersRF> tiersRF;
		if (idsRf.isEmpty()) {
			tiersRF = Collections.emptyList();
		}
		else {
			final Query query3 = getCurrentSession().createQuery("from TiersRF where id in (:ids)");
			query3.setParameterList("ids", idsRf);
			//noinspection unchecked
			tiersRF = (Collection<TiersRF>) query3.list();
		}

		return new CommunauteRFMembreInfo(ids.size(), ctbIds, tiersRF);
	}

	@Nullable
	@Override
	public Long getContribuableIdFor(@NotNull TiersRF tiersRF) {
		final Query query = getCurrentSession().createQuery("select r.contribuable.id  from RapprochementRF r where r.annulationDate is null and r.dateFin is null and r.tiersRF.id = :id");
		query.setParameter("id", tiersRF.getId());
		final Number number = (Number) query.uniqueResult();
		return number == null ? null : number.longValue();
	}
}
