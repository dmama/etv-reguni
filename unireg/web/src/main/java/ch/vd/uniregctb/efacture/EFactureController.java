package ch.vd.uniregctb.efacture;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.ResultatQuittancement;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.efacture.manager.EfactureManager;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.utils.RegDateEditor;
import ch.vd.uniregctb.utils.WebContextUtils;

@Controller
@RequestMapping(value = "/efacture")
public class EFactureController implements MessageSourceAware {

	private static final String CTB = "ctb";
	private static final String ID_DEMANDE = "idDemande";
	private static final String DATE_DEMANDE = "dateDemande";
	private static final String ACTION = "action";
	private static final String COMMENT = "comment";
	private static final String ACTION_URL = "actionUrl";
	private static final String LIBELLE_ACTION = "libelleAction";
	private static final String COMMAND = "command";
	private static final String MAXLEN = "maxlen";
	private EfactureManager efactureManager;
	private SecurityProviderInterface securityProvider;
	private MessageSource messageSource;

	private static void checkDroitVisuEfacture(SecurityProviderInterface securityProvider) {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_ALL, Role.GEST_EFACTURE)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour visualiser l'historique e-facture d'un contribuable");
		}
	}

	private static void checkDroitGestionaireEfacture(SecurityProviderInterface securityProvider) {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.GEST_EFACTURE)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour interagir avec les états e-facture d'un contribuable");
		}
	}

	private static void checkDroitQuittanceurEnSerie(SecurityProviderInterface securityProvider) {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.GEST_QUIT_EFACTURE)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour gérer les retours de confirmation d'inscription e-Facture");
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, true));
	}

	@ResponseBody
	@RequestMapping(value = "/histo.do", method = RequestMethod.GET)
	public DestinataireAvecHistoView histo(@RequestParam(value = CTB, required = true) long ctbId) {
		checkDroitVisuEfacture(securityProvider);
		return buildHistoDestinataire(ctbId);
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	public String edit(Model model, @RequestParam(value = CTB, required = true) long ctbId) {
		checkDroitGestionaireEfacture(securityProvider);
		final DestinataireAvecHistoView histo = buildHistoDestinataire(ctbId);
		if (histo == null) {
			throw new ActionException("Le contribuable ne possède aucun état e-facture avec lequel il est possible d'interagir");
		}

		model.addAttribute("histo", histo);
		return "tiers/edition/efacture/edit";
	}

	private int getMaxLengthForManualComment() {
		return efactureManager.getMaxLengthForManualComment();
	}

	@RequestMapping(value = "/suspend.do", method = RequestMethod.POST)
	public String suspend(@RequestParam(value = CTB, required = true) long ctbId, @RequestParam(value = COMMENT, required = false) String comment) throws Exception {
		checkDroitGestionaireEfacture(securityProvider);
		final String businessId = efactureManager.suspendreContribuable(ctbId, StringUtils.trimToNull(comment));
		if (!efactureManager.isReponseRecueDeEfacture(businessId)) {
			Flash.warning("Votre demande de suspension a bien été prise en compte, Elle sera traitée dès que possible par le système E-facture.");
		}
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);
	}

	@RequestMapping(value = "/activate.do", method = RequestMethod.POST)
	public String activate(@RequestParam(value = CTB, required = true) long ctbId, @RequestParam(value = COMMENT, required = false) String comment) throws Exception {
		checkDroitGestionaireEfacture(securityProvider);
		final String businessId = efactureManager.activerContribuable(ctbId, comment);
		if (!efactureManager.isReponseRecueDeEfacture(businessId)) {
			Flash.warning("Votre demande d'activation a bien été prise en compte, Elle sera traitée dès que possible par le système E-facture.");
		}
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);
	}

	@RequestMapping(value = "/validate.do", method = RequestMethod.POST)
	public String validate(@RequestParam(value = CTB, required = true) long ctbId, @RequestParam(value = ID_DEMANDE, required = true) String idDemande) throws Exception {
		checkDroitGestionaireEfacture(securityProvider);
		final String businessId = efactureManager.accepterDemande(idDemande);
		if (!efactureManager.isReponseRecueDeEfacture(businessId)) {
			Flash.warning("Votre demande de validation a bien été prise en compte, Elle sera traitée dès que possible par le système E-facture.");
		}
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);
	}

	@RequestMapping(value = "/refuse.do", method = RequestMethod.POST)
	public String refuse(@RequestParam(value = CTB, required = true) long ctbId, @RequestParam(value = ID_DEMANDE, required = true) String idDemande) throws Exception {
		checkDroitGestionaireEfacture(securityProvider);
		final String businessId = efactureManager.refuserDemande(idDemande);
		if (!efactureManager.isReponseRecueDeEfacture(businessId)) {
			Flash.warning("Votre demande de refus a bien été prise en compte, Elle sera traitée dès que possible par le système E-facture.");
		}
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);
	}

	@RequestMapping(value = "/wait-signature.do", method = RequestMethod.POST)
	public String waitForSignature(@RequestParam(value = CTB, required = true) long ctbId, @RequestParam(value = ID_DEMANDE, required = true) String idDemande,
	                               @RequestParam(value = DATE_DEMANDE, required = true) RegDate dateDemande) throws Exception {
		checkDroitGestionaireEfacture(securityProvider);
		final String businessId = efactureManager.envoyerDocumentAvecNotificationEFacture(ctbId, TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, idDemande, dateDemande);
		if (!efactureManager.isReponseRecueDeEfacture(businessId)) {
			Flash.warning("Votre demande de confirmation d'inscription a bien été prise en compte, Elle sera traitée dès que possible par le système E-facture.");
		}
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);
	}

	@RequestMapping(value = "/wait-contact.do", method = RequestMethod.POST)
	public String waitForContact(@RequestParam(value = CTB, required = true) long ctbId, @RequestParam(value = ID_DEMANDE, required = true) String idDemande,
	                             @RequestParam(value = DATE_DEMANDE, required = true) RegDate dateDemande) throws Exception {
		checkDroitGestionaireEfacture(securityProvider);
		final String businessId = efactureManager.envoyerDocumentAvecNotificationEFacture(ctbId, TypeDocument.E_FACTURE_ATTENTE_CONTACT, idDemande, dateDemande);
		if (!efactureManager.isReponseRecueDeEfacture(businessId)) {
			Flash.warning("Votre demande de prise de contact a bien été prise en compte, Elle sera traitée dès que possible par le système E-facture.");
		}
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);
	}

	@Nullable
	private DestinataireAvecHistoView buildHistoDestinataire(long ctbId) {
		return efactureManager.getDestinataireAvecSonHistorique(ctbId);
	}

	@RequestMapping(value = "/quittancement/show.do", method = RequestMethod.GET)
	public String showQuittancementForm() {
		checkDroitQuittanceurEnSerie(securityProvider);
		return "efacture/form";
	}

	@RequestMapping(value = "/quittancement/beep.do", method = RequestMethod.POST)
	public String doQuittancement(@RequestParam(value = "noctb", required = true) Long noCtb) throws Exception {
		checkDroitQuittanceurEnSerie(securityProvider);
		final ResultatQuittancement resultatQuittancement = efactureManager.quittancer(noCtb);
		final String messageQuittancement = efactureManager.getMessageQuittancement(resultatQuittancement, noCtb);
		if (resultatQuittancement.isOk()) {
			Flash.message(messageQuittancement);
		}
		else {
			Flash.error(messageQuittancement);
		}
		return "redirect:/efacture/quittancement/show.do";
	}

	public void setEfactureManager(EfactureManager efactureManager) {
		this.efactureManager = efactureManager;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public static enum AddCommentActionType {
		SUSPEND("/efacture/suspend.do", "label.efacture.bouton.suspendre"),
		ACTIVATE("/efacture/activate.do", "label.efacture.bouton.activer");

		private final String url;
		private final String labelKey;

		private AddCommentActionType(String url, String labelKey) {
			this.url = url;
			this.labelKey = labelKey;
		}
	}

	@RequestMapping(value = "add-comment.do", method = RequestMethod.GET)
	public String showAddCommentForm(Model model, @RequestParam(value = CTB, required = true) long noCtb, @RequestParam(value = ACTION, required = true) AddCommentActionType action) {
		model.addAttribute(CTB, noCtb);
		model.addAttribute(ACTION_URL, String.format("%s?%s=%d", action.url, CTB, noCtb));
		model.addAttribute(LIBELLE_ACTION, messageSource.getMessage(action.labelKey, null, WebContextUtils.getDefaultLocale()));
		model.addAttribute(COMMAND, new FreeCommentView());
		model.addAttribute(MAXLEN, getMaxLengthForManualComment());
		return "tiers/edition/efacture/add-comment";
	}
}
