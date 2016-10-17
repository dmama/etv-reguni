package ch.vd.uniregctb.evenement.ide;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.TypeAdresseTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Raphaël Marmier, 2016-10-17, <raphael.marmier@vd.ch>
 */
public class AnnonceIDEHibernateInterceptorTest extends BusinessTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnonceIDEHibernateInterceptorTest.class);

	private TiersDAO tiersDAO;

	@Before
	public void setUp() throws Exception {
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
	}

	@Test
	public void testOnChange() throws Exception {
		// Situation de base l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseInconnueAuCivil();
				Etablissement etablissement = addEtablissement();

				addActiviteEconomique(entreprise, etablissement, date(2016, 9, 5), null, true);

				addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 5), null, "Syntruc Asso");
				addDomicileEtablissement(etablissement, date(2016, 9, 5), null, MockCommune.Renens);
				addFormeJuridique(entreprise, date(2016, 9, 5), null, FormeJuridiqueEntreprise.ASSOCIATION);

				entreprise.setSecteurActivite("Fabrication d'objets synthétiques");

				final AdresseSuisse adresseSuisse = addAdresseSuisse(entreprise, TypeAdresseTiers.DOMICILE, date(2016, 9, 5), null, MockRue.Renens.QuatorzeAvril);
				adresseSuisse.setNumeroMaison("1");

				return entreprise.getNumero();
			}
		});

		// Premier contrôle et règlage du flag IdeDirty à false et modification de données qui ne doivent pas être prises en compte
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);
				assertEquals(true, entreprise.isIdeDirty());

				// Réinitialisation du flag
				entreprise.setIdeDirty(false);

				// Modification du siège de l'entreprise, ne doit pas entrainer de changement du flag
				final Etablissement etablissementPrincipal = tiersService.getEtablissementPrincipal(entreprise, date(2016, 10, 1));
				final List<DomicileEtablissement> domiciles = etablissementPrincipal.getSortedDomiciles(false);
				assertNotNull(domiciles);
				assertEquals(1, domiciles.size());
				final DomicileEtablissement domicile = domiciles.get(0);
				assertEquals(MockCommune.Renens.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale().intValue());
				domicile.setDateFin(date(2016, 10, 1));
				addDomicileEtablissement(etablissementPrincipal, date(2016, 10, 2), null, MockCommune.Aigle);
			}
		});

		// Vérification que le passage à dirty == false a bien fonctionné
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);
				assertEquals(false, entreprise.isIdeDirty());

				final Etablissement etablissementPrincipal = tiersService.getEtablissementPrincipal(entreprise, date(2016, 10, 1));
				final List<DomicileEtablissement> domiciles = etablissementPrincipal.getSortedDomiciles(false);
				assertNotNull(domiciles);
				assertEquals(2, domiciles.size());

				final DomicileEtablissement premierDomicile = domiciles.get(0);
				assertEquals(MockCommune.Renens.getNoOFS(), premierDomicile.getNumeroOfsAutoriteFiscale().intValue());
				assertEquals(date(2016, 9, 5), premierDomicile.getDateDebut());
				assertEquals(date(2016, 10, 1), premierDomicile.getDateFin());

				final DomicileEtablissement secondaDomicile = domiciles.get(1);
				assertEquals(MockCommune.Aigle.getNoOFS(), secondaDomicile.getNumeroOfsAutoriteFiscale().intValue());
				assertEquals(date(2016, 10, 2), secondaDomicile.getDateDebut());
				assertEquals(null, secondaDomicile.getDateFin());

				final List<RaisonSocialeFiscaleEntreprise> raisonsSocialesTriees = entreprise.getRaisonsSocialesTriees();
				assertNotNull(raisonsSocialesTriees);
				assertEquals(1, raisonsSocialesTriees.size());
				final RaisonSocialeFiscaleEntreprise premiereRaisonSociale = raisonsSocialesTriees.get(0);
				assertEquals("Syntruc Asso", premiereRaisonSociale.getRaisonSociale());

				// On modifie la raison sociale
				premiereRaisonSociale.setDateFin(date(2016, 10, 1));
				addRaisonSociale(entreprise, date(2016, 10, 2), null, "Synchrotronix Asso");
			}
		});

		// Vérification que le flag est bien mis à true suite à la nouvelle raison sociale
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);
				assertEquals(true, entreprise.isIdeDirty());

				final List<RaisonSocialeFiscaleEntreprise> raisonsSocialesTriees = entreprise.getRaisonsSocialesTriees();
				assertNotNull(raisonsSocialesTriees);
				assertEquals(2, raisonsSocialesTriees.size());

				final RaisonSocialeFiscaleEntreprise premiereRaisonSociale = raisonsSocialesTriees.get(0);
				assertEquals("Syntruc Asso", premiereRaisonSociale.getRaisonSociale());
				assertEquals(date(2016, 9, 5), premiereRaisonSociale.getDateDebut());
				assertEquals(date(2016, 10, 1), premiereRaisonSociale.getDateFin());

				final RaisonSocialeFiscaleEntreprise secondeRaisonSociale = raisonsSocialesTriees.get(1);
				assertEquals("Synchrotronix Asso", secondeRaisonSociale.getRaisonSociale());
				assertEquals(date(2016, 10, 2), secondeRaisonSociale.getDateDebut());
				assertEquals(null, secondeRaisonSociale.getDateFin());

			}
		});
	}
}