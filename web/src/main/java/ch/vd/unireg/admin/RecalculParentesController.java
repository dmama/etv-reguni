package ch.vd.unireg.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;

@Controller
public class RecalculParentesController {

	private static final Logger LOGGER = LoggerFactory.getLogger(RecalculParentesController.class);

	private TiersService tiersService;
	private SecurityProviderInterface securityProvider;
	private DataEventService dataEventService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/admin/refreshParentes.do", method = RequestMethod.POST)
	public String forceRecalcul(@RequestParam(value = "id", required = true) long id) {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
		}

		final Tiers tiers = tiersService.getTiers(id);
		if (tiers instanceof PersonnePhysique && ((PersonnePhysique) tiers).isHabitantVD()) {

			// [SIFISC-14023] rafaîchissement du cache civil avant le recalcul des parentés
			final Long noIndividu = ((PersonnePhysique) tiers).getNumeroIndividu();
			if (noIndividu != null) {
				LOGGER.info(String.format("Demande manuelle de rafraîchissement des relations de parenté du tiers %d (avec éviction du cache des données de l'individu %d)", tiers.getNumero(), noIndividu));
				dataEventService.onIndividuChange(noIndividu);
			}
			else {
				LOGGER.info(String.format("Demande manuelle de rafraîchissement des relations de parenté du tiers %d", tiers.getNumero()));
			}

			tiersService.refreshParentesSurPersonnePhysique((PersonnePhysique) tiers, true);
			Flash.message("Les relations de parenté ont été rafraîchies.");
		}
		else {
			Flash.error(String.format("Le tiers %s n'est pas une personne physique habitante.", FormatNumeroHelper.numeroCTBToDisplay(id)));
		}
		return "redirect:/tiers/visu.do?id=" + id;
	}
}
