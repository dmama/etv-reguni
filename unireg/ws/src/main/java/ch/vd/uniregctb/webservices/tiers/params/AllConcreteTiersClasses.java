package ch.vd.uniregctb.webservices.tiers.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers.Debiteur;
import ch.vd.uniregctb.webservices.tiers.DebiteurHisto;
import ch.vd.uniregctb.webservices.tiers.MenageCommun;
import ch.vd.uniregctb.webservices.tiers.MenageCommunHisto;
import ch.vd.uniregctb.webservices.tiers.PersonnePhysique;
import ch.vd.uniregctb.webservices.tiers.PersonnePhysiqueHisto;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AllConcreteTiersClasses")
public class AllConcreteTiersClasses {
	PersonnePhysique pp;
	PersonnePhysiqueHisto pp_histo;
	Debiteur deb;
	DebiteurHisto deb_histo;
	MenageCommun mc;
	MenageCommunHisto mc_histo;
}