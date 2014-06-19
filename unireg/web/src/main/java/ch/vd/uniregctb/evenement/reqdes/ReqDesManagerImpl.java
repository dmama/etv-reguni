package ch.vd.uniregctb.evenement.reqdes;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.reqdes.UniteTraitement;
import ch.vd.uniregctb.reqdes.UniteTraitementCriteria;
import ch.vd.uniregctb.reqdes.UniteTraitementDAO;

public class ReqDesManagerImpl implements ReqDesManager {

	private UniteTraitementDAO uniteTraitementDAO;

	public void setUniteTraitementDAO(UniteTraitementDAO uniteTraitementDAO) {
		this.uniteTraitementDAO = uniteTraitementDAO;
	}

	private static UniteTraitementCriteria buildCoreCriteria(ReqDesCriteriaView view) {
		final UniteTraitementCriteria core = new UniteTraitementCriteria();
		core.setNumeroMinute(StringUtils.trimToNull(view.getNumeroMinute()));
		core.setVisaNotaire(StringUtils.trimToNull(view.getVisaNotaire()));
		core.setDateTraitementMin(view.getDateTraitementMin());
		core.setDateTraitementMax(view.getDateTraitementMax());
		core.setEtatTraitement(view.getEtat());
		core.setDateReceptionMin(view.getDateReceptionMax());
		core.setDateTraitementMax(view.getDateReceptionMax());
		core.setDateActeMin(view.getDateActeMin());
		core.setDateActeMax(view.getDateActeMax());
		return core;
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@Override
	public List<ReqDesUniteTraitementListView> find(ReqDesCriteriaView criteria, ParamPagination pagination) {
		final List<UniteTraitement> uts = uniteTraitementDAO.find(buildCoreCriteria(criteria), pagination);
		final List<ReqDesUniteTraitementListView> views = new ArrayList<>(uts.size());
		for (UniteTraitement ut : uts) {
			views.add(new ReqDesUniteTraitementListView(ut));
		}
		return views;
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@Override
	public int count(ReqDesCriteriaView criteria) {
		return uniteTraitementDAO.getCount(buildCoreCriteria(criteria));
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@Override
	public ReqDesUniteTraitementDetailedView get(long idUniteTraitement) {
		final UniteTraitement ut = uniteTraitementDAO.get(idUniteTraitement);
		return ut == null ? null : new ReqDesUniteTraitementDetailedView(ut);
	}
}
