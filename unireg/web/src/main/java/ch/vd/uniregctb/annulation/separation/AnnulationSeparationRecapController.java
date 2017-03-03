package ch.vd.uniregctb.annulation.separation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.annulation.separation.manager.AnnulationSeparationRecapManager;
import ch.vd.uniregctb.annulation.separation.view.AnnulationSeparationRecapView;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.MenageCommun;

public class AnnulationSeparationRecapController extends AbstractSimpleFormController {

	protected final Logger LOGGER = LoggerFactory.getLogger(AnnulationSeparationRecapController.class);

	private static final String NUMERO_PARAMETER_NAME = "numero";

	private AnnulationSeparationRecapManager annulationSeparationRecapManager;

	public AnnulationSeparationRecapManager getAnnulationSeparationRecapManager() {
		return annulationSeparationRecapManager;
	}

	public void setAnnulationSeparationRecapManager(AnnulationSeparationRecapManager annulationSeparationRecapManager) {
		this.annulationSeparationRecapManager = annulationSeparationRecapManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		String numeroParam = request.getParameter(NUMERO_PARAMETER_NAME);
		Long numero = Long.parseLong(numeroParam);

		AnnulationSeparationRecapView annulationSeparationRecapView = annulationSeparationRecapManager.get(numero);
		
		checkAccesDossierEnLecture(annulationSeparationRecapView.getPremierePersonne().getNumero());
		//SIFISC-14029 on teste la présence de décision Aci sur le couple
		checkTraitementContribuableAvecDecisionAci(numero);
		
	 	// [UNIREG-1499] La 2nde personne peut etre null dans le cas d'un marié seul
		if (annulationSeparationRecapView.getSecondePersonne() != null) {
			checkAccesDossierEnLecture(annulationSeparationRecapView.getSecondePersonne().getNumero());			
		}
		

		return annulationSeparationRecapView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse,
	 *      org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws Exception {
		ModelAndView mav = super.showForm(request, response, errors, model);
		return mav;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#onBindAndValidate(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {
		super.onBindAndValidate(request, command, errors);
	}


	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
		throws Exception {

		AnnulationSeparationRecapView annulationSeparationRecapView = (AnnulationSeparationRecapView) command;
		final Long numeroPremierePersonne = annulationSeparationRecapView.getPremierePersonne().getNumero();
		checkAccesDossierEnEcriture(numeroPremierePersonne);
		checkTraitementContribuableAvecDecisionAci(numeroPremierePersonne);

		// [UNIREG-1499] La 2nde personne peut etre null dans le cas d'un marié seul

		if (annulationSeparationRecapView.getSecondePersonne() != null) {
			final Long numeroSecondePersonne = annulationSeparationRecapView.getSecondePersonne().getNumero();
			checkAccesDossierEnEcriture(numeroSecondePersonne);
			checkTraitementContribuableAvecDecisionAci(numeroSecondePersonne);
		}

		try {
			MenageCommun menage = annulationSeparationRecapManager.save(annulationSeparationRecapView);
			return new ModelAndView( new RedirectView("/tiers/visu.do?id=" + menage.getNumero(), true));
		}
		catch (MetierServiceException e) {
			final StringBuilder b = new StringBuilder();
			b.append("Exception lors de l'annulation de la séparation du ménage composé ");
			if (annulationSeparationRecapView.getSecondePersonne() != null) {
				b.append("des tiers ").append(FormatNumeroHelper.numeroCTBToDisplay(numeroPremierePersonne));
				b.append(" et ").append(FormatNumeroHelper.numeroCTBToDisplay(annulationSeparationRecapView.getSecondePersonne().getNumero()));
			}
			else {
				b.append("du tiers ").append(FormatNumeroHelper.numeroCTBToDisplay(numeroPremierePersonne));
			}
			LOGGER.error(b.toString(), e);
			throw new ActionException(e.getMessage());
		}
	}
}
