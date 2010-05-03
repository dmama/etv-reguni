package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="Canton")
@XmlEnum(String.class)
public enum Canton {

	ZH(1, "Zurich"),
	BE(2, "Berne"),
	LU(3, "Lucerne"),
	UR(4, "Uri"),
	SZ(5, "Schwyz"),
	OW(6, "Obwald"),
	NW(7, "Nidwald"),
	GL(8, "Glaris"),
	ZG(9, "Zoug"),
	FR(10, "Fribourg"),
	SO(11, "Soleure"),
	BS(12, "Bâle-Ville"),
	BL(13, "Bâle-Campagne"),
	SH(14, "Schaffhouse"),
	AR(15, "Appenzell Rh.-Ext."),
	AI(16, "Appenzell Rh.-Int."),
	SG(17, "Saint-Gall"),
	GR(18, "Grisons"),
	AG(19, "Argovie"),
	TG(20, "Thurgovie"),
	TI(21, "Tessin"),
	VD(22, "Vaud"),
	VS(23, "Valais"),
	NE(24, "Neuchâtel"),
	GE(25, "Genève"),
	JU(26, "Jura");

	private int noOfS;

	private String nom;

	private Canton(int noOfs, String nom){
		this.noOfS=noOfs;
		this.nom=nom;
	}

	public int getNoOfS() {
		return noOfS;
	}

	public String getNom() {
		return nom;
	}
}
