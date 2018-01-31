package ch.vd.uniregctb.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersService;

@Controller
public class RecalculFlagHabitantController {

	private static final Logger LOGGER = LoggerFactory.getLogger(RecalculFlagHabitantController.class);

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
	@RequestMapping(value = "/admin/refreshFlagHabitant.do", method = RequestMethod.POST)
	public String forceRecalcul(@RequestParam(value = "id", required = true) long id) {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
		}

		final Tiers tiers = tiersService.getTiers(id);
		if (tiers instanceof PersonnePhysique && ((PersonnePhysique) tiers).isConnuAuCivil()) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;

			LOGGER.info(String.format("Demande de recalcul manuel du flag 'habitant' du contribuable %d (avec éviction du cache des données de l'individu %d)", pp.getNumero(), pp.getNumeroIndividu()));
			dataEventService.onIndividuChange(pp.getNumeroIndividu());

			try {
				final TiersService.UpdateHabitantFlagResultat res = tiersService.updateHabitantFlag(pp, pp.getNumeroIndividu(), null);

				LOGGER.info(String.format("Résultat du recalcul manuel du flag 'habitant' du contribuable %d : %s",
				                          pp.getNumero(), res == TiersService.UpdateHabitantFlagResultat.PAS_DE_CHANGEMENT ? String.format("%s (%b)", res, pp.getHabitant()) : res));

				final String message;
				switch (res) {
					case PAS_DE_CHANGEMENT:
						message = String.format("le contribuable reste %s.", pp.isHabitantVD() ? "habitant" : "non-habitant");
						break;
					case CHANGE_EN_HABITANT:
						message = "le contribuable est maintenant habitant.";
						break;
					case CHANGE_EN_NONHABITANT:
						message = "le contribuable est maintenant non-habitant.";
						break;
					default:
						throw new IllegalArgumentException("Résultat inattendu : " + res);
				}
				Flash.message(String.format("Le flag \"habitant\" a été recalculé : %s.", message));
			}
			catch (TiersException e) {
				Flash.error("Erreur : " + e.getMessage());
			}
		}
		else {
			Flash.error(String.format("Le tiers %s n'est pas une personne physique connue au civil.", FormatNumeroHelper.numeroCTBToDisplay(id)));
		}
		return "redirect:/tiers/visu.do?id=" + id;
	}
}
