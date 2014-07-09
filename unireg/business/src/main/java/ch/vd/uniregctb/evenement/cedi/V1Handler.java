package ch.vd.uniregctb.evenement.cedi;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.xml.event.taxation.v1.DeclarationImpot;
import ch.vd.unireg.xml.event.taxation.v1.DossierElectronique;
import ch.vd.uniregctb.jms.EsbBusinessException;

public class V1Handler extends AbstractDossierElectroniqueHandler implements DossierElectroniqueHandler<DossierElectronique> {

	private static final Logger LOGGER = LoggerFactory.getLogger(V1Handler.class);

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/taxation/DossierElectronique-1-0.xsd");
	}

	@Override
	public Class<DossierElectronique> getHandledClass() {
		return DossierElectronique.class;
	}

	private static RetourDI.TypeDocument typeDocumentFromTypeSaisie(String code) {
		if (StringUtils.isBlank(code)) {
			return null;
		}

		if ("M".equals(code)) {
			return RetourDI.TypeDocument.MANUSCRITE;
		}
		else if ("E".equals(code)) {
			return RetourDI.TypeDocument.VAUDTAX;
		}

		return null;
	}

	public static RetourDI.TypeDocument fromTypeDocument(String code) {
		if (StringUtils.isBlank(code)) {
			return null;
		}

		if ("100".equals(code)) {
			return RetourDI.TypeDocument.MANUSCRITE;
		}
		else if ("109".equals(code)) {
			return RetourDI.TypeDocument.VAUDTAX;
		}

		return null;
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

		// Téléphone du 27 mai 2010 avec Bernard Gaberell: il faut utiliser le type de saisie pour déterminer
		// le type de document (et non pas le type de document sur la déclaration). Le truc c'est qu'il arrive
		// que des DIs électroniques ne puissent pas être scannées, elles sont alors entrées à la main et dans
		// ce cas le type de document change, mais pas le type de saisie.
		//
		// Précision par email du 27 mai 2010 de Bernard Gaberell:
		//  - Depuis la DI2009 (actuellement scannage en production) :
		//    La balise <TypeSaisie>M</TypeSaisie> détermine si l'on a à faire à une DI manuelle (ordinaire) ou électronique (VaudTax ou autres éditeurs).
		// 	    M - déclaration manuelle
		//      E - déclaration électronique (vaudtax ou autres éditeurs)
		//   Cette balise est valable seulement depuis la DI 2009.
		//  - Pour les DI antérieures à 2009 (2008, 2007, etc..) :
		//    Le type de document peut être déterminé par :
		//      100 - déclaration manuelle
		//      109 - déclaration vaudtax (ou autres éditeurs)
		RetourDI.TypeDocument typeDocument = typeDocumentFromTypeSaisie(document.getOperation().getTypeSaisie());
		if (typeDocument == null) {
			// il s'agit peut-être d'une ancienne DI
			typeDocument = fromTypeDocument(di.getTypeDocument());
		}
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
