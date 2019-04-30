package ch.vd.unireg.supergra;

import java.util.List;
import java.util.Set;

import org.hibernate.SQLQuery;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WebTestSpring3;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.OriginePersonnePhysique;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.type.CategorieEtranger;
import ch.vd.unireg.type.CategorieIdentifiant;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TarifImpotSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings({"JavaDoc"})
public class SuperGraManagerTest extends WebTestSpring3 {

	private SuperGraManager manager;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.manager = getBean(SuperGraManager.class, "superGraManager");
	}

	@Test
	public void testTransformePp2Mc() throws Exception {

		final long noInd = 123456;
		final Long idPrincipal = 12300001L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1966,3,12), "Paul", "Trohion", Sexe.MASCULIN);
			}
		});

		// création de la personne physique à transformer en ménage
		final Long id = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Paul", "Trohion", date(1965, 3, 12), Sexe.MASCULIN);
			pp.setNomNaissance("Troyas");
			pp.setAncienNumeroSourcier(4444L);
			pp.setNumeroAssureSocial("WWWW");
			pp.setNumeroOfsNationalite(333);
			pp.setOrigine(new OriginePersonnePhysique(MockCommune.Neuchatel.getNomOfficiel(), MockCommune.Neuchatel.getSigleCanton()));
			pp.setCategorieEtranger(CategorieEtranger._12_FONCT_INTER_SANS_IMMUNITE);
			pp.setDateDebutValiditeAutorisation(RegDate.get(2000, 1, 1));
			pp.setDateDeces(RegDate.get());
			pp.setMajoriteTraitee(true);
			addSituation(pp, date(1976, 1, 12), null, 0);
			addIdentificationPersonne(pp, CategorieIdentifiant.CH_ZAR_RCE, "WERT");
			return pp.getNumero();
		});

		// création du futur principal
		doInNewTransactionAndSession(status -> {
			addHabitant(idPrincipal, noInd);
			return null;
		});

		// transformation en ménage-commun
		manager.transformPp2Mc(id, date(1976,1,12), null, idPrincipal, null);

		// on vérifie qu'on a bien un ménage
		doInNewTransactionAndSession(status -> {
			final MenageCommun mc = hibernateTemplate.get(MenageCommun.class, id);
			assertNotNull(mc);

			final Set<RapportEntreTiers> rapports = mc.getRapportsObjet();
			assertNotNull(rapports);
			assertEquals(1, rapports.size());

			final RapportEntreTiers rapport0 = rapports.iterator().next();
			assertInstanceOf(AppartenanceMenage.class, rapport0);
			final AppartenanceMenage appart0 = (AppartenanceMenage) rapport0;

			final PersonnePhysique principal = hibernateTemplate.get(PersonnePhysique.class, appart0.getSujetId());
			assertNotNull(principal);
			assertEquals(idPrincipal, principal.getNumero());

			// [SIFISC-7972] on vérifie que les données spécifiques aux PP ont bien été annulées dans la base
			hibernateTemplate.execute(session -> {
				final SQLQuery query = session.createSQLQuery("select NUMERO_INDIVIDU, ANCIEN_NUMERO_SOURCIER, NH_NUMERO_ASSURE_SOCIAL, NH_NOM, NH_PRENOM, NH_DATE_NAISSANCE, NH_SEXE, " +
						                                              "NH_NO_OFS_NATIONALITE, NH_CAT_ETRANGER, " +
						                                              "NH_DATE_DEBUT_VALID_AUTORIS, DATE_DECES, MAJORITE_TRAITEE, NH_LIBELLE_ORIGINE, NH_CANTON_ORIGINE, NH_NOM_NAISSANCE from TIERS where NUMERO = ?");
				query.setParameter(0, id);
				final List list = query.list();
				assertNotNull(list);
				assertEquals(1, list.size());

				final Object line[] = (Object[]) list.get(0);
				for (int index = 0; index < line.length; ++index) {
					final Object o = line[index];
					assertNull("Index " + index, o);
				}
				return null;
			});
			return null;
		});
	}

	@Test
	public void testTransformeMc2Pp() throws Exception {

		final long noInd = 123456;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1966,3,12), "Paul", "Trohion", Sexe.MASCULIN);
			}
		});

		// création du ménage commun à transformer en personne physique
		final Long id = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Paul", "Trohion", date(1965, 3, 12), Sexe.MASCULIN);
			final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pp, null, date(1976, 1, 12), null);
			final MenageCommun menage = ensemble.getMenage();
			addSituation(menage, date(1976, 1, 12), null, 0, TarifImpotSource.NORMAL);
			return menage.getNumero();
		});

		// transformation en ménage-commun
		manager.transformMc2Pp(id, noInd);

		// on vérifie qu'on a bien une personne physique
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, id);
			assertNotNull(pp);
			assertEmpty(pp.getRapportsSujet());
			assertEmpty(pp.getRapportsObjet());
			return null;
		});
	}
}
