package ch.vd.uniregctb.webservice.sipf;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.util.ResourceUtils;

import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrDemande;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrReponse;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.SipfBVRPlus;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.SipfBVRPlus_Service;

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

	public BvrReponse getBVRDemande(BvrDemande bvrDemande) {
		initService();
		List<BvrDemande> bvrsDemande = new ArrayList<BvrDemande>();
		bvrsDemande.add(bvrDemande);
		List<BvrReponse> bvrsReponse = this.service.getBVRDemande(bvrsDemande);
		Iterator<BvrReponse> itBvrReponse = bvrsReponse.iterator();
		while (itBvrReponse.hasNext()) {
			BvrReponse bvrReponse = itBvrReponse.next();
			LOGGER.info("Année taxation : " + bvrReponse.getAnneeTaxation());
			LOGGER.info("Ligne codage : " + bvrReponse.getLigneCodage());
			LOGGER.info("Message : " + bvrReponse.getMessage());
			LOGGER.info("NDC : " + bvrReponse.getNdc());
			LOGGER.info("Numéro adhérent : " + bvrReponse.getNoAdherent());
			LOGGER.info("numéro référence : " + bvrReponse.getNoReference());
			LOGGER.info("-------------------------------------------------");
		}
		if (bvrsReponse.size() >= 1) {
			return bvrsReponse.get(0);
		} else {
			return null;
		}
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
		}
	}
}
