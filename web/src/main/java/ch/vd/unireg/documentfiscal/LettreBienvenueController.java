package ch.vd.unireg.documentfiscal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;

@Controller
@RequestMapping(value = "/autresdocs/lettrebienvenue")
public class LettreBienvenueController {

	private SecurityProviderInterface securityProvider;
	private AutreDocumentFiscalManager autreDocumentFiscalManager;

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setAutreDocumentFiscalManager(AutreDocumentFiscalManager autreDocumentFiscalManager) {
		this.autreDocumentFiscalManager = autreDocumentFiscalManager;
	}

	private static void checkDroitQuittanceurEnSerie(SecurityProviderInterface securityProvider) {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits pour gérer les retours en masse de lettres de bienvenue.");
		}
	}

	@RequestMapping(value = "/quittancement/show.do", method = RequestMethod.GET)
	public String showQuittancementForm() {
		checkDroitQuittanceurEnSerie(securityProvider);
		return "documentfiscal/lettrebienvenue/quittance";
	}

	@RequestMapping(value = "/quittancement/beep.do", method = RequestMethod.POST)
	public String doQuittancement(@RequestParam(value = "noctb", required = true) long noCtb) throws Exception {
		checkDroitQuittanceurEnSerie(securityProvider);
		final ResultatQuittancement result = autreDocumentFiscalManager.quittanceLettreBienvenuePourCtb(noCtb, RegDate.get());
		if (result.isOk()) {
			Flash.message(String.format("La lettre de bienvenue de l'entreprise %s a été quittancée avec succès.", FormatNumeroHelper.numeroCTBToDisplay(noCtb)));
		}
		else {
			Flash.error(result.getCauseErreur(noCtb));
		}
		return "redirect:/autresdocs/lettrebienvenue/quittancement/show.do";
	}
}
