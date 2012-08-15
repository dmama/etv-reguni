package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividuImpl;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class RelationsComparisonStrategyTest extends AbstractIndividuComparisonStrategyTest {

	private RelationsComparisonStrategy strategy;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		strategy = new RelationsComparisonStrategy();
	}

	private static interface RelationBuilder {
		void buildRelations(MockIndividu individu);
	}

	private void setupCivil(final long noIndividu, final long idEvt1, final RelationBuilder b1, final long idEvt2, final RelationBuilder b2) {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Pion", "Tartan", true);
				if (b1 != null) {
					b1.buildRelations(individu);
				}
				addIndividuFromEvent(idEvt1, individu, RegDate.get(), TypeEvenementCivilEch.ARRIVEE);

				final MockIndividu individuCorrige = createIndividu(noIndividu, null, "Long", "Zébu", true);
				if (b2 != null) {
					b2.buildRelations(individuCorrige);
				}
				addIndividuFromEvent(idEvt2, individuCorrige, RegDate.get(), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, idEvt1);
			}
		});
	}

	private static RelationVersIndividu buildRelation(DateRange range, long noAutreIndividu) {
		return new RelationVersIndividuImpl(noAutreIndividu, range.getDateDebut(), range.getDateFin());
	}

	private static void addRelationVersParent(MockIndividu individu, DateRange range, long noParent) {
		individu.getParents().add(buildRelation(range, noParent));
	}

	private static void addRelationVersEnfant(MockIndividu individu, DateRange range, long noEnfant) {
		individu.getEnfants().add(buildRelation(range, noEnfant));
	}

	private static void addRelationVersConjoint(MockIndividu individu, DateRange range, long noConjoint) {
		individu.getConjoints().add(buildRelation(range, noConjoint));
	}

	@Test
	public void testSansRelations() throws Exception {

		final long noIndividu = 6374237L;
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, null, idEvt2, null);
		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test
	public void testAvecMemesEnfants() throws Exception {

		final long noIndividu = 6374237L;
		final long noEnfant1 = 43678454L;
		final long noEnfant2 = 5647347L;
		final DateRange rangeEnfant1 = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final DateRange rangeEnfant2 = new DateRangeHelper.Range(date(2001, 1, 3), date(2001, 1, 6));
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersEnfant(individu, rangeEnfant1, noEnfant1);
				           addRelationVersEnfant(individu, rangeEnfant2, noEnfant2);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersEnfant(individu, rangeEnfant1, noEnfant1);
				           addRelationVersEnfant(individu, rangeEnfant2, noEnfant2);
			           }
		           }
		);

		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test
	public void testAvecMemesEnfantsMelanges() throws Exception {

		final long noIndividu = 6374237L;
		final long noEnfant1 = 43678454L;
		final long noEnfant2 = 5647347L;
		final DateRange rangeEnfant1 = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final DateRange rangeEnfant2 = new DateRangeHelper.Range(date(2001, 1, 3), date(2001, 1, 6));
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersEnfant(individu, rangeEnfant1, noEnfant1);
				           addRelationVersEnfant(individu, rangeEnfant2, noEnfant2);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersEnfant(individu, rangeEnfant2, noEnfant2);
				           addRelationVersEnfant(individu, rangeEnfant1, noEnfant1);
			           }
		           }
		);

		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test
	public void testAvecMemesEnfantsEtMemesDatesMelanges() throws Exception {

		final long noIndividu = 6374237L;
		final long noEnfant1 = 43678454L;
		final long noEnfant2 = 5647347L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersEnfant(individu, range, noEnfant1);
				           addRelationVersEnfant(individu, range, noEnfant2);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersEnfant(individu, range, noEnfant2);
				           addRelationVersEnfant(individu, range, noEnfant1);
			           }
		           }
		);

		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test
	public void testAvecMemesParents() throws Exception {

		final long noIndividu = 6374237L;
		final long noRelation1 = 43678454L;
		final long noRelation2 = 5647347L;
		final DateRange rangeRelation1 = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final DateRange rangeRelation2 = new DateRangeHelper.Range(date(2001, 1, 3), date(2001, 1, 6));
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, rangeRelation1, noRelation1);
				           addRelationVersParent(individu, rangeRelation2, noRelation2);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, rangeRelation1, noRelation1);
				           addRelationVersParent(individu, rangeRelation2, noRelation2);
			           }
		           }
		);

		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test
	public void testAvecMemesParentsMelanges() throws Exception {

		final long noIndividu = 6374237L;
		final long noRelation1 = 43678454L;
		final long noRelation2 = 5647347L;
		final DateRange rangeRelation1 = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final DateRange rangeRelation2 = new DateRangeHelper.Range(date(2001, 1, 3), date(2001, 1, 6));
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, rangeRelation1, noRelation1);
				           addRelationVersParent(individu, rangeRelation2, noRelation2);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, rangeRelation2, noRelation2);
				           addRelationVersParent(individu, rangeRelation1, noRelation1);
			           }
		           }
		);

		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test
	public void testAvecMemesParentsEtMemesDatesMelanges() throws Exception {

		final long noIndividu = 6374237L;
		final long noRelation1 = 43678454L;
		final long noRelation2 = 5647347L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, range, noRelation1);
				           addRelationVersParent(individu, range, noRelation2);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, range, noRelation2);
				           addRelationVersParent(individu, range, noRelation1);
			           }
		           }
		);

		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test
	public void testAvecMemesConjoints() throws Exception {

		final long noIndividu = 6374237L;
		final long noRelation1 = 43678454L;
		final long noRelation2 = 5647347L;
		final DateRange rangeRelation1 = new DateRangeHelper.Range(date(2000, 1, 1), date(2000, 5, 31));
		final DateRange rangeRelation2 = new DateRangeHelper.Range(date(2001, 1, 3), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, rangeRelation1, noRelation1);
				           addRelationVersConjoint(individu, rangeRelation2, noRelation2);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, rangeRelation1, noRelation1);
				           addRelationVersConjoint(individu, rangeRelation2, noRelation2);
			           }
		           }
		);

		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test
	public void testAvecMemesConjointsMelanges() throws Exception {

		final long noIndividu = 6374237L;
		final long noRelation1 = 43678454L;
		final long noRelation2 = 5647347L;
		final DateRange rangeRelation1 = new DateRangeHelper.Range(date(2000, 1, 1), date(2000, 5, 31));
		final DateRange rangeRelation2 = new DateRangeHelper.Range(date(2001, 1, 3), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, rangeRelation1, noRelation1);
				           addRelationVersConjoint(individu, rangeRelation2, noRelation2);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, rangeRelation2, noRelation2);
				           addRelationVersConjoint(individu, rangeRelation1, noRelation1);
			           }
		           }
		);

		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test
	public void testAvecMemesConjointsEtMemesDatesMelanges() throws Exception {

		final long noIndividu = 6374237L;
		final long noRelation1 = 43678454L;
		final long noRelation2 = 5647347L;
		final DateRange range = new DateRangeHelper.Range(date(2001, 1, 3), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, range, noRelation1);
				           addRelationVersConjoint(individu, range, noRelation2);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, range, noRelation2);
				           addRelationVersConjoint(individu, range, noRelation1);
			           }
		           }
		);

		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test
	public void testApparitionEnfant() throws Exception {
		final long noIndividu = 6374237L;
		final long noEnfant = 43678454L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, null, idEvt2, new RelationBuilder() {
			@Override
			public void buildRelations(MockIndividu individu) {
				addRelationVersEnfant(individu, range, noEnfant);
			}
		});
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations");
	}

	@Test
	public void testApparitionParent() throws Exception {
		final long noIndividu = 6374237L;
		final long noParent = 43678454L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, null, idEvt2, new RelationBuilder() {
			@Override
			public void buildRelations(MockIndividu individu) {
				addRelationVersParent(individu, range, noParent);
			}
		});
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations");
	}

	@Test
	public void testApparitionConjoint() throws Exception {
		final long noIndividu = 6374237L;
		final long noConjoint = 43678454L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, null, idEvt2, new RelationBuilder() {
			@Override
			public void buildRelations(MockIndividu individu) {
				addRelationVersConjoint(individu, range, noConjoint);
			}
		});
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations");
	}

	@Test
	public void testDisparitionEnfant() throws Exception {
		final long noIndividu = 6374237L;
		final long noEnfant = 43678454L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			@Override
			public void buildRelations(MockIndividu individu) {
				addRelationVersEnfant(individu, range, noEnfant);
			}
		}, idEvt2, null);
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations");
	}

	@Test
	public void testDisparitionParent() throws Exception {
		final long noIndividu = 6374237L;
		final long noParent = 43678454L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			@Override
			public void buildRelations(MockIndividu individu) {
				addRelationVersParent(individu, range, noParent);
			}
		}, idEvt2, null);
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations");
	}

	@Test
	public void testDisparitionConjoint() throws Exception {
		final long noIndividu = 6374237L;
		final long noConjoint = 43678454L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			@Override
			public void buildRelations(MockIndividu individu) {
				addRelationVersConjoint(individu, range, noConjoint);
			}
		}, idEvt2, null);
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations");
	}

	@Test
	public void testChangementEnfant() throws Exception {
		final long noIndividu = 6374237L;
		final long noEnfant1 = 43678454L;
		final long noEnfant2 = 34674367L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersEnfant(individu, range, noEnfant1);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersEnfant(individu, range, noEnfant2);
			           }
		           }
		);
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations");
	}

	@Test
	public void testChangementParent() throws Exception {
		final long noIndividu = 6374237L;
		final long noParent1 = 43678454L;
		final long noParent2 = 34674367L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, range, noParent1);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, range, noParent2);
			           }
		           });
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations");
	}

	@Test
	public void testChangementConjoint() throws Exception {
		final long noIndividu = 6374237L;
		final long noConjoint1 = 43678454L;
		final long noConjoint2 = 34674367L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, range, noConjoint1);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, range, noConjoint2);
			           }
		           }
		);
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations");
	}

	@Test
	public void testChangementDateDebutEnfant() throws Exception {
		final long noIndividu = 6374237L;
		final long noEnfant = 43678454L;
		final DateRange range1 = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final DateRange range2 = new DateRangeHelper.Range(date(2000, 1, 3), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersEnfant(individu, range1, noEnfant);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersEnfant(individu, range2, noEnfant);
			           }
		           }
		);
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations");
	}

	@Test
	public void testChangementDateFinEnfant() throws Exception {
		final long noIndividu = 6374237L;
		final long noEnfant = 43678454L;
		final DateRange range1 = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final DateRange range2 = new DateRangeHelper.Range(date(2000, 1, 1), date(2010, 3, 21));
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersEnfant(individu, range1, noEnfant);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersEnfant(individu, range2, noEnfant);
			           }
		           }
		);
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations");
	}

	@Test
	public void testChangementDateDebutParent() throws Exception {
		final long noIndividu = 6374237L;
		final long noParent = 43678454L;
		final DateRange range1 = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final DateRange range2 = new DateRangeHelper.Range(date(2000, 1, 3), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, range1, noParent);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, range2, noParent);
			           }
		           }
		);
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations");
	}

	@Test
	public void testChangementDateFinParent() throws Exception {
		final long noIndividu = 6374237L;
		final long noParent = 43678454L;
		final DateRange range1 = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final DateRange range2 = new DateRangeHelper.Range(date(2000, 1, 1), date(2010, 3, 21));
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, range1, noParent);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, range2, noParent);
			           }
		           }
		);
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations");
	}

	@Test
	public void testChangementDateDebutConjoint() throws Exception {
		final long noIndividu = 6374237L;
		final long noConjoint = 43678454L;
		final DateRange range1 = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final DateRange range2 = new DateRangeHelper.Range(date(2000, 1, 3), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, range1, noConjoint);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, range2, noConjoint);
			           }
		           }
		);
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations");
	}

	@Test
	public void testChangementDateFinConjoint() throws Exception {
		final long noIndividu = 6374237L;
		final long noConjoint = 43678454L;
		final DateRange range1 = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final DateRange range2 = new DateRangeHelper.Range(date(2000, 1, 1), date(2010, 3, 21));
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, range1, noConjoint);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, range2, noConjoint);
			           }
		           }
		);
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations");
	}
}
