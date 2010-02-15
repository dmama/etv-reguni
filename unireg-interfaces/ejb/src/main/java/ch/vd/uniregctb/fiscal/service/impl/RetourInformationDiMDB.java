package ch.vd.uniregctb.fiscal.service.impl;

import java.io.StringReader;

import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import ch.vd.registre.common.service.RegistreException;
import ch.vd.registre.fiscal.model.EnumTypeImposition;
import ch.vd.registre.fiscal.model.impl.ContribuableRetourInfoDiImpl;
import ch.vd.registre.fiscal.service.ServiceFiscal;
import ch.vd.registre.fiscal.service.ServiceFiscalHome;

public class RetourInformationDiMDB implements MessageDrivenBean, MessageListener {

	private static Logger log = Logger.getLogger(RetourInformationDiMDB.class); 
	
	private static String DECLARATION_IMPOT_XPATH = "/DossierElectronique/DeclarationImpot/";
	private static String COOR_CONTRIBUABLE_XPATH = "/DossierElectronique/DeclarationImpot/Identification/CoordonneesContribuable/";
	
	private Context context = null;
	private MessageDrivenContext mdbContext = null;
	private DocumentBuilderFactory builderFactory = null;
	private XPathFactory xpathFactory = null;

	public void ejbCreate() {
	}
	
	public void ejbRemove() throws EJBException {
	}

	public void setMessageDrivenContext(MessageDrivenContext mdbContext)
			throws EJBException {
		setMdbContext(mdbContext);
		
		try {
			setContext(new InitialContext());
			setBuilderFactory(DocumentBuilderFactory.newInstance());
			setXpathFactory(XPathFactory.newInstance());
		} catch (Exception e) {
			throw new EJBException(e);
		}
	}

	public void onMessage(Message message) {
		
		ContribuableRetourInfoDiImpl info = null;
		
		try {
			if (message instanceof TextMessage) {
				
				TextMessage tm = (TextMessage) message;

				Document doc = builderFactory.newDocumentBuilder().parse(new InputSource(new StringReader(tm.getText())));
				info = createContribuableRetourInfoDiFromDocument(doc);
				if (log.isDebugEnabled()) {
					log.debug("Invoke ServiceFiscal.modifierInformationsPersonnelles");
					log.debug("ContribuableRetourInfoDi : " + ToStringBuilder.reflectionToString(info));
				}
	
				ServiceFiscalHome home = (ServiceFiscalHome) PortableRemoteObject.narrow(context.lookup("java:comp/env/ejb/ServiceFiscal"), ServiceFiscalHome.class);
				ServiceFiscal service = home.create();
				service.modifierInformationsPersonnelles(info);
				
			}			
		} catch (RegistreException e) {
			mdbContext.getRollbackOnly();
			log.error("Erreur invocation ServiceFiscal.modifierInformationsPersonnelles", e);
			log.error("ContribuableRetourInfoDi : " + ToStringBuilder.reflectionToString(info));
		} catch (Exception e) {
			mdbContext.getRollbackOnly();
			log.fatal("Erreur technique", e);
			log.fatal("ContribuableRetourInfoDi : " + ToStringBuilder.reflectionToString(info));
			
		}
	}

	/** Visibility : public : in order to enable unit test */
	public ContribuableRetourInfoDiImpl createContribuableRetourInfoDiFromDocument(Document doc) throws XPathException {

		ContribuableRetourInfoDiImpl infoDI = new ContribuableRetourInfoDiImpl();
		XPath xpath = xpathFactory.newXPath();
		
		String noContribuable = xpath.evaluate(DECLARATION_IMPOT_XPATH + "@noContribuable", doc);
		String anneeFiscal = xpath.evaluate(DECLARATION_IMPOT_XPATH + "@periode", doc);
		String noImpotAnnee = xpath.evaluate(DECLARATION_IMPOT_XPATH + "@noSequenceDI", doc);
		String typeImposition = xpath.evaluate(DECLARATION_IMPOT_XPATH + "@typeDocument", doc);
		String email = xpath.evaluate(COOR_CONTRIBUABLE_XPATH + "AdresseMail", doc);
		String iban = xpath.evaluate(COOR_CONTRIBUABLE_XPATH + "CodeIBAN", doc);
		String noMobile = xpath.evaluate(COOR_CONTRIBUABLE_XPATH + "NoTelPortable", doc);
		String noTelephone =  xpath.evaluate(COOR_CONTRIBUABLE_XPATH + "TelephoneContact", doc);
		String titulaireCompte = xpath.evaluate(COOR_CONTRIBUABLE_XPATH + "TitulaireCompte", doc);
			
		infoDI.setNoContribuable(noContribuable==null?0:Integer.parseInt(noContribuable));
		infoDI.setAnneeFiscale(anneeFiscal==null?0:Short.parseShort(anneeFiscal));
		infoDI.setNoImpotAnnee(noImpotAnnee==null?0:Short.parseShort(noImpotAnnee));
		infoDI.setEmail(email);
		infoDI.setIban(iban);
		infoDI.setNoMobile(noMobile);
		infoDI.setNoTelephone(noTelephone);
		infoDI.setTitulaireCompte(titulaireCompte);
		infoDI.setTypeImposition(EnumTypeImposition.getEnum(typeImposition));
		
		return infoDI;

	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public MessageDrivenContext getMdbContext() {
		return mdbContext;
	}

	public void setMdbContext(MessageDrivenContext mdbContext) {
		this.mdbContext = mdbContext;
	}

	public DocumentBuilderFactory getBuilderFactory() {
		return builderFactory;
	}

	public void setBuilderFactory(DocumentBuilderFactory builderFactory) {
		this.builderFactory = builderFactory;
	}

	public XPathFactory getXpathFactory() {
		return xpathFactory;
	}

	public void setXpathFactory(XPathFactory xpathFactory) {
		this.xpathFactory = xpathFactory;
	}
}
