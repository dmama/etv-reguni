package ch.vd.uniregctb.registrefoncier.dao;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.key.BatimentRFKey;

public class BatimentRFDAOImpl extends BaseDAOImpl<BatimentRF, Long> implements BatimentRFDAO {
	protected BatimentRFDAOImpl() {
		super(BatimentRF.class);
	}

	@Nullable
	@Override
	public BatimentRF find(@NotNull BatimentRFKey key, @Nullable FlushMode flushModeOverride) {
		return findUnique("from BatimentRF where masterIdRF = :masterIdRF", buildNamedParameters(Pair.of("masterIdRF", key.getMasterIdRF())), flushModeOverride);
	}

	@NotNull
	@Override
	public Set<String> findActifs() {
		final Query query = getCurrentSession().createQuery("select b.masterIdRF from BatimentRF b left join b.implantations i where i.dateFin is null and b.implantations is not empty");
		//noinspection unchecked
		return new HashSet<>(query.list());
	}
}
