package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Raphaël Marmier, 2016-06-13, <raphael.marmier@vd.ch>
 */
public class PublicationFOSC implements Serializable {

	private static final long serialVersionUID = 27174353644323034L;

	private final RegDate date;
	private final String numero;
	private final String texte;

	public PublicationFOSC(@NotNull RegDate date, @NotNull String numero, @NotNull String texte) {
		this.date = date;
		this.numero = numero;
		this.texte = texte;
	}

	public RegDate getDate() {
		return date;
	}

	public String getNumero() {
		return numero;
	}

	public String getTexte() {
		return texte;
	}
}
