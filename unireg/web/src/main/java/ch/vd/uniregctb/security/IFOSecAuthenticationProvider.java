package ch.vd.uniregctb.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ch.vd.securite.model.Procedure;
import ch.vd.securite.model.ProfilOperateur;

public class IFOSecAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

	private static final Logger LOGGER = Logger.getLogger(IFOSecAuthenticationProvider.class);
    private String defaultPassword = "secret";

 	@Override
	protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication)
			throws AuthenticationException {
		// rien a faire
	}

	@Override
	protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {

		if(username == null || "".equals(username)) {
            LOGGER.error("retrieveUser(): username is null");
            throw new UsernameNotFoundException("username is null");
        }

		UserDetails result = new User(username, defaultPassword, true, true, true, true,
				createGrantedAuthorities(authentication));
        return result;
	}

    public void setDefaultPassword(String password)
    {
        defaultPassword = password;
    }

    @SuppressWarnings("unchecked")
	protected Collection<GrantedAuthority> createGrantedAuthorities(Authentication authentication)
    {
	    List<GrantedAuthority> grantedAuthoritiesIAM = new ArrayList<GrantedAuthority>(authentication.getAuthorities());

        UniregSecurityDetails details = (UniregSecurityDetails)authentication.getDetails();
        if (details == null) {
        	return grantedAuthoritiesIAM;
        }


        // IAM
        int authIAMLemgth = 0;
        if(grantedAuthoritiesIAM != null){
        	authIAMLemgth = grantedAuthoritiesIAM.size();
        }


        // IfoSec
        ProfilOperateur profil = details.getIfoSecProfil();
		List procedures = profil.getProcedures();
		int nbProcedures = (procedures == null ? 0 : procedures.size());


		int nbAuthorities = nbProcedures + authIAMLemgth;
		String[] codeProcedures = new String[nbAuthorities];
		// IfoSec
		for (int i = 0; i < nbProcedures; i++) {
			Procedure procedure = (Procedure) procedures.get(i);
			// correspondance procédure ifosec - code role
			String code = procedure.getCode();
			Role role = Role.fromIfoSec(code);
			codeProcedures[i] = (role == null ? code : role.getCode());
		}
		// IAM
		for (int i = 0; i < authIAMLemgth; i++) {
			codeProcedures[i + nbProcedures] = grantedAuthoritiesIAM.get(i).getAuthority();
		}

		// Crée les auth IAM + IfoSec
	    List<GrantedAuthority> grantedAuthorities = createGrantedAuthorities(codeProcedures);

        // Logging
        if (LOGGER.isTraceEnabled()) {
	        String logString = "";
			for (int i = 0; i < nbAuthorities; i++) {
				GrantedAuthority auth = grantedAuthorities.get(i);
				if (!logString.equals("")) {
					logString += ":";
				}
				logString += auth.getAuthority();
			}
			LOGGER.trace("Procs for " + authentication.getName() + " => " + logString);
		}

		return grantedAuthorities;
    }

	private List<GrantedAuthority> createGrantedAuthorities(final String[] roles)
	{
		List<GrantedAuthority> result = new ArrayList<GrantedAuthority>(roles.length);
	    for (String role : roles) {
		    result.add(new GrantedAuthorityImpl(role));
	    }
	    return result;
	}
}
