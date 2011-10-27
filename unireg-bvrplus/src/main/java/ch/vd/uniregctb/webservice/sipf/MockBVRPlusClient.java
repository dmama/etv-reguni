package ch.vd.uniregctb.webservice.sipf;

import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrDemande;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrReponse;

public class MockBVRPlusClient implements BVRPlusClient{

	public BvrReponse getBVRDemande(BvrDemande bvrDemande) throws BVRPlusClientException {
		return new BvrReponse();
	}

	public void ping() throws BVRPlusClientException {
	}
}
