package ch.vd.uniregctb.documentfiscal;

import javax.validation.Valid;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.EditiqueErrorHelper;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.RetourEditiqueControllerHelper;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatErreur;
import ch.vd.uniregctb.editique.EditiqueResultatReroutageInbox;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping("/autresdocs")
public class AutreDocumentFiscalController {

	private SecurityProviderInterface securityProvider;
	private AutreDocumentFiscalManager autreDocumentFiscalManager;
	private TiersMapHelper tiersMapHelper;
	private RetourEditiqueControllerHelper retourEditiqueControllerHelper;

	private static final Map<Role, Set<TypeAutreDocumentFiscalEmettableManuellement>> TYPES_DOC_ALLOWED = buildTypesDocAllowed();

	private static Map<Role, Set<TypeAutreDocumentFiscalEmettableManuellement>> buildTypesDocAllowed() {
		final Map<Role, Set<TypeAutreDocumentFiscalEmettableManuellement>> map = new EnumMap<>(Role.class);
		map.put(Role.ENVOI_AUTORISATION_RADIATION, EnumSet.of(TypeAutreDocumentFiscalEmettableManuellement.AUTORISATION_RADIATION));
		map.put(Role.ENVOI_DEMANDE_BILAN_FINAL, EnumSet.of(TypeAutreDocumentFiscalEmettableManuellement.DEMANDE_BILAN_FINAL));
		map.put(Role.ENVOI_LETTRE_TYPE_INFO_LIQUIDATION, EnumSet.of(TypeAutreDocumentFiscalEmettableManuellement.LETTRE_TYPE_INFORMATION_LIQUIDATION));
		return map;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setAutreDocumentFiscalManager(AutreDocumentFiscalManager autreDocumentFiscalManager) {
		this.autreDocumentFiscalManager = autreDocumentFiscalManager;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setRetourEditiqueControllerHelper(RetourEditiqueControllerHelper retourEditiqueControllerHelper) {
		this.retourEditiqueControllerHelper = retourEditiqueControllerHelper;
	}

	@InitBinder(value = "print")
	public void initPrintBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, "dateReference", new RegDateEditor(true, false, false));
		binder.setValidator(new ImprimerAutreDocumentFiscalValidator());
	}

	private void checkAnyRight() throws AccessDeniedException {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ENVOI_AUTORISATION_RADIATION, Role.ENVOI_DEMANDE_BILAN_FINAL, Role.ENVOI_LETTRE_TYPE_INFO_LIQUIDATION)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour gérer les autres documents fiscaux.");
		}
	}

	@RequestMapping(value = "/edit-list.do", method = RequestMethod.GET)
	public String showEditList(Model model, @RequestParam(value = "pmId") long idEntreprise) {
		checkAnyRight();
		return showEditList(model, new ImprimerAutreDocumentFiscalView(idEntreprise, null));
	}

	private Set<TypeAutreDocumentFiscalEmettableManuellement> getTypesAutreDocumentFiscalEmettablesManuellement() {
		final Set<TypeAutreDocumentFiscalEmettableManuellement> allowed = EnumSet.noneOf(TypeAutreDocumentFiscalEmettableManuellement.class);
		for (Map.Entry<Role, Set<TypeAutreDocumentFiscalEmettableManuellement>> entry : TYPES_DOC_ALLOWED.entrySet()) {
			if (SecurityHelper.isGranted(securityProvider, entry.getKey())) {
				allowed.addAll(entry.getValue());
			}
		}
		return allowed;
	}

	private Map<TypeAutreDocumentFiscalEmettableManuellement, String> getTypesAutreDocumentFiscalEmettableManuellement() {
		final Map<TypeAutreDocumentFiscalEmettableManuellement, String> all = tiersMapHelper.getTypesAutreDocumentFiscalEmettableManuellement();
		final Set<TypeAutreDocumentFiscalEmettableManuellement> allowed = getTypesAutreDocumentFiscalEmettablesManuellement();
		final Map<TypeAutreDocumentFiscalEmettableManuellement, String> filtered = new LinkedHashMap<>(all);        // conservation de l'ordre !
		filtered.keySet().retainAll(allowed);
		return filtered;
	}

	private String showEditList(Model model, ImprimerAutreDocumentFiscalView view) {
		final long idEntreprise = view.getNoEntreprise();
		model.addAttribute("pmId", idEntreprise);
		model.addAttribute("documents", autreDocumentFiscalManager.getAutresDocumentsFiscauxSansSuivi(idEntreprise));
		model.addAttribute("typesDocument", getTypesAutreDocumentFiscalEmettableManuellement());
		model.addAttribute("print", view);
		model.addAttribute("isRadieeRCOuDissoute", autreDocumentFiscalManager.hasAnyEtat(idEntreprise, TypeEtatEntreprise.DISSOUTE, TypeEtatEntreprise.RADIEE_RC));
		return "tiers/edition/pm/autresdocs";
	}

	@RequestMapping(value = "/print.do", method = RequestMethod.POST)
	public String imprimerNouveauDocument(@Valid @ModelAttribute("print") final ImprimerAutreDocumentFiscalView view, BindingResult bindingResult, Model model) throws IOException {
		if (bindingResult.hasErrors()) {
			return showEditList(model, view);
		}

		final Set<TypeAutreDocumentFiscalEmettableManuellement> allowed = getTypesAutreDocumentFiscalEmettablesManuellement();
		if (!allowed.contains(view.getTypeDocument())) {
			throw new AccessDeniedException("Vous ne possédez aucun des droits IfoSec permettant d'émettre ce type de document.");
		}

		final EditiqueResultat resultat;
		try {
			resultat = autreDocumentFiscalManager.createAndPrint(view);
		}
		catch (AutreDocumentFiscalException e) {
			throw new ActionException("Impossible d'imprimer le document voulu", e);
		}

		final String redirect = String.format("redirect:/autresdocs/edit-list.do?pmId=%d", view.getNoEntreprise());

		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox> inbox =
				new RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox>() {
					@Override
					public String doJob(EditiqueResultatReroutageInbox resultat) {
						return redirect;
					}
				};

		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> erreur = new RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur>() {
			@Override
			public String doJob(EditiqueResultatErreur resultat) {
				Flash.error(EditiqueErrorHelper.getMessageErreurEditique(resultat));
				return redirect;
			}
		};

		return retourEditiqueControllerHelper.traiteRetourEditiqueAfterRedirect(resultat,
		                                                                        view.getTypeDocument().name().toLowerCase(),
		                                                                        redirect,
		                                                                        false,
		                                                                        inbox,
		                                                                        null,
		                                                                        erreur);
	}
}
