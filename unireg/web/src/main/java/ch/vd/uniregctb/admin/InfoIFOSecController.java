package ch.vd.uniregctb.admin;

import ch.vd.securite.model.Procedure;
import ch.vd.securite.model.ProfilOperateur;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.security.Role;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Ce contrôleur affiche des informations sur les procédures Ifosec allouées sur l'utilisateur courant. Il est donc en read-only.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"UnusedDeclaration"})
public class InfoIFOSecController extends ParameterizableViewController {

	// private static final Logger LOGGER = Logger.getLogger(InfoController.class);

	private ServiceSecuriteService serviceSecurite;

	public void setServiceSecurite(ServiceSecuriteService serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ModelAndView mav = super.handleRequestInternal(request, response);

		final String visa = AuthenticationHelper.getCurrentPrincipal();
		final Integer oid = AuthenticationHelper.getCurrentOID();

		final ProfilOperateur profile = serviceSecurite.getProfileUtilisateur(visa, oid);
		List<Procedure> proceduresUnireg = null;
		List<Procedure> proceduresAutres = null;
		if (profile != null) {
			proceduresUnireg = getProceduresUnireg(profile);
			proceduresAutres= getProceduresAutres(profile);
		}

		mav.addObject("visa", visa);
		mav.addObject("oid", oid);
		mav.addObject("proceduresUnireg", proceduresUnireg);
		mav.addObject("proceduresAutres", proceduresAutres);
		mav.addObject("roles", Role.values());

		return mav;
	}

	@SuppressWarnings({"unchecked"})
	private List<Procedure> getProceduresUnireg(ProfilOperateur profile) {
		return (List<Procedure>) CollectionUtils.select((List<Procedure>) profile.getProcedures(), new Predicate() {
			public boolean evaluate(Object object) {
				Procedure p = (Procedure) object;
				return p.getCode().startsWith("UR");
			}
		});
	}

	@SuppressWarnings({"unchecked"})
	private List<Procedure> getProceduresAutres(ProfilOperateur profile) {
		return (List<Procedure>) CollectionUtils.select((List<Procedure>) profile.getProcedures(), new Predicate() {
			public boolean evaluate(Object object) {
				Procedure p = (Procedure) object;
				return !p.getCode().startsWith("UR");
			}
		});
	}
}