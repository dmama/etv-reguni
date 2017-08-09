package ch.vd.uniregctb.webservice.fidor.v5;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0007.v1.Country;
import ch.vd.evd0007.v1.ExtendedCanton;
import ch.vd.evd0007.v1.ListOfPoliticalEntities;
import ch.vd.evd0012.v1.CommuneFiscale;
import ch.vd.evd0012.v1.DistrictFiscal;
import ch.vd.evd0012.v1.ListOfFiscalEntities;
import ch.vd.evd0012.v1.Logiciel;
import ch.vd.evd0012.v1.RegionFiscale;
import ch.vd.fidor.xml.categorieentreprise.v1.CategorieEntreprise;
import ch.vd.fidor.xml.impotspecial.v1.ImpotSpecial;
import ch.vd.fidor.xml.post.v1.PostalLocality;
import ch.vd.fidor.xml.post.v1.Street;
import ch.vd.fidor.xml.regimefiscal.v2.RegimeFiscal;
import ch.vd.fidor.xml.ws.v5.categoriesentreprise.CategoriesEntreprise;
import ch.vd.fidor.xml.ws.v5.impotsspeciaux.ImpotSpeciaux;
import ch.vd.fidor.xml.ws.v5.postallocalities.PostalLocalities;
import ch.vd.fidor.xml.ws.v5.regimesfiscauxetendus.RegimesFiscaux;
import ch.vd.fidor.xml.ws.v5.streets.Streets;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

@SuppressWarnings("UnusedDeclaration")
public class FidorClientImpl implements FidorClient {

	private String serviceUrl;
	private String username;
	private String password;

	private String pingPath = "infrastructure/server/ping";
	private String communesPath = "listOfCommunesFiscales";
	private String paysPath = "listOfStatesTerritories";
	private String cantonsPath = "listOfCantons";
	private String logicielPath = "logiciel";
	private String allLogicielsPath = "listOfLogiciels";
	private String urlsPath = "listOfUrls";
	private String districtPath = "districtFiscal";
	private String regionPath = "regionFiscale";
	private String postalLocalitiesByReferenceDatePath = "postalLocalities/byReferenceDate";
	private String postalLocalitiesBySwissZipCodeIdPath = "postalLocalities/bySwissZipCodeId";
	private String postalLocalityPath = "postalLocality";
	private String streetsByPostalLocalityPath = "streets/byPostalLocality";
	private String streetsByEstridPath = "streets/byEstrid";
	private String regimeFiscalPath = "regimeFiscalEtendu";
	private String regimesFiscauxPath = "regimesFiscauxEtendus";
	private String categorieEntreprisePath = "categorieEntreprise";
	private String categoriesEntreprisePath = "categoriesEntreprise";
	private String impotsSpeciauxPath = "impotsSpeciaux";

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setPingPath(String pingPath) {
		this.pingPath = pingPath;
	}

	public void setCommunesPath(String communesPath) {
		this.communesPath = communesPath;
	}

	public void setCantonsPath(String cantonsPath) {
		this.cantonsPath = cantonsPath;
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

	public void setPostalLocalitiesByReferenceDatePath(String postalLocalitiesByReferenceDatePath) {
		this.postalLocalitiesByReferenceDatePath = postalLocalitiesByReferenceDatePath;
	}

	public void setPostalLocalitiesBySwissZipCodeIdPath(String postalLocalitiesBySwissZipCodeIdPath) {
		this.postalLocalitiesBySwissZipCodeIdPath = postalLocalitiesBySwissZipCodeIdPath;
	}

	public void setPostalLocalityPath(String postalLocalityPath) {
		this.postalLocalityPath = postalLocalityPath;
	}

	public void setStreetsByPostalLocalityPath(String streetsByPostalLocalityPath) {
		this.streetsByPostalLocalityPath = streetsByPostalLocalityPath;
	}

	public void setStreetsByEstridPath(String streetsByEstridPath) {
		this.streetsByEstridPath = streetsByEstridPath;
	}

	public void setRegimeFiscalPath(String regimeFiscalPath) {
		this.regimeFiscalPath = regimeFiscalPath;
	}

	public void setRegimesFiscauxPath(String regimesFiscauxPath) {
		this.regimesFiscauxPath = regimesFiscauxPath;
	}

	public void setCategorieEntreprisePath(String categorieEntreprisePath) {
		this.categorieEntreprisePath = categorieEntreprisePath;
	}

	public void setCategoriesEntreprisePath(String categoriesEntreprisePath) {
		this.categoriesEntreprisePath = categoriesEntreprisePath;
	}

	public void setImpotsSpeciauxPath(String impotsSpeciauxPath) {
		this.impotsSpeciauxPath = impotsSpeciauxPath;
	}

	@Override
	public void ping() {
		final WebClient wc = createWebClient(60000);    // 1 minute
		wc.path(pingPath);
		wc.accept(MediaType.TEXT_PLAIN_TYPE);
		try {
			final String response = wc.get(String.class);
			if (!"pong".equals(response)) {
				throw new FidorClientException("Did not receive 'pong' to my 'ping' ('" + response + "')", null);
			}
		}
		catch (WebApplicationException e) {
			throw new FidorClientException(e);
		}
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
		catch (WebApplicationException e) {
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
		catch (WebApplicationException e) {
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
		catch (WebApplicationException e) {
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
		catch (WebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@NotNull
	@Override
	public List<CommuneFiscale> findCommuneByNomOfficiel(@NotNull String nomOfficiel, @Nullable RegDate date) {

		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(communesPath);

		// l'id
		wc.query("nomOfficiel", nomOfficiel);

		// la date
		if (date != null) {
			wc.query("dateReference", RegDateHelper.dateToDisplayString(date));
		}

		try {
			final ListOfFiscalEntities list = wc.get(ListOfFiscalEntities.class);
			if (list == null || list.getNumberOfResults() == 0) {
				return Collections.emptyList();
			}
			return list.getListOfResults().getListOfCommunesFiscales().getCommuneFiscale();
		}
		catch (WebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public List<ExtendedCanton> getTousLesCantons() {
		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(cantonsPath);

		try {
			final ListOfPoliticalEntities list = wc.get(ListOfPoliticalEntities.class);
			if (list == null || list.getNumberOfResults().intValue() == 0) {
				return null;
			}
			return list.getListOfResults().getListOfCantons().getExtendedCanton();
		}
		catch (WebApplicationException e) {
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
		catch (WebApplicationException e) {
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
		catch (WebApplicationException e) {
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
		catch (WebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public List<Country> getPaysHisto(long ofsId) {
		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(paysPath);

		// l'id
		wc.query("id", ofsId);

		try {
			final ListOfPoliticalEntities list = wc.get(ListOfPoliticalEntities.class);
			if (list == null || list.getNumberOfResults().intValue() == 0) {
				return null;
			}
			return list.getListOfResults().getListOfStateTerritories().getStateTerritory();
		}
		catch (WebApplicationException e) {
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
		catch (WebApplicationException e) {
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
		catch (WebApplicationException e) {
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
		catch (WebApplicationException e) {
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
		catch (WebApplicationException e) {
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
		catch (WebApplicationException e) {
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
		catch (WebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public List<PostalLocality> getLocalitesPostales(RegDate dateReference, Integer npa, Integer noOrdrePostal, String nom, Integer cantonOfsId) {
		final WebClient wc = createWebClient(60000);    // 10 minutes !
		wc.path(postalLocalitiesByReferenceDatePath);
		wc.path(RegDateHelper.dateToDisplayString(dateReference == null ? RegDate.get() : dateReference));
		if (npa != null) {
			wc.query("swissZipCode", npa);
		}
		if (noOrdrePostal != null) {
			wc.query("swissZipCodeId", noOrdrePostal);
		}
		if (StringUtils.isNotBlank(nom)) {
			wc.query("name", nom);
		}
		if (cantonOfsId != null) {
			wc.query("cantonId", cantonOfsId);
		}

		try {
			final PostalLocalities result = wc.get(PostalLocalities.class);
			if (result == null || result.getNbOfResults() == 0) {
				return null;
			}
			return result.getPostalLocality();
		}
		catch (WebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public List<PostalLocality> getLocalitesPostalesHisto(int noOrdrePostal) {
		final WebClient wc = createWebClient(60000);    // 10 minutes !
		wc.path(postalLocalitiesBySwissZipCodeIdPath);
		wc.path(noOrdrePostal);

		try {
			final PostalLocalities result = wc.get(PostalLocalities.class);
			if (result == null || result.getNbOfResults() == 0) {
				return null;
			}
			return result.getPostalLocality();
		}
		catch (WebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public PostalLocality getLocalitePostale(RegDate dateReference, int noOrdrePostal) {
		final WebClient wc = createWebClient(60000);    // 10 minutes !
		wc.path(postalLocalityPath);
		wc.path(noOrdrePostal);
		wc.path(RegDateHelper.dateToDisplayString(dateReference == null ? RegDate.get() : dateReference));

		try {
			final Response response = wc.get();
			if (response.getStatus() >= 400) {
				throw new WebApplicationException(response);
			}

			final JAXBContext jaxbContext = JAXBContext.newInstance(ch.vd.fidor.xml.ws.v5.postallocality.ObjectFactory.class);
			final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

			//noinspection unchecked
			final JAXBElement<PostalLocality> data = (JAXBElement<PostalLocality>) unmarshaller.unmarshal((InputStream) response.getEntity());
			return data.getValue();
		}
		catch (WebApplicationException e) {
			if (e.getResponse().getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
				return null;
			}
			throw new FidorClientException(e);
		}
		catch (JAXBException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public List<Street> getRuesParNumeroOrdrePosteEtDate(int noOrdrePostal, RegDate dateReference) {
		final WebClient wc = createWebClient(60000);    // 10 minutes !
		wc.path(streetsByPostalLocalityPath);
		wc.path(noOrdrePostal);
		wc.path(RegDateHelper.dateToDisplayString(dateReference == null ? RegDate.get() : dateReference));

		try {
			final Streets result = wc.get(Streets.class);
			if (result == null || result.getNbOfResults() == 0) {
				return null;
			}
			return result.getStreet();
		}
		catch (WebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public List<Street> getRuesParEstrid(int estrid, RegDate dateReference) {
		final WebClient wc = createWebClient(60000);    // 10 minutes !
		wc.path(streetsByEstridPath);
		wc.path(estrid);
		if (dateReference != null) {
			wc.query("referenceDate", RegDateHelper.dateToDisplayString(dateReference));
		}

		try {
			final Streets result = wc.get(Streets.class);
			if (result == null || result.getNbOfResults() == 0) {
				return null;
			}
			return result.getStreet();
		}
		catch (WebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public RegimeFiscal getRegimeFiscalParCode(String code) {
		final WebClient wc = createWebClient(60000);    // 10 minutes !
		wc.path(regimeFiscalPath);
		wc.path(code);
		try {
			final Response response = wc.get();
			if (response.getStatus() >= 400) {
				throw new WebApplicationException(response);
			}

			final JAXBContext jaxbContext = JAXBContext.newInstance(ch.vd.fidor.xml.ws.v5.regimefiscal.ObjectFactory.class);
			final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

			//noinspection unchecked
			final JAXBElement<RegimeFiscal> data = (JAXBElement<RegimeFiscal>) unmarshaller.unmarshal((InputStream) response.getEntity());
			return data.getValue();
		}
		catch (WebApplicationException e) {
			if (e.getResponse().getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
				return null;
			}
			throw new FidorClientException(e);
		}
		catch (JAXBException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public List<RegimeFiscal> getRegimesFiscaux() {
		final WebClient wc = createWebClient(60000);    // 10 minutes !
		wc.path(regimesFiscauxPath);
		try {
			final RegimesFiscaux result = wc.get(RegimesFiscaux.class);
			if (result == null || result.getNbOfResults() == 0) {
				return null;
			}
			return result.getRegimeFiscal();
		}
		catch (WebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public CategorieEntreprise getCategorieEntrepriseParCode(String code) {
		final WebClient wc = createWebClient(60000);    // 10 minutes !
		wc.path(categorieEntreprisePath);
		wc.path(code);
		try {
			final Response response = wc.get();
			if (response.getStatus() >= 400) {
				throw new WebApplicationException(response);
			}

			final JAXBContext jaxbContext = JAXBContext.newInstance(ch.vd.fidor.xml.ws.v5.categorieentreprise.ObjectFactory.class);
			final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

			//noinspection unchecked
			final JAXBElement<CategorieEntreprise> data = (JAXBElement<CategorieEntreprise>) unmarshaller.unmarshal((InputStream) response.getEntity());
			return data.getValue();
		}
		catch (WebApplicationException e) {
			if (e.getResponse().getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
				return null;
			}
			throw new FidorClientException(e);
		}
		catch (JAXBException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public List<CategorieEntreprise> getCategoriesEntreprise() {
		final WebClient wc = createWebClient(60000);    // 10 minutes !
		wc.path(categoriesEntreprisePath);
		try {
			final CategoriesEntreprise result = wc.get(CategoriesEntreprise.class);
			if (result == null || result.getNbOfResults() == 0) {
				return null;
			}
			return result.getCategorieEntreprise();
		}
		catch (WebApplicationException e) {
			throw new FidorClientException(e);
		}
	}

	@Override
	public List<ImpotSpecial> getImpotsSpeciaux() {
		final WebClient wc = createWebClient(60000);    // 10 minutes !
		wc.path(impotsSpeciauxPath);
		try {
			final ImpotSpeciaux result = wc.get(ImpotSpeciaux.class);
			if (result == null || result.getNbOfResults() == 0) {
				return null;
			}
			return result.getImpotSpecial();
		}
		catch (WebApplicationException e) {
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
