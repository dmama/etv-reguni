package ch.vd.uniregctb.registrefoncier.immeuble;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.uniregctb.common.MimeTypeHelper;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

@Controller
@RequestMapping(value = "/registrefoncier/immeuble")
public class ImmeubleRFController {

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez pas les droits IfoSec de visualisation des données du Registre Foncier";

	private TiersDAO tiersDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private RegistreFoncierService registreFoncierService;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setImmeubleRFDAO(ImmeubleRFDAO immeubleRFDAO) {
		this.immeubleRFDAO = immeubleRFDAO;
	}

	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		this.registreFoncierService = registreFoncierService;
	}

	@SecurityCheck(rolesToCheck = {Role.VISU_ALL}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "graph.do", method = RequestMethod.GET, produces = MimeTypeHelper.MIME_PLAINTEXT + "; charset=UTF-8")
	@ResponseBody
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String graph(@RequestParam(required = false) String idRF,
	                    @RequestParam(required = false) String egrid,
	                    @RequestParam(required = false) Long id,
	                    @RequestParam(required = false) Long ctbId,
	                    @RequestParam(required = false, defaultValue = "false") boolean showEstimations) {

		final ImmeubleGraph graph = new ImmeubleGraph();

		if (ctbId != null) {
			// on affiche le graphe des droits et immeubles propriété de ce seul contribuable
			final Tiers tiers = tiersDAO.get(ctbId);
			if (tiers == null || !(tiers instanceof Contribuable)) {
				return null;
			}

			final Contribuable ctb = (Contribuable) tiers;
			final List<DroitRF> droits = registreFoncierService.getDroitsForCtb(ctb, true, false);
			final List<DroitProprieteRF> droitsPropriete = droits.stream()
					.filter(DroitProprieteRF.class::isInstance)
					.map(DroitProprieteRF.class::cast)
					.collect(Collectors.toList());

			graph.process(droitsPropriete);
		}
		else {
			// on affiche le graphe des droits, immeubles et propriétaires de toute la grappe
			final ImmeubleRF immeuble;
			if (id != null) {
				immeuble = immeubleRFDAO.get(id);
			}
			else if (StringUtils.isNotBlank(idRF)) {
				immeuble = immeubleRFDAO.find(new ImmeubleRFKey(idRF), null);
			}
			else if (StringUtils.isNotBlank(egrid)) {
				immeuble = immeubleRFDAO.findByEgrid(egrid);
			}
			else {
				throw new IllegalArgumentException("Aucun paramètre d'identification de l'immeuble n'a été donné.");
			}

			if (immeuble == null) {
				return null;
			}

			graph.process(immeuble, true);
		}

		return graph.toDot(showEstimations);
	}
}
