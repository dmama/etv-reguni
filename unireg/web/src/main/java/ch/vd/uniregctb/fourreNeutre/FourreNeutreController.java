package ch.vd.uniregctb.fourreNeutre;

import javax.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.uniregctb.fourreNeutre.view.FourreNeutreView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;

@Controller
@RequestMapping(value = "/fourre-neutre")
public class FourreNeutreController {
	private SecurityProviderInterface securityProvider;

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	private static void checkDroitFourreNeutre(SecurityProviderInterface securityProvider) {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.GEST_FOURRE_NEUTRE)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour gérer les fourres neutres");
		}
	}

	@RequestMapping(value = "/imprimer.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String showImprimerForm(@RequestParam(value = "numero", required = true) Long tiersId, Model model) throws Exception{
		checkDroitFourreNeutre(securityProvider);
		final FourreNeutreView view = new FourreNeutreView();
		view.setTiersId(tiersId);
		view.setPeriodeFiscale(2016);
		model.addAttribute("command",view);
		return "fourre-neutre/imprimer";
	}

	@RequestMapping(value = "/imprimer.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String doImprimerForm(@Valid @ModelAttribute("command") final FourreNeutreView view, BindingResult result) throws Exception{
		checkDroitFourreNeutre(securityProvider);
		return "redirect:/tiers/visu.do?id=" + view.getTiersId();

	}
}
