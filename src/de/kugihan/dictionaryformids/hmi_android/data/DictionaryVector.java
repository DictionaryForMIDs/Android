package de.kugihan.dictionaryformids.hmi_android.data;

import java.util.Collection;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

/**
 * Abstraction over a dictionary collection that propagates child events.
 */
public class DictionaryVector extends Observable implements Iterable<Dictionary> {

	private final Vector<Dictionary> dictionaries = new Vector<Dictionary>();

	private final Observer dictionaryObserver = new Observer() {
		@Override
		public void update(Observable observable, Object o) {
			setChanged();
			notifyObservers(o);
		}
	};

	public void add(int location, Dictionary dictionary) {
		dictionaries.add(location, dictionary);
		dictionary.addObserver(dictionaryObserver);
		setChanged();
		notifyObservers(dictionary);
	}

	public void addAll(Collection<? extends Dictionary> dictionaryCollection) {
		dictionaries.addAll(dictionaryCollection);
		for (Dictionary dictionary : dictionaryCollection) {
			dictionary.addObserver(dictionaryObserver);
		}
		setChanged();
		notifyObservers();
	}

	public Dictionary get(int location) {
		return dictionaries.get(location);
	}

	public void remove(int location) {
		final Dictionary dictionary = dictionaries.get(location);
		dictionaries.remove(location);
		dictionary.deleteObserver(dictionaryObserver);
		setChanged();
		notifyObservers(dictionary);
	}

	@Override
	public Iterator<Dictionary> iterator() {
		return dictionaries.iterator();
	}

	public int size() {
		return dictionaries.size();
	}

	public boolean isEmpty() {
		return dictionaries.isEmpty();
	}

	public Dictionary elementAt(int location) {
		return dictionaries.elementAt(location);
	}

	public Dictionary firstElement() {
		return dictionaries.firstElement();
	}
}