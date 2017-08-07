package ch.vd.unireg.wsclient.host.interfaces;


import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.infrastructure.model.rest.CollectiviteAdministrative;
import ch.vd.infrastructure.model.rest.ListeCollectiviteAdministrative;
import ch.vd.infrastructure.model.rest.ListeTypesCollectivite;
import ch.vd.infrastructure.model.rest.Rue;
import ch.vd.infrastructure.model.rest.TypeCollectivite;
import ch.vd.infrastructure.model.rest.TypeCommunicationPourTier;
import ch.vd.infrastructure.registre.common.model.rest.InstitutionFinanciere;
import ch.vd.infrastructure.registre.common.model.rest.ListeInstitutionsFinancieres;


public class ServiceInfrastructureClientImpl implements ServiceInfrastructureClient, InitializingBean {
	private WebClientPool wcPool = new WebClientPool();
	private String infrastructurePath = "infrastructure";
	private String collectivitesAdministrativesPath = "collectivitesAdministratives";
	private String collectiviteByNumeroPath = "collectivite/noColAdm";
	private String collectiviteBySiglePath = "collectivite/sigle";
	private String rueByNumeroOfsPath = "rue";
	private String oidForCommunePath = "oid/commune";
	private String typesCollectivitesPath = "typesCollectivites";
	private String institutionFinancierePath = "institutionFinanciere";
	private String institutionFinancieresPath = "institutionFinancieres";
	private String typesCommunicationPourTierPath = "typesCommunicationPourTier";

	private String pingPath = "ping";

	public ServiceInfrastructureClientImpl() {
	}

	public void setBaseUrl(String baseUrl) {
		StringBuilder s = new StringBuilder();
		s.append(baseUrl).append("/").append(infrastructurePath);
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


	public Rue getRueByNumero(Integer noOfs) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		Rue e;
		try {
			wc.path(this.rueByNumeroOfsPath);
			wc.path(noOfs);

			try {
				e = (Rue)wc.get(Rue.class);
			} catch (WebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}
	public CollectiviteAdministrative getCollectivite(int var1) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		CollectiviteAdministrative e;
		try {
			wc.path(this.collectiviteByNumeroPath);
			wc.path(Integer.valueOf(var1));

			try {
				e = (CollectiviteAdministrative)wc.get(CollectiviteAdministrative.class);
			} catch (WebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public CollectiviteAdministrative getCollectivite(String sigle) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		CollectiviteAdministrative e;
		try {
			wc.path(this.collectiviteBySiglePath);
			wc.path(sigle);

			try {
				e = (CollectiviteAdministrative)wc.get(CollectiviteAdministrative.class);
			} catch (WebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public ListeCollectiviteAdministrative getCollectivitesAdministratives(String sigleOfsCanton) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		ListeCollectiviteAdministrative e;
		try {
			wc.path(this.collectivitesAdministrativesPath);
			if(StringUtils.isNotEmpty(sigleOfsCanton)) {
				wc.query("sigleOfsCanton", new Object[]{sigleOfsCanton});
			}

			try {
				e = (ListeCollectiviteAdministrative)wc.get(ListeCollectiviteAdministrative.class);
			} catch (WebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public ListeCollectiviteAdministrative getCollectivitesAdministratives(TypeCollectivite[] types) throws ServiceInfrastructureClientException {
		String listeType = ServiceHelper.extractCodeTypeCollectivite(types);
		WebClient wc = this.wcPool.borrowClient(600000);

		ListeCollectiviteAdministrative e;
		try {
			wc.path(this.collectivitesAdministrativesPath);
			if(StringUtils.isNotEmpty(listeType)) {
				wc.query("typeCollectivite", listeType);
			}

			try {
				e = (ListeCollectiviteAdministrative)wc.get(ListeCollectiviteAdministrative.class);
			} catch (WebApplicationException var8) {
				throw new ServiceInfrastructureClientException(var8);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}


	public ListeCollectiviteAdministrative getCollectivitesAdministrativesPourTypeCommunication(TypeCommunicationPourTier type) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		ListeCollectiviteAdministrative e;
		try {
			wc.path(this.collectivitesAdministrativesPath);
			if(type != null) {
				wc.query("typeCommunicationPourTier", new Object[]{type});
			}

			try {
				e = (ListeCollectiviteAdministrative)wc.get(ListeCollectiviteAdministrative.class);
			} catch (WebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}



	public CollectiviteAdministrative getOidDeCommune(int noOfsCommune) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		CollectiviteAdministrative e;
		try {
			wc.path(this.oidForCommunePath);
			wc.path(Integer.valueOf(noOfsCommune));

			try {
				e = (CollectiviteAdministrative)wc.get(CollectiviteAdministrative.class);
			} catch (WebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public ListeTypesCollectivite getTypesCollectivites() throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		ListeTypesCollectivite e;
		try {
			wc.path(this.typesCollectivitesPath);

			try {
				e = (ListeTypesCollectivite)wc.get(ListeTypesCollectivite.class);
			} catch (WebApplicationException var6) {
				throw new ServiceInfrastructureClientException(var6);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		InstitutionFinanciere institutionFinanciere;
		try {
			wc.path(this.institutionFinancierePath);
			wc.path(Integer.valueOf(id));

			try {
				institutionFinanciere = (InstitutionFinanciere)wc.get(InstitutionFinanciere.class);
			} catch (WebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return institutionFinanciere;
	}

	@Override
	public ListeInstitutionsFinancieres getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		ListeInstitutionsFinancieres liste;
		try {
			wc.path(this.institutionFinancieresPath);
			wc.query("noClearing", noClearing);

			try {
				liste = (ListeInstitutionsFinancieres)wc.get(ListeInstitutionsFinancieres.class);
			} catch (WebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return liste;
	}


	public String ping() throws ServiceInfrastructureClientException {
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
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return var3;
	}

	private static void addCriterion(WebClient wc, String key, String value) {
		if(value != null) {
			wc.query(key, new Object[]{value});
		}

	}

	private static void addCriterion(WebClient wc, String key, Boolean value) {
		if(value != null) {
			wc.query(key, new Object[]{value.booleanValue()?"1":"0"});
		}

	}

	private static void addCriterion(WebClient wc, String key, Integer value) {
		if(value != null) {
			wc.query(key, new Object[]{value});
		}

	}
}
