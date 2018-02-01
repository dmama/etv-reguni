package ch.vd.unireg.identification.contribuable.tooltip;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseGenerique;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;

@Controller
@RequestMapping(value = "/identification/tooltip")
public class IdentificationTooltipController {

	private TiersDAO tiersDAO;
	private ServiceCivilService civilService;
	private AdresseService adresseService;

	public void setCivilService(ServiceCivilService civilService) {
		this.civilService = civilService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@RequestMapping(value = "/adresse.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String getAdresseToolTip(Model model, @RequestParam(value = "noCtb") long noCtb) {

		IdentificationAdresseTooltipView view = null;
		try {
			final Tiers tiers = tiersDAO.get(noCtb);
			final AdresseGenerique adresseGenerique = adresseService.getDerniereAdresseVaudoise(tiers, TypeAdresseFiscale.DOMICILE);
			if (adresseGenerique != null) {
				final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(tiers, adresseGenerique.getDateDebut(), TypeAdresseFiscale.DOMICILE, false);
				Assert.notNull(adresseEnvoi);

				final String complements = adresseEnvoi.getComplement();
				final String rue = (adresseEnvoi.getRueEtNumero() == null ? null : adresseEnvoi.getRueEtNumero().getRueEtNumero());
				final String localite = (adresseEnvoi.getNpaEtLocalite() == null ? null : adresseEnvoi.getNpaEtLocalite().toString());
				final String pays = (adresseEnvoi.getPays() == null ? null : adresseEnvoi.getPays().getNomCourt());
				final AdresseGenerique.SourceType source = adresseGenerique.getSource().getType();

				view = new IdentificationAdresseTooltipView(rue, complements, localite, pays, source);
			}
		}
		catch (Exception e) {
			view = new IdentificationAdresseTooltipView(e.getMessage());
		}

		if (view == null) {
			view = new IdentificationAdresseTooltipView();
		}
		model.addAttribute("command", view);
		return "/identification/tooltip/adresse";
	}

	@RequestMapping(value = "individu.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String getIndividuToolTip(Model model, @RequestParam(value = "noCtb") long noCtb) {
		IdentificationIndividuTooltipView view = null;
		final Tiers tiers = tiersDAO.get(noCtb);
		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.isHabitantVD()) {
				final long noIndividu = pp.getNumeroIndividu();
				final Individu individu = civilService.getIndividu(noIndividu, null, AttributeIndividu.NATIONALITES);
				view = new IdentificationIndividuTooltipView(individu);
			}
		}

		if (view == null) {
			view = new IdentificationIndividuTooltipView(null);
		}
		model.addAttribute("command", view);
		return "/identification/tooltip/individu";
	}

}
