package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividuImpl;
import ch.vd.unireg.interfaces.civil.data.TypeRelationVersIndividu;
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

	private interface RelationBuilder {
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
				addIndividuAfterEvent(idEvt1, individu, RegDate.get(), TypeEvenementCivilEch.ARRIVEE);

				final MockIndividu individuCorrige = createIndividu(noIndividu, null, "Long", "Zébu", true);
				if (b2 != null) {
					b2.buildRelations(individuCorrige);
				}
				addIndividuAfterEvent(idEvt2, individuCorrige, RegDate.get(), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, idEvt1);
			}
		});
	}

	private static RelationVersIndividu buildRelation(DateRange range, long noAutreIndividu, TypeRelationVersIndividu type) {
		return new RelationVersIndividuImpl(noAutreIndividu, type, range.getDateDebut(), range.getDateFin());
	}

	private static void addRelationVersParent(MockIndividu individu, DateRange range, long noParent, TypeRelationVersIndividu typeRelation) {
		individu.getParents().add(buildRelation(range, noParent, typeRelation));
	}

	private static void addRelationVersConjoint(MockIndividu individu, DateRange range, long noConjoint, TypeRelationVersIndividu typeRelation) {
		individu.getConjoints().add(buildRelation(range, noConjoint, typeRelation));
	}

	@Test(timeout = 10000L)
	public void testSansRelations() throws Exception {

		final long noIndividu = 6374237L;
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, null, idEvt2, null);
		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test(timeout = 10000L)
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
				           addRelationVersParent(individu, rangeRelation1, noRelation1, TypeRelationVersIndividu.PERE);
				           addRelationVersParent(individu, rangeRelation2, noRelation2, TypeRelationVersIndividu.MERE);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, rangeRelation1, noRelation1, TypeRelationVersIndividu.PERE);
				           addRelationVersParent(individu, rangeRelation2, noRelation2, TypeRelationVersIndividu.MERE);
			           }
		           }
		);

		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test(timeout = 10000L)
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
				           addRelationVersParent(individu, rangeRelation1, noRelation1, TypeRelationVersIndividu.PERE);
				           addRelationVersParent(individu, rangeRelation2, noRelation2, TypeRelationVersIndividu.MERE);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, rangeRelation2, noRelation2, TypeRelationVersIndividu.MERE);
				           addRelationVersParent(individu, rangeRelation1, noRelation1, TypeRelationVersIndividu.PERE);
			           }
		           }
		);

		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test(timeout = 10000L)
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
				           addRelationVersParent(individu, range, noRelation1, TypeRelationVersIndividu.PERE);
				           addRelationVersParent(individu, range, noRelation2, TypeRelationVersIndividu.MERE);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, range, noRelation2, TypeRelationVersIndividu.MERE);
				           addRelationVersParent(individu, range, noRelation1, TypeRelationVersIndividu.PERE);
			           }
		           }
		);

		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test(timeout = 10000L)
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
				           addRelationVersConjoint(individu, rangeRelation1, noRelation1, TypeRelationVersIndividu.CONJOINT);
				           addRelationVersConjoint(individu, rangeRelation2, noRelation2, TypeRelationVersIndividu.CONJOINT);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, rangeRelation1, noRelation1, TypeRelationVersIndividu.CONJOINT);
				           addRelationVersConjoint(individu, rangeRelation2, noRelation2, TypeRelationVersIndividu.CONJOINT);
			           }
		           }
		);

		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test(timeout = 10000L)
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
				           addRelationVersConjoint(individu, rangeRelation1, noRelation1, TypeRelationVersIndividu.CONJOINT);
				           addRelationVersConjoint(individu, rangeRelation2, noRelation2, TypeRelationVersIndividu.CONJOINT);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, rangeRelation2, noRelation2, TypeRelationVersIndividu.CONJOINT);
				           addRelationVersConjoint(individu, rangeRelation1, noRelation1, TypeRelationVersIndividu.CONJOINT);
			           }
		           }
		);

		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test(timeout = 10000L)
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
				           addRelationVersConjoint(individu, range, noRelation1, TypeRelationVersIndividu.CONJOINT);
				           addRelationVersConjoint(individu, range, noRelation2, TypeRelationVersIndividu.CONJOINT);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, range, noRelation2, TypeRelationVersIndividu.CONJOINT);
				           addRelationVersConjoint(individu, range, noRelation1, TypeRelationVersIndividu.CONJOINT);
			           }
		           }
		);

		assertNeutre(strategy, idEvt1, idEvt2);
	}

	@Test(timeout = 10000L)
	public void testApparitionParent() throws Exception {
		final long noIndividu = 6374237L;
		final long noParent = 43678454L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, null, idEvt2, new RelationBuilder() {
			@Override
			public void buildRelations(MockIndividu individu) {
				addRelationVersParent(individu, range, noParent, TypeRelationVersIndividu.PERE);
			}
		});
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations (parents (apparition))");
	}

	@Test(timeout = 10000L)
	public void testApparitionConjoint() throws Exception {
		final long noIndividu = 6374237L;
		final long noConjoint = 43678454L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, null, idEvt2, new RelationBuilder() {
			@Override
			public void buildRelations(MockIndividu individu) {
				addRelationVersConjoint(individu, range, noConjoint, TypeRelationVersIndividu.CONJOINT);
			}
		});
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations (conjoints (apparition))");
	}

	@Test(timeout = 10000L)
	public void testDisparitionParent() throws Exception {
		final long noIndividu = 6374237L;
		final long noParent = 43678454L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			@Override
			public void buildRelations(MockIndividu individu) {
				addRelationVersParent(individu, range, noParent, TypeRelationVersIndividu.MERE);
			}
		}, idEvt2, null);
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations (parents (disparition))");
	}

	@Test(timeout = 10000L)
	public void testDisparitionConjoint() throws Exception {
		final long noIndividu = 6374237L;
		final long noConjoint = 43678454L;
		final DateRange range = new DateRangeHelper.Range(date(2000, 1, 1), null);
		final long idEvt1 = 4367742354L;
		final long idEvt2 = 567437834342L;

		setupCivil(noIndividu, idEvt1, new RelationBuilder() {
			@Override
			public void buildRelations(MockIndividu individu) {
				addRelationVersConjoint(individu, range, noConjoint, TypeRelationVersIndividu.PARTENAIRE_ENREGISTRE);
			}
		}, idEvt2, null);
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations (conjoints (disparition))");
	}

	@Test(timeout = 10000L)
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
				           addRelationVersParent(individu, range, noParent1, TypeRelationVersIndividu.PERE);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, range, noParent2, TypeRelationVersIndividu.PERE);
			           }
		           });
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations (parents)");
	}

	@Test(timeout = 10000L)
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
				           addRelationVersConjoint(individu, range, noConjoint1, TypeRelationVersIndividu.CONJOINT);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, range, noConjoint2, TypeRelationVersIndividu.CONJOINT);
			           }
		           });
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations (conjoints)");
	}

	@Test(timeout = 10000L)
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
				           addRelationVersParent(individu, range1, noParent, TypeRelationVersIndividu.MERE);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, range2, noParent, TypeRelationVersIndividu.MERE);
			           }
		           });
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations (parents (dates))");
	}

	@Test(timeout = 10000L)
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
				           addRelationVersParent(individu, range1, noParent, TypeRelationVersIndividu.PERE);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersParent(individu, range2, noParent, TypeRelationVersIndividu.PERE);
			           }
		           });
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations (parents (dates))");
	}

	@Test(timeout = 10000L)
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
				           addRelationVersConjoint(individu, range1, noConjoint, TypeRelationVersIndividu.PARTENAIRE_ENREGISTRE);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, range2, noConjoint, TypeRelationVersIndividu.PARTENAIRE_ENREGISTRE);
			           }
		           });
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations (conjoints (dates))");
	}

	@Test(timeout = 10000L)
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
				           addRelationVersConjoint(individu, range1, noConjoint, TypeRelationVersIndividu.CONJOINT);
			           }
		           }, idEvt2, new RelationBuilder() {
			           @Override
			           public void buildRelations(MockIndividu individu) {
				           addRelationVersConjoint(individu, range2, noConjoint, TypeRelationVersIndividu.CONJOINT);
			           }
		           });
		assertNonNeutre(strategy, idEvt1, idEvt2, "relations (conjoints (dates))");
	}
}
