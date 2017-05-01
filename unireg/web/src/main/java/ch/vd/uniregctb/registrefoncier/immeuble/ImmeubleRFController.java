package ch.vd.uniregctb.registrefoncier.immeuble;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.uniregctb.common.MimeTypeHelper;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;

@Controller
@RequestMapping(value = "/registrefoncier/immeuble")
public class ImmeubleRFController {

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez pas les droits IfoSec de visualisation des données du Registre Foncier";

	private ImmeubleRFDAO immeubleRFDAO;

	public void setImmeubleRFDAO(ImmeubleRFDAO immeubleRFDAO) {
		this.immeubleRFDAO = immeubleRFDAO;
	}

	@SecurityCheck(rolesToCheck = {Role.VISU_ALL}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "graph.do", method = RequestMethod.GET, produces = MimeTypeHelper.MIME_PLAINTEXT + "; charset=UTF-8")
	@ResponseBody
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String graph(@RequestParam(required = false) String idRF,
	                    @RequestParam(required = false) String egrid,
	                    @RequestParam(required = false) Long id,
	                    @RequestParam(required = false, defaultValue = "false") boolean showEstimations) {

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

		final ImmeubleGraph graph = new ImmeubleGraph();
		graph.process(immeuble);

		return graph.toDot(showEstimations);
	}
}
