package ch.vd.unireg.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.security.IfoSecProcedure;
import ch.vd.unireg.security.IfoSecService;
import ch.vd.unireg.security.ProfileOperateur;
import ch.vd.unireg.security.Role;

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

		final ProfileOperateur profile = serviceSecurite.getProfileUtilisateur(visa, oid);
		List<IfoSecProcedure> proceduresUnireg = null;
		List<Role> rolesIfoSecByPass = null;
		List<IfoSecProcedure> proceduresAutres = null;
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

	private List<IfoSecProcedure> getProceduresUnireg(ProfileOperateur profile) {
		return profile.getProcedures().stream()
				.filter(p -> p.getCode().startsWith("UR"))
				.collect(Collectors.toList());
	}

	private List<Role> getProceduresIfoSecByPass(ProfileOperateur profile) {
		final List<Role> list = new ArrayList<>(ifoSecService.getBypass(profile.getVisaOperateur()));
		Collections.sort(list);
		return list;
	}

	private List<IfoSecProcedure> getProceduresAutres(ProfileOperateur profile) {
		return profile.getProcedures().stream()
				.filter(p -> !p.getCode().startsWith("UR"))
				.collect(Collectors.toList());
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