package ch.vd.uniregctb.interfaces;

import java.util.Collection;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public abstract class AbstractServiceCivilTest extends BusinessItTest {

	protected ServiceCivilService service;

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetIndividu() throws Exception {

		Individu jean = service.getIndividu(333528, date(2007, 12, 31));
		assertNotNull(jean);
		assertEquals("Jean-Eric", jean.getPrenom());

		jean = service.getIndividu(333528, date(2001, 12, 31));
		assertNotNull(jean);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetConjoint() throws Exception {

		Individu jean = service.getIndividu(333528, date(2006, 12, 31));
		assertNotNull(jean);

		Individu sara = service.getConjoint(jean.getNoTechnique(), date(2007, 1, 1));
		assertNotNull(sara);

		assertEquals("Sara", sara.getPrenom());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
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
	@Transactional(rollbackFor = Throwable.class)
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
	@Transactional(rollbackFor = Throwable.class)
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
