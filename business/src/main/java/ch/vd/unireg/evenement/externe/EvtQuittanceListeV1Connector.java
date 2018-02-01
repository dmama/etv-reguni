package ch.vd.unireg.evenement.externe;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.xml.event.lr.quittance.v1.EvtQuittanceListe;
import ch.vd.unireg.xml.event.lr.quittance.v1.Liste;
import ch.vd.unireg.common.XmlUtils;

public class EvtQuittanceListeV1Connector implements EvenementExterneConnector<EvtQuittanceListe> {

	@Override
	public EvenementExterne parse(EvtQuittanceListe eq) {
		if (isEvenementLR(eq)) {
			final QuittanceLR quittance = new QuittanceLR();
			quittance.setDateEvenement(XmlUtils.xmlcal2date(eq.getTimestampEvtQuittance()));
			quittance.setDateTraitement(DateHelper.getCurrentDate());
			final XMLGregorianCalendar dateDebut = eq.getIdentificationListe().getPeriodeDeclaration().getDateDebut();
			quittance.setDateDebut(XmlUtils.xmlcal2regdate(dateDebut));
			final XMLGregorianCalendar dateFin = eq.getIdentificationListe().getPeriodeDeclaration().getDateFin();
			quittance.setDateFin(XmlUtils.xmlcal2regdate(dateFin));
			quittance.setType(TypeQuittance.valueOf(eq.getTypeEvtQuittance().toString()));
			final int numeroDebiteur = eq.getIdentificationListe().getNumeroDebiteur();
			quittance.setTiersId((long) numeroDebiteur);
			return quittance;
		}
		else {
			return null;
		}
	}

	@Override
	public Class<EvtQuittanceListe> getSupportedClass() {
		return EvtQuittanceListe.class;
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/lr/evtQuittanceListe-v1.xsd");
	}

	private static boolean isEvenementLR(EvtQuittanceListe event) {
		final Liste type = event.getIdentificationListe().getTypeListe();
		return type == Liste.LR;
	}
}
