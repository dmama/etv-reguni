package ch.vd.uniregctb.fiscal.test;

import java.rmi.RemoteException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import ch.vd.common.test.EjbUtils;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.uniregctb.fiscal.model.Contribuable;
import ch.vd.uniregctb.fiscal.service.ServiceFiscal;

/**
 * Classe importée du projet host-Interfaces.
 *
 * @author Baba NGOM (xsibnm)
 */
public class ServiceFiscalTestCase3 extends TestCase {

	 private static final Logger LOGGER = Logger.getLogger(ServiceFiscalTestCase3.class);


	private ServiceFiscal serviceFiscal = null;

	@Override
	protected void setUp() throws Exception {
		serviceFiscal = (ServiceFiscal) EjbUtils.createBean(ServiceFiscal.JNDI_NAME);
	}

	@Override
	protected void tearDown() throws Exception {
		serviceFiscal.remove();
	}

	/**
	 * Test de recuperation du code de blocage de remboursement automatique d'un contribuable et test de modification de ce mm
	 * code
	 *
	 * @throws RemoteException
	 * @throws RegistreException
	 */
	public void testRecupEtMajCodeBlocageRembAuto() throws RemoteException, RegistreException {
		LOGGER.info("testRecupEtMajCodeBlocageRembAuto");
		final int idCtb = 97002614;
		final int pfi = 2005;

		Contribuable contribuable = serviceFiscal.getContribuable(idCtb, pfi);

		/* on garde la valeur du code blocage avant la modif */
		boolean codeBlocageRmbAutoInitial = contribuable.getCodeBlocageRmbtAuto();
		contribuable = null;

		/* on met a jour le code en inversant le code blocage */
		boolean codeBlocageRmbAutoModifie = !codeBlocageRmbAutoInitial;
		serviceFiscal.modifierCodeBlocageRmbtAuto(idCtb, codeBlocageRmbAutoModifie,"USR");
		/*
		 * On recupere a nouveau le contribuable et on regarde si son code blocage est bien inversé par rapport au code initial.
		 */
		contribuable = serviceFiscal.getContribuable(idCtb, 2005);
		assertEquals(codeBlocageRmbAutoModifie, contribuable.getCodeBlocageRmbtAuto());
	}

	// /**
	// * @throws RemoteException
	// * @throws RegistreException
	// */
	// public void testRecupSexeContribuable() throws RemoteException, RegistreException {
	//
	// final Integer idCtb = new Integer(10023543);
	// final int pfi = 2005;
	// // HashMap criteresRecherche = new HashMap();
	// // criteresRecherche.put(EnumCritereRechercheContribuable.NO_CONTRIBUABLE, idCtb);
	// // Collection result = serviceFiscal.rechercherContribuables(criteresRecherche, 49);
	// // Iterator resultIT = result.iterator();
	// // while(resultIT.hasNext()){
	// // ResultatRechercheContribuable resultatRechercheContribuable = (ResultatRechercheContribuable)resultIT.next();
	// // log.info("No Contribuable : " + resultatRechercheContribuable.getNoContribuable());
	// // log.info("Nom Contribuable : " + resultatRechercheContribuable.getNomIndividuPrincipal());
	// // log.info("Sexe individu principal : " + resultatRechercheContribuable.isSexeMasculinIndividuPrincipal());
	// // log.info("Sexe individu ¨conjoint : " + resultatRechercheContribuable.isSexeMasculinIndividuConjoint());
	// // }
	//
	//
	// Contribuable contr = serviceFiscal.getContribuable(idCtb.longValue(),pfi,false);
	// if (contr instanceof ContribuableFoyer) {
	// log.info("Contribuable foyer");
	// ContribuableFoyerImpl contribuableFoyer = (ContribuableFoyerImpl) contr;
	// ContribuableIndividuImpl contribuableIndividu = ((ContribuableIndividuImpl)contribuableFoyer.getPrincipal());
	// log.info("No Contribuable :" + contribuableFoyer.getNoContribuable());
	// IndividuImpl individu = (IndividuImpl) contribuableIndividu.getEntiteCivile();
	// log.info("Sexe individu principal : " + individu.isSexeMasculin());
	// assertTrue(individu.isSexeMasculin());
	// } else {
	// log.info("Contribuable Individu");
	// ContribuableIndividu contribuableIndividu = (ContribuableIndividu) contr;
	// Individu individu = (Individu)contribuableIndividu.getEntiteCivile();
	// log.info("No Contribuable : " + contribuableIndividu.getNoContribuable());
	// log.info("Sexe individu principal : " + individu.isSexeMasculin());
	// assertTrue(individu.isSexeMasculin());
	// }
	//
	// }

}