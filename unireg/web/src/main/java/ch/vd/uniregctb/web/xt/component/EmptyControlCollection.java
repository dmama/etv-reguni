package ch.vd.uniregctb.web.xt.component;

public class EmptyControlCollection extends ControlCollection
{
	private static final long serialVersionUID = -2029329477050643816L;

	public EmptyControlCollection (Control owner)
	{
		super(owner);
	}


	@Override
	public boolean add (Control child)
	{
		throw new RuntimeException(String.format("Control '%s' does not allow children.", getOwner().getId()));
	}

	public void addAt (int index, Control child)
	{
		throw new RuntimeException(String.format ("Control '%s' does not allow children.", getOwner().getId()));
	}
}