package ch.vd.unireg.registrefoncier.dao;

import javax.persistence.FlushModeType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.TiersRF;
import ch.vd.unireg.registrefoncier.TypeDroit;
import ch.vd.unireg.registrefoncier.key.AyantDroitRFKey;

public class AyantDroitRFDAOImpl extends BaseDAOImpl<AyantDroitRF, Long> implements AyantDroitRFDAO {
	protected AyantDroitRFDAOImpl() {
		super(AyantDroitRF.class);
	}

	@Nullable
	@Override
	public AyantDroitRF find(@NotNull AyantDroitRFKey key, @Nullable FlushModeType flushModeOverride) {
		return findUnique("from AyantDroitRF where idRF = :idRF", buildNamedParameters(Pair.of("idRF", key.getIdRF())), flushModeOverride);
	}

	@Override
	public Set<String> findAvecDroitsActifs(@NotNull TypeDroit typeDroit) {

		final Query query;
		if (typeDroit == TypeDroit.DROIT_PROPRIETE) {
			final String queryString = "select a.idRF from AyantDroitRF a left join a.droitsPropriete d where d.dateFinMetier is null and a.droitsPropriete is not empty";
			query = getCurrentSession().createQuery(queryString);
		}
		else if (typeDroit == TypeDroit.SERVITUDE) {
			final String queryString = "select a.idRF from AyantDroitRF a left join a.beneficesServitudes b left join b.servitude s " +
					"where b.annulationDate is null and (b.dateFin is null or :today < b.dateFin) " +
					"and s.annulationDate is null and (s.dateFinMetier is null or :today < s.dateFinMetier) and a.beneficesServitudes is not empty";
			query = getCurrentSession().createQuery(queryString);
			query.setParameter("today", RegDate.get());
		}
		else {
			throw new IllegalArgumentException("Type de droit inconnu = [" + typeDroit + "]");
		}

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
	public Long getContribuableIdFor(@NotNull TiersRF tiersRF) {
		final Query query = getCurrentSession().createQuery("select r.contribuable.id  from RapprochementRF r where r.annulationDate is null and r.dateFin is null and r.tiersRF.id = :id");
		query.setParameter("id", tiersRF.getId());
		final Number number = (Number) query.uniqueResult();
		return number == null ? null : number.longValue();
	}

	@Override
	public @NotNull List<Long> findCommunautesIds() {
		final Query query = getCurrentSession().createQuery("select id from CommunauteRF");
		//noinspection unchecked
		return (List<Long>) query.list();
	}
}
