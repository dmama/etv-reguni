package ch.vd.uniregctb.tiers.vignette;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Niveau;

@Controller
public class VignetteController {

	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private AdresseService adresseService;
	private ServiceInfrastructureService infraService;
	private MessageSource messageSource;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Retourne les informations contenues dans la vignette d'un tiers sous forme de données brutes (JSON, voir http://blog.springsource.com/2010/01/25/ajax-simplifications-in-spring-3-0/)
	 *
	 * @param numero le numéro du tiers
	 * @return les informations spécifiques à la vignette pour ce tiers
	 * @throws ch.vd.uniregctb.security.AccessDeniedException
	 *          si l'utilisateur ne possède les droits de visualisation suffisants.
	 */
	@RequestMapping(value = "/tiers/vignette-info.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public VignetteView info(@RequestParam("numero") long numero,
	                          @RequestParam(value = "fillActions", defaultValue = "false") boolean fillActions,
	                          @RequestParam(value = "fillAdresses", defaultValue = "false") boolean fillAdresses,
	                          @RequestParam(value = "fillEnsemble", defaultValue = "false") boolean fillEnsemble,
	                          @RequestParam(value = "fillRoles", defaultValue = "false") boolean fillRoles,
	                          @RequestParam(value = "fillUrlVers", defaultValue = "false") boolean fillUrlVers) throws AccessDeniedException {


		final Tiers tiers = tiersDAO.get(numero);
		if (tiers == null) {
			return null;
		}

		// contrôle effectué après le chargement du tiers pour éviter un exception s'il n'existe pas.
		final Niveau acces = SecurityProvider.getDroitAcces(numero);
		if (acces == null) {
			return new VignetteView(String.format("Vous ne possédez pas les droits de visualisation sur le contribuable n°%s.", FormatNumeroHelper.numeroCTBToDisplay(numero)));
		}

		return new VignetteView(tiers, fillEnsemble, fillAdresses, fillRoles, fillUrlVers, fillActions, tiersService, adresseService, infraService, messageSource);
	}
}
