package ch.vd.uniregctb.reqdes;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CoreDAOTest;

public abstract class AbstractReqDesDAOTest extends CoreDAOTest {

	protected EvenementReqDes addEvenementReqDes(RegDate date, long noMinute, String visaNotaire, String nomNotaire, String prenomNotaire) {
		final InformationsActeur notaire = new InformationsActeur(visaNotaire, nomNotaire, prenomNotaire);
		final EvenementReqDes evt = new EvenementReqDes();
		evt.setXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?><bidon/>");
		evt.setDateActe(date);
		evt.setNumeroMinute(noMinute);
		evt.setNotaire(notaire);
		return hibernateTemplate.merge(evt);
	}
}
