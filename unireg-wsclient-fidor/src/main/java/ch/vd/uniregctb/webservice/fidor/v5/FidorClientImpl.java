package ch.vd.uniregctb.webservice.fidor.v5;

import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

import ch.vd.evd0007.v1.Country;
import ch.vd.evd0007.v1.ListOfPoliticalEntities;
import ch.vd.evd0012.v1.CommuneFiscale;
import ch.vd.evd0012.v1.DistrictFiscal;
import ch.vd.evd0012.v1.ListOfFiscalEntities;
import ch.vd.evd0012.v1.Logiciel;
import ch.vd.evd0012.v1.RegionFiscale;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

@SuppressWarnings("UnusedDeclaration")
public class FidorClientImpl implements FidorClient {

	private String serviceUrl;
	private String username;
	private String password;

	private String communesPath = "listOfCommunesFiscales";
	private String paysPath = "listOfStatesTerritories";
	private String logicielPath = "logiciel";
	private String allLogicielsPath = "listOfLogiciels";
	private String urlsPath = "listOfUrls";
	private String districtPath = "districtFiscal";
	private String regionPath = "regionFiscale";

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setCommunesPath(String communesPath) {
		this.communesPath = communesPath;
	}

	public void setPaysPath(String paysPath) {
		this.paysPath = paysPath;
	}

	public void setLogicielPath(String logicielPath) {
		this.logicielPath = logicielPath;
	}

	public void setAllLogicielsPath(String allLogicielsPath) {
		this.allLogicielsPath = allLogicielsPath;
	}

	public void setUrlsPath(String urlsPath) {
		this.urlsPath = urlsPath;
	}

	public void setDistrictPath(String districtPath) {
		this.districtPath = districtPath;
	}

	public void setRegionPath(String regionPath) {
		this.regionPath = regionPath;
	}

	@Override
	public CommuneFiscale getCommuneParNoOFS(int ofsId, RegDate date) {

		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(communesPath);

		// l'id
		wc.query("numeroOfs", ofsId);

		// la date
		if (date == null) {
			date = RegDate.get();
		}
		wc.query("dateReference", RegDateHelper.dateToDisplayString(date));

		try {
			final ListOfFiscalEntities list = wc.get(ListOfFiscalEntities.class);
			if (list == null || list.getNumberOfResults() == 0) {
				return null;
			}
			if (list.getNumberOfResults() > 1) {
				throw new IllegalArgumentException("Plusieurs communes retournées pour le numéro Ofs = " + ofsId + " à la date " + RegDateHelper.dateToDisplayString(date));
			}
			final CommuneFiscale c = list.getListOfResults().getListOfCommunesFiscales().getCommuneFiscale().get(0);
			if (c.getNumeroOfs() != ofsId) {
				throw new IllegalArgumentException("Commune retournée incohérente avec la demande (demandé OFS " + ofsId + ", reçu " + c.getNumeroOfs() + ")");
			}
			return c;
		}
		catch (ServerWebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public List<CommuneFiscale> getCommunesParNoOFS(int ofsId) {

		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(communesPath);

		// l'id
		wc.query("numeroOfs", ofsId);

		try {
			final ListOfFiscalEntities list = wc.get(ListOfFiscalEntities.class);
			if (list == null || list.getNumberOfResults() == 0) {
				return null;
			}
			final List<CommuneFiscale> result = list.getListOfResults().getListOfCommunesFiscales().getCommuneFiscale();
			for (CommuneFiscale commune : result) {
				if (commune.getNumeroOfs() != ofsId) {
					throw new IllegalArgumentException("Commune retournée incohérente avec la demande (demandé OFS " + ofsId + ", reçu " + commune.getNumeroOfs() + ")");
				}
			}
			return result;
		}
		catch (ServerWebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public List<CommuneFiscale> getCommunesParCanton(int ofsId, RegDate date) {

		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(communesPath);

		// l'id
		wc.query("cantonId", ofsId);

		// la date
		if (date != null) {
			wc.query("dateReference", RegDateHelper.dateToDisplayString(date));
		}

		try {
			final ListOfFiscalEntities list = wc.get(ListOfFiscalEntities.class);
			if (list == null || list.getNumberOfResults() == 0) {
				return null;
			}
			return list.getListOfResults().getListOfCommunesFiscales().getCommuneFiscale();
		}
		catch (ServerWebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public List<CommuneFiscale> getToutesLesCommunes() {

		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(communesPath);

		try {
			final ListOfFiscalEntities list = wc.get(ListOfFiscalEntities.class);
			if (list == null || list.getNumberOfResults() == 0) {
				return null;
			}
			return list.getListOfResults().getListOfCommunesFiscales().getCommuneFiscale();
		}
		catch (ServerWebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public CommuneFiscale getCommuneParBatiment(int egid, RegDate date) {
		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(communesPath);

		// l'id
		wc.query("egid", egid);

		// la date
		if (date == null) {
			date = RegDate.get();
		}
		wc.query("dateReference", RegDateHelper.dateToDisplayString(date));

		try {
			final ListOfFiscalEntities list = wc.get(ListOfFiscalEntities.class);
			if (list == null || list.getNumberOfResults() == 0) {
				return null;
			}
			if (list.getNumberOfResults() > 1) {
				throw new IllegalArgumentException("Plusieurs communes retournées pour l'egid = " + egid + " à la date " + RegDateHelper.dateToDisplayString(date));
			}
			return list.getListOfResults().getListOfCommunesFiscales().getCommuneFiscale().get(0);
		}
		catch (ServerWebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public Country getPaysDetail(long ofsId, RegDate date) {
		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(paysPath);

		// l'id
		wc.query("id", ofsId);

		// la date de référence
		if (date == null) {
			date = RegDate.get();
		}
		wc.query("referenceDate", RegDateHelper.dateToDisplayString(date));

		try {
			final ListOfPoliticalEntities list = wc.get(ListOfPoliticalEntities.class);
			if (list == null || list.getNumberOfResults().intValue() == 0) {
				return null;
			}
			if (list.getNumberOfResults().intValue() > 1) {
				throw new IllegalArgumentException("Plusieurs pays retournés pour le numéro Ofs = " + ofsId + " et date " + RegDateHelper.dateToDisplayString(date));
			}
			final Country c = list.getListOfResults().getListOfStateTerritories().getStateTerritory().get(0);
			if (c.getCountry().getId() != ofsId) {
				throw new IllegalArgumentException("Pays retourné incohérent avec la demande (demandé OFS " + ofsId + ", reçu " + c.getCountry().getId() + ")");
			}
			return c;
		}
		catch (ServerWebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public Country getPaysDetail(String iso2Id, RegDate date) {
		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(paysPath);

		// l'id
		wc.query("iso2Id", iso2Id);

		// la date de référence
		if (date == null) {
			date = RegDate.get();
		}
		wc.query("referenceDate", RegDateHelper.dateToDisplayString(date));

		try {
			final ListOfPoliticalEntities list = wc.get(ListOfPoliticalEntities.class);
			if (list == null || list.getNumberOfResults().intValue() == 0) {
				return null;
			}
			if (list.getNumberOfResults().intValue() > 1) {
				throw new IllegalArgumentException("Plusieurs pays retournés pour le code ISO2 = " + iso2Id + " et date " + RegDateHelper.dateToDisplayString(date));
			}
			final Country c = list.getListOfResults().getListOfStateTerritories().getStateTerritory().get(0);
			if (iso2Id != null && !iso2Id.equalsIgnoreCase(c.getCountry().getIso2Id())) {
				throw new IllegalArgumentException("Pays retourné incohérent avec la demande (demandé ISO2 '" + iso2Id + "', reçu '" + c.getCountry().getIso2Id() + "')");
			}
			return c;
		}
		catch (ServerWebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public List<Country> getTousLesPays() {
		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(paysPath);

		try {
			final ListOfPoliticalEntities list = wc.get(ListOfPoliticalEntities.class);
			if (list == null || list.getNumberOfResults().intValue() == 0) {
				return null;
			}
			return list.getListOfResults().getListOfStateTerritories().getStateTerritory();
		}
		catch (ServerWebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public Logiciel getLogicielDetail(long logicielId) {
		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(logicielPath);

		// l'id
		wc.path(logicielId);

		try {
			final ListOfFiscalEntities list = wc.get(ListOfFiscalEntities.class);
			if (list == null || list.getNumberOfResults() == 0) {
				return null;
			}
			if (list.getNumberOfResults() > 1) {
				throw new IllegalArgumentException("Plusieurs logiciels retournés pour l'id = " + logicielId);
			}
			final Logiciel l = list.getListOfResults().getListOfLogiciels().getLogiciel().get(0);
			if (l.getIdLogiciel() != logicielId) {
				throw new IllegalArgumentException("Logiciel retourné incohérent avec la demande (demandé ID " + logicielId + ", reçu " + l.getIdLogiciel() + ")");
			}
			return l;
		}
		catch (ServerWebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() {
		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(allLogicielsPath);

		try {
			final ListOfFiscalEntities list = wc.get(ListOfFiscalEntities.class);
			if (list == null || list.getNumberOfResults() == 0) {
				return null;
			}
			return list.getListOfResults().getListOfLogiciels().getLogiciel();
		}
		catch (ServerWebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public DistrictFiscal getDistrict(int code) {
		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(districtPath);

		// le code
		wc.path(code);

		try {
			final ListOfFiscalEntities list = wc.get(ListOfFiscalEntities.class);
			if (list == null || list.getNumberOfResults() == 0) {
				return null;
			}
			if (list.getNumberOfResults() > 1) {
				throw new IllegalArgumentException("Plusieurs districts retournés pour le code = " + code);
			}
			final DistrictFiscal df = list.getListOfResults().getListOfDistrictsFiscaux().getDistrictFiscal().get(0);
			if (df.getCode() != code) {
				throw new IllegalArgumentException("District fiscal retourné incohérent avec la demande (demandé code " + code + ", reçu " + df.getCode() + ")");
			}
			return df;
		}
		catch (ServerWebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public RegionFiscale getRegion(int code) {
		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(regionPath);

		// le code
		wc.path(code);

		try {
			final ListOfFiscalEntities list = wc.get(ListOfFiscalEntities.class);
			if (list == null || list.getNumberOfResults() == 0) {
				return null;
			}
			if (list.getNumberOfResults() > 1) {
				throw new IllegalArgumentException("Plusieurs districts retournés pour le code = " + code);
			}
			final RegionFiscale rf = list.getListOfResults().getListOfRegionsFiscales().getRegionFiscale().get(0);
			if (rf.getCode() != code) {
				throw new IllegalArgumentException("Région fiscale retournée incohérente avec la demande (demandé code " + code + ", reçu " + rf.getCode() + ")");
			}
			return rf;
		}
		catch (ServerWebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public String getUrl(String app, String acces, String targetType, Map<String, String> map) {
		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(urlsPath);

		// les paramètres
		wc.query("application", app);
		wc.query("acces", acces);
		wc.query("targetType", targetType);

		if (map != null) {
			final StringBuilder parameterMap = new StringBuilder();
			for (Map.Entry<String, String> entry : map.entrySet()) {
				if (parameterMap.length() > 0) {
					parameterMap.append(',');
				}
				parameterMap.append(entry.getKey()).append(':').append(entry.getValue());
			}
			wc.query("parameterMap", parameterMap.toString());
		}

		try {
			final ListOfFiscalEntities list = wc.get(ListOfFiscalEntities.class);
			if (list == null || list.getNumberOfResults() == 0) {
				return null;
			}
			if (list.getNumberOfResults() > 1) {
				throw new IllegalArgumentException("Plusieurs URLs retournées pour les valeurs app = " + app + ", acces = " + acces + ", targetType = " + targetType);
			}
			return list.getListOfResults().getListOfURL().getUrl().get(0).getUrlAddress();
		}
		catch (ServerWebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	private WebClient createWebClient(int receiveTimeout) {
		final WebClient wc = WebClient.create(serviceUrl, username, password, null);
		final HTTPConduit conduit = (HTTPConduit) WebClient.getConfig(wc).getConduit();
		conduit.getClient().setReceiveTimeout(receiveTimeout);
		return wc;
	}
}
