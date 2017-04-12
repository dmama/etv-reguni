package ch.vd.uniregctb.lr.manager;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.ListeRecapitulativeCriteria;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.lr.view.ListeRecapitulativeSearchResult;

/**
 * Definition de services utiles pour la recherche de LR
 *
 * @author xcifde
 *
 */
public class ListeRecapListManagerImpl implements ListeRecapListManager{

	protected static final Logger LOGGER = LoggerFactory.getLogger(ListeRecapListManagerImpl.class);

	private ListeRecapitulativeDAO lrDAO;

	private AdresseService adresseService;

	public void setLrDAO(ListeRecapitulativeDAO lrDAO) {
		this.lrDAO = lrDAO;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	/**
	 * @see ch.vd.uniregctb.lr.manager.ListeRecapListManager#count(ListeRecapitulativeCriteria)
	 */
	@Override
	@Transactional(readOnly = true)
	public int count(ListeRecapitulativeCriteria lrCriteria) {
		return lrDAO.count(lrCriteria);
	}

	/**
	 * @see ch.vd.uniregctb.lr.manager.ListeRecapListManager#find(ListeRecapitulativeCriteria, ch.vd.uniregctb.common.ParamPagination)
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ListeRecapitulativeSearchResult> find(ListeRecapitulativeCriteria lrCriteria, ParamPagination paramPagination) throws AdresseException {
		final List<DeclarationImpotSource> bos = lrDAO.find(lrCriteria, paramPagination);
		final List<ListeRecapitulativeSearchResult> lrViews = new ArrayList<>(bos.size());
		for (DeclarationImpotSource lr : bos) {
			lrViews.add(new ListeRecapitulativeSearchResult(lr, adresseService));
		}
		return lrViews;
	}

}
