package misc;

import java.util.Observable;
import java.util.Observer;

import main.Message;

@SuppressWarnings("deprecation")
public interface MyObserver extends Observer {
	default void update(Observable obs, Object obj) {
		this.update((MyObservable)obs, (Message)obj);
	}
	
	void update(MyObservable mo, Message msg);

}
