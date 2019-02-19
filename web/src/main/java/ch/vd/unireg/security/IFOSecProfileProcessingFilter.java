package ch.vd.unireg.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.filter.GenericFilterBean;

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.interfaces.service.host.ProfileOperateurImpl;

/**
 * Ce filtre est utilisé pour récupérer le profile IFOSec de l'utilisateur connecté et le stocker dans le context de sécurité. Il ne fait aucun contrôle d'accès par lui-même.
 */
public class IFOSecProfileProcessingFilter extends GenericFilterBean {

	private ServiceSecuriteService serviceSecurite;

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

		final AbstractAuthenticationToken authentication = AuthenticationHelper.getAuthentication();
		final IFOSecDetails details = (IFOSecDetails) authentication.getDetails();

		if (details.getIfoSecProfil() == null) {

			// On récupère le visa et l'OID de l'utilisateur
			final String visa = new PrincipalSid(authentication).getPrincipal();
			final Integer oid = details.getIfoSecOID();
			if (StringUtils.isBlank(visa) || oid == null) {
				throw new IllegalArgumentException();
			}

			// On peut maintenant renseigner le profile
			final ProfileOperateur profil = getProfilOperateur(visa, oid);
			details.setIfoSecProfil(profil);

			// On ajoute les procédures Ifosec autorisées dans la liste (nécessaire pour que le jsp tag <authz> fonctionne)
			final List<GrantedAuthority> granted = new ArrayList<>();
			granted.add(new SimpleGrantedAuthority(profil.getVisaOperateur()));
			granted.addAll(getIfoSecGrantedAuthorities(profil));

			// Pour mettre-à-jour la liste des procédures autorisées, il faut créer un nouvel object 'user'
			final User user = new User(visa, "noPwd", true, true, true, true, granted);
			final UsernamePasswordAuthenticationToken newAuthentication = new UsernamePasswordAuthenticationToken(user, "noPwd", granted);
			newAuthentication.setDetails(details);

			AuthenticationHelper.setAuthentication(newAuthentication);
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}

	@SuppressWarnings({"unchecked"})
	protected static List<GrantedAuthority> getIfoSecGrantedAuthorities(ProfileOperateur profil) {

		final List<GrantedAuthority> granted;

		final List<IfoSecProcedure> procedures = profil.getProcedures();
		if (procedures != null) {
			granted = new ArrayList<>();
			for (IfoSecProcedure procedure : procedures) {
				final String ifoSec = procedure.getCode();
				final Role role = Role.fromIfoSec(ifoSec);
				if (role != null) {
					granted.add(new SimpleGrantedAuthority(role.getCode()));
				}
			}
		}
		else {
			granted = Collections.emptyList();
		}

		return granted;
	}

	/**
	 * Retour le profil d'opérateur d'un utilisateur pour un office d'impôt donné.
	 *
	 * @param visa le visa de l'utilisateur
	 * @param oid  le numéro de l'office d'impôt
	 * @return le profil de l'opérateur
	 */
	private ProfileOperateur getProfilOperateur(String visa, Integer oid) {
		final ProfileOperateur profil;
		// Récupération des procedures auxquelles a le droit l'opérateur
		if (oid != null) {
			profil = serviceSecurite.getProfileUtilisateur(visa, oid);
		}
		else {
			profil = new ProfileOperateurImpl();
		}
		return profil;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecurite(ServiceSecuriteService serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}
}
