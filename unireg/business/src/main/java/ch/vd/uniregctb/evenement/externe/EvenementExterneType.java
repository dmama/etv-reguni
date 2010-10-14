package ch.vd.uniregctb.evenement.externe;

import java.util.ArrayList;


import ch.vd.fiscalite.taxation.evtQuittanceListeV1.QuittanceType;
import ch.vd.schema.registreCivil.x20070914.evtRegCivil.EvtRegCivilDocument;

public enum EvenementExterneType {

	Quittancement(EmmetteurType.ImpotSource, QuittanceType.class),
	CivilUnitaire(EmmetteurType.RegistreCivil, EvtRegCivilDocument.EvtRegCivil.class);

	private Class<?> evenementRootClass;

	private EmmetteurType emmetteur;

	private EvenementExterneType( EmmetteurType emmetteur,Class<?> evenementRootClass) {
		this.emmetteur = emmetteur;
		this.evenementRootClass = evenementRootClass;
	}

	/**
	 * @return the emmetteur
	 */
	public EmmetteurType getEmmetteur() {
		return emmetteur;
	}


	public static EvenementExterneType[] getEvenementExterneType(EmmetteurType type) {
		EvenementExterneType[] e =EvenementExterneType.values();
		ArrayList<EvenementExterneType> list = new ArrayList<EvenementExterneType>();
		for (EvenementExterneType ev : e) {
			if ( ev.getEmmetteur().equals(type)){
				list.add(ev);
			}
		}
		return list.toArray(new EvenementExterneType[list.size()]);
	}

	/**
	 * @return the evenementRootClass
	 */
	public Class<?> getEvenementRootClass() {
		return evenementRootClass;
	}

	public int getOrdinal() {
		return this.ordinal();
	}

	public String getName() {
		return this.name();
	}
}
