package ch.vd.uniregctb.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.GenericFilterBean;

import ch.vd.registre.web.filter.IAMUtil;
import ch.vd.uniregctb.interfaces.service.host.IfoSecProcedureImpl;
import ch.vd.uniregctb.interfaces.service.host.IfoSecProfilImpl;
import ch.vd.uniregctb.utils.UniregModeHelper;

/**
 * Filtre qui permet de bypasser en développement la sécurité IAM/IFOSec en fonction des paramètres du fichier unireg.properties.
 */
public class DevSecurityBypassProcessingFilter extends GenericFilterBean {

	private static final Logger LOGGER = Logger.getLogger(DevSecurityBypassProcessingFilter.class);
	
	private static final Set<String> DEV_ENVS = new HashSet<String>(Arrays.asList("Developpement", "Hudson", "Standalone", "Integration TE"));

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

		if (SecurityContextHolder.getContext().getAuthentication() == null && (SecurityDebugConfig.isIamDebug() || SecurityDebugConfig.isIfoSecDebug())) {

			final String environnement = UniregModeHelper.getEnvironnement();
			if (!DEV_ENVS.contains(environnement)) {
				LOGGER.warn("Le bypass des fonctionnalités de sécurité n'est disponible qu'en développement. Aucune fonction bypassée.");
			}
			else if (!SecurityDebugConfig.isIamDebug()) {
				LOGGER.warn("Le bypass des fonctionnalités IFOSec nécessite l'activation du bypass des fonctionnalités d'IAM. Aucune fonction bypassée.");
			}
			else {

				// Récupération des infos de bypass IAM
				final String visa = SecurityDebugConfig.getIamBypassUser();
				if (StringUtils.isBlank(visa)) {
					throw new UsernameNotFoundException("Le visa de l'utilisateur n'est pas renseigné dans la requête.");
				}

				final String firstName = SecurityDebugConfig.getIamBypassFirstName();
				final String lastName = SecurityDebugConfig.getIamBypassLastName();
				final String[] roles = IAMUtil.createRoles(SecurityDebugConfig.getIamBypassApplication(), SecurityDebugConfig.getIamBypassRoles());

				final UniregSecurityDetails details = new UniregSecurityDetails();
				details.setIamFirstName(firstName);
				details.setIamLastName(lastName);
				details.setIamRoles(roles);

				LOGGER.info(String.format("[BYPASS IAM] Ouverture de la session pour l'utilisateur %s %s", firstName, lastName));

				final List<GrantedAuthorityImpl> granted = new ArrayList<GrantedAuthorityImpl>();
				granted.add(new GrantedAuthorityImpl(visa));

				if (SecurityDebugConfig.isIfoSecDebug()) {
					// Récupération des infos de bypass IFOSec
					final Integer oid = Integer.valueOf(SecurityDebugConfig.getIfoSecBypassOID());
					final String oidSigle = SecurityDebugConfig.getIfoSecBypassOIDSigle();
					final IfoSecProfil profil = getBypassProfil(visa, oid, oidSigle);
					final List<GrantedAuthorityImpl> ifoSecGranted = IFOSecProfileProcessingFilter.getIfoSecGrantedAuthorities(profil);

					details.setIfoSecOID(oid);
					details.setIfoSecOIDSigle(oidSigle);
					details.setIfoSecProfil(profil);
					granted.addAll(ifoSecGranted);

					LOGGER.info(String.format("[BYPASS IFOSec] Choix de l'OID %d (%s) pour l'utilisateur %s %s", oid, oidSigle, firstName, lastName));
				}

				// On peut maintenant enregistrer le context de sécurité
				final User user = new User(visa, "noPwd", true, true, true, true, granted);
				final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, "noPwd", granted);
				authentication.setDetails(details);

				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}

	private static IfoSecProfil getBypassProfil(String visa, Integer oid, String oidSigle) {

		final IfoSecProfilImpl profil = new IfoSecProfilImpl();
		profil.setVisaOperateur(visa);

		// Les procédures
		final String procedureStr = SecurityDebugConfig.getIfoSecBypassProcedures(visa);
		final List<IfoSecProcedure> listProcedure = new ArrayList<IfoSecProcedure>();
		for (String procedure : procedureStr.split(", ")) {
			procedure = procedure.replace("[", "");
			procedure = procedure.replace("]", "");
			final IfoSecProcedureImpl proc = new IfoSecProcedureImpl();
			proc.setCode(procedure);
			proc.setCodeActivite("O");
			proc.setNumero(0);
			listProcedure.add(proc);
		}
		profil.setProcedures(listProcedure);

		return profil;
	}

}
