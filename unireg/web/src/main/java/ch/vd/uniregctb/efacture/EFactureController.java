package ch.vd.uniregctb.efacture;

import org.jetbrains.annotations.Nullable;
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
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/efacture")
public class EFactureController {

	private static final String CTB = "ctb";
	private static final String ID_DEMANDE = "idDemande";
	private static final String DATE_DEMANDE = "dateDemande";
	private EfactureManager efactureManager;


	private static void checkDroitGestionaireEfacture() {
		if (!SecurityProvider.isAnyGranted(Role.GEST_EFACTURE)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour interagir avec les états e-facture d'un contribuable");
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

		if (!SecurityProvider.isAnyGranted(Role.VISU_ALL, Role.GEST_EFACTURE)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour visualiser l'historique e-facture d'un contribuable");
		}

		return buildHistoDestinataire(ctbId);
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	public String edit(Model model, @RequestParam(value = CTB, required = true) long ctbId) {

		checkDroitGestionaireEfacture();

		final DestinataireAvecHistoView histo = buildHistoDestinataire(ctbId);
		if (histo == null) {
			throw new ActionException("Le contribuable ne possède aucun état e-facture avec lequel il est possible d'interagir");
		}

		model.addAttribute("histo", histo);
		return "tiers/edition/efacture/edit";
	}

	@RequestMapping(value = "/suspend.do", method = RequestMethod.POST)
	public String suspend(@RequestParam(value = CTB, required = true) long ctbId) throws Exception{
		checkDroitGestionaireEfacture();
		final String businessId = efactureManager.suspendreContribuable(ctbId);
		if(!efactureManager.isReponseReçuDeEfacture(businessId)){
			Flash.warning("Votre demande de suspension a bien été prise en compte, Elle sera traitée dès que possible par le système E-facture.");
		}
		// TODO jde à faire
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);
	}

	@RequestMapping(value = "/activate.do", method = RequestMethod.POST)
	public String activate(@RequestParam(value = CTB, required = true) long ctbId) throws Exception{
		checkDroitGestionaireEfacture();

		final String businessId = efactureManager.activerContribuable(ctbId);
		if(!efactureManager.isReponseReçuDeEfacture(businessId)){
			Flash.warning("Votre demande d'activation a bien été prise en compte, Elle sera traitée dès que possible par le système E-facture.");
		}
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);
	}

	@RequestMapping(value = "/validate.do", method = RequestMethod.POST)
	public String validate(@RequestParam(value = CTB, required = true) long ctbId, @RequestParam(value = ID_DEMANDE, required = true) String idDemande) throws Exception {
		checkDroitGestionaireEfacture();
		final String businessId = efactureManager.accepterDemande(idDemande);
		if(!efactureManager.isReponseReçuDeEfacture(businessId)){
			Flash.warning("Votre demande de validation a bien été prise en compte, Elle sera traitée dès que possible par le système E-facture.");
		}
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);
	}

	@RequestMapping(value = "/refuse.do", method = RequestMethod.POST)
	public String refuse(@RequestParam(value = CTB, required = true) long ctbId, @RequestParam(value = ID_DEMANDE, required = true) String idDemande) throws Exception {
		checkDroitGestionaireEfacture();
		final String businessId = efactureManager.refuserDemande(idDemande);
		if(!efactureManager.isReponseReçuDeEfacture(businessId)){
			Flash.warning("Votre demande de refus a bien été prise en compte, Elle sera traitée dès que possible par le système E-facture.");
		}
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);
	}

	@RequestMapping(value = "/wait-signature.do", method = RequestMethod.POST)
	public String waitForSignature(@RequestParam(value = CTB, required = true) long ctbId, @RequestParam(value = ID_DEMANDE, required = true) String idDemande, @RequestParam(value = DATE_DEMANDE, required = true) RegDate dateDemande) throws Exception {
		checkDroitGestionaireEfacture();
		final String businessId = efactureManager.envoyerDocumentAvecNotificationEFacture(ctbId, TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, idDemande, dateDemande);
		if(!efactureManager.isReponseReçuDeEfacture(businessId)){
			Flash.warning("Votre demande de confirmation d'inscription a bien été prise en compte, Elle sera traitée dès que possible par le système E-facture.");
		}
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);

		//TODO BNM message flash pour confirmer l'envoi + envoi d'info à E-facture

	}

	@RequestMapping(value = "/wait-contact.do", method = RequestMethod.POST)
	public String waitForContact(@RequestParam(value = CTB, required = true) long ctbId, @RequestParam(value = ID_DEMANDE, required = true) String idDemande, @RequestParam(value = DATE_DEMANDE, required = true) RegDate dateDemande) throws Exception {
		checkDroitGestionaireEfacture();
		final String businessId = efactureManager.envoyerDocumentAvecNotificationEFacture(ctbId, TypeDocument.E_FACTURE_ATTENTE_CONTACT, idDemande, dateDemande);
		if(!efactureManager.isReponseReçuDeEfacture(businessId)){
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
		if (!SecurityProvider.isAnyGranted(Role.GEST_QUIT_EFACTURE)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour gérer les retours de confirmation d'inscription e-Facture");
		}
		return "efacture/form";
	}

	@RequestMapping(value = "/quittancement/beep.do", method = RequestMethod.POST)
	public String doQuittancement(@RequestParam(value = "noctb") Long noCtb) throws Exception{
		ResultatQuittancement resultatQuittancement = efactureManager.quittancer(noCtb);
		if (resultatQuittancement.isOk()) {
			Flash.message(efactureManager.getMessageQuittancement(resultatQuittancement, noCtb));
		} else {
			Flash.error(efactureManager.getMessageQuittancement(resultatQuittancement, noCtb));
		}
		return "redirect:/efacture/quittancement/show.do";
	}

	public void setEfactureManager(EfactureManager efactureManager) {
		this.efactureManager = efactureManager;
	}
}
