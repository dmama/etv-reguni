package ch.vd.uniregctb.evenement.cedi;

import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.xml.event.taxation.v2.DeclarationImpot;
import ch.vd.unireg.xml.event.taxation.v2.DossierElectronique;
import ch.vd.uniregctb.jms.EsbBusinessException;

public class V2Handler extends AbstractDossierElectroniqueHandler implements DossierElectroniqueHandler<DossierElectronique> {

	private static final Logger LOGGER = Logger.getLogger(V2Handler.class);

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/taxation/DossierElectronique-2-0.xsd");
	}

	@Override
	public Class<DossierElectronique> getHandledClass() {
		return DossierElectronique.class;
	}

	private static RetourDI.TypeDocument typeDocumentFromOrigineAcquisition(int code) {
		return code == 2 ? RetourDI.TypeDocument.MANUSCRITE : RetourDI.TypeDocument.VAUDTAX;
	}

	@Override
	public void doHandle(DossierElectronique document, Map<String, String> incomingHeaders) throws EsbBusinessException {

		final DeclarationImpot di = document.getDeclarationImpot();
		final DeclarationImpot.Identification.CoordonneesContribuable coordonnes = di.getIdentification() == null ? null : di.getIdentification().getCoordonneesContribuable(); // [UNIREG-2603]

		final RetourDI scan = new RetourDI();
		scan.setDateTraitement(DateHelper.getCurrentDate());
		scan.setNoContribuable(Long.parseLong(di.getNoContribuable()));
		scan.setPeriodeFiscale(Integer.parseInt(di.getPeriode()));
		scan.setNoSequenceDI(Integer.parseInt(di.getNoSequenceDI()));

		final RetourDI.TypeDocument typeDocument = typeDocumentFromOrigineAcquisition(document.getOperation().getOrigineAcquisitionDi().intValue());
		scan.setTypeDocument(typeDocument);

		if (coordonnes != null) { // le XSD permet de ne pas renseigner ces coordonnées
			scan.setEmail(coordonnes.getAdresseMail());
			scan.setIban(coordonnes.getCodeIBAN());
			scan.setNoMobile(coordonnes.getNoTelPortable());
			scan.setNoTelephone(coordonnes.getTelephoneContact());
			scan.setTitulaireCompte(coordonnes.getTitulaireCompte());
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Contenu du message : " + scan);
		}

		onEvent(scan, incomingHeaders);
	}
}
