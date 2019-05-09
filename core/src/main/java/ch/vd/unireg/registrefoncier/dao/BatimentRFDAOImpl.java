package ch.vd.unireg.registrefoncier.dao;

import javax.persistence.FlushModeType;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.registrefoncier.BatimentRF;
import ch.vd.unireg.registrefoncier.key.BatimentRFKey;

public class BatimentRFDAOImpl extends BaseDAOImpl<BatimentRF, Long> implements BatimentRFDAO {
	protected BatimentRFDAOImpl() {
		super(BatimentRF.class);
	}

	@Nullable
	@Override
	public BatimentRF find(@NotNull BatimentRFKey key, @Nullable FlushModeType flushModeOverride) {
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
