package ch.vd.uniregctb.evenement.externe;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.xml.event.lr.event.v3.Evenement;
import ch.vd.unireg.xml.event.lr.event.v3.EvtListe;
import ch.vd.unireg.xml.event.lr.event.v3.Liste;
import ch.vd.uniregctb.common.XmlUtils;

public class EvtListeV3Connector implements EvenementExterneConnector<EvtListe> {

	@Override
	public EvenementExterne parse(EvtListe el) {
		if (isEvenementLR(el) && isEvenementQuittanceOuAnnulation(el)) {
			final QuittanceLR quittance = new QuittanceLR();
			quittance.setDateEvenement(XmlUtils.xmlcal2date(el.getDateEvenement()));
			quittance.setDateTraitement(DateHelper.getCurrentDate());
			final XMLGregorianCalendar dateDebut = el.getCaracteristiquesListe().getPeriodeDeclaration().getDateDebut();
			quittance.setDateDebut(XmlUtils.xmlcal2regdate(dateDebut));
			final XMLGregorianCalendar dateFin = el.getCaracteristiquesListe().getPeriodeDeclaration().getDateFin();
			quittance.setDateFin(XmlUtils.xmlcal2regdate(dateFin));
			quittance.setType(jms2core(el.getTypeEvenement()));
			final int numeroDebiteur = el.getCaracteristiquesDebiteur().getNumeroDebiteur();
			quittance.setTiersId((long) numeroDebiteur);
			return quittance;
		}
		else {
			return null;
		}
	}

	@Override
	public Class<EvtListe> getSupportedClass() {
		return EvtListe.class;
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/lr/evtListe-3.xsd");
	}

	private static boolean isEvenementQuittanceOuAnnulation(EvtListe el) {
		return el.getTypeEvenement() == Evenement.QUITTANCE || el.getTypeEvenement() == Evenement.ANNULATION;
	}

	private static boolean isEvenementLR(EvtListe el) {
		return el.getCaracteristiquesListe().getTypeListe() == Liste.LR;
	}

	private static TypeQuittance jms2core(Evenement typeEvenement) {
		switch (typeEvenement) {
		case ANNULATION:
			return TypeQuittance.ANNULATION;
		case QUITTANCE:
			return TypeQuittance.QUITTANCEMENT;
		default:
			throw new IllegalArgumentException("Type d'événement non supporté = [" + typeEvenement + "]");
		}
	}
}
