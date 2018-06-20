package ch.vd.unireg.evenement.entreprise.interne;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.interne.creation.CreateEntrepriseStrategy;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Raphaël Marmier, 2016-12-19, <raphael.marmier@vd.ch>
 */
public class AbstractEntrepriseCivileStrategyTest extends WithoutSpringTest {

	@Test
	public void testNouvellePMauRC() throws Exception {

		final CreateEntrepriseStrategy strategy = new CreateEntrepriseStrategy(null, null);

		/*
			Création entreprise PM VD inscrite au RC
		 */
		{
			final MockEntrepriseCivile ent = createRcVd(10001L, 90001L, date(2016, 12, 4), FormeLegale.N_0106_SOCIETE_ANONYME, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, MockCommune.Lausanne.getNoOFS());
			final EvenementEntreprise event = createEvent(1L, 10001L, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, date(2016, 12, 4), A_TRAITER);
			final AbstractEntrepriseStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateEntrepriseStrategy.extraireInformationDeDateEtDeCreation(event, ent);

			assertEquals(date(2016, 12, 1), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 12, 2), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertTrue(informationDeDateEtDeCreation.isCreation());
		}

		/*
			Création entreprise PM HC inscrite au RC
		 */
		{
			final MockEntrepriseCivile ent = createRcVd(10001L, 90001L, date(2016, 12, 4), FormeLegale.N_0106_SOCIETE_ANONYME, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, MockCommune.Zurich.getNoOFS());
			final EvenementEntreprise event = createEvent(1L, 10001L, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, date(2016, 12, 4), A_TRAITER);

			final AbstractEntrepriseStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateEntrepriseStrategy.extraireInformationDeDateEtDeCreation(event, ent);

			assertEquals(date(2016, 12, 1), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 12, 2), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertTrue(informationDeDateEtDeCreation.isCreation());
		}
	}

	@Test
	public void testNouvelleAPMauRC() throws Exception {

		final CreateEntrepriseStrategy strategy = new CreateEntrepriseStrategy(null, null);

		/*
			Création entreprise APM VD inscrite au RC
		 */
		{
			final MockEntrepriseCivile ent = createRcVd(10001L, 90001L, date(2016, 12, 4), FormeLegale.N_0109_ASSOCIATION, TypeEntrepriseRegistreIDE.ASSOCIATION, MockCommune.Lausanne.getNoOFS());

			final EvenementEntreprise event = createEvent(1L, 10001L, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, date(2016, 12, 4), A_TRAITER);

			final AbstractEntrepriseStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateEntrepriseStrategy.extraireInformationDeDateEtDeCreation(event, ent);

			assertEquals(date(2016, 12, 1), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 12, 2), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertTrue(informationDeDateEtDeCreation.isCreation());
		}

		/*
			Création entreprise APM HC inscrite au RC
		 */
		{
			final MockEntrepriseCivile ent = createRcNonVD(10001L, 90001L, date(2016, 12, 4), FormeLegale.N_0109_ASSOCIATION, TypeEntrepriseRegistreIDE.ASSOCIATION, MockCommune.Zurich.getNoOFS());

			final EvenementEntreprise event = createEvent(1L, 10001L, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, date(2016, 12, 4), A_TRAITER);

			final AbstractEntrepriseStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateEntrepriseStrategy.extraireInformationDeDateEtDeCreation(event, ent);

			assertEquals(date(2016, 12, 1), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 12, 1), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertTrue(informationDeDateEtDeCreation.isCreation());
		}

	}

	@Test
	public void testNouvelleAPMnonRC() throws Exception {

		final CreateEntrepriseStrategy strategy = new CreateEntrepriseStrategy(null, null);

		/*
			Création entreprise APM VD non inscrite au RC
		 */
		{
			final MockEntrepriseCivile ent = createNonRcVd(10001L, 90001L, date(2016, 12, 4), FormeLegale.N_0109_ASSOCIATION, TypeEntrepriseRegistreIDE.ASSOCIATION, MockCommune.Lausanne.getNoOFS());

			final EvenementEntreprise event = createEvent(1L, 10001L, TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION, date(2016, 12, 4), A_TRAITER);

			final AbstractEntrepriseStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateEntrepriseStrategy.extraireInformationDeDateEtDeCreation(event, ent);

			assertEquals(date(2016, 12, 4), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 12, 4), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertTrue(informationDeDateEtDeCreation.isCreation());
		}

		/*
			Création entreprise APM non VD non inscrite au RC
		 */
		{
			final MockEntrepriseCivile ent = createNonRcNonVd(10001L, 90001L, date(2016, 12, 4), FormeLegale.N_0109_ASSOCIATION, TypeEntrepriseRegistreIDE.ASSOCIATION, MockCommune.Zurich.getNoOFS());

			final EvenementEntreprise event = createEvent(1L, 10001L, TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION, date(2016, 12, 4), A_TRAITER);

			final AbstractEntrepriseStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateEntrepriseStrategy.extraireInformationDeDateEtDeCreation(event, ent);

			assertEquals(date(2016, 12, 4), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 12, 4), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertTrue(informationDeDateEtDeCreation.isCreation());
		}
	}

	@Test
	public void testMutationPMauRC() throws Exception {

		final CreateEntrepriseStrategy strategy = new CreateEntrepriseStrategy(null, null);

		/*
			Arrivee entreprise PM VD inscrite au RC
		 */
		{
			final MockEntrepriseCivile ent = createRcVd(10001L, 90001L, date(2010, 12, 4), FormeLegale.N_0106_SOCIETE_ANONYME, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, MockCommune.Zurich.getNoOFS());
			final MockEtablissementCivil etablissementCivil = (MockEtablissementCivil) ent.getEtablissements().get(0);
			etablissementCivil.changeDomicile(date(2016, 10, 1), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS());
			final MockDonneesRC donneesRC = etablissementCivil.getDonneesRC();
			donneesRC.addInscription(date(2016, 10, 1), null, new InscriptionRC(StatusInscriptionRC.ACTIF, null, date(2016, 9, 27), null, date(2010, 12, 4), null));

			final EvenementEntreprise event = createEvent(1L, 10001L, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2016, 10, 1), A_TRAITER);

			final AbstractEntrepriseStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateEntrepriseStrategy.extraireInformationDeDateEtDeCreation(event, ent);

			assertEquals(date(2016, 9, 27), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 9, 27), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertFalse(informationDeDateEtDeCreation.isCreation());
		}

		/*
			Mutation entreprise PM HC inscrite au RC
		 */
		{
			final MockEntrepriseCivile ent = createRcVd(10001L, 90001L, date(2010, 12, 4), FormeLegale.N_0106_SOCIETE_ANONYME, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, MockCommune.Zurich.getNoOFS());
			final EvenementEntreprise event = createEvent(1L, 10001L, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2016, 10, 1), A_TRAITER);

			final AbstractEntrepriseStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateEntrepriseStrategy.extraireInformationDeDateEtDeCreation(event, ent);

			// Note: dans la pratique, ces dates ne sont pas utilisée lors l'ajout en base Unireg d'une tiers PM HC suite à la création d'un établissement secondaire VD. La date de l'événement est utilisée.
			assertEquals(date(2010, 12, 1), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2010, 12, 1), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertFalse(informationDeDateEtDeCreation.isCreation());
		}
	}

	@Test
	public void testMutationAPMauRC() throws Exception {

		final CreateEntrepriseStrategy strategy = new CreateEntrepriseStrategy(null, null);

		/*
			Arrivee entreprise APM VD inscrite au RC
		 */
		{
			final MockEntrepriseCivile ent = createRcVd(10001L, 90001L, date(2010, 12, 4), FormeLegale.N_0109_ASSOCIATION, TypeEntrepriseRegistreIDE.ASSOCIATION, MockCommune.Zurich.getNoOFS());
			final MockEtablissementCivil etablissementCivil = (MockEtablissementCivil) ent.getEtablissements().get(0);
			etablissementCivil.changeDomicile(date(2016, 10, 1), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS());
			final MockDonneesRC donneesRC = etablissementCivil.getDonneesRC();
			donneesRC.addInscription(date(2016, 10, 1), null, new InscriptionRC(StatusInscriptionRC.ACTIF, null, date(2016, 9, 27), null, date(2010, 12, 4), null));

			final EvenementEntreprise event = createEvent(1L, 10001L, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2016, 10, 1), A_TRAITER);

			final AbstractEntrepriseStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateEntrepriseStrategy.extraireInformationDeDateEtDeCreation(event, ent);

			assertEquals(date(2016, 9, 27), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 9, 27), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertFalse(informationDeDateEtDeCreation.isCreation());
		}

		/*
			Mutation entreprise APM HC inscrite au RC
		 */
		{
			final MockEntrepriseCivile ent = createRcVd(10001L, 90001L, date(2010, 12, 4), FormeLegale.N_0109_ASSOCIATION, TypeEntrepriseRegistreIDE.ASSOCIATION, MockCommune.Zurich.getNoOFS());
			final EvenementEntreprise event = createEvent(1L, 10001L, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2016, 10, 1), A_TRAITER);

			final AbstractEntrepriseStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateEntrepriseStrategy.extraireInformationDeDateEtDeCreation(event, ent);

			// Note: dans la pratique, ces dates ne sont pas utilisée lors l'ajout en base Unireg d'une tiers PM HC suite à la création d'un établissement secondaire VD. La date de l'événement est utilisée.
			assertEquals(date(2010, 12, 1), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2010, 12, 1), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertFalse(informationDeDateEtDeCreation.isCreation());
		}
	}

	@Test
	public void testMutationAPMnonRC() throws Exception {

		final CreateEntrepriseStrategy strategy = new CreateEntrepriseStrategy(null, null);

		/*
			Arrivee entreprise APM VD non inscrite au RC
		 */
		{
			final MockEntrepriseCivile ent = createNonRcVd(10001L, 90001L, date(2010, 12, 4), FormeLegale.N_0109_ASSOCIATION, TypeEntrepriseRegistreIDE.ASSOCIATION, MockCommune.Zurich.getNoOFS());
			final MockEtablissementCivil etablissementCivil = (MockEtablissementCivil) ent.getEtablissements().get(0);
			etablissementCivil.changeDomicile(date(2016, 10, 1), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS());
			final MockDonneesRC donneesRC = etablissementCivil.getDonneesRC();
			donneesRC.addInscription(date(2016, 10, 1), null, new InscriptionRC(StatusInscriptionRC.ACTIF, null, date(2016, 9, 27), null, date(2010, 12, 4), null));

			final EvenementEntreprise event = createEvent(1L, 10001L, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2016, 10, 1), A_TRAITER);

			final AbstractEntrepriseStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateEntrepriseStrategy.extraireInformationDeDateEtDeCreation(event, ent);

			assertEquals(date(2016, 9, 27), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 9, 27), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertFalse(informationDeDateEtDeCreation.isCreation());
		}

		/*
			Mutation entreprise APM HC non inscrite au RC
		 */
		{
			final MockEntrepriseCivile ent = createNonRcVd(10001L, 90001L, date(2010, 12, 4), FormeLegale.N_0109_ASSOCIATION, TypeEntrepriseRegistreIDE.ASSOCIATION, MockCommune.Zurich.getNoOFS());
			final EvenementEntreprise event = createEvent(1L, 10001L, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2016, 10, 1), A_TRAITER);

			final AbstractEntrepriseStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateEntrepriseStrategy.extraireInformationDeDateEtDeCreation(event, ent);

			// Note: dans la pratique, ces dates ne sont pas utilisée lors l'ajout en base Unireg d'une tiers PM HC suite à la création d'un établissement secondaire VD. La date de l'événement est utilisée.
			assertEquals(date(2016, 10, 1), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 10, 1), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertFalse(informationDeDateEtDeCreation.isCreation());
		}
	}

	protected MockEntrepriseCivile createRcVd(long cantonalId, long cantonalIdEtablissementPrincipal, RegDate dateEvt, FormeLegale formeJuridique, TypeEntrepriseRegistreIDE formeJuridiqueIde,
	                                          int noOFSCommune) {
		return MockEntrepriseFactory.createEntreprise(cantonalId, cantonalIdEtablissementPrincipal, "Synergy SA", dateEvt, null, formeJuridique,
		                                              TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, noOFSCommune, StatusInscriptionRC.ACTIF, dateEvt.addDays(-3),
		                                              StatusRegistreIDE.DEFINITIF, formeJuridiqueIde, "CHE999999996");
	}

	protected MockEntrepriseCivile createRcNonVD(long cantonalId, long cantonalIdEtablissementPrincipal, RegDate dateEvt, FormeLegale formeJuridique, TypeEntrepriseRegistreIDE formeJuridiqueIde,
	                                             int noOFSCommune) {
		return MockEntrepriseFactory.createEntreprise(cantonalId, cantonalIdEtablissementPrincipal, "Synergy SA", dateEvt, null, formeJuridique,
		                                              TypeAutoriteFiscale.COMMUNE_HC, noOFSCommune, StatusInscriptionRC.ACTIF, dateEvt.addDays(-3),
		                                              StatusRegistreIDE.DEFINITIF, formeJuridiqueIde, "CHE999999996");
	}


	protected MockEntrepriseCivile createNonRcVd(long cantonalId, long cantonalIdEtablissementPrincipal, RegDate dateEvt, FormeLegale formeJuridique, TypeEntrepriseRegistreIDE formeJuridiqueIde,
	                                             int noOFSCommune) {
		return MockEntrepriseFactory.createEntreprise(cantonalId, cantonalIdEtablissementPrincipal, "Synergy SA", dateEvt, null, formeJuridique,
		                                              TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, noOFSCommune, null, null,
		                                              StatusRegistreIDE.DEFINITIF, formeJuridiqueIde, "CHE999999996");
	}

	protected MockEntrepriseCivile createNonRcNonVd(long cantonalId, long cantonalIdEtablissementPrincipal, RegDate dateEvt, FormeLegale formeJuridique, TypeEntrepriseRegistreIDE formeJuridiqueIde,
	                                                int noOFSCommune) {
		return MockEntrepriseFactory.createEntreprise(cantonalId, cantonalIdEtablissementPrincipal, "Synergy SA", dateEvt, null, formeJuridique,
		                                              TypeAutoriteFiscale.COMMUNE_HC, noOFSCommune, null, null,
		                                              StatusRegistreIDE.DEFINITIF, formeJuridiqueIde, "CHE999999996");
	}

	@NotNull
	protected static EvenementEntreprise createEvent(Long noEvenement, Long noEntrepriseCivile, TypeEvenementEntreprise type, RegDate date, EtatEvenementEntreprise etat) {
		final EvenementEntreprise event = new EvenementEntreprise();
		event.setNoEvenement(noEvenement);
		event.setNoEntrepriseCivile(noEntrepriseCivile);
		event.setType(type);
		event.setDateEvenement(date);
		event.setEtat(etat);
		return event;
	}
}