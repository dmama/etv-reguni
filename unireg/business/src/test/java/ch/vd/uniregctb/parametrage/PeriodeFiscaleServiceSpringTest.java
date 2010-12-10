package ch.vd.uniregctb.parametrage;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.validation.ValidationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class PeriodeFiscaleServiceSpringTest extends BusinessTest {

	private PeriodeFiscaleDAO periodeFiscaleDAO;
	private ValidationService validationService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		periodeFiscaleDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		validationService = getBean(ValidationService.class, "validationService");
	}

	/**
	 * Vérifie que la méthode 'getPeriodeFiscaleByYear' ne déclenche pas l'intercepteur de validation des tiers
	 */
	@Test
	public void testGetPeriodeFiscaleByYearDoesntFlushSession() throws Exception {

		// Crée la période fiscale 2008 dans sa propre transaction pour initialiser la base de données
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PeriodeFiscale periode = new PeriodeFiscale();
				periode.setAnnee(2008);
				periodeFiscaleDAO.save(periode);
				return null;
			}
		});

		// Crée un habitant qui ne valide pas
		PersonnePhysique tiers = new PersonnePhysique(false);
		tiers.setNom("RRR");
		tiers = (PersonnePhysique) periodeFiscaleDAO.getHibernateTemplate().merge(tiers);
		tiers.setNom(null); // le nom est obligatoire
		assertTrue(validationService.validate(tiers).hasErrors());

		// On doit être capable de récupérer la période fiscale sans déclencher la validation du tiers ci-dessus
		final PeriodeFiscale periode = periodeFiscaleDAO.getPeriodeFiscaleByYear(2008);
		assertNotNull(periode);
		assertEquals(Integer.valueOf(2008), periode.getAnnee());
	}
}
