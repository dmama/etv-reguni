package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.parametrage.ParametreEnum;
import ch.vd.uniregctb.tiers.manager.ForFiscalManager;
import ch.vd.uniregctb.tiers.view.ForFiscalView;
import ch.vd.uniregctb.type.GenreImpot;

/**
 * Controller spring permettant la visualisation ou la saisie d'une objet metier donne.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class TiersForController extends AbstractTiersController {

	protected final Logger LOGGER = Logger.getLogger(TiersForController.class);

	private static final String ID_FOR_PARAMETER_NAME = "idFor";
	private static final String NUMERO_CTB_PARAMETER_NAME = "numero";
	private static final String NATURE_FOR_PARAMETER_NAME = "nature";
	private static final String NATURE_DPI_PARAMETER_VALUE = "DPI";

	private ForFiscalManager forFiscalManager;
	private ParametreAppService paramService;

	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {
		final Map<String, Object> referenceData = super.referenceData(request);
		referenceData.put(ParametreEnum.anneeMinimaleForDebiteur.name(), paramService.getAnneeMinimaleForDebiteur());
		return referenceData;
	}

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		ForFiscalView forFiscalView;
		String idFor = request.getParameter(ID_FOR_PARAMETER_NAME);
		Long numeroCtb = extractLongParam(request, NUMERO_CTB_PARAMETER_NAME);
		String natureFor = request.getParameter(NATURE_FOR_PARAMETER_NAME);
		checkAccesDossierEnLecture(numeroCtb);

		//les droits sont vérifiés à la sauvegarde (ForFiscalValidator)
		if (idFor != null && !"".equals(idFor.trim())) {
			Long id = Long.parseLong(idFor);
			forFiscalView = forFiscalManager.get(id);
		}
		else {
			if (natureFor != null && NATURE_DPI_PARAMETER_VALUE.equals(natureFor.trim())) {
				forFiscalView = forFiscalManager.create(numeroCtb, true);
			} else {
				forFiscalView = forFiscalManager.create(numeroCtb, false);
			}
		}

		return forFiscalView;
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		super.onSubmit(request, response, command, errors);
		ForFiscalView forFiscalView = (ForFiscalView) command;
		checkAccesDossierEnEcriture(forFiscalView.getNumeroCtb());

		if (forFiscalView.getId() == null) {
			forFiscalManager.addFor(forFiscalView);
		}
		else if (forFiscalView.isChangementModeImposition()) {
			forFiscalManager.updateModeImposition(forFiscalView);
		}
		else {
			forFiscalManager.updateFor(forFiscalView);
		}

		if (forFiscalView.getGenreImpot() == GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE) {
			return new ModelAndView("redirect:../fiscal/edit-for-debiteur.do?id=" + forFiscalView.getNumeroCtb());
		}
		else {
			return new ModelAndView("redirect:../fiscal/edit.do?id=" + forFiscalView.getNumeroCtb());
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setForFiscalManager(ForFiscalManager forFiscalManager) {
		this.forFiscalManager = forFiscalManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParamService(ParametreAppService paramService) {
		this.paramService = paramService;
	}
}
