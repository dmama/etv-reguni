package ch.vd.uniregctb.hibernate;

import java.sql.Connection;
import java.util.List;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class TypeAdresseCivilLegacyUserTypeTest extends CoreDAOTest {

	TiersDAO dao;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		dao = getBean(TiersDAO.class, "tiersDAO");
	}

	/*
	 * Ce test vérifie que le chargement d'un enum de type EnumTypeAdresse fonctionne correctement.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testLoad() throws Exception {

		loadDatabase("TypeAdresseCivilLegacyUserTypeTest.xml");

		// vérification de cohérence des données de base
		final PersonnePhysique habitant = dao.getHabitantByNumeroIndividu(100L);
		assertNotNull(habitant);
		assertEquals(new Long(100L), habitant.getNumeroIndividu());

		final List<AdresseTiers> adresses = habitant.getAdressesTiersSorted();
		assertEquals(4, adresses.size());

		// vérification de la validité de l'enum
		final AdresseCivile adresseCourrier = (AdresseCivile) adresses.get(0);
		final AdresseCivile adressePrincipal = (AdresseCivile) adresses.get(1);
		final AdresseCivile adresseSecondaire = (AdresseCivile) adresses.get(2);
		final AdresseCivile adresseTutelle = (AdresseCivile) adresses.get(3);
		assertEquals(TypeAdresseCivil.COURRIER, adresseCourrier.getType());
		assertEquals(TypeAdresseCivil.PRINCIPALE, adressePrincipal.getType());
		assertEquals(TypeAdresseCivil.SECONDAIRE, adresseSecondaire.getType());
		assertEquals(TypeAdresseCivil.TUTEUR, adresseTutelle.getType());
	}

	/*
	 * Ce test vérifie que la persistence d'un enum de type EnumTypeAdresse fonctionne correctement.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSave() throws Exception {

		// date de création des adresses, utilisées pour distinguer les types d'adresse
		final RegDate dateCourrier = RegDate.get(2000, 1, 1);
		final RegDate datePrincipale = RegDate.get(2000, 2, 1);
		final RegDate dateSecondaire = RegDate.get(2000, 3, 1);
		final RegDate dateTutelle = RegDate.get(2000, 4, 1);

		doInNewTransaction(new TxCallback<Object>() {

			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique habitant = new PersonnePhysique(true);
				habitant.setNumeroIndividu(200L);

				// données d'entrée
				final AdresseCivile adresseCourrier = new AdresseCivile();
				adresseCourrier.setDateDebut(dateCourrier);
				adresseCourrier.setUsage(TypeAdresseTiers.COURRIER);
				adresseCourrier.setType(TypeAdresseCivil.COURRIER);
				habitant.addAdresseTiers(adresseCourrier);

				final AdresseCivile adressePrincipal = new AdresseCivile();
				adressePrincipal.setDateDebut(datePrincipale);
				adressePrincipal.setUsage(TypeAdresseTiers.POURSUITE);
				adressePrincipal.setType(TypeAdresseCivil.PRINCIPALE);
				habitant.addAdresseTiers(adressePrincipal);

				final AdresseCivile adresseSecondaire = new AdresseCivile();
				adresseSecondaire.setDateDebut(dateSecondaire);
				adresseSecondaire.setUsage(TypeAdresseTiers.DOMICILE);
				adresseSecondaire.setType(TypeAdresseCivil.SECONDAIRE);
				habitant.addAdresseTiers(adresseSecondaire);

				final AdresseCivile adresseTutelle = new AdresseCivile();
				adresseTutelle.setDateDebut(dateTutelle);
				adresseTutelle.setUsage(TypeAdresseTiers.REPRESENTATION);
				adresseTutelle.setType(TypeAdresseCivil.TUTEUR);
				habitant.addAdresseTiers(adresseTutelle);

				// sauvegarde dans la base (+ commit de la transaction)
				dao.save(habitant);
				return null;
			}
		});

		// vérification de la validité des valeurs dans la base
		Connection dsCon = dataSource.getConnection();
		try {
			DatabaseConnection connection = createNewConnection(dsCon);
			IDataSet dataSet = connection.createDataSet();
			ITable table = dataSet.getTable("ADRESSE_TIERS");
			assertEquals(4, table.getRowCount());

			for (int i = 0; i < 4; ++i) {
				Number index = (Number) table.getValue(i, "DATE_DEBUT");
				RegDate dateAdresse = RegDate.fromIndex(index.intValue(), false);
				String type = (String) table.getValue(i, "STYPE");

				if (dateCourrier.equals(dateAdresse)) {
					assertEquals("C", type);
				}
				else if (datePrincipale.equals(dateAdresse)) {
					assertEquals("P", type);
				}
				else if (dateSecondaire.equals(dateAdresse)) {
					assertEquals("S", type);
				}
				else if (dateTutelle.equals(dateAdresse)) {
					assertEquals("T", type);
				}
			}
		}
		finally {
			dsCon.close();
		}
	}

}