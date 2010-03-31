package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.fidor.FidorService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.utils.UniregProperties;

/**
 * Classe controller utilisée pour l'affichage de listes
 *
 * @author xcifde
 *
 */
public abstract class AbstractTiersListController extends AbstractTiersController {

	protected final Logger LOGGER = Logger.getLogger(AbstractTiersListController.class);

	protected TiersService tiersService;
	private FidorService fidorService;

	/**
	 * Bouton rechercher
	 */
	public final static String BOUTON_RECHERCHER = "rechercher";

	/**
	 * Bouton effacer
	 */
	public final static String BOUTON_EFFACER = "effacer";


	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#suppressValidation(javax.servlet.http.HttpServletRequest, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected boolean suppressValidation(HttpServletRequest request, Object command, BindException errors) {
		if (getTarget() != null ||
				request.getParameter(BOUTON_EFFACER) != null) {
			return true;
		}
		return super.suppressValidation(request, command, errors);
	}

	/**
	 * Methode annexe de showForm utilisée par tour les controllers de type List
	 *
	 * @param request
	 * @param response
	 * @param errors
	 * @param model
	 * @param criteriaName
	 * @param listAttributeName
	 * @param bean
	 * @return
	 * @throws Exception
	 */
	protected ModelAndView showFormForList(	HttpServletRequest request, HttpServletResponse response, BindException errors, Map<?, ?> model,
											String criteriaName, String listAttributeName, TiersCriteriaView bean, boolean keepCriteriaInSession)
	throws Exception {
		String buttonEffacer = request.getParameter(ACTION_PARAMETER_NAME);
		ModelAndView mav  =  super.showForm(request, response, errors, model);
		if (errors.getErrorCount() == 0) {
			if(buttonEffacer == null) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Affichage du formulaire de recherche...");
				}
				if (bean != null && !bean.isEmpty()) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Critères de recherche=" + bean);
					}
					if (StringUtils.isNotBlank(bean.getNumeroAVS())){
						bean.setNumeroAVS(FormatNumeroHelper.removeSpaceAndDash(bean.getNumeroAVS()));
					}
					try {
						List<TiersIndexedData> results = searchTiers(bean);

						ArrayList<TiersIndexedDataView> displaysTiers = new ArrayList<TiersIndexedDataView>();
						for (TiersIndexedData v : results) {
							TiersIndexedDataView oneTiersLine = new TiersIndexedDataView(v.getDocument());
							displaysTiers.add(oneTiersLine);

							// Populate les URLs TAO/SIPF/...
							Long numero = v.getNumero();
							oneTiersLine.setUrlTaoPP(fidorService.getUrlTaoPP(numero));
							oneTiersLine.setUrlTaoBA(fidorService.getUrlTaoBA(numero));
							oneTiersLine.setUrlTaoIS(fidorService.getUrlTaoIS(numero));
							oneTiersLine.setUrlSipf(fidorService.getUrlSipf(numero));
						}

						mav.addObject(listAttributeName, displaysTiers);
					} catch (TooManyResultsIndexerException ee) {
						errors.reject("error.preciser.recherche");
					} catch (IndexerException e) {
						LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
						errors.reject("error.recherche");
					}
				}
			}
		} else {
			mav.addObject(listAttributeName, null);
		}
		if (!keepCriteriaInSession) {
			removeModuleFromSession(request, criteriaName);
		}
		return mav;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setFidorService(FidorService fidorService) {
		this.fidorService = fidorService;
	}
}
