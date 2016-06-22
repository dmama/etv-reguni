package ch.vd.unireg.wsclient.host.interfaces;


import javax.ws.rs.core.MediaType;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.infrastructure.model.rest.Canton;
import ch.vd.infrastructure.model.rest.CollectiviteAdministrative;
import ch.vd.infrastructure.model.rest.Commune;
import ch.vd.infrastructure.model.rest.ListeCantons;
import ch.vd.infrastructure.model.rest.ListeCollectiviteAdministrative;
import ch.vd.infrastructure.model.rest.ListeCommunes;
import ch.vd.infrastructure.model.rest.ListeLocalites;
import ch.vd.infrastructure.model.rest.ListePays;
import ch.vd.infrastructure.model.rest.ListeRues;
import ch.vd.infrastructure.model.rest.ListeTypesCollectivite;
import ch.vd.infrastructure.model.rest.ListeTypesCommunicationPourTier;
import ch.vd.infrastructure.model.rest.Localite;
import ch.vd.infrastructure.model.rest.Pays;
import ch.vd.infrastructure.model.rest.Rue;
import ch.vd.infrastructure.model.rest.TypeCollectivite;
import ch.vd.infrastructure.model.rest.TypeCommunicationPourTier;


public class ServiceInfrastructureClientImpl implements ServiceInfrastructureClient, InitializingBean {
	private WebClientPool wcPool = new WebClientPool();
	private String cantonsPath = "cantons";
	private String paysPath = "pays";
	private String localitesPath = "localites";
	private String collectivitesAdministrativesPath = "CollectivitesAdministratives";
	private String collectiviteByNumeroPath = "collectivite/noColAdm";
	private String collectiviteBySiglePath = "collectivite/sigle";
	private String localiteByNumeroOdrePath = "localite";
	private String rueByNumeroOfsPath = "rue";
	private String ruesPath = "rues";
	private String communePath = "commune";
	private String communeByIdPath = "commune/id";
	private String oidForCommunePath = "oid/commune";
	private String typesCollectivitesPath = "typesCollectivites";
	private String institutionFinancierePath = "institutionFinanciere";
	private String institutionFinancieresPath = "institutionFinancieres";
	private String typesCommunicationPourTierPath = "typesCommunicationPourTier";
	private String pingPath = "ping";

	public ServiceInfrastructureClientImpl() {
	}

	public void setBaseUrl(String baseUrl) {
		this.wcPool.setBaseUrl(baseUrl);
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

	public ListeCantons getCantons() throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		ListeCantons e;
		try {
			wc.path(this.cantonsPath);

			try {
				e = (ListeCantons)wc.get(ListeCantons.class);
			} catch (ServerWebApplicationException var6) {
				throw new ServiceInfrastructureClientException(var6);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public ListeCantons getCantons(Pays var1) throws ServiceInfrastructureClientException {
		return null;
	}

	public CollectiviteAdministrative getCollectivite(int var1) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		CollectiviteAdministrative e;
		try {
			wc.path(this.collectiviteByNumeroPath);
			wc.path(Integer.valueOf(var1));

			try {
				e = (CollectiviteAdministrative)wc.get(CollectiviteAdministrative.class);
			} catch (ServerWebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public Localite getLocalite(int noOrdre) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		Localite e;
		try {
			wc.path(this.localiteByNumeroOdrePath);
			wc.path(Integer.valueOf(noOrdre));

			try {
				e = (Localite)wc.get(Localite.class);
			} catch (ServerWebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public ListeLocalites getLocalites(Canton var1) throws ServiceInfrastructureClientException {
		return null;
	}

	public ListeLocalites getLocalites(String sigleOfsCanton) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		ListeLocalites e;
		try {
			wc.path(this.localiteByNumeroOdrePath);
			if(StringUtils.isNotEmpty(sigleOfsCanton)) {
				wc.query("sigleOfsCanton", new Object[]{sigleOfsCanton});
			}

			try {
				e = (ListeLocalites)wc.get(ListeLocalites.class);
			} catch (ServerWebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public Rue getRueByNumero(Integer noOfs) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		Rue e;
		try {
			wc.path(this.rueByNumeroOfsPath);
			wc.path(noOfs);

			try {
				e = (Rue)wc.get(Rue.class);
			} catch (ServerWebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public ListeRues getRues(String sigleOfsCanton, int numeroOrdreLocalite) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		ListeRues e;
		try {
			wc.path(this.ruesPath);
			if(StringUtils.isNotEmpty(sigleOfsCanton)) {
				wc.query("sigleOfsCanton", new Object[]{sigleOfsCanton});
			}

			wc.query("numeroOrdreLocalite", new Object[]{Integer.valueOf(numeroOrdreLocalite)});

			try {
				e = (ListeRues)wc.get(ListeRues.class);
			} catch (ServerWebApplicationException var8) {
				throw new ServiceInfrastructureClientException(var8);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public ListeRues getRues(int var1) throws ServiceInfrastructureClientException {
		return null;
	}

	public ListeRues getRues(Canton var1) throws ServiceInfrastructureClientException {
		return null;
	}

	public ListeRues getRues(String var1) throws ServiceInfrastructureClientException {
		return null;
	}

	public CollectiviteAdministrative getCollectivite(String sigle) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		CollectiviteAdministrative e;
		try {
			wc.path(this.collectiviteBySiglePath);
			wc.path(sigle);

			try {
				e = (CollectiviteAdministrative)wc.get(CollectiviteAdministrative.class);
			} catch (ServerWebApplicationException var7) {
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
			} catch (ServerWebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public ListeCollectiviteAdministrative getCollectivitesAdministratives(TypeCollectivite[] types) throws ServiceInfrastructureClientException {
		String listeType = Arrays.toString(types);
		WebClient wc = this.wcPool.borrowClient(600000);

		ListeCollectiviteAdministrative e;
		try {
			wc.path(this.collectivitesAdministrativesPath);
			if(StringUtils.isNotEmpty(listeType)) {
				wc.query("typeCollectivite", new Object[]{listeType});
			}

			try {
				e = (ListeCollectiviteAdministrative)wc.get(ListeCollectiviteAdministrative.class);
			} catch (ServerWebApplicationException var8) {
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
			} catch (ServerWebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public Commune getCommune(int numero) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		Commune e;
		try {
			wc.path(this.communePath);
			wc.path(Integer.valueOf(numero));

			try {
				e = (Commune)wc.get(Commune.class);
			} catch (ServerWebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public Commune getCommuneById(String id) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		Commune e;
		try {
			wc.path(this.communeByIdPath);
			wc.path(id);

			try {
				e = (Commune)wc.get(Commune.class);
			} catch (ServerWebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public ListeCommunes getCommunes(Canton canton) throws ServiceInfrastructureClientException {
		return null;
	}

	public ListeCommunes getCommunes(String sigleOfsCanton) throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		ListeCommunes e;
		try {
			wc.path(this.communePath);
			if(StringUtils.isNotEmpty(sigleOfsCanton)) {
				wc.query("sigleOfsCanton", new Object[]{sigleOfsCanton});
			}

			try {
				e = (ListeCommunes)wc.get(ListeCommunes.class);
			} catch (ServerWebApplicationException var7) {
				throw new ServiceInfrastructureClientException(var7);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}

	public ListePays getListePays() throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		ListePays e;
		try {
			wc.path(this.paysPath);

			try {
				e = (ListePays)wc.get(ListePays.class);
			} catch (ServerWebApplicationException var6) {
				throw new ServiceInfrastructureClientException(var6);
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
			} catch (ServerWebApplicationException var7) {
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
			} catch (ServerWebApplicationException var6) {
				throw new ServiceInfrastructureClientException(var6);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
	}





	public ListeTypesCommunicationPourTier getTypesCommunicationPourTier() throws ServiceInfrastructureClientException {
		WebClient wc = this.wcPool.borrowClient(600000);

		ListeTypesCommunicationPourTier e;
		try {
			wc.path(this.typesCommunicationPourTierPath);

			try {
				e = (ListeTypesCommunicationPourTier)wc.get(ListeTypesCommunicationPourTier.class);
			} catch (ServerWebApplicationException var6) {
				throw new ServiceInfrastructureClientException(var6);
			}
		} finally {
			this.wcPool.returnClient(wc);
		}

		return e;
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
			} catch (ServerWebApplicationException var7) {
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
