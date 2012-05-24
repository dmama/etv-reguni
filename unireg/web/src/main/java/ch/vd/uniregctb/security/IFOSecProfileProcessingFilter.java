package ch.vd.uniregctb.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.filter.GenericFilterBean;

import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.interfaces.service.host.IfoSecProfilImpl;

/**
 * Ce filtre est utilisé pour récupérer le profile IFOSec de l'utilisateur connecté et le stocker dans le context de sécurité. Il ne fait aucun contrôle d'accès par lui-même.
 */
public class IFOSecProfileProcessingFilter extends GenericFilterBean {

	private ServiceSecuriteService serviceSecurite;

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

		final AbstractAuthenticationToken authentication = (AbstractAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		final IFOSecDetails details = (IFOSecDetails) authentication.getDetails();

		if (details.getIfoSecProfil() == null) {

			// On récupère le visa et l'OID de l'utilisateur
			final String visa = new PrincipalSid(authentication).getPrincipal();
			final Integer oid = details.getIfoSecOID();
			if (StringUtils.isBlank(visa) || oid == null) {
				throw new IllegalArgumentException();
			}

			// On peut maintenant renseigner le profile
			final IfoSecProfil profil = getProfilOperateur(visa, oid);
			details.setIfoSecProfil(profil);

			// On ajoute les procédures Ifosec autorisées dans la liste (nécessaire pour que le jsp tag <authz> fonctionne)
			final List<GrantedAuthorityImpl> granted = new ArrayList<GrantedAuthorityImpl>();
			granted.add(new GrantedAuthorityImpl(profil.getVisaOperateur()));
			granted.addAll(getIfoSecGrantedAuthorities(profil));

			// Pour mettre-à-jour la liste des procédures autorisées, il faut créer un nouvel object 'user'
			final User user = new User(visa, "noPwd", true, true, true, true, granted);
			final UsernamePasswordAuthenticationToken newAuthentication = new UsernamePasswordAuthenticationToken(user, "noPwd", granted);
			newAuthentication.setDetails(details);

			SecurityContextHolder.getContext().setAuthentication(newAuthentication);
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}

	@SuppressWarnings({"unchecked"})
	protected static List<GrantedAuthorityImpl> getIfoSecGrantedAuthorities(IfoSecProfil profil) {

		final List<GrantedAuthorityImpl> granted;

		final List<IfoSecProcedure> procedures = profil.getProcedures();
		if (procedures != null) {
			granted = new ArrayList<GrantedAuthorityImpl>();
			for (IfoSecProcedure procedure : procedures) {
				final String ifoSec = procedure.getCode();
				final Role role = Role.fromIfoSec(ifoSec);
				if (role != null) {
					granted.add(new GrantedAuthorityImpl(role.getCode()));
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
	private IfoSecProfil getProfilOperateur(String visa, Integer oid) {
		final IfoSecProfil profil;
		// Récupération des procedures auxquelles a le droit l'opérateur
		if (oid != null) {
			profil = serviceSecurite.getProfileUtilisateur(visa, oid);
		}
		else {
			profil = new IfoSecProfilImpl();
		}
		return profil;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecurite(ServiceSecuriteService serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}
}
