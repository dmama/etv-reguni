package ch.vd.uniregctb.parametrage;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.validation.ValidationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class PeriodeFiscaleServiceTest extends BusinessTest {

	private PeriodeFiscaleService periodeFiscaleService;
	private PeriodeFiscaleDAO periodeFiscaleDAO;
	private ValidationService validationService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		periodeFiscaleDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		periodeFiscaleService = getBean(PeriodeFiscaleService.class, "periodeFiscaleService");
		validationService = getBean(ValidationService.class, "validationService");
	}

	/**
	 * Vérifie que la méthode 'getPeriodeFiscaleByYear' ne déclenche pas l'intercepteur de validation des tiers
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPeriodeFiscaleByYearDoesntFlushSession() throws Exception {

		// Crée la période fiscale 2008 dans sa propre transaction pour initialiser la base de données
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PeriodeFiscale periode = newPeriodeFiscale(2008);
				periodeFiscaleDAO.save(periode);
				return null;
			}
		});

		// Crée un habitant qui ne valide pas
		PersonnePhysique tiers = new PersonnePhysique(false);
		tiers.setNom("RRR");
		tiers = periodeFiscaleDAO.getHibernateTemplate().merge(tiers);
		tiers.setNom(null); // le nom est obligatoire
		assertTrue(validationService.validate(tiers).hasErrors());

		// On doit être capable de récupérer la période fiscale sans déclencher la validation du tiers ci-dessus
		final PeriodeFiscale periode = periodeFiscaleDAO.getPeriodeFiscaleByYear(2008);
		assertNotNull(periode);
		assertEquals(Integer.valueOf(2008), periode.getAnnee());
	}

	/**
	 * Tests unitaires pour {@link PeriodeFiscaleServiceImpl#initNouvellePeriodeFiscale()}
	 */
	@Test
	public void testInitNouvellePeriodeFiscale () throws Exception {
		final int anneeDernierePeriode = 2008;
		// Initialisisation de la base
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PeriodeFiscale avantDernierePeriode = newPeriodeFiscale(anneeDernierePeriode -1);
				periodeFiscaleDAO.save(avantDernierePeriode);
				PeriodeFiscale dernierePeriode = newPeriodeFiscale(anneeDernierePeriode);
				dernierePeriode.setAnnee(anneeDernierePeriode);
				periodeFiscaleDAO.save(dernierePeriode);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PeriodeFiscale periodeFiscale = periodeFiscaleService.initNouvellePeriodeFiscale();
				assertEquals(anneeDernierePeriode + 1, periodeFiscale.getAnnee().intValue());
				checkParametrePeriodeFiscale(periodeFiscale);
				return null;
			}
		});
	}

	/**
	 * Verifie que la coherence ds {@link ch.vd.uniregctb.declaration.ParametrePeriodeFiscale} en fonction de leur {@link PeriodeFiscale}.
	 * => les années des termes doivent bien être l'année suivante.
	 */
	private void checkParametrePeriodeFiscale(PeriodeFiscale pf) {
		for (ParametrePeriodeFiscale ppf : pf.getParametrePeriodeFiscale() ) {
			Assert.assertEquals(pf.getAnnee() + 1, ppf.getTermeGeneralSommationEffectif().year());
			Assert.assertEquals(pf.getAnnee() + 1, ppf.getTermeGeneralSommationReglementaire().year());
			Assert.assertEquals(pf.getAnnee() + 1, ppf.getDateFinEnvoiMasseDI().year());
		}
	}

	/**
	 * Instancie un objet {@link PeriodeFiscale}, avec des {@link ParametrePeriodeFiscale} par défaut
	 *
	 * @param annee l'année de la {@link PeriodeFiscale}
	 */
	private PeriodeFiscale newPeriodeFiscale(int annee) {
		PeriodeFiscale pf = new PeriodeFiscale();
		pf.setId((long) annee);
		pf.setAnnee(annee);
		pf.setDefaultPeriodeFiscaleParametres();
		return pf;
	}

}
