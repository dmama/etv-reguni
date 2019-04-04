package ch.vd.unireg.security;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.GenericFilterBean;

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.interfaces.service.host.ProcedureSecuriteImpl;
import ch.vd.unireg.interfaces.service.host.ProfileOperateurImpl;
import ch.vd.unireg.utils.UniregModeHelper;

/**
 * Filtre qui permet de bypasser en développement la sécurité IAM/RefSec en fonction des paramètres du fichier unireg.properties.
 */
public class DevSecurityBypassProcessingFilter extends GenericFilterBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(DevSecurityBypassProcessingFilter.class);
	
	private static final Set<String> DEV_ENVS = new HashSet<>(Arrays.asList("Developpement", "Hudson", "Integration TE"));

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

		if (AuthenticationHelper.getAuthentication() == null && (SecurityDebugConfig.isIamDebug() || SecurityDebugConfig.isSecurityDebug())) {

			final String environnement = UniregModeHelper.getEnvironnement();
			if (!DEV_ENVS.contains(environnement)) {
				LOGGER.warn("Le bypass de la sécurité n'est disponible qu'en développement. Aucune fonction bypassée.");
			}
			else if (!SecurityDebugConfig.isIamDebug()) {
				LOGGER.warn("Le bypass de la sécurité nécessite l'activation du bypass des fonctionnalités d'IAM. Aucune fonction bypassée.");
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

				final List<GrantedAuthority> granted = new ArrayList<>();
				granted.add(new SimpleGrantedAuthority(visa));

				if (SecurityDebugConfig.isSecurityDebug()) {
					// Récupération des infos de bypass de sécurité
					final Integer oid = Integer.valueOf(SecurityDebugConfig.getSecurityBypassOID());
					final String oidSigle = SecurityDebugConfig.getSecurityBypassOIDSigle();
					final ProfileOperateur profil = getBypassProfil(visa, oid, oidSigle);
					final List<GrantedAuthority> grantedAuthorities = SecuriteProfileProcessingFilter.getGrantedAuthorities(profil);

					details.setOID(oid);
					details.setOIDSigle(oidSigle);
					details.setProfil(profil);
					granted.addAll(grantedAuthorities);

					LOGGER.info(String.format("[BYPASS Sécurité] Choix de l'OID %d (%s) pour l'utilisateur %s %s", oid, oidSigle, firstName, lastName));
				}

				// On peut maintenant enregistrer le context de sécurité
				final User user = new User(visa, "noPwd", true, true, true, true, granted);
				final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, "noPwd", granted);
				authentication.setDetails(details);

				AuthenticationHelper.setAuthentication(authentication);
			}
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}

	private static ProfileOperateur getBypassProfil(String visa, Integer oid, String oidSigle) {

		// Les procédures
		final String procedureStr = SecurityDebugConfig.getSecurityBypassProcedures(visa);

		final Stream<String> codesStream;
		if ("ALL".equals(procedureStr)) {
			codesStream = Arrays.stream(Role.values())
					.map(Role::getCodeProcedure);
		}
		else {
			codesStream = Arrays.stream(procedureStr.split("[, ]"))
					.map(code -> code.replace("[", "").replace("]", ""));
		}
		final List<ProcedureSecurite> listProcedure = codesStream
				.filter(StringUtils::isNotBlank)
				.map(code -> new ProcedureSecuriteImpl(code, null))
				.collect(Collectors.toList());

		return new ProfileOperateurImpl(visa, listProcedure);
	}

}
