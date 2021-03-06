package ch.vd.unireg.evenement.civil.interne.fin.permis;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.unireg.evenement.civil.interne.MessageCollector;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.type.TypePermis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FinPermis2Test extends AbstractEvenementCivilInterneTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(FinPermis2Test.class);

	private static final long NUMERO_INDIVIDU = 97136; // Roberto
	private static final long NUMERO_INDIVIDU_2 = 238947; // Rosa
	private static final long NUMERO_INDIVIDU_PERMIS_L = 89123; // Hélène
	private static final RegDate DATE_OBTENTION_NATIONALITE = RegDate.get(2007, 6, 1);
	private static final RegDate DATE_FIN_PERMIS = RegDate.get(2008, 10, 1);

	/** Le fichier de données de test. */
	private static final String DB_UNIT_DATA_FILE = "FinPermis2Test.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockIndividuConnector() {

			@Override
			protected void init() {
				super.init();

				RegDate dateNaissanceRoberto = RegDate.get(1961, 3, 12);
				MockIndividu roberto = addIndividu(NUMERO_INDIVIDU, dateNaissanceRoberto, "Martin", "Roberto", true);
				addDefaultAdressesTo(roberto);
				addNationalite(roberto, MockPays.Espagne, dateNaissanceRoberto, null);
				addNationalite(roberto, MockPays.Suisse, DATE_OBTENTION_NATIONALITE, null);
				addPermis(roberto, TypePermis.COURTE_DUREE, RegDate.get(2005, 3, 12), RegDate.get(2007, 5, 31), false);
				addPermis(roberto, TypePermis.ETABLISSEMENT, DATE_OBTENTION_NATIONALITE, DATE_FIN_PERMIS, false);

				RegDate dateNaissanceRosa = RegDate.get(1980, 5, 30);
				MockIndividu rosa = addIndividu(NUMERO_INDIVIDU_2, dateNaissanceRosa, "Rosa", "Martinez", false);
				addDefaultAdressesTo(rosa);
				addNationalite(rosa, MockPays.Espagne, dateNaissanceRosa, null);
				addPermis(rosa, TypePermis.COURTE_DUREE, RegDate.get(2003, 10, 25), null, false);
				addPermis(rosa, TypePermis.ETABLISSEMENT, DATE_OBTENTION_NATIONALITE, DATE_FIN_PERMIS, false);
			}

		});
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandlePermisCNationaliteSuisse() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de fin de permis C d'un individu ayant obtenu la nationalité suisse.");

		Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU, DATE_FIN_PERMIS);
		FinPermis finPermis = createValidFinPermisC(individu, DATE_FIN_PERMIS );

		final MessageCollector collector = buildMessageCollector();
		finPermis.validate(collector, collector);
		assertFalse("La fin de permis C devrait être ignorée", collector.hasErreurs());

		finPermis.handle(collector);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandlePermisCSansNationaliteSuisse() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de fin de permis C d'un individu n'ayant pas obtenu la nationalité suisse.");

		Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU_2, DATE_FIN_PERMIS);
		FinPermis finPermis = createValidFinPermisC(individu, DATE_FIN_PERMIS );

		final MessageCollector collector = buildMessageCollector();
		finPermis.validate(collector, collector);
		assertTrue("La fin de permis C devrait passer en traitement manuel", collector.getErreurs().size() == 1);

		finPermis.handle(collector);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleAutrePermis() throws EvenementCivilException {

		LOGGER.debug("Test de traitement d'un événement de fin de permis autre que C.");

		Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU_PERMIS_L, DATE_FIN_PERMIS);
		FinPermis finPermis = createValidFinPermisNonC(individu, DATE_FIN_PERMIS );

		final MessageCollector collector = buildMessageCollector();
		finPermis.validate(collector, collector);
		assertFalse("La fin de permis non C devrait être ignorée", collector.hasErreurs());

		finPermis.handle(collector);

	}

	private FinPermis createValidFinPermisC(Individu individu, RegDate dateFinNationalite) {
		return new FinPermis(individu, null, dateFinNationalite, 5652, TypePermis.ETABLISSEMENT, context);
	}

	private FinPermis createValidFinPermisNonC(Individu individu, RegDate dateFinNationalite) {
		return new FinPermis(individu, null, dateFinNationalite, 5652, TypePermis.COURTE_DUREE, context);
	}
}
