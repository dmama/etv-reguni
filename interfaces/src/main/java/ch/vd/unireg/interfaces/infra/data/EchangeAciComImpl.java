package ch.vd.unireg.interfaces.infra.data;

import java.util.List;
import java.util.stream.Collectors;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.xml.XmlUtils;

public class EchangeAciComImpl implements EchangeAciCom {
	private RegDate dateDebut;
	private RegDate dateFin;
	private TypeCommunication typeCommunication;
	private SupportEchange supportEchange;

	private EchangeAciComImpl(RegDate dateDebut, RegDate dateFin, TypeCommunication typeCommunication, SupportEchange supportEchange) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeCommunication = typeCommunication;
		this.supportEchange = supportEchange;
	}

	public static List<EchangeAciCom> get(List<ch.vd.fidor.xml.colladm.v1.EchangeAciCom> echangesAciCom) {
		return echangesAciCom.stream()
				.map(aux -> new EchangeAciComImpl(XmlUtils.cal2regdate(aux.getDateDebut()), XmlUtils.cal2regdate(aux.getDateFin()),
				                                  aux.getTypeCommunication() != null ? TypeCommunication.getTypeCommunication(aux.getTypeCommunication()) : null,
				                                  aux.getSupportEchange() != null ? SupportEchange.getSupportEchange(aux.getSupportEchange()) : null))
				.collect(Collectors.toList());
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public TypeCommunication getTypeCommunication() {
		return typeCommunication;
	}

	@Override
	public SupportEchange getSupportEchange() {
		return supportEchange;
	}
}
