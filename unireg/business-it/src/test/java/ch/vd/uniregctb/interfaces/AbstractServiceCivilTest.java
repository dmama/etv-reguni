package ch.vd.uniregctb.interfaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EtatCivilList;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.LocalisationType;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public abstract class AbstractServiceCivilTest extends BusinessItTest {

	protected ServiceCivilService service;

	@Test
	public void testGetIndividu() throws Exception {

		Individu jean = service.getIndividu(333528, date(2007, 12, 31));
		assertNotNull(jean);
		assertEquals("Jean-Eric", jean.getPrenom());

		jean = service.getIndividu(333528, date(2001, 12, 31));
		assertNotNull(jean);
	}

	@Test
	public void testGetIndividuComplet() throws Exception {

		final Individu jean =
				service.getIndividu(333528, null, AttributeIndividu.ADRESSES, AttributeIndividu.ORIGINE, AttributeIndividu.NATIONALITE, AttributeIndividu.ADOPTIONS, AttributeIndividu.ENFANTS,
						AttributeIndividu.PARENTS, AttributeIndividu.PERMIS, AttributeIndividu.TUTELLE);
		assertNotNull(jean);
		assertEquals("Jean-Eric", jean.getPrenom());
		assertEquals("Cuendet", jean.getNom());
		assertEquals("Cuendet", jean.getNomNaissance());
		assertEquals(date(1974, 3, 22), jean.getDateNaissance());
		assertNull(jean.getDateDeces());
		assertEquals("27474184116", jean.getNoAVS11());
		assertEquals("7565492819118", jean.getNouveauNoAVS());
		assertEquals(333528, jean.getNoTechnique());
		assertNull(jean.getNumeroRCE());
		assertNull(jean.getPermis());
		assertNull(jean.getTutelle());

		// On vérifie les états-civils
		final EtatCivilList etatsCivils = jean.getEtatsCivils();
		assertNotNull(etatsCivils);
		assertEquals(2, etatsCivils.size());
		assertEtatCivil(date(1974, 3, 22), date(1997, 10, 9), TypeEtatCivil.CELIBATAIRE, etatsCivils.get(0));
		assertEtatCivil(date(1997, 10, 10), null, TypeEtatCivil.MARIE, etatsCivils.get(1));

		// On vérifie les adresses
		final Collection<Adresse> adresses = jean.getAdresses();
		assertNotNull(adresses);
		assertEquals(4, adresses.size());

		final List<Adresse> principales = new ArrayList<Adresse>();
		final List<Adresse> courriers = new ArrayList<Adresse>();
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
		assertAdresseCivile(date(2004, 8, 15), date(2011, 1, 31), "Route de Saint-Prex", "1168", "Villars-sous-Yens",
				principale0); // on ne teste pas l'egid car l'épuration de données de RCPers retourne une autre valeur (pas forcément plus fausse)
		if (principale0.getLocalisationPrecedente() != null) { // host-interfaces ne connaît pas cette info, on ne teste donc que si elle est renseignée (= RCPers)
			assertLocalisation(LocalisationType.CANTON_VD, 5633, principale0.getLocalisationPrecedente());
		}
		if (principale0.getLocalisationSuivante() != null) { // host-interfaces ne connaît pas cette info, on ne teste donc que si elle est renseignée (= RCPers)
			assertLocalisation(LocalisationType.CANTON_VD, 5498, principale0.getLocalisationSuivante());
		}

		final Adresse principale1 = principales.get(1);
		assertNotNull(principale1);
		assertAdresseCivile(date(2011, 2, 1), null, "Le Pré des Buis", "1315", "La Sarraz", 280057519, 1, principale1);
		if (principale1.getLocalisationPrecedente() != null) { // host-interfaces ne connaît pas cette info, on ne teste donc que si elle est renseignée (= RCPers)
			assertLocalisation(LocalisationType.CANTON_VD, 5652, principale1.getLocalisationPrecedente());
		}
		assertNull(principale1.getLocalisationSuivante());

		// On vérifie les adresses courrier
		assertEquals(2, courriers.size());
		// TODO (msi) en attente de correction du SIREF-1487 : assertAdresseCivile(null, date(2011, 1, 31), "La Tuilière", "1168", "Villars-sous-Yens", null, null, courriers.get(0));
		// TODO (msi) en attente du déploiement de la nouvelle version du XSD en intégration : assertAdresseCivile(date(2011, 2, 1), null, "Le Pré des Buis 1", "1315", "La Sarraz", null, null, courriers.get(1));

		// TODO (msi) quand les relations seront disponibles en intégration final List<RelationVersIndividu> conjoints = jean.getConjoints();
		// TODO (msi) quand les relations seront disponibles en intégration final List<RelationVersIndividu> parents = jean.getParents();
		// TODO (msi) quand les relations seront disponibles en intégration final Collection<RelationVersIndividu> enfants = jean.getEnfants();

		// On vérifie les origines
		final Collection<Origine> origines = jean.getOrigines();
		assertNotNull(origines);
		assertEquals(1, origines.size());

		final Origine origine = origines.iterator().next();
		assertNotNull(origine);
		assertEquals("Sainte-Croix", origine.getNomLieu());

		// On vérifie les nationalités
		final List<Nationalite> nationalites = jean.getNationalites();
		assertNotNull(nationalites);
		assertEquals(1, nationalites.size());

		final Nationalite nationalite = nationalites.get(0);
		assertNotNull(nationalite);
		// RCPers n'expose pas les dates de début/fin sur les nationalités : assertEquals(date(1974, 3, 22), nationalite.getDateDebutValidite());
		// RCPers n'expose pas les dates de début/fin sur les nationalités : assertNull(nationalite.getDateFinValidite());
		assertEquals("CH", nationalite.getPays().getSigleOFS());
	}

	@Test
	public void testGetConjoint() throws Exception {

		Individu jean = service.getIndividu(333528, date(2006, 12, 31));
		assertNotNull(jean);

		Individu sara = service.getConjoint(jean.getNoTechnique(), date(2007, 1, 1));
		assertNotNull(sara);

		assertEquals("Sara", sara.getPrenom());
	}

	@Test
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

	@Test
	public void testGetIndividuConjoint() {
		Individu jeanMarc = service.getIndividu(132720L, date(2006, 12, 31));
		assertNotNull(jeanMarc);
		Individu conjoint = service.getConjoint(jeanMarc.getNoTechnique(), date(2006, 1, 1));
		//Celibataire
		assertNull(conjoint);

		conjoint = service.getConjoint(jeanMarc.getNoTechnique(), date(2007, 6, 24));
		//Marié
		assertNotNull(conjoint);
		assertEquals("Amélie", conjoint.getPrenom());
		assertEquals(845875, conjoint.getNoTechnique());

		conjoint = service.getConjoint(jeanMarc.getNoTechnique(), date(2008, 6, 28));
		//Séparé
		assertNotNull(conjoint);
		assertEquals("Amélie", conjoint.getPrenom());
		assertEquals(845875, conjoint.getNoTechnique());


		conjoint = service.getConjoint(jeanMarc.getNoTechnique(), date(2009, 7, 28));
		//Divorcé
		assertNull(conjoint);
		conjoint = service.getConjoint(jeanMarc.getNoTechnique(), date(2010, 3, 28));

		//Remarié
		assertNotNull(conjoint);
		assertEquals(387602, conjoint.getNoTechnique());

	}

	@Test
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
}
