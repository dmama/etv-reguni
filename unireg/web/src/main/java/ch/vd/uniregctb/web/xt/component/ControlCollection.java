package ch.vd.uniregctb.web.xt.component;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.util.Assert;
import org.springmodules.xt.ajax.component.Component;

public class ControlCollection extends ArrayList<Control> {

	private static final long serialVersionUID = 3666888894650460791L;

	private final Control owner;

	public ControlCollection(Control owner) {
		Assert.notNull(owner);
		this.owner = owner;
	}

	/**
	 * @return the owner
	 */
	public Control getOwner() {
		return owner;
	}

	@Override
	public boolean add(Control o) {
		Assert.notNull(o);
		boolean status = super.add(o);
		if (status) {
			owner.addedControl(o);
		}
		return status;
	}

	@Override
	public void add(int index, Control element) {
		Assert.notNull(element);
		super.add(index, element);
		owner.addedControl(element);
	}

	public void add(int index, Component element) {
		Assert.notNull(element);
		Control control = new WrapperControlComponent(element);
		super.add(index, control);
		owner.addedControl(control);
	}

	@Override
	public boolean addAll(Collection<? extends Control> c) {
		boolean status =  super.addAll(c);
		if (status) {
			for (Control control : c) {
				owner.addedControl(control);
			}
		}
		return status;
	}

	@Override
	public boolean remove(Object o) {
		Control ctrl = (Control) o;
		boolean status = super.remove(ctrl);
		if (status) {
			owner.removedControl(ctrl);
		}
		return status;
	}

	@Override
	public Control remove(int index) {
		Control ctrl = super.remove(index);
		if (ctrl != null) {
			owner.removedControl(ctrl);
		}
		return ctrl;
	}

	@Override
	public boolean removeAll(Collection<?> c) {

		return super.removeAll(c);
	}

}
