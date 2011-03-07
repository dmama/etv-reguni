package ch.vd.uniregctb.evenement.civil.interne.fin.permis;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.type.TypePermis;

import static org.junit.Assert.assertTrue;

public class FinPermisHandlerTest extends AbstractEvenementHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(FinPermisHandlerTest.class);

	private static final long NUMERO_INDIVIDU = 97136; // Roberto
	private static final long NUMERO_INDIVIDU_2 = 238947; // Rosa
	private static final long NUMERO_INDIVIDU_PERMIS_L = 89123; // Hélène
	private static final RegDate DATE_OBTENTION_NATIONALITE = RegDate.get(2007, 6, 1);
	private static final RegDate DATE_FIN_PERMIS = RegDate.get(2008, 10, 1);

	/** Le fichier de données de test. */
	private static final String DB_UNIT_DATA_FILE = "FinPermisHandlerTest.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil() {

			@Override
			protected void init() {
				super.init();

				RegDate dateNaissanceRoberto = RegDate.get(1961, 3, 12);
				MockIndividu roberto = addIndividu(NUMERO_INDIVIDU, dateNaissanceRoberto, "Martin", "Roberto", true);
				addDefaultAdressesTo(roberto);
				addOrigine(roberto, MockPays.Espagne, null, RegDate.get(1976, 1, 16));
				addNationalite(roberto, MockPays.Espagne, dateNaissanceRoberto, null, 0);
				addNationalite(roberto, MockPays.Suisse, DATE_OBTENTION_NATIONALITE, null, 1);
				addPermis(roberto, TypePermis.COURTE_DUREE, RegDate.get(2005, 3, 12), RegDate.get(2007, 5, 31), 0, false);
				addPermis(roberto, TypePermis.ETABLISSEMENT, DATE_OBTENTION_NATIONALITE, DATE_FIN_PERMIS, 1, false);

				RegDate dateNaissanceRosa = RegDate.get(1980, 5, 30);
				MockIndividu rosa = addIndividu(NUMERO_INDIVIDU_2, dateNaissanceRosa, "Rosa", "Martinez", false);
				addDefaultAdressesTo(rosa);
				addOrigine(rosa, MockPays.Espagne, null, dateNaissanceRosa);
				addNationalite(rosa, MockPays.Espagne, dateNaissanceRosa, null, 0);
				addPermis(rosa, TypePermis.COURTE_DUREE, RegDate.get(2003, 10, 25), null, 0, false);
				addPermis(rosa, TypePermis.ETABLISSEMENT, DATE_OBTENTION_NATIONALITE, DATE_FIN_PERMIS, 1, false);
			}

		});
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	public void testHandlePermisCNationaliteSuisse() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de fin de permis C d'un individu ayant obtenu la nationalité suisse.");

		Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU, DATE_FIN_PERMIS .year());
		FinPermisAdapter finPermis = createValidFinPermisC(individu, DATE_FIN_PERMIS );

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		finPermis.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de la séparation.", erreurs);

		finPermis.validate(erreurs, warnings);
		assertTrue("La fin de permis C devrait être ignorée", erreurs.size() == 0);

		finPermis.handle(warnings);
	}

	@Test
	public void testHandlePermisCSansNationaliteSuisse() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de fin de permis C d'un individu n'ayant pas obtenu la nationalité suisse.");

		Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU_2, DATE_FIN_PERMIS .year());
		FinPermisAdapter finPermis = createValidFinPermisC(individu, DATE_FIN_PERMIS );

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		finPermis.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de la séparation.", erreurs);

		finPermis.validate(erreurs, warnings);
		assertTrue("La fin de permis C devrait passer en traitement manuel", erreurs.size() == 1);

		finPermis.handle(warnings);
	}

	@Test
	public void testHandleAutrePermis() {

		LOGGER.debug("Test de traitement d'un événement de fin de permis autre que C.");

		Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU_PERMIS_L, DATE_FIN_PERMIS .year());
		FinPermisAdapter finPermis = createValidFinPermisNonC(individu, DATE_FIN_PERMIS );

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		finPermis.checkCompleteness(erreurs, warnings);
		assertEmpty("Une erreur est survenue lors du checkCompleteness de la séparation.", erreurs);

		finPermis.validate(erreurs, warnings);
		assertTrue("La fin de permis non C devrait être ignorée", erreurs.size() == 0);

		finPermis.handle(warnings);

	}

	private FinPermisAdapter createValidFinPermisC(Individu individu, RegDate dateFinNationalite) {
		return new FinPermisAdapter(individu, null, dateFinNationalite, 5652, TypePermis.ETABLISSEMENT, context);
	}

	private FinPermisAdapter createValidFinPermisNonC(Individu individu, RegDate dateFinNationalite) {
		return new FinPermisAdapter(individu, null, dateFinNationalite, 5652, TypePermis.COURTE_DUREE, context);
	}
}
