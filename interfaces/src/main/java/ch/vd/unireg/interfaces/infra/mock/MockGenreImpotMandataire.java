package ch.vd.unireg.interfaces.infra.mock;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;

public class MockGenreImpotMandataire implements GenreImpotMandataire, Serializable {

	public static final MockGenreImpotMandataire GI = new MockGenreImpotMandataire("GI", "Gain immobilier");
	public static final MockGenreImpotMandataire DM = new MockGenreImpotMandataire("DM", "Droit de mutation");
	public static final MockGenreImpotMandataire SUCC = new MockGenreImpotMandataire("SUCC", "Succession");
	public static final MockGenreImpotMandataire DON = new MockGenreImpotMandataire("DON", "Donation");
	public static final MockGenreImpotMandataire IFONC = new MockGenreImpotMandataire("IFONC", "Impôt foncier");

	private static final long serialVersionUID = -9112721806126283476L;

	public static final MockGenreImpotMandataire[] ALL = buildAllMocks();

	@NotNull
	private static MockGenreImpotMandataire[] buildAllMocks() {
		final List<MockGenreImpotMandataire> mocks = new ArrayList<>();
		for (Field field : MockGenreImpotMandataire.class.getFields()) {
			if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
				if (MockGenreImpotMandataire.class.isAssignableFrom(field.getType())) {
					try {
						final MockGenreImpotMandataire value = (MockGenreImpotMandataire) field.get(null);
						if (value != null) {
							mocks.add(value);
						}
					}
					catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return mocks.toArray(new MockGenreImpotMandataire[0]);
	}

	private final String code;
	private final String libelle;

	private MockGenreImpotMandataire(String code, String libelle) {
		this.code = code;
		this.libelle = libelle;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getLibelle() {
		return libelle;
	}
}
