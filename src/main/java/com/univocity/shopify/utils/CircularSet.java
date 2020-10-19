package com.univocity.shopify.utils;

import java.util.*;

public class CircularSet<T> implements Set<T> {

	private final Object[] circle;
	private int i;
	private final Set<T> elements;

	public CircularSet(int size) {
		this(size, new HashSet<T>(size));
	}

	public CircularSet(int size, Set<T> elementHolder) {
		this.elements = elementHolder;
		elementHolder.clear();
		this.circle = new Object[size];
		this.i = 0;
	}

	@Override
	public int size() {
		return elements.size();
	}

	@Override
	public boolean isEmpty() {
		return elements.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return elements.contains(o);
	}

	@Override
	public Iterator<T> iterator() {
		return elements.iterator();
	}

	@Override
	public Object[] toArray() {
		return elements.toArray();
	}

	@Override
	public <T1> T1[] toArray(T1[] a) {
		return elements.toArray(a);
	}

	@Override
	public boolean add(T t) {
		if (elements.add(t)) {
			i = i % circle.length;
			Object old = circle[i];
			if (old != null) {
				elements.remove(old);
			}
			circle[i++] = t;
			return true;
		}
		return false;
	}

	@Override
	public boolean remove(Object o) {
		return elements.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return elements.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		boolean modified = false;
		for (T e : c) {
			modified |= add(e);
		}
		return modified;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return elements.retainAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return elements.removeAll(c);
	}

	@Override
	public void clear() {
		Arrays.fill(circle, null);
		elements.clear();
	}
}
