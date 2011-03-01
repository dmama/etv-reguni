package ch.vd.uniregctb.evenement.fin.nationalite;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;

import static org.junit.Assert.assertTrue;

public class FinNationaliteHandlerTest extends AbstractEvenementHandlerTest {
	
	private static final Logger LOGGER = Logger.getLogger(FinNationaliteHandlerTest.class);
	
	private static final long NUMERO_INDIVIDU = 34567;
	
	/** Le fichier de données de test. */
	private static final String DB_UNIT_DATA_FILE = "FinNationaliteHandlerTest.xml";

	private static final RegDate DATE_FIN_NATIONALITE = RegDate.get(2008, 10);
	
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE);
	}
	
	@Test
	public void testHandleNationaliteSuisse() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de fin de nationalité suisse.");

		Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU, 2008);
		FinNationalite finNationalite = createValidFinNationaliteSuisse(individu, DATE_FIN_NATIONALITE);
		
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		
		finNationalite.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de la séparation.", erreurs);
		
		finNationalite.validate(erreurs, warnings);
		assertTrue("La fin de nationalité suisse devrait être traitée manuellement", erreurs.size() == 1);
	}

	@Test
	public void testHandleNationaliteNonSuisse() throws Exception {
		
		LOGGER.debug("Test de traitement d'un événement de fin de nationalité non suisse.");

		Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU, 2008);
		FinNationalite finNationalite = createValidFinNationaliteNonSuisse(individu, DATE_FIN_NATIONALITE);
		
		List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		
		finNationalite.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de la séparation.", erreurs);
		
		finNationalite.validate(erreurs, warnings);
		assertTrue("La fin de nationalité non suisse devrait être ignorée", erreurs.size() == 0);
	}

	private FinNationalite createValidFinNationaliteSuisse(Individu individu, RegDate dateFinNationalite) {
		MockFinNationalite finNationalite = new MockFinNationalite(individu, null, dateFinNationalite, 5652, true);
		finNationalite.setHandler(evenementCivilHandler);
		return finNationalite;
	}	

	private FinNationalite createValidFinNationaliteNonSuisse(Individu individu, RegDate dateFinNationalite) {
		MockFinNationalite finNationalite = new MockFinNationalite(individu, null, dateFinNationalite, 5652, false);
		finNationalite.setHandler(evenementCivilHandler);
		return finNationalite;
	}
}
