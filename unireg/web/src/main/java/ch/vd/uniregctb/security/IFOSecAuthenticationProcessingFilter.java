package ch.vd.uniregctb.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationCredentialsNotFoundException;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.ui.webapp.AuthenticationProcessingFilter;
import org.acegisecurity.userdetails.UserDetails;
import org.apache.log4j.Logger;

import ch.vd.infrastructure.model.CollectiviteAdministrative;
import ch.vd.infrastructure.model.impl.CollectiviteAdministrativeImpl;
import ch.vd.securite.model.Procedure;
import ch.vd.securite.model.ProfilOperateur;
import ch.vd.securite.model.impl.ProcedureImpl;
import ch.vd.securite.model.impl.ProfilOperateurImpl;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;

public class IFOSecAuthenticationProcessingFilter  extends AuthenticationProcessingFilter {

	private static final Logger LOGGER = Logger.getLogger(IFOSecAuthenticationProcessingFilter.class);

//	private static final String IFOSEC_PROCEDURES_ATTRIBUTE_KEY = "_ifosec-procedures-key";
    public static final String IFOSEC_OID_REQUEST_KEY = "_ifosec-oid-key";
	protected static final String IFOSEC_PROFILE_KEY = "_ifosec-oid-key";
	public static final String IFOSEC_OID_USER_LIST = "collectivites";
	public static final String USER_OID_SIGLE = "OIDSigle";

    private ServiceSecuriteService serviceSecurite = null;

    private boolean redirectResponse;

    /**
     *
     */
    public IFOSecAuthenticationProcessingFilter() {
        redirectResponse = false;

        /*
         * Obligatoire, requis par la classe parente
         */
        setDefaultTargetUrl("/");
        setAuthenticationFailureUrl("/authenticationFailed.do");

        /*
         * Ce filtre n'est là que pour enrichir l'objet Authentication
         * et au besoin demander l'OID.
         */
        setContinueChainBeforeSuccessfulAuthentication(true);
    }

    public boolean isRedirectResponse() {
        return redirectResponse;
    }

    public void setRedirectResponse(boolean redirectResponse) {
        this.redirectResponse = redirectResponse;
    }

    @Override
	public Authentication attemptAuthentication(HttpServletRequest request) throws AuthenticationException {

    	if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Request: " + request.getRequestURL());
		}
    	setAuthenticationFailureUrl("/authenticationFailed.do");
    	/*
    	 * Récupération du contexte Acegi
    	 */
        SecurityContext context = SecurityContextHolder.getContext();
        if(context == null) {
        	throw new AuthenticationCredentialsNotFoundException("Aucun contexte de sécurité trouvé");
        }

        /*
         * Récupération des données d'authentification
         */
        Authentication auth = context.getAuthentication();
        if ( (auth == null) ) {
        	throw new AuthenticationCredentialsNotFoundException("Aucun contexte de sécurité trouvé");
        }

    	/*
    	 * on recherche l'OID dans la requete (si on vient du formulaire de selection d'OID)
    	 */
    	String oidStr = request.getParameter(IFOSEC_OID_REQUEST_KEY);

    	// l'OID n'est pas renseigné, on tente de le récupérer (dans le cas ou l'opérateur ne travaille que pour une collectivité administrative)
    	if (oidStr == null) {
    		if(!isDebug()){
				try {
					String visa = getVisaOperateur(auth);
	    			List<?> collectivites = serviceSecurite.getCollectivitesUtilisateur(visa);
					if(collectivites != null && collectivites.size() < 1){
						LOGGER.warn("l'opérateur " + visa + " ne possède aucune collectivités");
						throw new AuthenticationFailedException("Authentification échouée : " +
								"l'opérateur " + visa + " ne possède aucune collectivités");
					}
					if (collectivites != null && collectivites.size() == 1) {
						oidStr = String.valueOf( ((CollectiviteAdministrative) collectivites.get(0)).getNoColAdm() );
					}

					/*
					 * Si l'operateur n'est rattaché à aucune
					 */
					if (collectivites != null && collectivites.size() > 1 ) {
						request.getSession().setAttribute(IFOSEC_OID_USER_LIST, collectivites);
						throw new MultipleOIDFoundException("Plusieurs OID trouvé, obligé de choisir");
					}
				}
				catch (MultipleOIDFoundException e) {
					setAuthenticationFailureUrl("/chooseOID.do");
					throw e;
				}
				catch (AuthenticationFailedException e) {
					throw e;
				}
				catch (Exception e) {
					LOGGER.error("impossible de récupérer les collectivités de l'opérateur");
					throw new AuthenticationFailedException("Authentification échouée : " +
							"impossible de récupérer les collectivités de l'opérateur", e);
				}
    		}
    	}

        /*
         * positionnement des roles IFOSECs
         */
    	Integer oid = null;
    	if (oidStr != null) {
    		oid = Integer.parseInt(oidStr);
    	}
        setDetails(request, auth, oid);
        //pour mettre à jour GrantedAuthority
        Authentication authResult = this.getAuthenticationManager().authenticate(auth);
        context.setAuthentication(authResult);
        //Authentication authResult1 = AuthenticationHelper.getAuthentication();
		return authResult;
    }

    /**
     * Récupère le visa opérateur à partir d'un objet Authentication
     * @param auth	l'objet Authentication
     * @return le visa opérateur
     */
    private String getVisaOperateur(Authentication auth) {
    	String result = null;
        Object principal = auth.getPrincipal();

        if (principal instanceof UserDetails) {
            result = ((UserDetails) principal).getUsername();
        }
        else if (principal != null) {
            result = principal.toString();
        }
        else {
            LOGGER.warn("getVisaOperateur: le principal est null");
        }

        return result;
	}

    /**
     * Enrichit l'objet Authentication avec les données IFOSEC rattachée à une collectivité administrative particulière.
     * @param auth	l'objet Authentication à enrichir
     * @param oid	l'OID de la collectivité administrative
     */
	protected void setDetails(HttpServletRequest request, Authentication auth, Integer oid) {

		setAuthenticationFailureUrl("/authenticationFailed.do");

		String visaOperateur = getVisaOperateur(auth);
		try {
			ProfilOperateur profil = null;
			if (isDebug()) {
				oid = Integer.parseInt(SecurityDebugConfig.getIfoSecBypassOID());
				String oidSigle = SecurityDebugConfig.getIfoSecBypassOIDSigle();
    			ProfilOperateurImpl profilImpl = new ProfilOperateurImpl();
				profilImpl.setVisaOperateur(visaOperateur);
				//la collectivité
				CollectiviteAdministrativeImpl collec = new CollectiviteAdministrativeImpl();
				collec.setNoColAdm(oid);
				collec.setSigle(oidSigle);
				collec.setNomComplet1(oidSigle);
				collec.setSigleCanton("VD");
				collec.setNoCCP("");
				profilImpl.setCollectivite(collec);

				String procedureStr = SecurityDebugConfig.getIfoSecBypassProcedures(visaOperateur);

				// Les procédures
				List<Procedure> listProcedure = new ArrayList<Procedure>();
				String[] procedureArray = procedureStr.split(", ");
				for(String procedure : procedureArray){
					procedure = procedure.replace("[", "");
					procedure = procedure.replace("]", "");
					ProcedureImpl proc = new ProcedureImpl();
					proc.setCode(procedure);
					proc.setCodeActivite("O");
					proc.setNumero(0);
					listProcedure.add(proc);
				}
				profilImpl.setProcedures(listProcedure);
				profil = profilImpl;
			}
			else {
				/**
				 * Récupération des procedures auxquelles a le droit l'opérateur
				 */
				if (oid != null) {
					profil = serviceSecurite.getProfileUtilisateur(visaOperateur, oid);
				}
				else {
					profil = new ProfilOperateurImpl();
				}
			}

			UniregSecurityDetails details = getAuthDetails(auth);

			String oidSigle = "";
			if(profil.getCollectivite() != null) {
				oidSigle = profil.getCollectivite().getSigle();
				if (LOGGER.isDebugEnabled()&& getProcedure(profil) != null) {
					String procLog = "";
					for(Procedure proc : getProcedure(profil)){
						if(proc.getCode().substring(0, 2).equals("UR"))
							procLog += proc.getCode() + " ";
					}
					LOGGER.debug("VISA:" + visaOperateur + " OID:" + oidSigle + "(" + oid +")" + " IfoSec:" + procLog);
				}
			}

			/**
			 * Enregistrement du profil dans l'objet Authentication
			 */
			details.setIfoSecOID(oid);
			details.setIfoSecOIDSigle(oidSigle);
			details.setIfoSecProfil(profil);
	        request.getSession().setAttribute(USER_OID_SIGLE, oidSigle);
		}
		catch (Exception e) {
			LOGGER.error("Impossible de récupérer les procedures auxquelles a le droit l'opérateur", e);
			throw new AuthenticationFailedException("Authentification échouée : " +
				"Impossible de récupérer les procedures auxquelles a le droit l'opérateur");
		}
    }

	@SuppressWarnings("unchecked")
	private List<Procedure> getProcedure(ProfilOperateur profil) {
		return profil.getProcedures();
	}

	private UniregSecurityDetails getAuthDetails(Authentication auth) {
		UniregSecurityDetails d = (UniregSecurityDetails)auth.getDetails();
		return d;
	}

	/*
	 * (non-Javadoc)
	 * @see org.acegisecurity.ui.AbstractProcessingFilter#requiresAuthentication(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
    @Override
	protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {

    	//LOGGER.debug("Request: "+request.getRequestURL());

    	setAuthenticationFailureUrl("/authenticationFailed.do");

    	boolean requires = true;
    	if (!SecurityDebugConfig.isReloadEachTime()) {
	    	ProfilOperateur profil = getProfilOperateurCourant(request);
	    	if (profil != null) {
	    		requires = false;
	    	}
    	}
        return requires;
    }

    /**
     * Récupère le profil opérateur courant.
     * @return
     */
    private ProfilOperateur getProfilOperateurCourant(HttpServletRequest request) {
    	/*
    	 * Récupération du contexte Acegi
    	 */
        SecurityContext context = SecurityContextHolder.getContext();
        if(context == null)
        	return null;

        /*
         * Récupération des données d'authentification
         */
        Authentication auth = context.getAuthentication();
        if ( (auth == null) )
        	return null;

		/**
		 * Récupération du profil dans l'objet Authentication
		 */
		UniregSecurityDetails details = getAuthDetails(auth);
		return details.getIfoSecProfil();
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult)
        throws IOException
    {
    	/*if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Request: " + request.getRequestURL());
		}*/
    }

	/**
	 * @param serviceSecurite the serviceSecurite to set
	 */
	public void setServiceSecuriteService(ServiceSecuriteService serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}

	private boolean isDebug() {
		return SecurityDebugConfig.isIfoSecDebug();
	}
}
