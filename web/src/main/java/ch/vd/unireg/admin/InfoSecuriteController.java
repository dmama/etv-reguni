package ch.vd.unireg.admin;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.interfaces.service.ServiceSecuriteBypass;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.security.ProcedureSecurite;
import ch.vd.unireg.security.ProfileOperateur;
import ch.vd.unireg.security.Role;

/**
 * Ce contrôleur affiche des informations sur les procédures de sécurité allouées sur l'utilisateur courant. Il est donc en read-only.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Controller
public class InfoSecuriteController {

	private ServiceSecuriteService securiteService;

	@RequestMapping(value = "/admin/ifosec.do")
	public String index() {
		return "redirect:securite.do";
	}

	@RequestMapping(value = "/admin/securite.do")
	public String index(Model mav) {

		final String visa = AuthenticationHelper.getCurrentPrincipal();
		final Integer oid = AuthenticationHelper.getCurrentOID();

		final ProfileOperateur profile = securiteService.getProfileUtilisateur(visa, oid);
		List<ProcedureSecurite> proceduresUnireg = null;
		List<Role> rolesSecuriteByPass = null;
		List<ProcedureSecurite> proceduresAutres = null;
		if (profile != null) {
			proceduresUnireg = getProceduresUnireg(profile);
			rolesSecuriteByPass = getProceduresSecuriteByPass(profile);
			proceduresAutres= getProceduresAutres(profile);
		}

		mav.addAttribute("visa", visa);
		mav.addAttribute("oid", oid);
		mav.addAttribute("proceduresUnireg", proceduresUnireg);
		mav.addAttribute("rolesSecuriteByPass", rolesSecuriteByPass);
		mav.addAttribute("proceduresAutres", proceduresAutres);
		mav.addAttribute("roles", Role.values());

		return "admin/securite";
	}

	private List<ProcedureSecurite> getProceduresUnireg(ProfileOperateur profile) {
		return profile.getProcedures().stream()
				.filter(p -> p.getCode().startsWith("UR"))
				.collect(Collectors.toList());
	}

	private List<Role> getProceduresSecuriteByPass(ProfileOperateur profile) {
		if (securiteService instanceof ServiceSecuriteBypass) {
			final ServiceSecuriteBypass securiteBypass = (ServiceSecuriteBypass) this.securiteService;
			return securiteBypass.getBypass(profile.getVisaOperateur()).stream()
					.sorted(Comparator.naturalOrder())
					.collect(Collectors.toList());
		}
		else {
			return Collections.emptyList();
		}
	}

	private List<ProcedureSecurite> getProceduresAutres(ProfileOperateur profile) {
		return profile.getProcedures().stream()
				.filter(p -> !p.getCode().startsWith("UR"))
				.collect(Collectors.toList());
	}


	public void setSecuriteService(ServiceSecuriteService securiteService) {
		this.securiteService = securiteService;
	}
}