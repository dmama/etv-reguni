package ch.vd.unireg.registrefoncier.dao;

import javax.persistence.FlushModeType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.SituationRF;
import ch.vd.unireg.registrefoncier.TypeDroit;
import ch.vd.unireg.registrefoncier.key.ImmeubleRFKey;

public class ImmeubleRFDAOImpl extends BaseDAOImpl<ImmeubleRF, Long> implements ImmeubleRFDAO {

	protected static final int MIN_NO_OFS_FRACTION_COMMUNE = 8000;

	protected ImmeubleRFDAOImpl() {
		super(ImmeubleRF.class);
	}

	@Nullable
	@Override
	public ImmeubleRF find(@NotNull ImmeubleRFKey key, @Nullable FlushModeType flushModeOverride) {
		return findUnique("from ImmeubleRF where idRF = :idRF", buildNamedParameters(Pair.of("idRF", key.getIdRF())), flushModeOverride);
	}

	@Override
	public @Nullable ImmeubleRF findByEgrid(@NotNull String egrid) {
		final Query query = getCurrentSession().createQuery("from ImmeubleRF where egrid = :egrid");
		query.setParameter("egrid", egrid);
		//noinspection unchecked
		return (ImmeubleRF) query.uniqueResult();
	}

	@Override
	public @Nullable ImmeubleRF getBySituation(int noOfsCommune, int noParcelle, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) {

		final String common;
		if (noOfsCommune >= MIN_NO_OFS_FRACTION_COMMUNE) {
			// cas spécial des fractions de commune, on va chercher le numéro Ofs surchargé au niveau de la situation
			common = "select s.immeuble from SituationRF s where s.annulationDate is null and s.noOfsCommuneSurchargee = :noOfsCommune and s.noParcelle = :noParcelle";
		}
		else {
			// cas général : on va chercher le numéro Ofs de la commune
			common = "select s.immeuble from SituationRF s where s.annulationDate is null and s.noOfsCommuneSurchargee is null and s.commune.annulationDate is null and s.commune.noOfs = :noOfsCommune and s.noParcelle = :noParcelle";
		}

		final Query query;
		if (index1 == null) {
			query = getCurrentSession().createQuery(common + " and s.index1 is null and s.index2 is null and s.index3 is null");
		}
		else if (index2 == null) {
			query = getCurrentSession().createQuery(common + " and s.index1 = :index1 and s.index2 is null and s.index3 is null");
			query.setParameter("index1", index1);
		}
		else if (index3 == null) {
			query = getCurrentSession().createQuery(common + " and s.index1 = :index1 and s.index2 = :index2 and s.index3 is null");
			query.setParameter("index1", index1);
			query.setParameter("index2", index2);
		}
		else {
			query = getCurrentSession().createQuery(common + " and s.index1 = :index1 and s.index2 = :index2 and s.index3 = :index3");
			query.setParameter("index1", index1);
			query.setParameter("index2", index2);
			query.setParameter("index3", index3);
		}
		query.setParameter("noOfsCommune", noOfsCommune);
		query.setParameter("noParcelle", noParcelle);

		//noinspection unchecked
		return (ImmeubleRF) query.uniqueResult();
	}

	@NotNull
	@Override
	public List<SituationRF> findImmeublesParSituation(int noOfsCommune, int noParcelle, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) {

		final String common;
		if (noOfsCommune >= MIN_NO_OFS_FRACTION_COMMUNE) {
			// cas spécial des fractions de commune, on va chercher le numéro Ofs surchargé au niveau de la situation
			common = "from SituationRF s where s.annulationDate is null and s.noOfsCommuneSurchargee = :noOfsCommune and s.noParcelle = :noParcelle";
		}
		else {
			// cas général : on va chercher le numéro Ofs de la commune
			common = "from SituationRF s where s.annulationDate is null and s.noOfsCommuneSurchargee is null and s.commune.annulationDate is null and s.commune.noOfs = :noOfsCommune and s.noParcelle = :noParcelle";
		}

		final Query query;
		if (index1 == null) {
			// aucun index renseigné
			query = getCurrentSession().createQuery(common);
		}
		else if (index2 == null) {
			// index1 renseigné
			final String queryString = common + " and s.index1 = :index1";
			query = getCurrentSession().createQuery(queryString);
			query.setParameter("index1", index1);
		}
		else if (index3 == null) {
			// index1 et index2 renseignés
			final String queryString = common + " and s.index1 = :index1 and s.index2 = :index2";
			query = getCurrentSession().createQuery(queryString);
			query.setParameter("index1", index1);
			query.setParameter("index2", index2);
		}
		else {
			// index1, index2 et index3 renseignés
			final String queryString = common + " and s.index1 = :index1 and s.index2 = :index2 and s.index3 = :index3";
			query = getCurrentSession().createQuery(queryString);
			query.setParameter("index1", index1);
			query.setParameter("index2", index2);
			query.setParameter("index3", index3);
		}
		query.setParameter("noOfsCommune", noOfsCommune);
		query.setParameter("noParcelle", noParcelle);

		//noinspection unchecked
		return query.list();
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

	@NotNull
	@Override
	public List<Long> findImmeubleIdsAvecDatesDeFinDroitsACalculer() {
		// [SIFISC-24558] On ignore les servitudes car leurs dates de fin ne peuvent pas être calculée
		// (= une date limite est fixées à la conclusion de l'acte notariée ou la servitude s'éteint lors du décès du bénéficiaire).
		final Query query = getCurrentSession().createQuery("select distinct immeuble.id from DroitProprieteRF where dateFinMetier is null and dateFin is not null order by immeuble.id");
		final List<?> list = query.list();
		return list.stream()
				.map(n -> ((Number) n).longValue())
				.collect(Collectors.toList());
	}

	@Override
	public @NotNull List<Long> getAllIds() {
		final Query query = getCurrentSession().createQuery("select id from ImmeubleRF where annulationDate is null order by id");
		final List<?> list = query.list();
		return list.stream()
				.map(Number.class::cast)
				.map(Number::longValue)
				.collect(Collectors.toList());
	}

	@Override
	public @NotNull Set<String> findAvecDroitsActifs(TypeDroit typeDroit) {
		final Query query;
		if (typeDroit == TypeDroit.DROIT_PROPRIETE) {
			final String queryString = "select i.idRF from ImmeubleRF i left join i.droitsPropriete d where d.dateFin is null and i.droitsPropriete is not empty";
			query = getCurrentSession().createQuery(queryString);
		}
		else if (typeDroit == TypeDroit.SERVITUDE) {
			final String queryString = "select i.idRF from ImmeubleRF i left join i.servitudes d where (d.dateFin is null or :today < d.dateFin) and i.servitudes is not empty";
			query = getCurrentSession().createQuery(queryString);
			query.setParameter("today", RegDate.get());
		}
		else {
			throw new IllegalArgumentException("Type de droit inconnu = [" + typeDroit + "]");
		}

		//noinspection unchecked
		return new HashSet<>(query.list());

	}
}
