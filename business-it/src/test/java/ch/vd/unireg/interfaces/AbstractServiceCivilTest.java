package ch.vd.unireg.interfaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.interfaces.civil.ServiceCivilRaw;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.EtatCivilList;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.PermisList;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.service.ServiceCivilImpl;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypePermis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AbstractServiceCivilTest extends BusinessItTest {

	protected ServiceCivilService service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final ServiceCivilRaw raw = getBean(ServiceCivilRaw.class, "serviceCivilRcPers");
		final ServiceInfrastructureService infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		service = new ServiceCivilImpl(infraService, raw);
	}

	@Test(timeout = 10000)
	public void testGetIndividu() throws Exception {

		Individu jean = service.getIndividu(333528, date(2007, 12, 31));
		assertNotNull(jean);
		assertEquals("Jean-Eric", jean.getPrenomUsuel());

		jean = service.getIndividu(333528, date(2001, 12, 31));
		assertNotNull(jean);
	}

	@Test(timeout = 10000)
	public void testGetIndividuComplet() throws Exception {

		final Individu individu = service.getIndividu(692185, null, AttributeIndividu.ADRESSES, AttributeIndividu.ORIGINE, AttributeIndividu.NATIONALITES, AttributeIndividu.PARENTS, AttributeIndividu.PERMIS);
		assertNotNull(individu);
		assertEquals("Jean-Marc", individu.getPrenomUsuel());
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

		final List<EtatCivil> ecList = etatsCivils.asList();
		assertNotNull(ecList);
		assertEquals(2, ecList.size());
		assertEtatCivil(date(1982, 2, 18), TypeEtatCivil.CELIBATAIRE, ecList.get(0));
		assertEtatCivil(date(2009, 9, 12), TypeEtatCivil.MARIE, ecList.get(1));

		// On vérifie les adresses
		final Collection<Adresse> adresses = individu.getAdresses();
		assertNotNull(adresses);
		assertEquals(6, adresses.size());

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
		assertEquals(3, principales.size());

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
		assertAdresseCivile(date(2010, 1, 15), date(2014, 6, 15), "Rue Jean-Louis-de-Bons", "1006", "Lausanne", 885742, principale1);
		assertNull(principale1.getLocalisationPrecedente());
		assertLocalisation(LocalisationType.CANTON_VD, MockCommune.YverdonLesBains.getNoOFS(), principale1.getLocalisationSuivante());

		final Adresse principale2 = principales.get(2);
		assertNotNull(principale2);
		assertAdresseCivile(date(2014, 6, 16), null, "Rue Saint-Georges", "1400", "Yverdon-les-Bains", 280093095, principale2);
		assertLocalisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), principale2.getLocalisationPrecedente());
		assertNull(principale2.getLocalisationSuivante());

		// On vérifie les adresses courrier
		assertEquals(3, courriers.size());
		assertAdresseCivile(null, date(2010, 1, 14), "Av. d'Ouchy 24C", "1006", "Lausanne", null, null, courriers.get(0));
		assertAdresseCivile(date(2010, 1, 15), date(2014, 6, 15), "Rue Jean-Louis-de- Bons", "1006", "Lausanne", null, null, courriers.get(1));
		assertAdresseCivile(date(2014, 6, 16), null, "Rue Saint-Georges", "1400", "Yverdon-les-Bains", null, null, courriers.get(2));

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
		assertEquals(ServiceInfrastructureRaw.SIGLE_CANTON_VD, origine0.getSigleCanton());

		final Origine origine1 = originesList.get(1);
		assertNotNull(origine1);
		assertEquals("Yvorne", origine1.getNomLieu());
		assertEquals(ServiceInfrastructureRaw.SIGLE_CANTON_VD, origine1.getSigleCanton());

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
		assertEquals("Théodora", conjoint.getPrenomUsuel());
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
		assertEquals("Amélie", conjoint.getPrenomUsuel());
		assertEquals(845875, conjoint.getNoTechnique());

		//Séparé
		conjoint = service.getConjoint(jeanMarc.getNoTechnique(), date(2008, 6, 28));
		assertNotNull(conjoint);
		assertEquals("Amélie", conjoint.getPrenomUsuel());
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
		assertEquals(date(2013, 12, 20), permis0.getDateDebut());
		assertEquals(date(2015, 2, 28), permis0.getDateFin());
		assertEquals(TypePermis.SEJOUR, permis0.getTypePermis());
	}
}
