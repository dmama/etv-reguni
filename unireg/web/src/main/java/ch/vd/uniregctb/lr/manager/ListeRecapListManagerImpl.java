package ch.vd.uniregctb.lr.manager;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.adresse.AdresseException;
import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.ListeRecapCriteria;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.lr.view.ListeRecapDetailView;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Definition de services utiles pour la recherche de LR
 *
 * @author xcifde
 *
 */
public class ListeRecapListManagerImpl implements ListeRecapListManager{

	protected static final Logger LOGGER = Logger.getLogger(ListeRecapListManagerImpl.class);

	private ListeRecapitulativeDAO lrDAO;

	private TiersService tiersService;

	private AdresseService adresseService;

	public ListeRecapitulativeDAO getLrDAO() {
		return lrDAO;
	}

	public void setLrDAO(ListeRecapitulativeDAO lrDAO) {
		this.lrDAO = lrDAO;
	}

	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public AdresseService getAdresseService() {
		return adresseService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	private ListeRecapDetailView getView(DeclarationImpotSource lr) throws AdresseException {

		final ListeRecapDetailView lrView = new ListeRecapDetailView();
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) lr.getTiers();

		final List<String> nomCourrier = getAdresseService().getNomCourrier(dpi, null, false);
		lrView.setNomCourrier(nomCourrier);

		lrView.setNumero(dpi.getNumero());
		lrView.setCategorie(dpi.getCategorieImpotSource());
		lrView.setNomCourrier2(dpi.getComplementNom());
		lrView.setModeCommunication(lr.getModeCommunication());
		lrView.setDateDebutPeriode(lr.getDateDebut());
		lrView.setDateFinPeriode(lr.getDateFin());
		lrView.setDelaiAccorde(lr.getDelaiAccordeAu());
		lrView.setDateRetour(lr.getDateRetour());
		lrView.setEtat(lr.getDernierEtat().getEtat());
		lrView.setAnnule(lr.isAnnule());
		lrView.setId(lr.getId());

		return lrView;
	}

	/**
	 * @see ch.vd.uniregctb.lr.manager.ListeRecapListManager#count(ch.vd.uniregctb.declaration.ListeRecapCriteria)
	 */
	@Transactional(readOnly = true)
	public int count(ListeRecapCriteria lrCriteria) {
		return lrDAO.count(lrCriteria);
	}

	/**
	 * @see ch.vd.uniregctb.lr.manager.ListeRecapListManager#find(ch.vd.uniregctb.declaration.ListeRecapCriteria, ch.vd.uniregctb.common.ParamPagination)
	 */
	@Transactional(readOnly = true)
	public List<ListeRecapDetailView> find(ListeRecapCriteria lrCriteria, ParamPagination paramPagination) throws AdresseException {

		final List<DeclarationImpotSource> bos = lrDAO.find(lrCriteria, paramPagination);
		final List<ListeRecapDetailView> lrViews = new ArrayList<ListeRecapDetailView>(bos.size());
		for (DeclarationImpotSource lr : bos) {
				lrViews.add(getView(lr));
		}
		return lrViews;
	}

}
