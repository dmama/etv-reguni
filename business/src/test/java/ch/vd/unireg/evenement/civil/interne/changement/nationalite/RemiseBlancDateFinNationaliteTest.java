package ch.vd.unireg.evenement.civil.interne.changement.nationalite;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.unireg.evenement.civil.interne.MessageCollector;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.type.TypePermis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RemiseBlancDateFinNationaliteTest extends AbstractEvenementCivilInterneTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(RemiseBlancDateFinNationaliteTest.class);

	private static final long NUMERO_INDIVIDU = 93256;

	/** Le fichier de données de test. */
	private static final String DB_UNIT_DATA_FILE = "RemiseBlancDateFinNationaliteTest.xml";

	private static final RegDate DATE_FIN_NATIONALITE = RegDate.get(2008, 12, 1);

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockIndividuConnector(){

			@Override
			protected void init() {
				final MockIndividu marine = addIndividu(34567, RegDate.get(1964, 4, 8), "DELACROIX", "Marine", false);
				addDefaultAdressesTo(marine);
				addNationalite(marine, MockPays.Suisse, RegDate.get(1973, 8, 20), DATE_FIN_NATIONALITE);
				addPermis(marine, TypePermis.ETABLISSEMENT, RegDate.get(1973, 8, 20), null, false);
				
				addIndividu(NUMERO_INDIVIDU, RegDate.get(1965, 8, 12), "Mariano", "Luis", true);
			}

		});
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleNationaliteSuisse() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de mise à blanc de la date de fin de nationalité suisse.");

		Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU, date(2008, 12, 31));
		RemiseBlancDateFinNationalite remiseBlancFinNationalite = createRemiseBlancDateFinNationaliteSuisse(individu, DATE_FIN_NATIONALITE);

		final MessageCollector collector = buildMessageCollector();
		remiseBlancFinNationalite.validate(collector, collector);
		assertTrue("La mise à blanc de la date de fin de nationalité suisse devrait être traitée manuellement", collector.getErreurs().size() == 1);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleNationaliteNonSuisse() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de mise à blanc de la date de fin de nationalité non suisse.");

		Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU, date(2008, 12, 31));
		RemiseBlancDateFinNationalite remiseBlancFinNationalite = createRemiseBlancDateFinNationaliteNonSuisse(individu, DATE_FIN_NATIONALITE);

		final MessageCollector collector = buildMessageCollector();
		remiseBlancFinNationalite.validate(collector, collector);
		assertFalse("La mise à blanc de la date de fin de nationalité non suisse devrait être ignorée", collector.hasErreurs());
	}

	private RemiseBlancDateFinNationalite createRemiseBlancDateFinNationaliteSuisse(Individu individu, RegDate date) {
		return new RemiseBlancDateFinNationaliteSuisse(individu, null, date, 5652, context);
	}

	private RemiseBlancDateFinNationalite createRemiseBlancDateFinNationaliteNonSuisse(Individu individu, RegDate date) {
		return new RemiseBlancDateFinNationaliteNonSuisse(individu, null, date, 5652, context);
	}
}
