package ch.vd.uniregctb.evenement.civil.interne.fin.nationalite;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;

import static org.junit.Assert.assertTrue;

public class FinNationaliteTest extends AbstractEvenementCivilInterneTest {
	
	private static final Logger LOGGER = Logger.getLogger(FinNationaliteTest.class);
	
	private static final long NUMERO_INDIVIDU = 34567;
	
	/** Le fichier de données de test. */
	private static final String DB_UNIT_DATA_FILE = "FinNationaliteTest.xml";

	private static final RegDate DATE_FIN_NATIONALITE = RegDate.get(2008, 10);
	
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE);
	}
	
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleNationaliteSuisse() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de fin de nationalité suisse.");

		Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU, date(2008, 12, 31));
		FinNationalite finNationalite = createValidFinNationaliteSuisse(individu, DATE_FIN_NATIONALITE);
		
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		
		finNationalite.validate(erreurs, warnings);
		assertTrue("La fin de nationalité suisse devrait être traitée manuellement", erreurs.size() == 1);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleNationaliteNonSuisse() throws Exception {
		
		LOGGER.debug("Test de traitement d'un événement de fin de nationalité non suisse.");

		Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU, date(2008, 12, 31));
		FinNationalite finNationalite = createValidFinNationaliteNonSuisse(individu, DATE_FIN_NATIONALITE);
		
		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		
		finNationalite.validate(erreurs, warnings);
		assertTrue("La fin de nationalité non suisse devrait être ignorée", erreurs.isEmpty());
	}

	private FinNationalite createValidFinNationaliteSuisse(Individu individu, RegDate dateFinNationalite) {
		return new FinNationaliteSuisse(individu, null, dateFinNationalite, 5652, context);
	}	

	private FinNationalite createValidFinNationaliteNonSuisse(Individu individu, RegDate dateFinNationalite) {
		return new FinNationaliteNonSuisse(individu, null, dateFinNationalite, 5652, context);
	}
}
