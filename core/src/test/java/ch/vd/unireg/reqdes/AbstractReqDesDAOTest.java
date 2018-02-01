package ch.vd.unireg.reqdes;

import java.util.Date;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CoreDAOTest;

public abstract class AbstractReqDesDAOTest extends CoreDAOTest {

	protected EvenementReqDes addEvenementReqDes(RegDate date, Long noAffaire, String noMinute, String visaNotaire, String nomNotaire, String prenomNotaire) {
		final InformationsActeur notaire = new InformationsActeur(visaNotaire, nomNotaire, prenomNotaire);
		final EvenementReqDes evt = new EvenementReqDes();
		evt.setXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?><bidon/>");
		evt.setDateActe(date);
		evt.setNumeroMinute(noMinute);
		evt.setNotaire(notaire);
		evt.setNoAffaire(noAffaire);
		return hibernateTemplate.merge(evt);
	}

	protected UniteTraitement addUniteTraitement(EvenementReqDes evt, EtatTraitement etat, @Nullable Date dateTraitement) {
		final UniteTraitement ut = new UniteTraitement();
		ut.setEvenement(evt);
		ut.setEtat(etat);
		ut.setDateTraitement(dateTraitement);
		return hibernateTemplate.merge(ut);
	}

	protected PartiePrenante addPartiePrenante(UniteTraitement ut, String nom, String prenoms) {
		final PartiePrenante pp = new PartiePrenante();
		pp.setUniteTraitement(ut);
		pp.setNom(nom);
		pp.setPrenoms(prenoms);
		return hibernateTemplate.merge(pp);
	}
}
