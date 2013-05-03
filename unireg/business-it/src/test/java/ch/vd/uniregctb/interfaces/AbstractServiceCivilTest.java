package ch.vd.uniregctb.interfaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.EtatCivilList;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.PermisList;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypePermis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class AbstractServiceCivilTest extends BusinessItTest {

	protected ServiceCivilService service;

	@Test(timeout = 10000)
	public void testGetIndividu() throws Exception {

		Individu jean = service.getIndividu(333528, date(2007, 12, 31));
		assertNotNull(jean);
		assertEquals("Jean-Eric", jean.getPrenom());

		jean = service.getIndividu(333528, date(2001, 12, 31));
		assertNotNull(jean);
	}

	@Test(timeout = 10000)
	public void testGetIndividuComplet() throws Exception {

		final Individu individu =
				service.getIndividu(692185, null, AttributeIndividu.ADRESSES, AttributeIndividu.ORIGINE, AttributeIndividu.NATIONALITES, AttributeIndividu.ADOPTIONS, AttributeIndividu.ENFANTS,
						AttributeIndividu.PARENTS, AttributeIndividu.PERMIS);
		assertNotNull(individu);
		assertEquals("Jean-Marc", individu.getPrenom());
		assertEquals("Delacrétaz", individu.getNom());
		assertEquals("Delacrétaz", individu.getNomNaissance());
		assertEquals(date(1982, 2, 18), individu.getDateNaissance());
		assertNull(individu.getDateDeces());
		assertEquals("28082149114", individu.getNoAVS11());
		assertEquals("7563110255669", individu.getNouveauNoAVS());
		assertEquals(692185, individu.getNoTechnique());
		assertNull(individu.getNumeroRCE());
		assertEmpty(individu.getPermis());

		// On vérifie les états-civils
		final EtatCivilList etatsCivils = individu.getEtatsCivils();
		assertNotNull(etatsCivils);
		assertEquals(2, etatsCivils.size());
		assertEtatCivil(date(1982, 2, 18), date(2009, 9, 11), TypeEtatCivil.CELIBATAIRE, etatsCivils.get(0));
		assertEtatCivil(date(2009, 9, 12), null, TypeEtatCivil.MARIE, etatsCivils.get(1));

		// On vérifie les adresses
		final Collection<Adresse> adresses = individu.getAdresses();
		assertNotNull(adresses);
		assertEquals(4, adresses.size());

		final List<Adresse> principales = new ArrayList<>();
		final List<Adresse> courriers = new ArrayList<>();
		for (Adresse adresse : adresses) {
			if (adresse.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE) {
				principales.add(adresse);
			}
			else if (adresse.getTypeAdresse() == TypeAdresseCivil.COURRIER) {
				courriers.add(adresse);
			}
			else {
				fail();
			}
		}
		Collections.sort(principales, new DateRangeComparator<Adresse>());
		Collections.sort(courriers, new DateRangeComparator<Adresse>());

		// On vérifie les adresses principales
		assertEquals(2, principales.size());

		final Adresse principale0 = principales.get(0);
		assertNotNull(principale0);
		assertEquals(date(2004, 9, 1), principale0.getDateDebut());
		assertEquals(date(2010, 1, 14), principale0.getDateFin());
		assertTrue("Av. d'Ouchy 24C".equals(principale0.getRue()) || "Avenue d'Ouchy".equals(principale0.getRue()));
		assertEquals("1006", principale0.getNumeroPostal());
		assertEquals("Lausanne", principale0.getLocalite());
		assertLocalisation(LocalisationType.HORS_SUISSE, MockPays.PaysInconnu.getNoOFS(), principale0.getLocalisationPrecedente());
		assertNull(principale0.getLocalisationSuivante());

		final Adresse principale1 = principales.get(1);
		assertNotNull(principale1);
		assertAdresseCivile(date(2010, 1, 15), null, "Rue Jean-Louis-de-Bons", "1006", "Lausanne", 885742, principale1);
		assertNull(principale1.getLocalisationPrecedente());
		assertNull(principale1.getLocalisationSuivante());

		// On vérifie les adresses courrier
		assertEquals(2, courriers.size());
		assertAdresseCivile(null, date(2010, 1, 14), "Av. d'Ouchy 24C", "1006", "Lausanne", null, null, courriers.get(0));
		if (courriers.get(1).getNumeroRue() != null) {
			// host-interface retourne simplement le numéro technique de la rue
			assertEquals(Integer.valueOf(30553), courriers.get(1).getNumeroRue());
		}
		else {
			// rcpers retourne toutes les infos résolues, mais pas le numéro technique
			assertAdresseCivile(date(2010, 1, 15), null, "Rue Jean-Louis-de- Bons", "1006", "Lausanne", null, null, courriers.get(1));
		}

		// On vérifie les parents
		final List<RelationVersIndividu> parents = individu.getParents();
		assertEmpty(parents); // cette information n'existe pas dans le registre à l'heure actuelle (et c'est le cas pour beaucoup d'individus, sauf les jeunes générations)

		// On vérifie les divers et variés conjoints (hem hem...)
		final List<RelationVersIndividu> conjoints = individu.getConjoints();
		assertNotNull(conjoints);
		assertEquals(1, conjoints.size());

		final RelationVersIndividu conjoint0 = conjoints.get(0);
		assertNotNull(conjoint0);
		assertEquals(590369, conjoint0.getNumeroAutreIndividu());

		// On vérifie les enfants
		final Collection<RelationVersIndividu> enfants = individu.getEnfants();
		assertNotNull(enfants);
		assertEquals(1, enfants.size());

		final RelationVersIndividu enfant0 = enfants.iterator().next();
		assertNotNull(enfant0);
		assertEquals(1031455, enfant0.getNumeroAutreIndividu());

		// On vérifie les origines
		final Collection<Origine> origines = individu.getOrigines();
		assertNotNull(origines);
		assertEquals(2, origines.size());

		final List<Origine> originesList = new ArrayList<>(origines);
		Collections.sort(originesList, new Comparator<Origine>() {
			@Override
			public int compare(Origine o1, Origine o2) {
				return o1.getNomLieu().compareTo(o2.getNomLieu());
			}
		});

		final Origine origine0 = originesList.get(0);
		assertNotNull(origine0);
		assertEquals("La Praz", origine0.getNomLieu());

		final Origine origine1 = originesList.get(1);
		assertNotNull(origine1);
		assertEquals("Yvorne", origine1.getNomLieu());

		// On vérifie la nationalité
		final Collection<Nationalite> nationalites = individu.getNationalites();
		assertNotNull(nationalites);
		assertEquals(1, nationalites.size());

		final Nationalite nationalite = nationalites.iterator().next();
		assertEquals(date(1982, 2, 18), nationalite.getDateDebut());
		assertNull(nationalite.getDateFin());
		assertEquals("CH", nationalite.getPays().getSigleOFS());
	}

	@Test(timeout = 10000)
	public void testGetConjoint() throws Exception {

		Individu individu = service.getIndividu(692185, null);
		assertNotNull(individu);

		Individu conjoint = service.getConjoint(individu.getNoTechnique(), null);
		assertNotNull(conjoint);
		assertEquals(590369, conjoint.getNoTechnique());
		assertEquals("Théodora", conjoint.getPrenom());
	}

	@Test(timeout = 10000)
	public void testGetNumeroIndividuConjoint() {
		Individu jeanMarc = service.getIndividu(132720L, date(2006, 12, 31));
		assertNotNull(jeanMarc);
		Long numeroAmelie = service.getNumeroIndividuConjoint(jeanMarc.getNoTechnique(), date(2006, 1, 1));
		assertNull(numeroAmelie);

		numeroAmelie = service.getNumeroIndividuConjoint(jeanMarc.getNoTechnique(), date(2008, 5, 27));
		assertNotNull(numeroAmelie);

		numeroAmelie = service.getNumeroIndividuConjoint(jeanMarc.getNoTechnique(), date(2008, 6, 25));
		assertEquals(845875, numeroAmelie.longValue());
	}

	@Test(timeout = 20000)
	public void testGetIndividuConjoint() {
		Individu jeanMarc = service.getIndividu(132720L, date(2006, 12, 31));
		assertNotNull(jeanMarc);

		//Celibataire
		Individu conjoint = service.getConjoint(jeanMarc.getNoTechnique(), date(2006, 1, 1));
		assertNull(conjoint);

		//Marié
		conjoint = service.getConjoint(jeanMarc.getNoTechnique(), date(2007, 6, 24));
		assertNotNull(conjoint);
		assertEquals("Amélie", conjoint.getPrenom());
		assertEquals(845875, conjoint.getNoTechnique());

		//Séparé
		conjoint = service.getConjoint(jeanMarc.getNoTechnique(), date(2008, 6, 28));
		assertNotNull(conjoint);
		assertEquals("Amélie", conjoint.getPrenom());
		assertEquals(845875, conjoint.getNoTechnique());

		//Divorcé
		conjoint = service.getConjoint(jeanMarc.getNoTechnique(), date(2009, 7, 28));
		assertNull(conjoint);

		//Remarié
		conjoint = service.getConjoint(jeanMarc.getNoTechnique(), date(2010, 3, 28));
		assertNotNull(conjoint);
		assertEquals(387602, conjoint.getNoTechnique());
	}

	@Test(timeout = 10000)
	public void testGetAdressesAvecEgidEtEwid() {

		final Individu ind0 = service.getIndividu(1015956, date(2010, 12, 31), AttributeIndividu.ADRESSES);
		assertNotNull(ind0);

		final Collection<Adresse> adresses = ind0.getAdresses();
		assertNotNull(adresses);
		assertEquals(2, adresses.size());

		for (Adresse adresse : adresses) {
			if (adresse.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE) {
				assertEquals(Integer.valueOf(3037134), adresse.getEgid());
				assertEquals(Integer.valueOf(3), adresse.getEwid());
			}
		}
	}

	@Test(timeout = 10000)
	public void testGetIndividuAvecPermis() {

		final Individu ind = service.getIndividu(986204, null, AttributeIndividu.PERMIS);
		assertNotNull(ind);

		final PermisList permis = ind.getPermis();
		assertNotNull(permis);
		assertEquals(1, permis.size());

		final Permis permis0 = permis.get(0);
		assertNotNull(permis0);
		assertNull(permis0.getDateDebut());
		assertNull(permis0.getDateFin());
		assertEquals(TypePermis.SEJOUR, permis0.getTypePermis());
	}
}
