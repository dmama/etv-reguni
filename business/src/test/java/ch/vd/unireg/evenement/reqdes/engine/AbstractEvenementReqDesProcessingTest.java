package ch.vd.unireg.evenement.reqdes.engine;

import java.util.Date;
import java.util.HashSet;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.reqdes.EtatTraitement;
import ch.vd.unireg.reqdes.EvenementReqDes;
import ch.vd.unireg.reqdes.EvenementReqDesDAO;
import ch.vd.unireg.reqdes.InformationsActeur;
import ch.vd.unireg.reqdes.ModeInscription;
import ch.vd.unireg.reqdes.PartiePrenante;
import ch.vd.unireg.reqdes.RolePartiePrenante;
import ch.vd.unireg.reqdes.TransactionImmobiliere;
import ch.vd.unireg.reqdes.TypeInscription;
import ch.vd.unireg.reqdes.TypeRole;
import ch.vd.unireg.reqdes.UniteTraitement;
import ch.vd.unireg.reqdes.UniteTraitementDAO;

public abstract class AbstractEvenementReqDesProcessingTest extends BusinessTest {

	protected UniteTraitementDAO uniteTraitementDAO;
	protected EvenementReqDesDAO evenementReqDesDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		uniteTraitementDAO = getBean(UniteTraitementDAO.class, "reqdesUniteTraitementDAO");
		evenementReqDesDAO = getBean(EvenementReqDesDAO.class, "reqdesEvenementDAO");
	}

	protected EvenementReqDes addEvenementReqDes(InformationsActeur notaire, @Nullable InformationsActeur operateur, RegDate dateActe, String noMinute) {
		final EvenementReqDes evt = new EvenementReqDes();
		evt.setNotaire(notaire);
		evt.setOperateur(operateur);
		evt.setDateActe(dateActe);
		evt.setNumeroMinute(noMinute);
		evt.setXml("<tubidu/>");
		return evenementReqDesDAO.save(evt);
	}

	protected UniteTraitement addUniteTraitement(EvenementReqDes evt, EtatTraitement etat, @Nullable Date dateTraitement) {
		final UniteTraitement ut = new UniteTraitement();
		ut.setEvenement(evt);
		ut.setDateTraitement(dateTraitement);
		ut.setEtat(etat);
		return uniteTraitementDAO.save(ut);
	}

	protected PartiePrenante addPartiePrenante(UniteTraitement ut, String nom, String prenoms) {
		final PartiePrenante pp = new PartiePrenante();
		pp.setUniteTraitement(ut);
		pp.setNom(nom);
		pp.setPrenoms(prenoms);
		return hibernateTemplate.merge(pp);
	}

	protected TransactionImmobiliere addTransactionImmobiliere(EvenementReqDes evt, String description, ModeInscription modeInscription, TypeInscription typeInscription, int noOfsCommune) {
		final TransactionImmobiliere ti = new TransactionImmobiliere();
		ti.setEvenementReqDes(evt);
		ti.setDescription(description);
		ti.setModeInscription(modeInscription);
		ti.setTypeInscription(typeInscription);
		ti.setOfsCommune(noOfsCommune);
		return hibernateTemplate.merge(ti);
	}

	protected RolePartiePrenante addRole(PartiePrenante pp, TransactionImmobiliere transactionImmobiliere, TypeRole typeRole) {
		final RolePartiePrenante role = new RolePartiePrenante();
		role.setTransaction(transactionImmobiliere);
		role.setRole(typeRole);
		addRole(pp, role);
		return role;
	}

	protected void addRole(PartiePrenante pp, RolePartiePrenante role) {
		if (pp.getRoles() == null) {
			pp.setRoles(new HashSet<>());
		}
		pp.getRoles().add(role);
	}
}
