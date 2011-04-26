package ch.vd.uniregctb.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import ch.vd.infrastructure.model.CollectiviteAdministrative;
import ch.vd.infrastructure.model.impl.CollectiviteAdministrativeImpl;
import ch.vd.registre.base.utils.Assert;
import ch.vd.securite.model.Procedure;
import ch.vd.securite.model.ProfilOperateur;
import ch.vd.securite.model.impl.ProcedureImpl;
import ch.vd.securite.model.impl.ProfilOperateurImpl;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;

public class IFOSecAuthenticationProcessingFilter extends UsernamePasswordAuthenticationFilter {

	private static final Logger LOGGER = Logger.getLogger(IFOSecAuthenticationProcessingFilter.class);

	public static final String IFOSEC_OID_REQUEST_KEY = "_ifosec-oid-key";
	public static final String USER_OID_SIGLE = "OIDSigle";

	private ServiceSecuriteService serviceSecurite = null;

	public IFOSecAuthenticationProcessingFilter() {

		// Obligatoire, requis par la classe parente
		setFilterProcessesUrl("/");
		setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/authenticationFailed.do"));

		// Ce filtre n'est là que pour enrichir l'objet Authentication et au besoin demander l'OID.
		setContinueChainBeforeSuccessfulAuthentication(true);
	}

	/**
	 * Détermine si l'utilisateur doit s'autentifier ou non.
	 *
	 * @param request  la requête http
	 * @param response la réponse http
	 * @return <b>vrai</b> si l'utilisateur doit s'autentifier; <b>faux</b> s'il est déjà autentifié.
	 */
	@Override
	protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
		setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/authenticationFailed.do"));
		return SecurityDebugConfig.isReloadEachTime() || getProfilOperateurCourant() == null;
	}

	@Override
	// Note (msi,bnm) : on arrive ici que si la méthode requiresAuthentication() à retourné true, c'est-à-dire que si le profile utilisateur courant est nul.
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Request: " + request.getRequestURL());
		}
		setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/authenticationFailed.do"));

		// Récupération du contexte de sécurité
		SecurityContext context = SecurityContextHolder.getContext();
		if (context == null) {
			throw new AuthenticationCredentialsNotFoundException("Aucun contexte de sécurité trouvé");
		}

		// Récupération des données d'authentification
		Authentication auth = context.getAuthentication();
		if ((auth == null)) {
			throw new AuthenticationCredentialsNotFoundException("Aucun contexte de sécurité trouvé");
		}

		// on recherche l'OID dans la requete (si on vient du formulaire de selection d'OID)
		final OfficeImpot oi;
		try {
			oi = getOfficeImpot(request, getVisaOperateur(auth));
		}
		catch (MultipleOIDFoundException e) {
			setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/chooseOID.do"));
			throw e;
		}

		// positionnement des roles IFOSECs
		setDetails(request, auth, oi);

		//pour mettre à jour GrantedAuthority
		Authentication authResult = this.getAuthenticationManager().authenticate(auth);
		context.setAuthentication(authResult);

		return authResult;
	}

	/**
	 * Retourne l'office d'impôt associé à l'utilisateur pour la session courante. <p> Il y a deux cas de figure : <ol> <li><b>s'il s'agit de la première requête de l'utilisateur (ouverture de
	 * session)</b>, aucun office d'impôt n'est sélectionné. Dans ce cas, si l'utilisateur possède un seul office d'impôt, on le retourne. Par contre, si l'utilisateur possède plusieurs offices d'impôts,
	 * on le redirige sur une page spéciale qui lui permettra de sélectionner celui qu'il veut.</li> <li><b>s'il la session est déjà ouverte</b>, c'est que l'utilisateur possèdait plusieurs offices
	 * d'impôt et qu'il vient d'en sélectionner un depuis la page spéciale. Dans ce cas, on retourne l'office d'impôt sélectionné par l'utilisateur. </ol>
	 *
	 * @param request la requête http
	 * @param visa    le visa de l'opérateur connecté
	 * @return l'office d'impôt à utiliser pour la session courante.
	 */
	@SuppressWarnings({"unchecked"})
	private OfficeImpot getOfficeImpot(HttpServletRequest request, String visa) {
		final OfficeImpot oi;

		final String oidStr = request.getParameter(IFOSEC_OID_REQUEST_KEY);
		if (oidStr == null) {
			// l'OID n'est pas renseigné, on tente de le détermine automatiquement (dans le cas ou l'opérateur ne travaille que pour une collectivité administrative)
			oi = determineOfficeImpot(visa);
		}
		else {
			// Si on arrive ici, c'est que l'utilisateur a dû choisir un office d'impôt parmi une liste.
			final int oid = Integer.parseInt(oidStr);

			final List<CollectiviteAdministrative> list = serviceSecurite.getCollectivitesUtilisateur(visa);
			if (list == null || list.isEmpty()) {
				throw new AuthenticationFailedException("Authentification échouée : l'opérateur " + visa + " ne possède aucune collectivités");
			}

			CollectiviteAdministrative c = null;
			for (CollectiviteAdministrative ca : list) {
				if (ca.getNoColAdm() == oid) {
					c = ca;
					break;
				}
			}
			if (c == null) {
				throw new AuthenticationFailedException("L'OID choisi [" + oid + "] ne fait pas partie des collectivités de l'utilisateur [" + visa + "]");
			}

			oi = new OfficeImpot(oid, c.getSigle());
		}
		return oi;
	}

	private static class OfficeImpot {
		private Integer id;
		private String sigle;

		private OfficeImpot(Integer id, String sigle) {
			this.id = id;
			this.sigle = sigle;
		}

		public Integer getId() {
			return id;
		}

		public String getSigle() {
			return sigle;
		}
	}

	/**
	 * Tente de déterminer l'office d'impôt à utiliser pour ouvrir une session pour l'utilisateur spécifié.
	 *
	 * @param visa le visa de l'utilisateur
	 * @return l'office d'impôt trouvé.
	 * @throws MultipleOIDFoundException si l'utilisateur possède plus d'un office d'impôt.
	 */
	private OfficeImpot determineOfficeImpot(String visa) throws MultipleOIDFoundException {
		final OfficeImpot oi;
		if (isDebug()) {
			final Integer oid = Integer.valueOf(SecurityDebugConfig.getIfoSecBypassOID());
			final String sigle = SecurityDebugConfig.getIfoSecBypassOIDSigle();
			oi = new OfficeImpot(oid, sigle);
		}
		else {
			final List<CollectiviteAdministrative> collectivites;
			try {
				collectivites = serviceSecurite.getCollectivitesUtilisateur(visa);
			}
			catch (Exception e) {
				LOGGER.error("impossible de récupérer les collectivités de l'opérateur", e);
				throw new AuthenticationFailedException("Authentification échouée : impossible de récupérer les collectivités de l'opérateur", e);
			}
			if (collectivites == null || collectivites.isEmpty()) {
				LOGGER.warn("l'opérateur " + visa + " ne possède aucune collectivités");
				throw new AuthenticationFailedException("Authentification échouée : l'opérateur " + visa + " ne possède aucune collectivités");
			}

			if (collectivites.size() > 1) {
				throw new MultipleOIDFoundException("Plusieurs OID trouvé, obligé de choisir", collectivites);
			}

			Assert.isEqual(1, collectivites.size());
			final CollectiviteAdministrative ca = collectivites.get(0);
			oi = new OfficeImpot(ca.getNoColAdm(), ca.getSigle());
		}
		return oi;
	}

	/**
	 * Récupère le visa opérateur à partir d'un objet Authentication
	 *
	 * @param auth l'objet Authentication
	 * @return le visa opérateur
	 */
	private static String getVisaOperateur(Authentication auth) {
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
	 *
	 * @param request la requête http
	 * @param auth    l'objet Authentication à enrichir
	 * @param oi      l'OID de la collectivité administrative
	 */
	protected void setDetails(HttpServletRequest request, Authentication auth, OfficeImpot oi) {

		setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/authenticationFailed.do"));

		try {
			final String visaOperateur = getVisaOperateur(auth);
			final ProfilOperateur profil = getProfilOperateur(visaOperateur, oi.getId());

			// Enregistrement du profil dans l'objet Authentication
			final UniregSecurityDetails details = (UniregSecurityDetails) auth.getDetails();
			details.setIfoSecOID(oi.getId());
			details.setIfoSecOIDSigle(oi.getSigle());
			details.setIfoSecProfil(profil);
			request.getSession().setAttribute(USER_OID_SIGLE, oi.getSigle());

			if (LOGGER.isDebugEnabled()) {
				final List<Procedure> procedures = getProcedure(profil);
				if (procedures != null) {
					String procLog = "";
					for (Procedure proc : procedures) {
						if (proc.getCode().substring(0, 2).equals("UR"))
							procLog += proc.getCode() + " ";
					}
					LOGGER.debug("VISA:" + visaOperateur + " OID:" + oi.getSigle() + "(" + oi.getId() + ")" + " IfoSec:" + procLog);
				}
			}
		}
		catch (Exception e) {
			LOGGER.error("Impossible de récupérer les procedures auxquelles a le droit l'opérateur", e);
			throw new AuthenticationFailedException("Authentification échouée : " + "Impossible de récupérer les procedures auxquelles a le droit l'opérateur");
		}
	}

	/**
	 * Retour le profil d'opérateur d'un utilisateur pour un office d'impôt donné.
	 *
	 * @param visa le visa de l'utilisateur
	 * @param oid  le numéro de l'office d'impôt
	 * @return le profil de l'opérateur
	 */
	private ProfilOperateur getProfilOperateur(String visa, Integer oid) {
		final ProfilOperateur profil;
		if (isDebug()) {
			final ProfilOperateur profil1;// On va chercher les profils de debug spécifiés dans le fichier unireg.properties
			final String oidSigle = SecurityDebugConfig.getIfoSecBypassOIDSigle();
			final ProfilOperateurImpl profilImpl = new ProfilOperateurImpl();
			profilImpl.setVisaOperateur(visa);
			//la collectivité
			final CollectiviteAdministrativeImpl collec = new CollectiviteAdministrativeImpl();
			collec.setNoColAdm(oid);
			collec.setSigle(oidSigle);
			collec.setNomComplet1(oidSigle);
			collec.setSigleCanton("VD");
			collec.setNoCCP("");
			profilImpl.setCollectivite(collec);

			final String procedureStr = SecurityDebugConfig.getIfoSecBypassProcedures(visa);

			// Les procédures
			final List<Procedure> listProcedure = new ArrayList<Procedure>();
			for (String procedure : procedureStr.split(", ")) {
				procedure = procedure.replace("[", "");
				procedure = procedure.replace("]", "");
				final ProcedureImpl proc = new ProcedureImpl();
				proc.setCode(procedure);
				proc.setCodeActivite("O");
				proc.setNumero(0);
				listProcedure.add(proc);
			}
			profilImpl.setProcedures(listProcedure);
			profil1 = profilImpl;
			profil = profil1;
		}
		else {
			// Récupération des procedures auxquelles a le droit l'opérateur
			if (oid != null) {
				profil = serviceSecurite.getProfileUtilisateur(visa, oid);
			}
			else {
				profil = new ProfilOperateurImpl();
			}
		}
		return profil;
	}

	@SuppressWarnings("unchecked")
	private static List<Procedure> getProcedure(ProfilOperateur profil) {
		return profil.getProcedures();
	}

	/**
	 * @return le profil opérateur courant.
	 */
	private ProfilOperateur getProfilOperateurCourant() {
		// Récupération du contexte de sécurité
		SecurityContext context = SecurityContextHolder.getContext();
		if (context == null) {
			return null;
		}

		// Récupération des données d'authentification
		Authentication auth = context.getAuthentication();
		if (auth == null) {
			return null;
		}

		// Récupération du profil dans l'objet Authentication
		UniregSecurityDetails details = (UniregSecurityDetails) auth.getDetails();
		return details.getIfoSecProfil();
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) throws IOException {
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecuriteService(ServiceSecuriteService serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}

	private boolean isDebug() {
		return SecurityDebugConfig.isIfoSecDebug();
	}
}
