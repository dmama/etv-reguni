package ch.vd.uniregctb.evenement.civil.interne.fin.permis;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.type.TypePermis;

import static org.junit.Assert.assertTrue;

public class FinPermis2Test extends AbstractEvenementCivilInterneTest {

	private static final Logger LOGGER = Logger.getLogger(FinPermis2Test.class);

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

		serviceCivil.setUp(new DefaultMockServiceCivil() {

			@Override
			protected void init() {
				super.init();

				RegDate dateNaissanceRoberto = RegDate.get(1961, 3, 12);
				MockIndividu roberto = addIndividu(NUMERO_INDIVIDU, dateNaissanceRoberto, "Martin", "Roberto", true);
				addDefaultAdressesTo(roberto);
				addOrigine(roberto, MockPays.Espagne.getNomMinuscule());
				addNationalite(roberto, MockPays.Espagne, dateNaissanceRoberto, null);
				addNationalite(roberto, MockPays.Suisse, DATE_OBTENTION_NATIONALITE, null);
				setPermis(roberto, TypePermis.ETABLISSEMENT, DATE_OBTENTION_NATIONALITE, DATE_FIN_PERMIS, false);

				RegDate dateNaissanceRosa = RegDate.get(1980, 5, 30);
				MockIndividu rosa = addIndividu(NUMERO_INDIVIDU_2, dateNaissanceRosa, "Rosa", "Martinez", false);
				addDefaultAdressesTo(rosa);
				addOrigine(rosa, MockPays.Espagne.getNomMinuscule());
				addNationalite(rosa, MockPays.Espagne, dateNaissanceRosa, null);
				setPermis(rosa, TypePermis.ETABLISSEMENT, DATE_OBTENTION_NATIONALITE, DATE_FIN_PERMIS, false);
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

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		finPermis.validate(erreurs, warnings);
		assertTrue("La fin de permis C devrait être ignorée", erreurs.isEmpty());

		finPermis.handle(warnings);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandlePermisCSansNationaliteSuisse() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de fin de permis C d'un individu n'ayant pas obtenu la nationalité suisse.");

		Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU_2, DATE_FIN_PERMIS);
		FinPermis finPermis = createValidFinPermisC(individu, DATE_FIN_PERMIS );

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		finPermis.validate(erreurs, warnings);
		assertTrue("La fin de permis C devrait passer en traitement manuel", erreurs.size() == 1);

		finPermis.handle(warnings);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleAutrePermis() throws EvenementCivilException {

		LOGGER.debug("Test de traitement d'un événement de fin de permis autre que C.");

		Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU_PERMIS_L, DATE_FIN_PERMIS);
		FinPermis finPermis = createValidFinPermisNonC(individu, DATE_FIN_PERMIS );

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		finPermis.validate(erreurs, warnings);
		assertTrue("La fin de permis non C devrait être ignorée", erreurs.isEmpty());

		finPermis.handle(warnings);

	}

	private FinPermis createValidFinPermisC(Individu individu, RegDate dateFinNationalite) {
		return new FinPermis(individu, null, dateFinNationalite, 5652, TypePermis.ETABLISSEMENT, context);
	}

	private FinPermis createValidFinPermisNonC(Individu individu, RegDate dateFinNationalite) {
		return new FinPermis(individu, null, dateFinNationalite, 5652, TypePermis.COURTE_DUREE, context);
	}
}
