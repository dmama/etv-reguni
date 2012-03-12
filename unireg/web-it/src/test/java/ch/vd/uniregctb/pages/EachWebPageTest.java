package ch.vd.uniregctb.pages;

import ch.vd.uniregctb.common.WebitTest;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class EachWebPageTest extends WebitTest {

	//private static final Logger LOGGER = Logger.getLogger(EachWebPageTest.class);

	private static final String DBUNIT_FILENAME = "EachWebPageTest.xml";

	private static boolean alreadySetUp = false;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (!alreadySetUp) {
			alreadySetUp = true;

			loadDatabase(DBUNIT_FILENAME);
		}
	}

	// Ne Pas supprimer ce FIXME(FDE)!!! Il faut ajouter les nouvelles pages a chaque itération
	// FIXME(FDE) : Ajouter les nouvelles pages + ajouter un test pour toutes les pages avec paramètres en utilisant des valeurs
	// inexistantes
	// Ne Pas supprimer ce FIXME(FDE)!!! Il faut ajouter les nouvelles pages a chaque itération

	/*--------------------------
	 *
	 *Recherche des tiers
	 *
	 *-------------------------*/
	@Test
	public void testTiersList() throws Exception {
		assertPage("/tiers/list.do", "Recherche des tiers");
	}

	/*--------------------------
	 *
	 * Création d'un tiers fiscal
	 *
	 *-------------------------*/
	// Page d'ajout d'un habitant
	@Test
	public void testTiersNewNonHabitant() throws Exception {
		assertPage("/tiers/create.do?nature=NonHabitant&onglet=civil", "Création d'une PP inconnue au contrôle des habitants");
	}

	// Page d'ajout d'une autre communauté
	@Test
	public void testTiersNewAC() throws Exception {
		assertPage("/tiers/create.do?nature=AutreCommunaute&onglet=civil", "Création d'une PM non connue du registre");
	}

	/*--------------------------
	 *
	 * Edition d'un tiers
	 *
	 *-------------------------*/
	// Page d'édition d'un tiers (couple)
	@Test
	public void testEditTiers() throws Exception {
		assertPage("/tiers/create.do?id=86006202", "Edition d'un tiers");
	}

	@Test
	public void testEditTiersInexistant() throws Exception {
		assertPage("/tiers/create.do?id=12345678", "Page d'erreur", "Le tiers spécifié n'existe pas");
	}

	/*--------------------------
	 *
	 * Visualisation d'un tiers
	 *
	 *-------------------------*/
	// Page de visualisation d'un tiers (couple)
	@Test
	public void testVisuTiers() throws Exception {
		assertPage("/tiers/visu.do?id=86006202", "Visualisation d'un tiers");
	}

	@Test
	public void testVisuTiersInexistant() throws Exception {
		assertPage("/tiers/visu.do?id=12345678", "Page d'erreur", "Le tiers id=[12345678] n'existe pas");
	}

	/*--------------------------------------------
	 *
	 * Visualisation/Edition d'un tiers (overlays)
	 *
	 *--------------------------------------------*/
	// Page de visualisation d'une LR
	@Test
	public void testTiersVisuLR() throws Exception {
		assertPage("/tiers/lr.do?idLr=1", "");
	}

	@Test
	public void testTiersVisuLRInexistante() throws Exception {
		assertPage("/tiers/lr.do?idLr=12345678", "Page d'erreur", "La LR spécifiée n'existe pas");
	}

	// Page de visualisation d'une DI
	@Test
	public void testTiersVisuDI() throws Exception {
		assertPage("/tiers/di.do?idDi=2", "");
	}

	@Test
	public void testTiersVisuDIInexistante() throws Exception {
		assertPage("/tiers/di.do?idDi=12345678", "Page d'erreur", "La DI spécifiée n'existe pas");
	}

	// Page de visualisation d'un for
	@Test
	public void testTiersEditFor() throws Exception {
		assertPage("/fiscal/for.do?idFor=7", "Edition d'un for fiscal du tiers 129.000.01");
	}

	@Test
	public void testTiersEditForInexistant() throws Exception {
		assertPage("/fiscal/for.do?idFor=12345678", "Page d'erreur", "Le for fiscal spécifié n'existe pas");
	}

	// Page de visualisation d'une adresse
	@Test
	public void testTiersEditAdresse() throws Exception {
		assertPage("/adresses/adresse.do?idAdresse=1", "Création d'une adresse sur le tiers 129.000.01");
	}

	@Test
	public void testTiersEditAdresseInexistant() throws Exception {
		assertPage("/adresses/adresse.do?idAdresse=12345678", "Page d'erreur", "L'adresse spécifiée n'existe pas");
	}

	// Page de visualisation d'un rapport
	@Test
	public void testTiersEditRapport() throws Exception {
		assertPage("/tiers/rapport.do?idRapport=1&sens=SUJET", "Edition du rapport entre le tiers n°123.000.01 et le tiers n°860.062.02");
	}

	@Test
	public void testTiersEditRapportInexistant() throws Exception {
		assertPage("/tiers/rapport.do?idRapport=12345678&sens=SUJET", "Page d'erreur", "Le rapport spécifié n'existe pas");
	}

	// Page de visualisation d'un rapport
	@Test
	public void testTiersEditSituationFamille() throws Exception {
		assertPage("/fiscal/situation-famille.do?numero=86006202", "Ajout d'une situation de famille du tiers 860.062.02");
	}

	@Test
	public void testTiersEditSituationFamilleInexistante() throws Exception {
		assertPage("/fiscal/situation-famille.do?numero=12345678", "Page d'erreur", "Le tiers id=[12345678] n'existe pas");
	}

	/*----------------------
	 *
	 * Rapport de prestation
	 *
	 *----------------------*/
	// Rapport de prestation - Page de recherche des débiteurs potentiels
	@Test
	public void testDebiteurList() throws Exception {
		assertPage("/rt/list-debiteur.do?numeroSrc=12900001", "Rapport de prestation - Recherche du débiteur");
	}

	@Test
	public void testDebiteurListInexistant() throws Exception {
		assertPage("/rt/list-debiteur.do?numeroSrc=12345678", "Page d'erreur", "Le sourcier spécifié n'existe pas");
	}

	// Rapport de prestation - Page de recherche des sourciers potentiels
	@Test
	public void testSourcierList() throws Exception {
		assertPage("/rt/list-sourcier.do?numeroDpi=1678432", "Rapport de prestation - Recherche du sourcier");
	}

	@Test
	public void testSourcierListInexistant() throws Exception {
		assertPage("/rt/list-sourcier.do?numeroDpi=12345678", "Page d'erreur", "Le débiteur spécifié n'existe pas");
	}

	// Rapport de prestation - Page récapitulative
	@Test
	public void testRapportPrestationRecap() throws Exception {
		assertPage("/rt/edit.do?numeroSrc=12900001&numeroDpi=1678432", "Récapitulatif du rapport de prestation");
	}

	@Test
	public void testRapportPrestationRecapSourcierInexistant() throws Exception {
		assertPage("/rt/edit.do?numeroSrc=12345678&numeroDpi=1678432", "Page d'erreur",
				"Le sourcier spécifié n'existe pas");
	}

	@Test
	public void testRapportPrestationRecapDebiteurInexistant() throws Exception {
		assertPage("/rt/edit.do?numeroSrc=12900001&numeroDpi=12345678", "Page d'erreur",
				"Le débiteur spécifié n'existe pas");
	}

	/*----------------------
	 *
	 * Rapport entre tiers
	 *
	 -----------------------*/
	// Rapport entre tiers - Page de recherche des tiers
	@Test
	public void testRapportSearch() throws Exception {
		assertPage("/rapport/search.do?numero=86006202", "Recherche du tiers à lier");
	}

	@Test
	public void testRapportSearchInexistant() throws Exception {
		assertPage("/rapport/search.do?numero=12345678", "Page d'erreur", "Le tiers spécifié n'existe pas");
	}

	// Rapport entre tiers - Page d'édition
	@Test
	public void testRapportEdit() throws Exception {
		assertPage("/rapport/edit.do?numeroTiers=86006202&numeroTiersLie=12900001", "Récapitulatif du rapport entre tiers");
	}

	@Test
	public void testRapportEditTiersInexistant() throws Exception {
		assertPage("/rapport/edit.do?numeroTiers=12345678&numeroTiersLie=12900001", "Page d'erreur",
				"Le tiers id=[12345678] n'existe pas");
	}

	@Test
	public void testRapportEditTiersLieInexistant() throws Exception {
		assertPage("/rapport/edit.do?numeroTiers=86006202&numeroTiersLie=12345678", "Page d'erreur",
				"Le tiers id=[12345678] n'existe pas");
	}

	/*----------------------
	 *
	 * Couple
	 *
	 *----------------------*/
	@Test
	public void testCreateCouple() throws Exception {
		assertPage("/couple/create.do", "Création d'un nouveau ménage commun");
	}

	/*----------------------
	 *
	 * Separation
	 *
	 *----------------------*/
	// Séparation - Recherche d'un ménage commun
	@Test
	public void testSeparationList() throws Exception {
		assertPage("/separation/list.do", "Séparation - Recherche d'un ménage commun");
	}

	// Récapitulatif du couple à séparer
	@Test
	public void testSeparationRecap() throws Exception {
		assertPage("/separation/recap.do?numeroCple=86006202", "Récapitulatif du couple à séparer");
	}

	/*----------------------
	 *
	 * Décès
	 *
	 *----------------------*/
	// Décès - Recherche d'une personne physique
	@Test
	public void testDecesList() throws Exception {
		assertPage("/deces/list.do", "Décès - Recherche d'une personne physique");
	}

	// Récapitulatif du décès
	@Test
	public void testDecesRecap() throws Exception {
		assertPage("/deces/recap.do?numero=12300001", "Récapitulatif du décès");
	}

	/*----------------------
	 *
	 * Fusion
	 *
	 *----------------------*/
	// Fusion - Recherche d'une PP inconnue au C.hab
	@Test
	@Ignore
	public void testFusionListNonHabitant() throws Exception {
		assertPage("/fusion/list-non-habitant.do", "Fusion - Recherche d'une PP inconnue au C.hab");
	}

	// Fusion - Recherche d'une PP inconnue au C.hab
	@Test
	@Ignore
	public void testFusionListHabitant() throws Exception {
		assertPage("/fusion/list-habitant.do?numeroNonHab=12900001", "Fusion - Recherche d'une PP référencée au C.hab");
	}

	@Test
	@Ignore
	public void testFusionListHabitantInexistant() throws Exception {
		assertPage("/fusion/list-habitant.do?numeroNonHab=12345678", "Page d'erreur",
				"Le non-habitant spécifié n'existe pas");
	}

	// Fusion - Récapitulatif des personnes à fusionner
	@Test
	@Ignore
	public void testFusionRecap() throws Exception {
		assertPage("/fusion/recap.do?numeroNonHab=12900001&numeroHab=12300003", "Récapitulatif des personnes à fusionner");
	}

	@Test
	@Ignore
	public void testFusionRecapNonHabitantInexistant() throws Exception {
		assertPage("/fusion/recap.do?numeroNonHab=12345678&numeroHab=12300003", "Page d'erreur",
				"Le non-habitant spécifié n'existe pas");
	}

	@Test
	@Ignore
	public void testFusionRecapHabitantInexistant() throws Exception {
		assertPage("/fusion/recap.do?numeroNonHab=12900001&numeroHab=12345678", "Page d'erreur",
				"L'habitant spécifié n'existe pas");
	}

	/*----------------------
	 *
	 * Listes récapitulatives
	 *
	 *----------------------*/
	// Page de recherche des LR
	@Test
	public void testLrList() throws Exception {
		assertPage("/lr/list.do", "Recherche des listes récapitulatives");
	}

	// Page d'édition d'une LR
	@Test
	public void testLrEdit() throws Exception {
		assertPage("/lr/edit.do?id=1", "Edition d'une liste récapitulative");
	}

	@Test
	public void testLrEditInexistante() throws Exception {
		assertPage("/lr/edit.do?id=12345678", "Page d'erreur", "La LR spécifiée n'existe pas");
	}

	// Page d'édition des LR d'un débiteur
	@Test
	public void testLrEditDebiteur() throws Exception {
		assertPage("/lr/edit-debiteur.do?numero=1678432", "Edition des listes récapitulatives d'un débiteur");
	}

	@Test
	public void testLrEditDebiteurInexistant() throws Exception {
		assertPage("/lr/edit-debiteur.do?numero=12345678", "Page d'erreur",
				"Le tiers spécifié n'existe pas");
	}

	// Page d'édition d'un délai d'une LR
	@Test
	public void testLrDelai() throws Exception {
		assertPage("/lr/delai.do?idLR=1", "Ajout d'un délai sur la liste récapitulative 2004 (01.01.2008 - 31.03.2008) du débiteur 16.784.32");
	}

	@Test
	public void testLrDelaiInexistante() throws Exception {
		assertPage("/lr/delai.do?idLR=12345678", "Page d'erreur", "La LR spécifiée n'existe pas");
	}

	/*----------------------
	 *
	 * Déclaration d'impôt
	 *
	 *----------------------*/
	// Page d'édition d'une DI
	@Test
	public void testDiEdit() throws Exception {
		assertPage("/di/edit.do?action=editdi&id=2", "Edition de la déclaration d'impôt");
	}

	@Test
	public void testDiEditInexistante() throws Exception {
		assertPage("/di/edit.do?action=editdi&id=12345678", "Page d'erreur", "La DI spécifiée n'existe pas");
	}

	// Page d'édition des DI d'un contribuable
	@Test
	public void testDiEditContribuable() throws Exception {
		assertPage("/di/edit.do?action=listdis&numero=86006202", "Edition des déclarations d'impôt d'un contribuable");
	}

	@Test
	public void testDiEditContribuableInexistant() throws Exception {
		assertPage("/di/edit.do?action=listdis&numero=12345678", "Page d'erreur", "Le tiers id=[12345678] n'existe pas");
	}

	// Page d'édition d'un délai d'une DI
	@Test
	public void testDiDelai() throws Exception {
		assertPage("/di/delai.do?idDI=2", "Ajout d'un délai sur la déclaration 2005 (01.01.2005 - 31.12.2005) du contribuable 860.062.02");
	}

	@Test
	public void testDiDelaiInexistante() throws Exception {
		assertPage("/di/delai.do?idDI=12345678", "Page d'erreur", "La DI spécifiée n'existe pas");
	}

	// Page d'impression d'une DI
	@Test
	public void testDiImpression() throws Exception {
		assertPage("/di/impression.do?id=2", "");
	}

	@Test
	public void testDiImpressionInexistante() throws Exception {
		assertPage("/di/impression.do?id=12345678", "Page d'erreur", "La DI spécifiée n'existe pas");
	}

	/*-----------------------------------------
	 *
	 * Recherche des taches / nouveaux dossiers
	 *
	 *----------------------------------------*/
	// Page de recherche des taches
	@Test
	public void testTacheList() throws Exception {
		assertPage("/tache/list.do", "Recherche des tâches");
	}

	// Page de recherche des taches
	@Test
	public void testNouveauDossierList() throws Exception {
		assertPage("/tache/list-nouveau-dossier.do", "Recherche des nouveaux dossiers");
	}

	/*-----------------
	 *
	 * Mouvements
	 *
	 *----------------*/
	// Edition des mouvements d'un contribuable
	@Test
	public void testMvtCtbEdit() throws Exception {
		assertPage("/mouvement/edit-contribuable.do?numero=86006202", "Edition des mouvements de dossier");
	}

	@Test
	public void testMvtCtbEditInexistant() throws Exception {
		assertPage("/mouvement/edit-contribuable.do?numero=12345678", "Page d'erreur",
				"Le tiers id=[12345678] n'existe pas");
	}

	// Edition d'un mouvement
	@Test
	public void testMvtEdit() throws Exception {
		assertPage("/mouvement/edit.do?numero=86006202", "Edition du mouvement de dossier");
	}

	@Test
	public void testMvtEditInexistant() throws Exception {
		assertPage("/mouvement/edit.do?numero=12345678", "Page d'erreur", "Le tiers id=[12345678] n'existe pas");
	}

	/*----------------------
	 *
	 * Evenements civils
	 *
	 *----------------------*/
	// Page de recherche des événements civils
	@Test
	public void testEvtList() throws Exception {
		assertPage("/evenement/list.do", "Recherche des événements");
	}

	// Page de visualisation d'un événement civil
	@Test
	public void testEvtVisu() throws Exception {
		assertPage("/evenement/visu.do?id=9876", "Caractéristiques de l'événement");
	}

	@Test
	public void testEvtVisuInexistant() throws Exception {
		assertPage("/evenement/visu.do?id=12345678", "Page d'erreur", "L'événement spécifié (no 12345678) n'existe pas");
	}

	/*----------------------
	 *
	 * Droits d'accès
	 *
	 *----------------------*/

	// Page de recherche par dossier
	@Test
	public void testAccesList() throws Exception {
		assertPage("/acces/list-pp.do", "Accès par dossier - Recherche d'une personne physique");
	}

	// Page de restrictions de dossier
	@Test
	public void testAccesRestrictionsPP() throws Exception {
		assertPage("/acces/restrictions-pp.do?numero=12300002", "Droits d'accès du dossier");
	}

	// Page d'édition de droit d'accès par dossier
	@Test
	public void testAccesEdit() throws Exception {
		assertPage("/acces/edit-acces-pp.do?numero=12300002", "Création d'un droit d'accès sur la personne physique n°123.000.02");
	}

	// Page de sélection d'un utilisateur
	@Test
	public void testAccesSelectUtilisateur() throws Exception {
		assertPage("/acces/select-utilisateur.do", "Accès par utilisateur - Recherche d'un utilisateur");
	}

	// Page des droits d'accès pour un utilisateur
	@Test
	public void testAccesRestrictionsUtilisateur() throws Exception {
		assertPage("/acces/restrictions-utilisateur.do?noIndividuOperateur=582380", "Droits d'accès de l'utilisateur");
	}

	// Page de recherche d'un dossier pour un utilisateur
	@Test
	public void testAccesListPersonneUtilisateur() throws Exception {
		assertPage("/acces/list-pp-utilisateur.do?noIndividuOperateur=582380", "Accès par utilisateur - Recherche d'une personne physique");
	}

	// Page de récapitulation de droit d'accès par dossier
	@Test
	public void testAccesRecapDossierUtilisateur() throws Exception {
		assertPage("/acces/recap-pp-utilisateur.do?numero=12300002&noIndividuOperateur=582380", "Création d'un droit d'accès");
	}

	// Page de Copie / Transfert de droit d'accès
	@Test
	public void testAccesCopieTransfert() throws Exception {
		assertPage("/acces/select-utilisateurs.do", "Copie / Transfert - Recherche des utilisateurs");
	}

	// Page de de confirmation de Copie de droit d'accès
	@Test
	public void testAccesConfirmCopie() throws Exception {
		assertPage("/acces/confirm-copie.do?noOperateurReference=582380&noOperateurDestination=860474&typeOperation=COPIE", "Confirmation de la copie des droits d'accès");
	}

	// Page de de confirmation de transfert de droit d'accès
	@Test
	public void testAccesConfirmTransfert() throws Exception {
		assertPage("/acces/confirm-copie.do?noOperateurReference=582380&noOperateurDestination=860474&typeOperation=TRANSFERT", "Confirmation du transfert des droits d'accès");
	}

	/*----------------------
	 *
	 * Administration
	 *
	 *----------------------*/
	// Page d'admin
	@Test
	public void testAdminTiersImport() throws Exception {
		assertPage("/admin/tiersImport/list.do", "Import d'un script");
	}

	// Page d'indexation
	@Test
	public void testAdminIndexation() throws Exception {
		assertPage("/admin/indexation.do", "Gestion de l'indexation");
	}

	// Page d'administration des batchs
	@Test
	public void testAdminBatch() throws Exception {
		assertPage("/admin/batch.do", "Gestion des batchs");
	}

	// Page d'audit
	@Test
	public void testAdminAudit() throws Exception {
		assertPage("/admin/audit.do", "Audit logs");
	}

	/**
	 * Contrôle du titre d'une page web
	 *
	 * @param url
	 *            l'url de la page à contrôler
	 * @param title
	 *            le titre de la page
	 */
	private void assertPage(String url, String title) throws Exception {
		assertPage(url, title, null);
	}

	@SuppressWarnings("unchecked")
	private void assertPage(String url, String title, String contenu) throws Exception {

		final HtmlPage page = getHtmlPage(url);

		// vérification du titre
		final String message = "Page '" + url + "': titre attendu '" + title + "', trouvé '" + page.getTitleText() + "'.";
		if ("".equals(title.trim())) {
			assertEquals(message, "", page.getTitleText().trim());
		}
		else {
			assertContains(title, page.getTitleText(), message);
		}

		if (contenu != null) {
			// vérification du contenu
			boolean trouve = false;
			for (HtmlElement element : page.getHtmlElementDescendants()) {
				if (element.asText().contains(contenu)) {
					trouve = true;
					break;
				}
			}
			if (!trouve) {
				Assert.fail("Le corps de la page '" + url + "' ne contient pas le texte '" + contenu + "'.");
			}
		}
	}

}
