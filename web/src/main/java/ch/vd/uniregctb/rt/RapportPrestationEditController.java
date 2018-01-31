package ch.vd.uniregctb.rt;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.rt.manager.RapportPrestationEditManager;
import ch.vd.uniregctb.rt.view.RapportPrestationView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;

public class RapportPrestationEditController  extends AbstractRapportPrestationController {

	protected final Logger LOGGER = LoggerFactory.getLogger(RapportPrestationEditController.class);

	private static final String NUMERO_SOURCIER_PARAMETER_NAME = "numeroSrc";
	private static final String NUMERO_DEBITEUR_PARAMETER_NAME = "numeroDpi";
	private static final String PROVENANCE_PARAMETER_NAME = "provenance";

	private static final String PROVENANCE_SOURCIER_VALUE = "sourcier";
	private static final String PROVENANCE_DEBITEUR_VALUE = "debiteur";
	private static final String PROVENANCE_LISTE_RAPPORTS = "listeRPI";


	private RapportPrestationEditManager rapportPrestationEditManager;

	public void setRapportPrestationEditManager(RapportPrestationEditManager rapportPrestationEditManager) {
		this.rapportPrestationEditManager = rapportPrestationEditManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		//vérification droit création rapport de travail
		if(!SecurityHelper.isGranted(securityProvider, Role.RT)){
			throw new AccessDeniedException("Vous ne possédez pas le droit de créer un rapport de travail");
		}

		String numeroSrcParam = request.getParameter(NUMERO_SOURCIER_PARAMETER_NAME);
		String numeroDpiParam = request.getParameter(NUMERO_DEBITEUR_PARAMETER_NAME);
		String provenance = request.getParameter(PROVENANCE_PARAMETER_NAME);
		Long numeroSrc = Long.parseLong(numeroSrcParam);
		Long numeroDpi = Long.parseLong(numeroDpiParam);

		RapportPrestationView  rapportView = rapportPrestationEditManager.get(numeroSrc, numeroDpi, provenance);

		checkAccesDossierEnLecture(numeroSrc);
		checkAccesDossierEnLecture(numeroDpi);

		return rapportView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
		throws Exception {
		ModelAndView mav = super.onSubmit(request, response, command, errors);

		RapportPrestationView rapportView = (RapportPrestationView) command;

		checkAccesDossierEnEcriture(rapportView.getDebiteur().getNumero());
		checkAccesDossierEnEcriture(rapportView.getSourcier().getNumero());

		rapportPrestationEditManager.save(rapportView);

		String provenance = rapportView.getProvenance();
		if (provenance.equals(PROVENANCE_SOURCIER_VALUE)) {
			mav.setView(new RedirectView("../tiers/visu.do?id=" + rapportView.getSourcier().getNumero()));
		}
		if (provenance.equals(PROVENANCE_DEBITEUR_VALUE)) {
			mav.setView(new RedirectView("../tiers/visu.do?id=" + rapportView.getDebiteur().getNumero()));
		}
		if (provenance.equals(PROVENANCE_LISTE_RAPPORTS)) {
			mav.setView(new RedirectView("../rapports-prestation/list.do?idDpi=" + rapportView.getDebiteur().getNumero()));
		}

		return mav;
	}

}
