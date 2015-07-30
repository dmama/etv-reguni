package ch.vd.uniregctb.evenement.civil.interne;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.evenement.civil.EvenementCivilMessageCollector;
import ch.vd.uniregctb.evenement.common.EvenementErreur;
import ch.vd.uniregctb.evenement.common.EvenementRegistreErreurFactory;
import ch.vd.uniregctb.type.TypeEvenementErreur;

public class MessageCollector extends EvenementCivilMessageCollector<MessageCollector.Msg> {

	public static final class Msg implements EvenementErreur {

		private final String msg;
		private final TypeEvenementErreur type;

		public Msg(String msg, TypeEvenementErreur type) {
			this.msg = msg;
			this.type = type;
		}

		@Override
		public String getMessage() {
			return msg;
		}

		@Override
		public String getCallstack() {
			return null;
		}

		@Override
		public TypeEvenementErreur getType() {
			return type;
		}
	}

	private static final class MsgFactory extends EvenementRegistreErreurFactory<Msg> {
		@Override
		protected Msg createErreur(String message, @Nullable Exception e, TypeEvenementErreur type) {
			return new Msg(buildActualMessage(message, e), type);
		}
	}
	
	private static final MsgFactory ERREUR_FACTORY = new MsgFactory();

	public MessageCollector() {
		super(ERREUR_FACTORY);
	}
}
