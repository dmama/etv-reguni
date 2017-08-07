package ch.vd.unireg.wsclient.host.interfaces;


import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.infrastructure.model.rest.ListeCollectiviteAdministrative;
import ch.vd.infrastructure.model.rest.TypeCollectivite;
import ch.vd.securite.model.rest.ListeOperateurs;
import ch.vd.securite.model.rest.Operateur;
import ch.vd.securite.model.rest.ProfilOperateur;


public class ServiceSecuriteClientImpl implements ServiceSecuriteClient, InitializingBean {
	private WebClientPool wcPool = new WebClientPool();
	private String securitePath = "securite";
	private String collectiviteUtilisateurPath = "collectiviteUtilisateur";
	private String collectiviteUtilisateurCommunicationTierPath = "collectiviteUtilisateurCommunicationTier";
	private String profilUtilisateurPath = "profilUtilisateur";
	private String operateursPath = "operateurs";
	private String operateurByIndividuNoTechnique = "operateur/individuNoTechnique";
	private String operateurByVisa = "operateur/visa";
	private String pingPath = "ping";

	public ServiceSecuriteClientImpl() {
	}

	public void setBaseUrl(String baseUrl) {
		StringBuilder s = new StringBuilder();
		s.append(baseUrl).append("/").append(securitePath);
		this.wcPool.setBaseUrl(s.toString());
	}

	public void setUsername(String username) {
		this.wcPool.setUsername(username);
	}

	public void setPassword(String password) {
		this.wcPool.setPassword(password);
	}

	public void afterPropertiesSet() throws Exception {
		this.wcPool.init();
	}

	public ListeCollectiviteAdministrative getCollectivitesUtilisateur(String visaOperateur) throws SecuriteException {
		WebClient wc = this.wcPool.borrowClient(600000);

		ListeCollectiviteAdministrative e;
		try {
			wc.path(this.collectiviteUtilisateurPath);
			wc.path(visaOperateur);

			try {
				e = (ListeCollectiviteAdministrative)wc.get(ListeCollectiviteAdministrative.class);
			} catch (WebApplicationException var7) {
				if(var7.getResponse().getStatus()== Response.Status.NOT_FOUND.getStatusCode()){
					return null;
				}
				throw new ServiceSecuriteClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public ListeCollectiviteAdministrative getCollectivitesUtilisateurCommunicationTier(String visaOperateur) throws ServiceSecuriteClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		ListeCollectiviteAdministrative e;
		try {
			wc.path(this.collectiviteUtilisateurCommunicationTierPath);
			wc.path(visaOperateur);

			try {
				e = (ListeCollectiviteAdministrative)wc.get(ListeCollectiviteAdministrative.class);
			} catch (WebApplicationException var7) {
				if(var7.getResponse().getStatus()== Response.Status.NOT_FOUND.getStatusCode()){
					return null;
				}
				throw new ServiceSecuriteClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public ProfilOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) throws ServiceSecuriteClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		ProfilOperateur e;
		try {
			wc.path(this.profilUtilisateurPath);
			wc.path(visaOperateur);
			wc.path(Integer.valueOf(codeCollectivite));

			try {
				e = (ProfilOperateur)wc.get(ProfilOperateur.class);
			} catch (WebApplicationException var8) {
				if(var8.getResponse().getStatus()== Response.Status.NOT_FOUND.getStatusCode()){
					return null;
				}
				throw new ServiceSecuriteClientException(var8);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public ListeOperateurs getOperateurs(TypeCollectivite[] types) throws ServiceSecuriteClientException {
		WebClient wc = this.wcPool.borrowClient(600000);
		String collectivites  = ServiceHelper.extractCodeTypeCollectivite(types);;

		ListeOperateurs e;
		try {
			wc.path(this.operateursPath);
			if(StringUtils.isNotEmpty(collectivites)) {
				wc.query("collectivites", collectivites);
			}

			try {
				e = (ListeOperateurs)wc.get(ListeOperateurs.class);
			} catch (WebApplicationException var8) {
				if(var8.getResponse().getStatus()== Response.Status.NOT_FOUND.getStatusCode()){
					return null;
				}
				throw new ServiceSecuriteClientException(var8);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public Operateur getOperateur(long individuNoTechnique) throws ServiceSecuriteClientException {
		return this.getOperateur(individuNoTechnique, true);
	}

	public Operateur getOperateurTous(long individuNoTechnique) throws ServiceSecuriteClientException {
		return this.getOperateur(individuNoTechnique, false);
	}

	private Operateur getOperateur(long individuNoTechnique, boolean seulementActif) throws ServiceSecuriteClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		Operateur e;
		try {
			wc.path(this.operateurByIndividuNoTechnique);
			wc.path(Long.valueOf(individuNoTechnique));
			wc.query("seulementActif", new Object[]{Boolean.valueOf(seulementActif)});

			try {
				e = (Operateur)wc.get(Operateur.class);
			} catch (WebApplicationException var9) {
				if(var9.getResponse().getStatus()== Response.Status.NOT_FOUND.getStatusCode()){
					return null;
				}
				throw new ServiceSecuriteClientException(var9);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public Operateur getOperateur(String visa) throws ServiceSecuriteClientException {
		return this.getOperateur(visa, true);
	}

	public Operateur getOperateurTous(String visa) throws ServiceSecuriteClientException {
		return this.getOperateur(visa, false);
	}

	private Operateur getOperateur(String visa, boolean seulementActif) throws ServiceSecuriteClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		Operateur e;
		try {
			wc.path(this.operateurByVisa);
			wc.path(visa);
			wc.query("seulementActif", new Object[]{Boolean.valueOf(seulementActif)});

			try {
				e = (Operateur)wc.get(Operateur.class);
			} catch (WebApplicationException var8) {
				if(var8.getResponse().getStatus()== Response.Status.NOT_FOUND.getStatusCode()){
					return null;
				}
				throw new ServiceSecuriteClientException(var8);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public String ping() throws ServiceSecuriteClientException {
		WebClient wc = this.wcPool.borrowClient('\uea60');

		String var3;
		try {
			wc.path(this.pingPath);
			wc.accept(new MediaType[]{MediaType.TEXT_PLAIN_TYPE});

			try {
				String e = (String)wc.get(String.class);
				if(StringUtils.isEmpty(e)) {
					throw new ServiceInfrastructureClientException("Did not receive response to my \'ping\' (\'" + e + "\')", (Throwable)null);
				}

				var3 = e;
			} catch (WebApplicationException var7) {
				throw new ServiceSecuriteClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return var3;
	}
}
