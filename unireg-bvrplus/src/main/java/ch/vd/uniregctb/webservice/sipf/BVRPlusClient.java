package ch.vd.uniregctb.webservice.sipf;

import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrDemande;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrReponse;

public interface BVRPlusClient {

	public BvrReponse getBVRDemande(BvrDemande bvrDemande) ;

}
