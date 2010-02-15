package ch.vd.uniregctb.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.util.Assert;

import weblogic.GetMessage;

import ch.vd.ifosdi.metier.registre.ContribuableSDI;
import ch.vd.registre.base.date.PartialDateException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EtatCivil;
import ch.vd.registre.civil.model.Individu;
import ch.vd.registre.common.model.CoordonneesFinancieres;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.registre.fiscal.model.ContribuableFoyer;
import ch.vd.registre.fiscal.model.DeclarationQuittance;
import ch.vd.registre.fiscal.model.EnumCritereRechercheContribuable;
import ch.vd.registre.fiscal.model.EnumTypeImposition;
import ch.vd.registre.fiscal.model.ResultatRechercheContribuable;
import ch.vd.registre.fiscal.model.impl.ContribuableRetourInfoDiImpl;
import ch.vd.registre.fiscal.model.impl.DeclarationQuittanceImpl;
import ch.vd.registre.fiscal.service.ServiceFiscal;
import ch.vd.registre.fiscal.service.ServiceFiscalHome;
import ch.vd.uniregctb.perfs.PerfsAccessFile;
import ch.vd.uniregctb.perfs.PerfsAccessFileIterator;
import ch.vd.uniregctb.utils.ContribuableComparator;
import ch.vd.uniregctb.utils.EjbUtils;

import com.thoughtworks.xstream.XStream;

/*@ContextConfiguration(locations = {
 "classpath:ut/uintf-ejbit-interfaces.xml",
 "classpath:ut/uintf-ejbit-beans.xml",
 "classpath:ut/uintf-ejbit-database.xml",
 "classpath:ut/uintf-ejbit-services.xml",
 "classpath:unireg-business-services.xml",
 "classpath:unireg-business-interfaces.xml",
 ClientConstants.UNIREG_BUSINESS_MIGREG


 })*/
public class EjbClient {

	protected static ServiceFiscal serviceFiscalUnireg;
	protected static ServiceFiscal serviceFiscalHost;

	protected static ContribuableComparator comparator;
	protected static XStream xstream;

	// private static ServiceFiscalImpl serviceImpl;
	private static final long CONTRIBUABLE_SIMPLE = 10120904;
	private static List listContribuable = null;
	private static final Logger LOGGER = Logger.getLogger(EjbClient.class);
	private static final long NANO_TO_MILLI = 1000000;

	/**
	 * @param args
	 * @throws java.text.ParseException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException, java.text.ParseException {

		final CommandLine line = parseCommandLine(args);
		if (line == null) {
			return;
		}

		EjbClient client = new EjbClient();
		long noContribuable = 37308702;

		if (new File("log4j.xml").exists()) {
			// run depuis ant
			DOMConfigurator.configure("log4j.xml");
		}
		else {
			// run depuis éclipse
			DOMConfigurator.configure("src/main/java/ch/vd/uniregctb/client/log4j.xml");
		}

		final Integer queriesCount = Integer.valueOf(line.getOptionValue("queries", "1000"));
		final String ctbIdAsString = line.getOptionValue("ctb");
		final String ctbIdsFilename = line.getOptionValue("ctbList");
		final String periodeAsString = line.getOptionValue("periode");
		final String accessFilename = line.getOptionValue("accessFile");
		final String cmdTraitement = (String) line.getArgList().get(0);

		if ((ctbIdAsString == null && ctbIdsFilename == null && accessFilename == null) && !cmdTraitement.equals("modif")
				&& !cmdTraitement.equals("sansdi")) {
			System.err.println("Une des deux options 'ctb' ou 'ctbList' doit être spécifiée.");
			System.exit(1);
		}

		if (periodeAsString == null && !cmdTraitement.equals("modif") && !cmdTraitement.equals("modifier")) {
			System.err.println("Une periode doit être spécifiée.");
			System.exit(1);
		}

		try {
			client.setUp();
		} catch (Exception e1) {
			
			LOGGER.error(e1.getMessage());
		}

		if (cmdTraitement.equals("charge") || cmdTraitement.equals("compare")) {
			if (ctbIdsFilename != null) {
				try {

					LOGGER.info(" fichier traité = " + ctbIdsFilename);
					listContribuable = getIdsFromFile(ctbIdsFilename);
				}
				catch (IOException e) {
					System.out.println("Problème de chargement des contribuables :" + e.getMessage());

				}
			}
		}

		LOGGER.info("Démarrage du test");

		if (cmdTraitement.equals("charge")) {
			Long tempsTraitement = null;

			if (ctbIdsFilename != null) {
				tempsTraitement = client.runEjb(listContribuable, Integer.parseInt(periodeAsString));

			}
			else {
				PerfsAccessFile accessFile;
				accessFile = new PerfsAccessFile(accessFilename);
				PerfsAccessFileIterator iter = new PerfsAccessFileIterator(accessFile);
				try {
					tempsTraitement = client.runEjbPerfs(iter, Integer.parseInt(periodeAsString));
				} catch (NumberFormatException e) {
					// 
					e.printStackTrace();
				} catch (Exception e) {
					// 
					e.printStackTrace();
				}
			}
			LOGGER.info(" Temps de traitement = " + tempsTraitement);
		}
		else if (cmdTraitement.equals("get")) {

			LOGGER.info(" contribuable a recuperer = " + ctbIdAsString);
			try {
				client.getContribuable(Long.parseLong(ctbIdAsString), Integer.parseInt(periodeAsString));
			}
			catch (RegistreException e) {
				// 
				e.printStackTrace();
			}
			catch (IOException e) {
				// 
				e.printStackTrace();
			}
		}
		else if (cmdTraitement.equals("getCtrl")) {

			LOGGER.info(" contribuableSDI a recuperer = " + ctbIdAsString);
			try {
				client.getContribuableSDI(Integer.parseInt(ctbIdAsString));
			}
			catch (RegistreException e) {
				// 
				e.printStackTrace();
			}
			catch (IOException e) {
				// 
				e.printStackTrace();
			}
			LOGGER.info(" Fin de traitement pour " + ctbIdAsString);
		}
		else if (cmdTraitement.equals("compare")) {
			int ret = client.executeComparaison(listContribuable, Integer.parseInt(periodeAsString));
		}
		else if (cmdTraitement.equals("modif")) {
			client.getContribuableModifie();
		}
		else if (cmdTraitement.equals("sansdi")) {
			client.getContribuableSansDI(Integer.parseInt(periodeAsString));
		}
		else if (cmdTraitement.equals("modifier")) {
			try {
				client.modifierInfo(Integer.parseInt(ctbIdAsString));
				LOGGER.info(" contribuable " + ctbIdAsString + " modifié");
			}
			catch (NumberFormatException e) {
				// 
				e.printStackTrace();
			}
			catch (RegistreException e) {
				// 
				e.printStackTrace();
			}
			catch (IOException e) {
				// 
				e.printStackTrace();
			}
		}

	}

	public void setUp() throws Exception {
		xstream = new XStream();
		//serviceFiscalUnireg = (ServiceFiscal) ObjectBeanFactory.getInstance().getBean("serviceFiscalUnireg");
		serviceFiscalUnireg = (ServiceFiscal) (ServiceFiscal) EjbUtils.createBean(ServiceFiscalHome.JNDI_NAME);
		serviceFiscalHost = (ServiceFiscal) ObjectBeanFactory.getInstance().getBean("serviceFiscalHost");
		comparator = (ContribuableComparator) ObjectBeanFactory.getInstance().getBean("contribuableComparator");

	}

	public void getContribuable(Long noContribuable, int annee) throws RegistreException, IOException {

		ContribuableFoyer contribuableUnireg = null;
		try {
			//getContribuableModifie();
			//getContribuableSansDI(2008);
			//getContribuableSDI(noContribuable.intValue());
			//quittanceDeclaration();
			//contribuableUnireg = (ContribuableFoyer) serviceFiscalHost.getContribuableInfoGenerale(noContribuable, annee,false);
			//contribuableUnireg = (ContribuableFoyer) serviceFiscalUnireg.getContribuableInfoGenerale(noContribuable, annee,false);
			//getFors( noContribuable, annee);
			//getCoordonneesFinancieres(noContribuable);
			//RechercherContribuables(noContribuable);
			modifierInfo(noContribuable.intValue());
			
		}
		catch (Exception e) {
			// 
			e.printStackTrace();
		}
		/*if(contribuableUnireg!=null){
			List<EtatCivil> etatsCivils= new ArrayList<EtatCivil>(((Individu)contribuableUnireg.getEntiteCivile()).getEtatsCivils());
			Assert.notNull(etatsCivils.get(0));
		}*/
		xstream.setMode(XStream.NO_REFERENCES);
		String ctbUniregXml = xstream.toXML(contribuableUnireg);
		
		File fileCtbUniregXml = new File("Ctb-UniregValid" + noContribuable + ".xml");
		//File fileCtbUniregXml = new File("Ctb-HostPreProd" + noContribuable + ".xml");
		FileUtils.writeStringToFile(fileCtbUniregXml, ctbUniregXml);

	}
	
	public void quittanceDeclaration() throws RegistreException, IOException {

		
		try {
				List quittancesList = new ArrayList();
				List list = FileUtils.readLines(new File("ctbAQuittancerOne.txt"));
			
				for (Iterator iterator = list.iterator(); iterator.hasNext();) {
					 long  noDeclaration = Long.parseLong((String) iterator.next());
					 DeclarationQuittance noDec = new DeclarationQuittanceImpl(noDeclaration);
					 quittancesList.add(noDec);
					
				}

				
			
			

			

	       // long  noDeclaration = Long.parseLong("1004971220080101");
	       // long  noDeclaration = Long.parseLong("1000325520080101");
	      //  DeclarationQuittance noDec2 = new DeclarationQuittanceImpl(noDeclaration);
	       //   noDeclaration = Long.parseLong("1003025620080101");
	         // noDeclaration = Long.parseLong("1000325620080101");
	       // DeclarationQuittance noDec3 = new DeclarationQuittanceImpl(noDeclaration);
	      //    noDeclaration = Long.parseLong("1003025920080101");
	         // noDeclaration = Long.parseLong("1000325920080101");
	      //  DeclarationQuittance noDec4 = new DeclarationQuittanceImpl(noDeclaration);
	      //    noDeclaration = Long.parseLong("1003026020080101");
	        //  noDeclaration = Long.parseLong("1000326020080101");
	      //  DeclarationQuittance noDec5 = new DeclarationQuittanceImpl(noDeclaration);
	          

	       // quittancesList.add(noDec2);
	        //quittancesList.add(noDec3);
	       // quittancesList.add(noDec4);
	       // quittancesList.add(noDec5);
			
			List listResult = serviceFiscalUnireg.quittanceDeclarations(quittancesList);
			int i=0;
			
		}
		catch (Exception e) {
			// 
			e.printStackTrace();
		}
		
	}
	
	
	public void getFors(long noContribuable,int annee) throws Exception, RegistreException{
		
		Collection forsUnireg = serviceFiscalUnireg.getFors(noContribuable, annee);		
		String stringForUnireg = xstream.toXML(forsUnireg);
		File fileForUniregXml = new File("forsUnireg" + noContribuable + ".xml");
		FileUtils.writeStringToFile(fileForUniregXml, stringForUnireg);
		
	}
	public void getCoordonneesFinancieres(long noContribuable) throws RegistreException, IOException {

		CoordonneesFinancieres coordonnees = null;
		CoordonneesFinancieres coordonneesHost = null;
		try {
			coordonnees = (CoordonneesFinancieres) serviceFiscalUnireg.getCoordonneesFinancieres(noContribuable);
			coordonneesHost = (CoordonneesFinancieres) serviceFiscalHost.getCoordonneesFinancieres(noContribuable);

		}
		catch (Exception e) {
			// 
			e.printStackTrace();
		}
		String cooUniregXml = xstream.toXML(coordonnees);
		String cooHostXml = xstream.toXML(coordonneesHost);
		File fileCooUniregXml = new File("CooUnireg-Debug" + noContribuable + ".xml");
		File fileCooHostXml = new File("CooHost-Debug" + noContribuable + ".xml");
		FileUtils.writeStringToFile(fileCooUniregXml, cooUniregXml);
		FileUtils.writeStringToFile(fileCooHostXml, cooHostXml);

	}
	
	@SuppressWarnings("unchecked")
	public void RechercherContribuables(Long noContribuable) throws RegistreException, IOException {
		HashMap criteresRecherche = new HashMap();
		
		ResultatRechercheContribuable resultat= null;
		Collection collectionUnireg =null;
		Collection collectionFoyer =null;
		Collection collectionHost =null;
	//	criteresRecherche.put(EnumCritereRechercheContribuable.NOM,"huber");
		 
		//criteresRecherche.put(EnumCritereRechercheContribuable.NOM,"Haue-Pedersen");
		//criteresRecherche.put(EnumCritereRechercheContribuable.PRENOM,null);
		//criteresRecherche.put(EnumCritereRechercheContribuable.LOCALITE_POSTALE,"NYON");
		//criteresRecherche.put(EnumCritereRechercheContribuable.PAYS,"Sénégal");
		//criteresRecherche.put(EnumCritereRechercheContribuable.NO_AVS,"67904266000");
		//criteresRecherche.put(EnumCritereRechercheContribuable.NO_CONTRIBUABLE,noContribuable.intValue());
		//criteresRecherche.put(EnumCritereRechercheContribuable.NO_INDIVIDU,null);		
		
		criteresRecherche.put(EnumCritereRechercheContribuable.DATE_NAISSANCE,RegDate.get(1966,3,3).asJavaDate());//03.03.1966
		//criteresRecherche.put(EnumCritereRechercheContribuable.NO_NPA,1081);
		try {
			collectionUnireg =  serviceFiscalUnireg.rechercherContribuables(criteresRecherche, 49);
			long numeroDardare= 10310605;
			collectionFoyer =  serviceFiscalUnireg.getNoContribuableFoyer(numeroDardare , 2007, 2008);

		}
		catch (Exception e) {
			// 
			e.printStackTrace();
		}
		if (collectionUnireg != null) {
			ArrayList<ResultatRechercheContribuable> listResultUnireg = new ArrayList<ResultatRechercheContribuable>(
					collectionUnireg);			
			
		
			FileWriter writerResultat = new FileWriter("rechercheContribuableUniVide.xml", false);
			for (ResultatRechercheContribuable r : listResultUnireg) {
				String resXml = xstream.toXML(r);
				writerResultat.write(resXml+"\n");				
			}
			writerResultat.close();
			
			
 }
		if (collectionFoyer!=null) {
			FileWriter writerFoyer = new FileWriter("numeroFoyer.xml", false);
			ArrayList<Long> listnumero = new ArrayList<Long>(collectionFoyer);
			for (long r : listnumero) {
				String resXml = xstream.toXML(r);
				writerFoyer.write(resXml+"\n");				
			}
			writerFoyer.close();
		}
		/*
		try {
			collectionHost = serviceFiscalHost.rechercherContribuables(criteresRecherche, 49);

		}
		catch (Exception e) {
			// 
			e.printStackTrace();
		}
			
		ArrayList<ResultatRechercheContribuable> listResultHost = new ArrayList<ResultatRechercheContribuable>(
				collectionHost);
			FileWriter writerResultatHost = new FileWriter("rechercheContribuableHostV.xml", false);
			for (ResultatRechercheContribuable r : listResultHost) {
				String resXml = xstream.toXML(r);
				writerResultatHost.write(resXml+"\n");				
			}
			writerResultatHost.close();*/

		

	}

	public void getContribuableSDI(int noContribuable) throws RegistreException, IOException {

		ContribuableSDI contribuableUnireg = null;

		try {
			contribuableUnireg = serviceFiscalUnireg.getCtrlContribuable(RegDate.get(2009,8,1).asJavaDate(), noContribuable);

		}
		catch (Exception e) {
			// 
			e.printStackTrace();
		}

		String ctbUniregXml = xstream.toXML(contribuableUnireg);
		File fileCtbUniregXml = new File("CtbSDI" + noContribuable + "Unireg.xml");
		FileUtils.writeStringToFile(fileCtbUniregXml, ctbUniregXml);

	}

	public void modifierInfo(int noContribuable) throws RegistreException, IOException {

		/** Contribuable */
		final short ANNEE_DI = 2008;

		/** Contribuable */
		final short NUM_DI = 1;
		try {
			ContribuableRetourInfoDiImpl contribuableRetour = new ContribuableRetourInfoDiImpl();
			contribuableRetour.setNoContribuable(noContribuable);
			contribuableRetour.setEmail("zouzou@gmail.com");
			contribuableRetour.setIban("CFE2145000321457");
			contribuableRetour.setNoMobile("0789651243");
			contribuableRetour.setNoTelephone("0215478936");
			contribuableRetour.setTitulaireCompte("Famille devel");
			contribuableRetour.setTypeImposition(EnumTypeImposition.ELECTRONIQUE);
			contribuableRetour.setAnneeFiscale(ANNEE_DI);
			contribuableRetour.setNoImpotAnnee(NUM_DI);
			serviceFiscalUnireg.modifierInformationsPersonnelles(contribuableRetour);
		}
		catch (Exception e) {
			// 
			e.printStackTrace();
		}

	}

	public long runEjb(List<Long> listContribuable, int annee) {
		ContribuableFoyer contribuableUnireg = null;
		int i = 0;
		int errorsCount = 0;
		int threadCount = 1;
		int size = listContribuable.size();
		final int modulo = (size <= 100 ? 1 : size / 100);

		LOGGER.info("debut du test de performance  :" + Calendar.getInstance().getTime().toString());
		long startTime = System.nanoTime();
		for (long noContribuable : listContribuable) {
			i++;

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Traitement du tiers n°" + noContribuable);
			}

			if (i % modulo == 0) { // à chaque changement de pourcent
				LOGGER.info(((100 * i) / size) + "% des tiers traite");
			}
			try {
				contribuableUnireg = (ContribuableFoyer) serviceFiscalUnireg.getContribuable(noContribuable, annee);
				if (LOGGER.isTraceEnabled()) {
					if (contribuableUnireg != null) {
						LOGGER.trace("Récupéré le tiers n°" + noContribuable + " (" + contribuableUnireg.getClass().getSimpleName() + ")");
					}
					else {
						LOGGER.trace("Le tiers n°" + noContribuable + " n'existe pas.");
					}
				}

			}
			catch (RemoteException e) {
				// 
				LOGGER.error("Problème d'accès distant  :" + noContribuable + e.getMessage());
				errorsCount++;
			}
			catch (RegistreException e) {
				// 
				LOGGER.error("Problème Registre :" + noContribuable + e.getMessage());
				errorsCount++;
			}
		}
		LOGGER.info(" fin du test  :" + Calendar.getInstance().getTime().toString());
		// Affiche les statistiques
		long time = (System.nanoTime() - startTime) / NANO_TO_MILLI;
		LOGGER.info("Nombre de requêtes      : " + size);
		LOGGER.info("Nombre de threads       : " + threadCount);
		LOGGER.info("Nombre d'erreurs        : " + errorsCount);
		LOGGER.info("Temps d'exécution total : " + time + " ms");
		LOGGER.info(" - bande passante       : " + size * 1000 / time + " requêtes/secondes");
		LOGGER.info(" - ping moyen           : " + threadCount * time / size + " ms");
		return time;
	}

	// permet de faire les tests de charges
	// implementation a factoriser plus tard
	public long runEjbPerfs(PerfsAccessFileIterator ids, int annee) throws Exception {
		ContribuableFoyer contribuableUnireg = null;
		int i = 0;
		int errorsCount = 0;
		int threadCount = 1;
		
		long queryTime = 0;
		int queryCount = 0;

		LOGGER.info("debut du test de performance  :" + Calendar.getInstance().getTime().toString());
		long startTime = System.nanoTime();

		for (Long id = ids.getNextId(); id != null; id = ids.getNextId()) {

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Traitement du tiers n°" + id);
			}

			if (++i % 100 == 0) { // à chaque changement de pourcent
				LOGGER.info(i + " tiers traite");
			}
			try {
				long before = System.nanoTime();
				contribuableUnireg = (ContribuableFoyer) serviceFiscalUnireg.getContribuableInfoGenerale(id, annee, false);
				
					//getFors( id, annee);
				
				long after = System.nanoTime();
				long delta = after - before;
				queryTime += delta;
				++queryCount;

				if (LOGGER.isTraceEnabled()) {
					long ping = delta / NANO_TO_MILLI;
					if (contribuableUnireg != null) {
						
						
						List<EtatCivil> etatsCivils= new ArrayList<EtatCivil>(((Individu)contribuableUnireg.getEntiteCivile()).getEtatsCivils());
						Assert.notNull(etatsCivils.get(0));
						
						LOGGER.trace("Récupéré le tiers n°" + id + " de type " + contribuableUnireg.getClass().getSimpleName() + " ("
								+ ping + " ms)");
					}
					else {
						LOGGER.trace("Le tiers n°" + id + " n'existe pas (" + ping + " ms)");
					}
				}

			}
			catch (RemoteException e) {
				// 
				LOGGER.error("Problème d'accès distant  :" + id + e.getMessage());
				errorsCount++;
			}
			catch (RegistreException e) {
				// 
				LOGGER.error("Problème Registre :" + id + e.getMessage());
				errorsCount++;
			}
		}
		LOGGER.info(" fin du test  :" + Calendar.getInstance().getTime().toString());
		// Affiche les statistiques
		int size = ids.getCount();
		final int modulo = (size <= 100 ? 1 : size / 100);
		long time = (System.nanoTime() - startTime) / NANO_TO_MILLI;
		LOGGER.info("Nombre de requêtes      : " + size);
		LOGGER.info("Nombre de threads       : " + threadCount);
		LOGGER.info("Nombre d'erreurs        : " + errorsCount);
		LOGGER.info("Temps d'exécution total : " + time + " ms");
		LOGGER.info(" - bande passante       : " + size * 1000 / time + " requêtes/secondes");
		LOGGER.info(" - ping moyen           : " + threadCount * time / size + " ms");
		return time;
	}

	public int executeComparaison(List<Long> listContribuable, int annee) {
		ContribuableFoyer contribuableUnireg = null;
		ContribuableFoyer contribuableHost = null;
		LOGGER.info("debut de comparaison :" + Calendar.getInstance().getTime().toString());
		for (long noContribuable : listContribuable) {
			try {
				contribuableUnireg = (ContribuableFoyer) serviceFiscalUnireg.getContribuable(noContribuable, annee);
				contribuableHost = (ContribuableFoyer) serviceFiscalHost.getContribuable(noContribuable, annee);
				try {
					comparator.compare(contribuableHost, contribuableUnireg);
					System.out.println("Contribuable: " + noContribuable + " comparé");
				}
				catch (Exception e) {
					// 
					e.printStackTrace();
				}
				// if (contribuableUnireg == null) {
				// System.out.println("Contribuable: " + noContribuable + " non trouve");
				// }
				// else {
				// System.out.println("Contribuable: " + contribuableUnireg.getNomCourrier1());
				// }
			}
			catch (RemoteException e) {
				// 
				System.out.println("Problème d'accès distant  :" + noContribuable + e.getMessage());
			}
			catch (RegistreException e) {
				// 
				System.out.println("Problème Registre :" + noContribuable + e.getMessage());
			}
		}

		System.out.println("fin de recherche  :" + Calendar.getInstance().getTime().toString());

		return 0;
	}

	public void getContribuableModifie() {
		RegDate debut = RegDate.get(2008, RegDate.SEPTEMBRE, 8);
		RegDate fin = RegDate.get(2008, RegDate.SEPTEMBRE, 9);

		List<Integer> listId = null;
		
		try {
			LOGGER.info("debut de recherche des ctb modifiés:" + Calendar.getInstance().getTime().toString());
			listId = (List<Integer>) serviceFiscalUnireg.getListeCtbModifies(debut.asJavaDate(), fin.asJavaDate(), 10090115);
		}
		catch (RemoteException e) {
			// 
			e.printStackTrace();
		}
		catch (PartialDateException e) {
			// 
			e.printStackTrace();
		}
		catch (RegistreException e) {
			// 
			e.printStackTrace();
		}
		LOGGER.info("debut de sauvegarde des resultats :" + Calendar.getInstance().getTime().toString());
		File fileContribuableModifie = new File("contribuableModifies" + fin.get().year() + ".xml");
		try {
			FileUtils.writeStringToFile(fileContribuableModifie, xstream.toXML(listId));
		}
		catch (IOException e) {
			// 
			e.printStackTrace();
		}
	}

	public void getContribuableSansDI(int periode) {
		List<Integer> listId = null;
		;
		try {
			LOGGER.info("debut de recherche des Sans DI:" + Calendar.getInstance().getTime().toString());
			listId = (List<Integer>) serviceFiscalUnireg.getListeCtbSansDIPeriode(periode, 10140200);
		}
		catch (RemoteException e) {
			// 
			e.printStackTrace();
		}
		catch (PartialDateException e) {
			// 
			e.printStackTrace();
		}
		catch (RegistreException e) {
			LOGGER.info("Erreur registre:" + e.getMessage());
		}
		LOGGER.info("debut de sauvegarde des ctb sans DI:" + Calendar.getInstance().getTime().toString());
		File fileContribuableModifie = new File("contribuableSansDIUnireg" + periode + ".xml");
		try {
			FileUtils.writeStringToFile(fileContribuableModifie, xstream.toXML(listId));
		}
		catch (IOException e) {
			// 
			e.printStackTrace();
		}
	}

	public static long[] readContribuablesFromFile(File file) throws IOException {
		// FileUtils.lineIterator(/* file */ null);

		List list = FileUtils.readLines(file);
		long[] result = new long[list != null ? list.size() : 0];
		int index = 0;
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			String line = (String) iterator.next();
			result[index++] = (new Long(line)).longValue();
		}

		return result;
	}

	private static List<Long> getIdsFromFile(String ctbIdsFilename) throws FileNotFoundException {
		List<Long> ids = new ArrayList<Long>();
		File file = new File(ctbIdsFilename);
		if (!file.exists()) {
			System.err.println("Le fichier '" + ctbIdsFilename + "' n'existe pas.");
			System.exit(1);
		}
		if (!file.canRead()) {
			System.err.println("Le fichier '" + ctbIdsFilename + "' n'est pas lisible.");
			System.exit(1);
		}
		Scanner s = new Scanner(file);
		try {
			while (s.hasNextLine()) {
				String line = s.nextLine();
				String idAsString = line.replaceAll("[^0-9]", "");
				Long id = Long.valueOf(idAsString);
				ids.add(id);
			}
		}
		finally {
			s.close();
		}
		return ids;
	}

	@SuppressWarnings("static-access")
	private static CommandLine parseCommandLine(String[] args) {

		final CommandLine line;
		try {
			CommandLineParser parser = new GnuParser();
			Option help = new Option("help", "affiche ce message");
			Option charge = new Option("charge", "permet de lancer un test de performance sur une liste de contribuable");
			Option modifier = new Option("modifier", "modifie les informations personnelles d'un ctb");
			Option get = new Option("get", "Recupere un contribuable dont le numéro est passé en paramètre");
			Option getCtrl = new Option("get", "Recupere un contribuable de type contribuable SDI dont le numéro est passé en paramètre");
			Option compare = new Option("compare", "fait la comparaison entre un contribuable unireg et un contribuable host");
			Option sansdi = OptionBuilder.withArgName("periode").hasArg().withDescription(
					"recupere la liste des contribuables sans DI pour une période").create("sansdi");
			Option ctb = OptionBuilder.withArgName("id").hasArg().withDescription("numéro du contribuable à récupérer").create("ctb");
			Option ctbList = OptionBuilder.withArgName("filename").hasArg().withDescription(
					"fichier avec les numéros des contribuables à récupérer (un numéro par ligne)").create("ctbList");
			Option accessFile = OptionBuilder.withArgName("filename").hasArg().withDescription(
					"fichier avec les numéros des contribuables et les temps d'accès (format host-interface)").create("accessFile");

			Option periode = OptionBuilder.withArgName("annee").hasArg().withDescription(
					"retourne un contribuable pour une période fiscale").create("periode");
			Option queries = OptionBuilder.withArgName("count").hasArg().withDescription("nombre de requêtes (défaut=1000)").create(
					"queries");

			Options options = new Options();
			options.addOption(help);
			options.addOption(charge);
			options.addOption(modifier);
			options.addOption(get);
			options.addOption(getCtrl);
			options.addOption(compare);
			options.addOption(sansdi);
			options.addOption(ctb);
			options.addOption(accessFile);
			options.addOption(ctbList);

			options.addOption(periode);
			options.addOption(queries);

			// parse the command line arguments
			line = parser.parse(options, args);

			if (line.hasOption("help") || line.getArgs().length != 1) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("EjbClient [options]  charge, get,compare ", "Options:", options, null);
				return null;
			}
		}
		catch (ParseException exp) {
			System.err.println("Erreur de parsing.  Raison: " + exp.getMessage());
			return null;
		}

		return line;
	}

}
