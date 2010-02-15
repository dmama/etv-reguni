/**
 *
 */
package ch.vd.uniregctb.fiscal.service.impl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.RemoveException;


import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.ejb.support.AbstractStatelessSessionBean;
import org.springframework.util.Log4jConfigurer;

import ch.vd.ifosdi.metier.exceptions.BusinessException;
import ch.vd.ifosdi.metier.registre.ContribuableSDI;
import ch.vd.registre.common.model.CoordonneesFinancieres;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.registre.fiscal.model.Contribuable;
import ch.vd.registre.fiscal.model.ContribuableRetourInfoDi;
import ch.vd.registre.fiscal.model.DeclarationQuittance;
import ch.vd.registre.fiscal.model.RechercherComplementInformationContribuable;
import ch.vd.registre.fiscal.model.ResultatRechercherNoContribuable;
import ch.vd.registre.fiscal.model.impl.ContribuableRetourInfoDiImpl;
import ch.vd.registre.fiscal.service.ServiceFiscal;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.fiscal.logging.EnumTypeLogging;
import ch.vd.uniregctb.fiscal.logging.PerformancesLogger;
import ch.vd.uniregctb.interfaces.ObjectBeanFactory;

/**
 * @author Baba NGOM xsibnm
 * @version $Revision: 1.0 $
 */
public class ServiceFiscalBean  extends AbstractStatelessSessionBean implements ServiceFiscal {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/** Serial Version UID. */

	private static boolean loggerLoaded=false;
	/** Le loggeur log4j. */
	private static final Logger LOGGER = Logger.getLogger(ServiceFiscalBean.class);
	private static Log4jConfigurer log4jConfigurer = (Log4jConfigurer)ObjectBeanFactory.getInstance().getBean("log4jInitialization");
	public static final Logger LOGGER_ERROR = Logger.getLogger(LOGGER.getName() + ".err");

	private ServiceFiscalImpl serviceFiscalImpl = (ServiceFiscalImpl) ObjectBeanFactory.getInstance().getBean("serviceFiscalImpl");
	private String ignoreI107UserIds = (String)ObjectBeanFactory.getInstance().getBean("ignoreI107UserIds");
	//private String log4jConfigLocation = (String)ObjectBeanFactory.getInstance().getBean("log4JConfigLocation");
	

	private ServiceFiscalImpl getServiceFiscalImpl() {
		/*if (!loggerLoaded) {
			DOMConfigurator.configure(log4jConfigLocation);
			loggerLoaded = true;
		}*/
		return serviceFiscalImpl;
	}

	public void setServiceFiscalImpl(ServiceFiscalImpl serviceFiscalImpl) {
		this.serviceFiscalImpl = serviceFiscalImpl;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void ejbActivate() throws EJBException {
		

	}

	/**
	 * MÃ©thode appelÃ©e Ã  la crÃ©ation du bean.
	 */
	@Override
	public void ejbCreate() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void ejbPassivate() throws EJBException {
		

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void ejbRemove() {

	}



	/**
	 * {@inheritDoc}
	 */
	public EJBHome getEJBHome() throws RemoteException {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Handle getHandle() throws RemoteException {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getPrimaryKey() throws RemoteException {
	
		return null;
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean isContribuableI107(long noContribuable) throws RemoteException, RegistreException {
		
		
		return getServiceFiscalImpl().isContribuableI107(noContribuable);
	}
		

	/**
	 * {@inheritDoc}
	 */
	public void remove() throws RemoteException, RemoveException {
		

	}

	/**
	 * {@inheritDoc}
	 */
	public ResultatRechercherNoContribuable RechercherNoContribuable(
			ch.vd.registre.fiscal.model.RechercherNoContribuable rechercherNoContribuable) throws RemoteException, RegistreException {
		ResultatRechercherNoContribuable resultat = null;
		try {
			resultat = getServiceFiscalImpl().RechercherNoContribuable(rechercherNoContribuable);
		}
		catch (RegistreException e) {
			LOGGER.info("ProblÃ¨me durant l'appel d'un service fiscal", e);
			throw e;
		}

		return resultat;
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection getAssujettissements(long noContribuable, int annee) throws RemoteException, RegistreException {
		Collection result = null;

		/** Appel au service. */
		try {
			result = getServiceFiscalImpl().getAssujettissements(noContribuable, annee);
		}
		catch (RuntimeException e) {
			LOGGER.warn("ProblÃ¨me durant l'appel d'un service fiscal", e);
			throw e;
		}
		catch (RegistreException e) {
			LOGGER.info("ProblÃ¨me durant l'appel d'un service fiscal", e);
			throw e;
		}

		return result;
	}

	public Contribuable getContribuableInfoGenerale(long noContribuable, int annee, boolean recupererContribuableFoyer)
			throws RegistreException {
		Contribuable result = null;
		long startTime = System.currentTimeMillis();
		/** Appel au service. */
		try {
			result = getServiceFiscalImpl().getContribuable(noContribuable, annee, recupererContribuableFoyer, false,isRetrieveI107());
		}
		catch (RuntimeException e) {
			LOGGER.warn("probleme durant l'appel d'un service fiscal", e);
			throw e;
		}
		catch (RegistreException e) {
			LOGGER.info("probleme durant l'appel d'un service fiscal", e);
			throw e;
		}
		finally {
		PerformancesLogger.REGISTRE_FISCAL_SERVICE_PERFS_LOGGER.info(startTime,getSessionContext().getCallerPrincipal(), "getContribuableInfoGenerale",
				EnumTypeLogging.All, new String[]{String.valueOf(noContribuable), String.valueOf(annee),
						String.valueOf(recupererContribuableFoyer)});
		}
		return result;
		
	}

	/**
	 * {@inheritDoc}
	 */
	public Contribuable getContribuable(long noContribuable, int annee) throws RemoteException, RegistreException {
		
		return this.getContribuable(noContribuable, annee, false);
	}

	/**
	 * {@inheritDoc}
	 */
	public Contribuable getContribuable(long noContribuable, int annee, boolean recupererContribuableFoyer) throws RemoteException,
			RegistreException {
		Contribuable result = null;
		long startTime = System.currentTimeMillis();
		/** Appel au service. */
		try {
			result = getServiceFiscalImpl().getContribuable(noContribuable, annee, recupererContribuableFoyer, true,isRetrieveI107());
		}
		catch (RuntimeException e) {
			LOGGER.warn("Probleme durant l'appel d'un service fiscal", e);
			throw e;
		}finally {
			/** Log du temps d'exï¿½cution. */
			PerformancesLogger.REGISTRE_FISCAL_SERVICE_PERFS_LOGGER.info(startTime, getSessionContext().getCallerPrincipal(), "getContribuable", EnumTypeLogging.All,
					new String[]{String.valueOf(noContribuable), String.valueOf(annee), String.valueOf(recupererContribuableFoyer)});
		}


		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public CoordonneesFinancieres getCoordonneesFinancieres(long noContribuable) throws RemoteException, RegistreException {
		CoordonneesFinancieres result = null;
		long startTime = System.currentTimeMillis();
		/** Appel au service. */
		try {
			result = getServiceFiscalImpl().getCoordonneesFinancieres(noContribuable,isRetrieveI107());
		}
		catch (RuntimeException e) {
			LOGGER.warn("probleme durant l'appel d'un service fiscal", e);
			throw e;
		}
		catch (RegistreException e) {
			LOGGER.info("probleme durant l'appel d'un service fiscal", e);
			throw e;
		}finally {
			/** Log du temps d'exï¿½cution. */
			PerformancesLogger.REGISTRE_FISCAL_SERVICE_PERFS_LOGGER.info(startTime, getSessionContext().getCallerPrincipal(), "getCoordonneesFinancieres",
					EnumTypeLogging.All, new String[]{String.valueOf(noContribuable)});
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public CoordonneesFinancieres getCoordonneesFinancieres(long noContribuable, int annee) throws RemoteException, RegistreException {
		CoordonneesFinancieres result = null;
		long startTime = System.currentTimeMillis();
		/** Appel au service. */
		try {
			result = getServiceFiscalImpl().getCoordonneesFinancieres(noContribuable,isRetrieveI107());
		}
		catch (RuntimeException e) {
			LOGGER.warn("probleme durant l'appel d'un service fiscal", e);
			throw e;
		}
		catch (RegistreException e) {
			LOGGER.info("probleme durant l'appel d'un service fiscal", e);
			throw e;
		}finally {
			/** Log du temps d'exï¿½cution. */
			PerformancesLogger.REGISTRE_FISCAL_SERVICE_PERFS_LOGGER.info(startTime, getSessionContext().getCallerPrincipal(), "getCoordonneesFinancieres",
					EnumTypeLogging.All, new String[]{String.valueOf(noContribuable), String.valueOf(annee)});
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection getFors(long noContribuable, int annee) throws RemoteException, RegistreException {
		Collection result = null;

		long startTime = System.currentTimeMillis();
		/** Appel au service. */
		try {

			result = getServiceFiscalImpl().getFors(noContribuable, annee);
		}
		catch (RuntimeException e) {
			LOGGER.warn("Problème durant l'appel d'un service fiscal", e);
			throw e;
		}
		catch (RegistreException e) {
			LOGGER.info("Problème durant l'appel d'un service fiscal", e);
			throw e;
		}finally {
			/** Log du temps d'exï¿½cution. */
			PerformancesLogger.REGISTRE_FISCAL_SERVICE_PERFS_LOGGER.info(startTime, getSessionContext().getCallerPrincipal(), "getFors", EnumTypeLogging.All,
				new String[]{String.valueOf(noContribuable), String.valueOf(annee)});
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection getListeCtbModifies(Date dateDebutRech, Date dateFinRech, int numeroCtbDepart) throws RegistreException{
		/** Appel au service. */
		Collection result = null;
		long startTime = System.currentTimeMillis();

		try {
			String principal = getSessionContext().getCallerPrincipal().getName();
			AuthenticationHelper.setPrincipal(principal);

			result= getServiceFiscalImpl().getListeCtbModifies(dateDebutRech, dateFinRech, numeroCtbDepart);
		} catch (RuntimeException e) {
			LOGGER.warn("Problème durant l'appel d'un service fiscal", e);
			throw e;
		} catch (RegistreException e) {
			LOGGER.info("Problème durant l'appel d'un service fiscal", e);
			throw e;
		}finally {
			/** Log du temps d'exï¿½cution. */
			PerformancesLogger.REGISTRE_FISCAL_SERVICE_PERFS_LOGGER.info(startTime, getSessionContext().getCallerPrincipal(), "getListeCtbModifies", EnumTypeLogging.All,
					new String[]{String.valueOf(dateDebutRech), String.valueOf(dateFinRech), String.valueOf(numeroCtbDepart)});
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection getListeCtbSansDIPeriode(int periodeFiscale, int numeroCtbDepart) throws RemoteException, RegistreException {
		/** Appel au service. */
		long startTime = System.currentTimeMillis();				
		try {
			String principal = getSessionContext().getCallerPrincipal().getName();
			AuthenticationHelper.setPrincipal(principal);

			return getServiceFiscalImpl().getListeCtbSansDIPeriode(periodeFiscale, numeroCtbDepart);
		} catch (RuntimeException e) {
			LOGGER.warn("Problème durant l'appel d'un service fiscal", e);
			throw e;
		} catch (RegistreException e) {
			LOGGER.info("Problème durant l'appel d'un service fiscal", e);
			throw e;
		}
		 finally {
				/** Log du temps d'exï¿½cution. */
				PerformancesLogger.REGISTRE_FISCAL_SERVICE_PERFS_LOGGER.info(startTime, getSessionContext().getCallerPrincipal(), "getListeCtbSansDIPeriode",
						EnumTypeLogging.All, new String[]{String.valueOf(periodeFiscale), String.valueOf(numeroCtbDepart)});
			}
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection getNoContribuableFoyer(long noContribuableIndividu, int anneeDebut, int anneeFin) throws RemoteException,
			RegistreException {
		Collection result = null;
		long startTime = System.currentTimeMillis();		

		/** Appel au service. */
		try {
			String principal = getSessionContext().getCallerPrincipal().getName();
			AuthenticationHelper.setPrincipal(principal);

			result = getServiceFiscalImpl().getNoContribuableFoyer(noContribuableIndividu, anneeDebut, anneeFin);
		}
		catch (RuntimeException e) {
			LOGGER.warn("Problème durant l'appel d'un service fiscal", e);
			throw e;
		}
		catch (RegistreException e) {
			LOGGER.info("Problème durant l'appel d'un service fiscal", e);
			throw e;
		}finally {
			/** Log du temps d'exï¿½cution. */
			PerformancesLogger.REGISTRE_FISCAL_SERVICE_PERFS_LOGGER.info(startTime, getSessionContext().getCallerPrincipal(), "getNoContribuableFoyer",
					EnumTypeLogging.All, new String[]{String.valueOf(noContribuableIndividu), String.valueOf(anneeDebut), String.valueOf(anneeFin)});
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public void modifierCodeBlocageRmbtAuto(long noContribuable, boolean code, String user) throws RemoteException, RegistreException {
		/** Appel au service. */
		
		long startTime = System.currentTimeMillis();		
		try {

			AuthenticationHelper.setPrincipal(user);
			getServiceFiscalImpl().modifierCodeBlocageRmbtAuto(noContribuable, code, user,isRetrieveI107());
		}
		catch (RuntimeException e) {
			LOGGER.warn("Problème durant l'appel d'un service fiscal", e);
			throw e;
		}
		catch (RegistreException e) {
			LOGGER.info("Problème durant l'appel d'un service fiscal", e);
			throw e;
		}
		finally {
			AuthenticationHelper.resetAuthentication();
			/** Log du temps d'exï¿½cution. */
			PerformancesLogger.REGISTRE_FISCAL_SERVICE_PERFS_LOGGER.info(startTime, getSessionContext().getCallerPrincipal(), "modifierCodeBlocageRmbtAuto",
					EnumTypeLogging.All, new String[]{String.valueOf(noContribuable), String.valueOf(code), String.valueOf(user)});
		}

	}

	/**
	 * {@inheritDoc}
	 */
	public void modifierInformationsPersonnelles(ContribuableRetourInfoDi contribuableRetourInfoDi) throws RemoteException, RegistreException {
		long startTime = System.currentTimeMillis();
		/** Appel au service. */
		try {
			//String principal = getSessionContext().getCallerPrincipal().getName();
			String principal = "unireg-interfaces";
			AuthenticationHelper.setPrincipal(principal);
			getServiceFiscalImpl().modifierInformationsPersonnelles(contribuableRetourInfoDi, isRetrieveI107());
		} catch (RuntimeException e) {
			LOGGER.warn("Problème durant l'appel d'un service fiscal", e);
			throw e;
		} catch (RegistreException e) {
			LOGGER.info("Problème durant l'appel d'un service fiscal", e);
			throw e;
		}finally {
			/** Log du temps d'exï¿½cution. */
			PerformancesLogger.REGISTRE_FISCAL_SERVICE_PERFS_LOGGER.info(startTime, getSessionContext().getCallerPrincipal(), "modifierInformationsPersonnelles",
					EnumTypeLogging.All, new String[]{(contribuableRetourInfoDi instanceof ContribuableRetourInfoDiImpl ? String.valueOf(((ContribuableRetourInfoDiImpl)contribuableRetourInfoDi).getNoContribuable()) : "UNKNOWN")});
		}
	}

	/**
	 * {@inheritDoc}
	 */

	public List quittanceDeclarations(List declarationQuittances) throws RemoteException, RegistreException {

		try {
			String principal = getSessionContext().getCallerPrincipal().getName();
			AuthenticationHelper.setPrincipal(principal);
			return getServiceFiscalImpl().quittanceDeclarations(declarationQuittances);
		} 
		catch (RuntimeException e) {
			LOGGER.error(
					"Probleme durant l'appel de quittanceDeclarations avec déclarations = "
							+ toString(declarationQuittances), e);
			throw e;
		} catch (RegistreException e) {
			LOGGER.error(
					"Probleme durant l'appel de quittanceDeclarations avec déclarations = "
							+ toString(declarationQuittances), e);
			throw e;
		} catch (Exception e) {
			LOGGER.error(
					"Probleme durant l'appel de quittanceDeclarations avec déclarations = "
							+ toString(declarationQuittances), e);
			throw new RuntimeException(e);
		}
	}

	private static String toString(
			List<DeclarationQuittance> declarationQuittances) {
		List<Long> ids = new ArrayList<Long>(declarationQuittances.size());
		for (DeclarationQuittance decl : declarationQuittances) {
			ids.add(decl.getNoDeclaration());
		}
		return ArrayUtils.toString(ids.toArray());
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection rechercherContribuables(HashMap criteresRecherche, int nbResultat) throws RemoteException, RegistreException {
		Collection result = null;
		long startTime = System.currentTimeMillis();
		/** Appel au service. */
		try {
			result = getServiceFiscalImpl().rechercherContribuables(criteresRecherche, nbResultat);
		}
		catch (RuntimeException e) {
			LOGGER.warn("Problème durant l'appel d'un service fiscal", e);
			throw e;
		}
		catch (RegistreException e) {
			LOGGER.info("Problème durant l'appel d'un service fiscal", e);
			throw e;
		}finally {
			/** Log du temps d'exï¿½cution. */
			PerformancesLogger.REGISTRE_FISCAL_SERVICE_PERFS_LOGGER.info(startTime, getSessionContext().getCallerPrincipal(), "rechercherContribuables",
					EnumTypeLogging.All, new String[]{criteresRecherche.toString(), String.valueOf(nbResultat)});
		}

		return result;
	}

	/**
	 * Interroge le registre pour recueillir les informations sur le contribuable à la date en paramètre
	 *
	 * @param dateRef :
	 *            date de référence pour l'accès au registre - doit ne contenir que des données année, mois, jour. si des heures, minutes,
	 *            seconds ou milliseconds sont passées, le résultat est imprévisible
	 * @param numeroCtb :
	 *            numéro du contribuable dans le registre
	 * @return ch.vd.ifotao.metier.registre.Contribuable
	 * @throws BusinessException
	 *             si le contribuable n'existe pas
	 */
	public ContribuableSDI getCtrlContribuable(Date dateRef, int numeroCtb) throws RemoteException, BusinessException {

		ContribuableSDI contribuable = null;
		long startTime = System.currentTimeMillis();
		try {
			contribuable = getServiceFiscalImpl().getCtrlContribuable(dateRef, numeroCtb);
			return contribuable;

		}
		catch (ch.vd.ifosdi.metier.exceptions.BusinessException ex) {
			LOGGER.info("Problème durant l'appel d'un service fiscal", ex);
			throw ex;

		}finally {
			/** Log du temps d'exï¿½cution. */
			PerformancesLogger.REGISTRE_FISCAL_SERVICE_PERFS_LOGGER.info(startTime, getSessionContext().getCallerPrincipal(), "getCtrlContribuable",
					EnumTypeLogging.All, new String[]{String.valueOf(numeroCtb),dateRef.toString()});
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public RechercherComplementInformationContribuable getComplementInformationContribuable( long noContribuable) throws RemoteException, RegistreException {
		throw new RuntimeException("NotSupported");
	}

	@Override
	protected void onEjbCreate() throws CreateException {	

	}

	public boolean isIdentical(EJBObject arg0) throws RemoteException {
		
		return false;
	}
	
	private boolean isRetrieveI107() {
		final String principal = getSessionContext().getCallerPrincipal().getName();
		final Map ignoreI107UsersMap = readIgnoreI107Users();
		final boolean retrieveI107 = buildRetrieveI107(principal, ignoreI107UsersMap);
		return retrieveI107;
	}
	
	protected  Map readIgnoreI107Users() {
		Map resultWithoutDuplicates = new LinkedHashMap();
		if(ignoreI107UserIds != null) {
			StringTokenizer stringTokenizer = new StringTokenizer(ignoreI107UserIds, ",");
			while (stringTokenizer.hasMoreTokens()) {
				final String token = stringTokenizer.nextToken().trim();
				if(token.length() > 0 )
				resultWithoutDuplicates.put(token, token);
			}
		}
		
		return resultWithoutDuplicates;
	}
	
	public  boolean buildRetrieveI107(String principal, Map ignoreI107UsersMap) {
		final boolean retrieveI107;
		if(ignoreI107UsersMap != null) {
			retrieveI107 = ignoreI107UsersMap.get(principal) == null; 
		} else {
			retrieveI107 = true;
		}
		
		return retrieveI107;
	}
}
