package ch.vd.uniregctb.evenement.civil.interne;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.evenement.civil.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurFactory;
import ch.vd.uniregctb.evenement.civil.EvenementCivilMessageCollector;
import ch.vd.uniregctb.type.TypeEvenementErreur;

public class MessageCollector extends EvenementCivilMessageCollector<MessageCollector.Msg> {

	public static final class Msg implements EvenementCivilErreur {

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

	private static final class MsgFactory extends EvenementCivilErreurFactory<Msg> {
		@Override
		protected Msg createErreur(String message, @Nullable Exception e, TypeEvenementErreur type) {
			return new Msg(buildActualMessage(message, e), type);
		}
	}

	public MessageCollector() {
		super(new MsgFactory());
	}
}
