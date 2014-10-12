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

	private int firstUnloadedDictionaryPosition() {
		for (int i = 0; i < dictionaries.size(); i++) {
			if (dictionaries.get(i).getFile() != null) {
				continue;
			}
			return i;
		}
		return dictionaries.size();
	}

	public void addAfterLoadedDictionaries(Dictionary dictionary) {
		int position = firstUnloadedDictionaryPosition();
		this.add(position, dictionary);
	}

	public void addEnd(Dictionary dictionary) {
		this.add(dictionaries.size(), dictionary);
	}

	public void addAll(Collection<? extends Dictionary> dictionaryCollection) {
		dictionaries.addAll(dictionaryCollection);
		for (Dictionary dictionary : dictionaryCollection) {
			dictionary.addObserver(dictionaryObserver);
		}
		setChanged();
		notifyObservers();
	}

	public void addAllFromIterable(Iterable<Dictionary> additionalDictionaries) {
		for (Dictionary dictionary : additionalDictionaries) {
			dictionary.addObserver(dictionaryObserver);
			dictionaries.add(dictionary);
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

	public void remove(Dictionary dictionary) {
		if (!dictionaries.contains(dictionary)) {
			dictionary = findMatchOrNull(dictionary);
		}
		if (dictionary == null) {
			return;
		}
		dictionaries.remove(dictionary);
		dictionary.deleteObserver(dictionaryObserver);
		setChanged();
		notifyObservers();
	}

	public void removeAll(Collection<? extends Dictionary> dictionaryCollection) {
		dictionaries.removeAll(dictionaryCollection);
		for (Dictionary dictionary : dictionaryCollection) {
			dictionary.deleteObserver(dictionaryObserver);
		}
		setChanged();
		notifyObservers();
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

	public boolean contains(Dictionary dictionaryToSearch) {
		Dictionary dictionary = findMatchOrNull(dictionaryToSearch);
		return dictionary != null;
	}

	public Dictionary findMatchOrNull(Dictionary dictionaryToSearch) {
		for (Dictionary dictionary : dictionaries) {
			if (dictionaryToSearch.equalsDictionary(dictionary)) {
				return dictionary;
			}
		}
		return null;
	}

	public Dictionary findMatchOrNull(DictionaryType type, String path) {
		for (Dictionary dictionary : dictionaries) {
			if (dictionary.equalsDictionary(type, path)) {
				return dictionary;
			}
		}
		return null;
	}
}
