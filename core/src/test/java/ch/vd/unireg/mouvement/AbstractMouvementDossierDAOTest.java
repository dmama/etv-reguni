package ch.vd.unireg.mouvement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.Sexe;

public abstract class AbstractMouvementDossierDAOTest extends CoreDAOTest {

	private TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
	}

	protected TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	protected CollectiviteAdministrative addCollectiviteAdministrative(int noCA) {
		final CollectiviteAdministrative ca = new CollectiviteAdministrative();
		ca.setNumeroCollectiviteAdministrative(noCA);
		return (CollectiviteAdministrative) tiersDAO.save(ca);
	}

	@Override
	protected PersonnePhysique addNonHabitant(String prenom, String nom, RegDate dateNaissance, Sexe sexe) {
		final PersonnePhysique nh = new PersonnePhysique(false);
		nh.setPrenomUsuel(prenom);
		nh.setNom(nom);
		nh.setDateNaissance(dateNaissance);
		nh.setSexe(sexe);
		return hibernateTemplate.merge(nh);
	}

	protected ReceptionDossierArchives addMouvementDossierArchives(Contribuable ctb, CollectiviteAdministrative oid, EtatMouvementDossier etat) {
		final ReceptionDossierArchives mvt = new ReceptionDossierArchives();
		mvt.setContribuable(ctb);
		mvt.setCollectiviteAdministrativeReceptrice(oid);
		mvt.setEtat(etat);
		return hibernateTemplate.merge(mvt);
	}

	protected MouvementDossier addMouvementDossierClassementGeneral(Contribuable ctb, CollectiviteAdministrative oid, EtatMouvementDossier etat) {
		final ReceptionDossierClassementGeneral mvt = new ReceptionDossierClassementGeneral();
		mvt.setContribuable(ctb);
		mvt.setCollectiviteAdministrativeReceptrice(oid);
		mvt.setEtat(etat);
		return hibernateTemplate.merge(mvt);
	}

	protected EnvoiDossierVersCollectiviteAdministrative addMouvementDossierEnvoi(Contribuable ctb, CollectiviteAdministrative oidDepart, CollectiviteAdministrative oidArrivee, EtatMouvementDossier etat) {
		final EnvoiDossierVersCollectiviteAdministrative mvt = new EnvoiDossierVersCollectiviteAdministrative(oidArrivee);
		mvt.setContribuable(ctb);
		mvt.setCollectiviteAdministrativeEmettrice(oidDepart);
		mvt.setEtat(etat);
		return hibernateTemplate.merge(mvt);
	}

}
