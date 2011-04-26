package ch.vd.uniregctb.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import ch.vd.securite.model.Procedure;
import ch.vd.securite.model.ProfilOperateur;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.security.IfoSecService;
import ch.vd.uniregctb.security.Role;

/**
 * Ce contrôleur affiche des informations sur les procédures Ifosec allouées sur l'utilisateur courant. Il est donc en read-only.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Controller
public class InfoIFOSecController {

	private ServiceSecuriteService serviceSecurite;
	private IfoSecService ifoSecService;

	@RequestMapping(value = "/admin/ifosec.do")
	public String index(Model mav) {

		final String visa = AuthenticationHelper.getCurrentPrincipal();
		final Integer oid = AuthenticationHelper.getCurrentOID();

		final ProfilOperateur profile = serviceSecurite.getProfileUtilisateur(visa, oid);
		List<Procedure> proceduresUnireg = null;
		List<Role> rolesIfoSecByPass = null;
		List<Procedure> proceduresAutres = null;
		if (profile != null) {
			proceduresUnireg = getProceduresUnireg(profile);
			rolesIfoSecByPass = getProceduresIfoSecByPass(profile);
			proceduresAutres= getProceduresAutres(profile);
		}

		mav.addAttribute("visa", visa);
		mav.addAttribute("oid", oid);
		mav.addAttribute("proceduresUnireg", proceduresUnireg);
		mav.addAttribute("rolesIfoSecByPass", rolesIfoSecByPass);
		mav.addAttribute("proceduresAutres", proceduresAutres);
		mav.addAttribute("roles", Role.values());

		return "admin/ifosec";
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

	private List<Role> getProceduresIfoSecByPass(ProfilOperateur profile) {
		final List<Role> list = new ArrayList<Role>(ifoSecService.getBypass(profile.getVisaOperateur()));
		Collections.sort(list);
		return list;
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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecurite(ServiceSecuriteService serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIfoSecService(IfoSecService ifoSecService) {
		this.ifoSecService = ifoSecService;
	}
}