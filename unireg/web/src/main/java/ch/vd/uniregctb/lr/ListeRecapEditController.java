package ch.vd.uniregctb.lr;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.EditiqueCommunicationException;
import ch.vd.uniregctb.common.EditiqueErrorHelper;
import ch.vd.uniregctb.common.TicketService;
import ch.vd.uniregctb.common.TicketTimeoutException;
import ch.vd.uniregctb.declaration.DeclarationGenerationOperation;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatErreur;
import ch.vd.uniregctb.lr.manager.ListeRecapEditManager;
import ch.vd.uniregctb.lr.view.ListeRecapDetailView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;

public class ListeRecapEditController extends AbstractListeRecapController {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ListeRecapListController.class);

	/**
	 * Le nom du parametre utilise dans la request.
	 */
	public static final String LR_ID_PARAMETER_NAME = "id";
	public static final String DELAI_ID_PARAMETER_NAME = "idDelai";
	public static final String DEBITEUR_ID_PARAMETER_NAME = "numero";

	public static final String BUTTON_SAUVER = "__confirmed_save";
	public static final String BUTTON_IMPRIMER_LR = "imprimerLR";
	public static final String BUTTON_SOMMER_LR = "sommerLR";
	public static final String BUTTON_DUPLICATA_LR = "duplicataLR";
	public static final String BUTTON_ANNULER_LR = "annulerLR";
	public static final String TARGET_ANNULER_DELAI = "annulerDelai";

	private ListeRecapEditManager lrEditManager;
	private TicketService ticketService;

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		final String idLrParam = request.getParameter(LR_ID_PARAMETER_NAME);
		final String idDpiParam = request.getParameter(DEBITEUR_ID_PARAMETER_NAME);

		ListeRecapDetailView lrEditView = null;

		if (!SecurityHelper.isGranted(securityProvider, Role.LR)) {
			throw new AccessDeniedException("vous n'avez pas le droit d'accéder aux listes récapitulatives pour l'application Unireg");
		}

		if (StringUtils.isNotBlank(idLrParam)) {
			final long id = Long.parseLong(idLrParam);
			lrEditView = lrEditManager.get(id);
		}
		else if (StringUtils.isNotBlank(idDpiParam)) {
			final long idDpi = Long.parseLong(idDpiParam);
			lrEditView = lrEditManager.creerLr(idDpi);
		}

		return lrEditView;
	}

	@Override
	protected boolean suppressValidation(HttpServletRequest request, Object command, BindException errors) {
		if (getTarget() != null || request.getParameter(BUTTON_IMPRIMER_LR) != null || request.getParameter(BUTTON_DUPLICATA_LR) != null) {
			return true;
		}
		return super.suppressValidation(request, command, errors);
	}

	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#onBindAndValidate(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {
		ListeRecapDetailView lrDetailView = (ListeRecapDetailView) command;
		if (BUTTON_IMPRIMER_LR.equals(getTarget())) {
			if (lrDetailView.getDelaiAccorde() == null) {
				ValidationUtils.rejectIfEmpty(errors, "delaiAccorde", "error.delai.accorde.vide");
			}
			else
			{
				if (lrDetailView.getRegDelaiAccorde().isBefore(RegDate.get())) {
					ValidationUtils.rejectIfEmpty(errors, "delaiAccorde", "error.delai.accorde.anterieur.date.jour");
				}
			}
		}
		super.onBindAndValidate(request, command, errors);
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
		ListeRecapDetailView bean = (ListeRecapDetailView) command;

		if (getTarget() != null) {
			if (BUTTON_IMPRIMER_LR.equals(getTarget())) {
				final DeclarationGenerationOperation tickettingKey = new DeclarationGenerationOperation(bean.getDpi().getNumero());
				try {
					final TicketService.Ticket ticket = ticketService.getTicket(tickettingKey, Duration.ofMillis(500));
					try {
						final TraitementRetourEditique<EditiqueResultatErreur> erreur = new TraitementRetourEditique<EditiqueResultatErreur>() {
							@Override
							public ModelAndView doJob(EditiqueResultatErreur resultat) {
								final String message = String.format("%s Veuillez imprimer un duplicata de la liste récapitulative.", EditiqueErrorHelper.getMessageErreurEditique(resultat));
								throw new EditiqueCommunicationException(message);
							}
						};

						final EditiqueResultat resultat = lrEditManager.envoieImpressionLocalLR(bean);
						traiteRetourEditique(resultat, response, "lr", null, null, erreur);
					}
					finally {
						ticket.release();
					}
				}
				catch (TicketTimeoutException e) {
					throw new ActionException("Une LR est actuellement en cours d'impression pour ce débiteur. Veuillez ré-essayer ultérieurement.", e);
				}
			}
		}
		else {

			final TraitementRetourEditique<EditiqueResultatErreur> erreur = new TraitementRetourEditique<EditiqueResultatErreur>() {
				@Override
				public ModelAndView doJob(EditiqueResultatErreur resultat) {
					final String message = String.format("%s Veuillez ré-essayer plus tard.", EditiqueErrorHelper.getMessageErreurEditique(resultat));
					throw new EditiqueCommunicationException(message);
				}
			};

			if (request.getParameter(BUTTON_DUPLICATA_LR) != null) {
				final EditiqueResultat resultat = lrEditManager.envoieImpressionLocalDuplicataLR(bean);
				traiteRetourEditique(resultat, response, "lr", null, null, erreur);
			}
			else if (request.getParameter(BUTTON_ANNULER_LR) != null) {
				lrEditManager.annulerLR(bean);
				return new ModelAndView("redirect:edit-debiteur.do?numero=" + bean.getDpi().getNumero().toString());
			}
			else if (request.getParameter(BUTTON_SOMMER_LR) != null) {
				final EditiqueResultat resultat = lrEditManager.envoieImpressionLocalSommationLR(bean);
				traiteRetourEditique(resultat, response, "sommationLr", null, null, erreur);
			}
			else if (request.getParameter(BUTTON_SAUVER) != null) {
				lrEditManager.save(bean);
				this.setModified(false);
				return new ModelAndView("redirect:edit-debiteur.do?numero=" + bean.getDpi().getNumero().toString());
			}
		}
		lrEditManager.refresh(bean);
		return showForm(request, response, errors);
	}

	public void setLrEditManager(ListeRecapEditManager lrEditManager) {
		this.lrEditManager = lrEditManager;
	}

	public void setTicketService(TicketService ticketService) {
		this.ticketService = ticketService;
	}
}
