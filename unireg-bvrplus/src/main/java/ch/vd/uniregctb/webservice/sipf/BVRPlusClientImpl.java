package ch.vd.uniregctb.webservice.sipf;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.message.Message;
import org.apache.log4j.Logger;
import org.springframework.util.ResourceUtils;

import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrDemande;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrReponse;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.GetPingRequest;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.PingRequest;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.SipfBVRPlus;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.SipfBVRPlus_Service;

@SuppressWarnings({"UnusedDeclaration"})
public class BVRPlusClientImpl implements BVRPlusClient {

	private static final Logger LOGGER = Logger.getLogger(BVRPlusClientImpl.class);

	private SipfBVRPlus service;

	private String serviceUrl;

	private String username;

	private String password;

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public BvrReponse getBVRDemande(BvrDemande bvrDemande) throws BVRPlusClientException {

		initService();

		List<BvrReponse> bvrsReponse;
		try {
			bvrsReponse = this.service.getBVRDemande(Arrays.asList(bvrDemande));
		}
		catch (WebServiceException e) {
			throw new BVRPlusClientException(e.getMessage(), e);
		}

		for (BvrReponse r : bvrsReponse) {
			LOGGER.info("Année taxation : " + r.getAnneeTaxation());
			LOGGER.info("Ligne codage : " + r.getLigneCodage());
			LOGGER.info("Message : " + r.getMessage());
			LOGGER.info("NDC : " + r.getNdc());
			LOGGER.info("Numéro adhérent : " + r.getNoAdherent());
			LOGGER.info("numéro référence : " + r.getNoReference());
			LOGGER.info("-------------------------------------------------");
		}

		if (bvrsReponse.size() >= 1) {
			return bvrsReponse.get(0);
		}
		else {
			return null;
		}
	}

	public void ping() throws BVRPlusClientException {

		initService();

		final GetPingRequest param = new GetPingRequest();
		final PingRequest request = new PingRequest();
		request.setText("Y a quelqu'un ?");
		param.getListPingRequest().add(request);

		this.service.ping(param);
	}

	public void initService() {
		if (this.service == null) {
			URL wsdlUrl;
			try {
				wsdlUrl = ResourceUtils.getURL("classpath:SipfBVRPlus-v1.wsdl");
			}
			catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			SipfBVRPlus_Service bvrplusService = new SipfBVRPlus_Service(wsdlUrl);
			this.service = bvrplusService.getSOAPOverHTTP();
			Map<String, Object> context = ((BindingProvider) service).getRequestContext();
			if (StringUtils.isNotBlank(this.username)) {
				context.put(BindingProvider.USERNAME_PROPERTY, this.username);
				context.put(BindingProvider.PASSWORD_PROPERTY, this.password);
			}
			context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, this.serviceUrl);

			// Désactive la validation du schéma (= ignore silencieusement les éléments inconnus), de manière à permettre l'évolution ascendante-compatible du WSDL.
			context.put(Message.SCHEMA_VALIDATION_ENABLED, false);
			context.put("set-jaxb-validation-event-handler", false);
		}
	}
}
