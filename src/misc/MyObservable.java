package misc;

import java.util.Observable;

import main.Message;

@SuppressWarnings("deprecation")
public class MyObservable extends Observable {
	public void notifyObservers(Message m) {
		super.setChanged();
		super.notifyObservers(m);
	}
	
	public void addObserver(MyObserver mo) {
		super.addObserver(mo);
	}
}
