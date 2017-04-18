package ch.vd.uniregctb.evenement.civil.interne;

import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.tiers.MockTiersDAO;
import ch.vd.uniregctb.tiers.TiersDAO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EvenementCivilInterneCompositeTest extends WithoutSpringTest {

	private EvenementCivilInterne[] mockEvenementsComposants;
	private EvenementCivilEch mockEvenementCivilEch;
	private EvenementCivilContext mockEvenementCivilContext;
	private EvenementCivilOptions mockEvenementCivilOptions;
	private EvenementCivilErreurCollector mockEvtCivErrCol;
	private EvenementCivilWarningCollector mockEvtCivWarnCol;

	@Before
	public void init() throws EvenementCivilException {

		mockEvenementsComposants = new EvenementCivilInterne[] {
				new EvenementCivilInterneMock(),
				new EvenementCivilInterneMock(),
				new EvenementCivilInterneMock()
		};
		mockEvenementCivilEch = new EvenementCivilEch() {
			@NotNull
			@Override
			public RegDate getDateEvenement() {
				return RegDate.get(2011, 1, 1);
			}

			@Override
			public Long getId() {
				return 1L;
			}

			@Override
			public Long getNumeroIndividu() {
				return 1234567L;
			}
		};
		mockEvenementCivilContext = new EvenementCivilContext(null, null, null) {
			private MockTiersDAO mockTiersDAO = new MockTiersDAO();
			@Override
			public TiersDAO getTiersDAO() {
				return mockTiersDAO;
			}
		};
		mockEvenementCivilOptions = new EvenementCivilOptions(false);
		mockEvtCivErrCol = new EvenementCivilErreurCollector() {

			@Override
			public void addErreur(Exception e) {

			}

			@Override
			public void addErreur(String msg) {

			}

			@Override
			public boolean hasErreurs() {
				return false;
			}
		};
		mockEvtCivWarnCol = new EvenementCivilWarningCollector() {

			@Override
			public void addWarning(String msg) {

			}

			@Override
			public boolean hasWarnings() {
				return false;
			}
		};

	}

	@Test
	public void testValidate() throws Exception {

		// Scénario:
		//
		// - On s'attend à ce que toutes les méthodes validateXXX() des événements composants soient appelées lors de
		//   l'appel de validate() sur le composite
		final boolean[] flags =  new boolean[6];

		mockEvenementsComposants = new EvenementCivilInterne[] {
				new EvenementCivilInterneMock() {
					@Override
					protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						flags[0] = true;
					}

					@Override
					protected void validateCommon(EvenementCivilErreurCollector erreurs) {
						flags[1] = true;
					}
				},
				new EvenementCivilInterneMock(){
					@Override
					protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						flags[2] = true;
					}

					@Override
					protected void validateCommon(EvenementCivilErreurCollector erreurs) {
						flags[3] = true;
					}

				},
				new EvenementCivilInterneMock(){
					@Override
					protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						flags[4] = true;
					}

					@Override
					protected void validateCommon(EvenementCivilErreurCollector erreurs) {
						flags[5] = true;
					}
				}
		};

		// Création de l'instance à tester
		EvenementCivilInterne evtComposite = new EvenementCivilInterneComposite(
				mockEvenementCivilEch, mockEvenementCivilContext,mockEvenementCivilOptions,
				mockEvenementsComposants);

		// Appel de la méthode testée
    	evtComposite.validate(mockEvtCivErrCol, mockEvtCivWarnCol);

		// Vérification que le scénario c'est bien déroulé
		for(boolean flag : flags ) {
			if (!flag) {
				fail("Une méthode validate sur un objet composant n'a pas été appelée");
			}
		}

	}

	@Test
	public void testHandle_TousTraites() throws Exception {

		// Scénario:
		//
		// - On s'attend à ce que toutes les méthodes handle() des événements composants soient appelées lors de
		//   l'appel de handle() sur le composite
		//
		// - Dans ce cas, tous les evenements sont traités, le handleStatus du composite doit aussi être traités

		final boolean[] flags =  new boolean[3];

		mockEvenementsComposants = new EvenementCivilInterne[] {
				new EvenementCivilInterneMock() {
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						flags[0] = true;
						return HandleStatus.TRAITE;
					}
				},
				new EvenementCivilInterneMock(){
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						flags[1] = true;
						return HandleStatus.TRAITE;
					}
				},
				new EvenementCivilInterneMock(){
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						flags[2] = true;
						return HandleStatus.TRAITE;
					}
				}
		};

		final HandleStatus hs = testHandle();
		assertEquals(HandleStatus.TRAITE, hs);
		for(boolean flag : flags ) {
			if (!flag) {
				fail("Une méthode handle sur un objet composant n'a pas été appelée");
			}
		}
	}

	@Test
	public void testHandle_TraiteEtRedondant() throws Exception {

		// Scénario:
		//
		// - On s'attend à ce que toutes les méthodes handle() des événements composants soient appelées lors de
		//   l'appel de handle() sur le composite
		//
		// - Dans ce cas, tous un evenement est traité et les autres sont redondants
		//   le handleStatus du composite doit être traité

		final boolean[] flags =  new boolean[3];

		mockEvenementsComposants = new EvenementCivilInterne[] {
				new EvenementCivilInterneMock() {
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						flags[0] = true;
						return HandleStatus.TRAITE;
					}
				},
				new EvenementCivilInterneMock(){
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						flags[1] = true;
						return HandleStatus.REDONDANT;
					}
				},
				new EvenementCivilInterneMock(){
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						flags[2] = true;
						return HandleStatus.REDONDANT;
					}
				}
		};

		HandleStatus hs = testHandle();
		assertEquals(HandleStatus.TRAITE, hs);

		for(boolean flag : flags ) {
			if (!flag) {
				fail("Une méthode handle sur un objet composant n'a pas été appelée");
			}
		}

	}

	@Test
	public void testHandle_TousRedondants() throws Exception {

		// Scénario:
		//
		// - On s'attend à ce que toutes les méthodes handle() des événements composants soient appelées lors de
		//   l'appel de handle() sur le composite
		//
		// - Dans ce cas, tous les evenements sont redondants, le handleStatus du composite doit aussi être redondant

		final boolean[] flags =  new boolean[3];

		mockEvenementsComposants = new EvenementCivilInterne[] {
				new EvenementCivilInterneMock() {
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						flags[0] = true;
						return HandleStatus.REDONDANT;
					}
				},
				new EvenementCivilInterneMock(){
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						flags[1] = true;
						return HandleStatus.REDONDANT;
					}
				},
				new EvenementCivilInterneMock(){
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						flags[2] = true;
						return HandleStatus.REDONDANT;
					}
				}
		};

		HandleStatus hs = testHandle();
		assertEquals(HandleStatus.REDONDANT, hs);

		for(boolean flag : flags ) {
			if (!flag) {
				fail("Une méthode handle sur un objet composant n'a pas été appelée");
			}
		}

	}

	/*
	 * Factorise le code pour les differents tests de la method handle()
	 */
	private HandleStatus testHandle() throws EvenementCivilException {

		// Création de l'instance à tester
		EvenementCivilInterne evtComposite = new EvenementCivilInterneComposite(
				mockEvenementCivilEch, mockEvenementCivilContext,mockEvenementCivilOptions,
				mockEvenementsComposants);

		return evtComposite.handle(mockEvtCivWarnCol);
	}

	@Test
	public void testFillRequiredParts() throws EvenementCivilException {

		// Scénario:
		//
		// - Chaque événements possède une part differente le composite devrait en possède 3

		final Set<AttributeIndividu> attrs = EnumSet.noneOf(AttributeIndividu.class);

		mockEvenementsComposants = new EvenementCivilInterne[] {
				new EvenementCivilInterneMock(){
					@Override
					protected void fillRequiredParts(Set<AttributeIndividu> parts) {
						parts.add(AttributeIndividu.ORIGINE);
					}
				},
				new EvenementCivilInterneMock(){
					@Override
					protected void fillRequiredParts(Set<AttributeIndividu> parts) {
						parts.add(AttributeIndividu.ADRESSES);
					}
				},
				new EvenementCivilInterneMock(){
					@Override
					protected void fillRequiredParts(Set<AttributeIndividu> parts) {
						parts.add(AttributeIndividu.NATIONALITES);
					}
				}
		};

		// Création de l'instance à tester
		EvenementCivilInterne evtComposite = new EvenementCivilInterneComposite(
				mockEvenementCivilEch, mockEvenementCivilContext, mockEvenementCivilOptions,
				mockEvenementsComposants);

		// Appel de la méthode testée
		assertTrue(attrs.isEmpty());
		evtComposite.fillRequiredParts(attrs);
		assertEquals(3, attrs.size());
	}

	@Test
	public void testIsContribuableObligatoirementConnuAvantTraitement_AucunObligatoire() throws EvenementCivilException {
		// Scénario:
		// - Aucun des evenements composants ne doit connaitre le contribuable avant le traitement,
		//   Dans ce cas, le composite non plus.
		final boolean[] flags =  new boolean[3];

		mockEvenementsComposants = new EvenementCivilInterne[] {
				new EvenementCivilInterneMock(){
					@Override
					protected boolean isContribuableObligatoirementConnuAvantTraitement() {
						flags[0] = true;
						return false;
					}
				},
				new EvenementCivilInterneMock(){
					@Override
					protected boolean isContribuableObligatoirementConnuAvantTraitement() {
						flags[1] = true;
						return false;
					}
				},
				new EvenementCivilInterneMock(){
					@Override
					protected boolean isContribuableObligatoirementConnuAvantTraitement() {
						flags[2] = true;
						return false;
					}
				}
		};

		boolean res = testIsContribuableObligatoirementConnuAvantTraitement();
		assertFalse(res);
		for(boolean flag : flags ) {
			if (!flag) {
				fail("Une méthode isContribuableObligatoirementConnuAvantTraitement sur un objet composant n'a pas été appelée");
			}
		}
	}

	@Test
	public void testIsContribuableObligatoirementConnuAvantTraitement_CertainObligatoire() throws EvenementCivilException {
		// Scénario:
		// - Certain evenements composant doivent connaitre le contribuable avant le traitement,
		//   Dans ce cas, le composite aussi.

		mockEvenementsComposants = new EvenementCivilInterne[] {
				new EvenementCivilInterneMock(){
					@Override
					protected boolean isContribuableObligatoirementConnuAvantTraitement() {
						return false;
					}
				},
				new EvenementCivilInterneMock(){
					@Override
					protected boolean isContribuableObligatoirementConnuAvantTraitement() {
						return true;
					}
				},
				new EvenementCivilInterneMock(){
					@Override
					protected boolean isContribuableObligatoirementConnuAvantTraitement() {
						return false;
					}
				}
		};
		boolean res = testIsContribuableObligatoirementConnuAvantTraitement();
		assertTrue(res);
	}

	@Test
	public void testIsContribuableObligatoirementConnuAvantTraitement_TousObligatoire() throws EvenementCivilException {
		// Scénario:
		// - Tous les evenements composants doivent connaitre le contribuable avant le traitement,
		//   Dans ce cas, le composite aussi.

		mockEvenementsComposants = new EvenementCivilInterne[] {
				new EvenementCivilInterneMock(){
					@Override
					protected boolean isContribuableObligatoirementConnuAvantTraitement() {
						return true;
					}
				},
				new EvenementCivilInterneMock(){
					@Override
					protected boolean isContribuableObligatoirementConnuAvantTraitement() {
						return true;
					}
				},
				new EvenementCivilInterneMock(){
					@Override
					protected boolean isContribuableObligatoirementConnuAvantTraitement() {
						return true;
					}
				}
		};
		boolean res = testIsContribuableObligatoirementConnuAvantTraitement();
		assertTrue(res);

	}

	private boolean testIsContribuableObligatoirementConnuAvantTraitement() throws EvenementCivilException {

		// Création de l'instance à tester
		EvenementCivilInterne evtComposite = new EvenementCivilInterneComposite(
				mockEvenementCivilEch, mockEvenementCivilContext,mockEvenementCivilOptions,
				mockEvenementsComposants);

		return evtComposite.isContribuableObligatoirementConnuAvantTraitement();
	}

}

class EvenementCivilInterneMock extends EvenementCivilInterne {

	protected EvenementCivilInterneMock() {
		//noinspection NullableProblems
		super(null, null, null, null, null);
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		return HandleStatus.TRAITE;
	}

	@Override
	protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {}
}