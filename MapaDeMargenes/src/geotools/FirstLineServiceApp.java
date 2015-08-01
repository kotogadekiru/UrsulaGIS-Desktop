package geotools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.stage.Stage;

public class FirstLineServiceApp extends Application {

	@Override
	public void start(Stage stage) throws Exception {
		FirstLineService service = new FirstLineService();
		service.setUrl("http://google.com");
		service.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

			@Override
			public void handle(WorkerStateEvent t) {
				System.out.println("done:" + t.getSource().getValue());
			}
		});
		service.start();
	}

	public static void main(String[] args) {
		launch();
	}

	private static class FirstLineService extends Service<String> {
		private StringProperty url = new SimpleStringProperty();

		public final void setUrl(String value) {
			url.set(value);
		}

		public final String getUrl() {
			return url.get();
		}

		public final StringProperty urlProperty() {
			return url;
		}

		@Override
        protected Task<String> createTask() {
            return new Task<String>() {
                @Override
                protected String call() {
                	String ret="";
                    try {
                   URL urll = new URL(getUrl());
                    	InputStreamReader isr = new InputStreamReader(urll.openStream()  );
                    	BufferedReader in = new BufferedReader(isr);   
                        ret= in.readLine();
                    } catch(Exception e){
                    	e.printStackTrace();
                    }
                    return ret;
               }
        };
    } //fin de createTask
	}
}