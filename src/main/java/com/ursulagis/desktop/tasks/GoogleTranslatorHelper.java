package com.ursulagis.desktop.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.auth.Credentials;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.TranslateOptions.Builder;

import com.ursulagis.desktop.dao.config.Configuracion;
import gov.nasa.worldwind.geom.Position;
import com.ursulagis.desktop.gui.Messages;
import javafx.concurrent.Task;
/**
 * Clase que toma un archivo messages_[locale].properties y lo traduce al locale indicado usando google translator
 */
public class GoogleTranslatorHelper extends Task<File>{
	private static String key ="AIzaSyC6m54rSOpbe5Tar_b2O2XWGkxCn7BImnU";
	private static String project = "UrsulaGIS";
	private static String GEOCODE_API_GOOGLE_URL ="https://maps.googleapis.com/maps/api/geocode/json";//?address=";

	private ResourceBundle baseBoundle=null;
	private Locale outLocale=null;

	public GoogleTranslatorHelper(ResourceBundle _baseBoundle,Locale _outLocale) {
		this.baseBoundle=_baseBoundle;
		this.outLocale=_outLocale;
	}

	public static Position obtenerPositionDirect(String query){
		GenericUrl url = new GenericUrl(GEOCODE_API_GOOGLE_URL);
		url.put("address", query);
		url.put("key", key);
		System.out.println("buscando la traduccion de "+query+" con el url \n"+url);
		HttpResponse response = makeRequest(url);
		try {
			return 	parseGeoCodeResponse(response);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}


	}

	private static Position parseGeoCodeResponse(HttpResponse response) throws IOException {
		GenericJson content = response.parseAs(GenericJson.class);
		System.out.println("response content:\n"+content);

		//{"results":[{"address_components":[{"long_name":"Pehuaj�","short_name":"Pehuaj�","types":["locality","political"]},{"long_name":"Pehuaj� Partido","short_name":"Pehuaj� Partido","types":["administrative_area_level_2","political"]},{"long_name":"Buenos Aires Province","short_name":"Buenos Aires Province","types":["administrative_area_level_1","political"]},{"long_name":"Argentina","short_name":"AR","types":["country","political"]}],"formatted_address":"Pehuaj�, Buenos Aires Province, Argentina","geometry":{"bounds":{"northeast":{"lat":-35.7909625,"lng":-61.8469892},"southwest":{"lat":-35.8613171,"lng":-61.9405142}},"location":{"lat":-35.8107166,"lng":-61.8987832},"location_type":"APPROXIMATE","viewport":{"northeast":{"lat":-35.7909625,"lng":-61.8469892},"southwest":{"lat":-35.8613171,"lng":-61.9405142}}},"place_id":"ChIJ86BrWCz4wJURA89cs7G_REg","types":["locality","political"]}],"status":"OK"}
		ArrayMap<String,Object> data = (ArrayMap<String,Object>) content.getUnknownKeys();
		for(String key :data.keySet()){
			ArrayList<Object> val =(ArrayList<Object>) data.get(key);
			for(Object o:val){
				System.out.println("object: "+o);

				ArrayMap<String,Object> valMap = (ArrayMap<String,Object>) o;
				ArrayMap<String,Object> geometry = (ArrayMap<String, Object>)valMap.get("geometry");
				ArrayMap<String,Object> location = (ArrayMap<String, Object>)geometry.get("location");
				BigDecimal lat = (BigDecimal) location.get("lat");
				BigDecimal lng = (BigDecimal) location.get("lng");

				//			    "lat" : 37.4224764,
				//	            "lng" : -122.0842499
				System.out.println("lat: "+lat+ " lon: "+lng);
				return Position.fromDegrees(lat.doubleValue(), lng.doubleValue());
			}

		}
		return null;
	}

	/**
	 * metodo que ejecuta un request
	 * @param url
	 * @return HttResponse
	 */
	private static HttpResponse makeRequest(GenericUrl url){
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
		JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
					}
				});

		try {
			HttpRequest request = requestFactory.buildGetRequest(url);
			return request.execute();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}	
	}
	/**
	 * 
	 * @param s
	 * @return s traducido al outLocale
	 */
	private String traducir(String s) {
		 //TODO(developer): Uncomment these lines.
		// import com.google.cloud.translate.*;
		// Translate translate = TranslateOptions.getDefaultInstance().getService();
		TranslateOptions translateOptions = TranslateOptions.getDefaultInstance();
		
		Builder builder = translateOptions.toBuilder();
		builder.setTargetLanguage(outLocale.getLanguage());
		builder.setProjectId(project);
		
		Credentials creds = builder.getCredentials();
	
		//translateOptions.
		//Translate.translate();
		//Translation translation = translate.translate("�Hola Mundo!");
		//System.out.printf("Translated Text:\n\t%s\n", translation.getTranslatedText());
		return s;
	}

	@Override
	protected File call() {
		// TODO generate messages_[loc].properties File
		Configuracion config = Configuracion.getInstance();
		String fileName = config.ursulaGISFolder+"\\messages_"+outLocale.getLanguage()+".properties";
		System.out.println("writing file "+fileName);
		File ret = new File(fileName);
		//todo recorrer outlocale 
		Enumeration<String> keys = baseBoundle.getKeys();
		Stream<String> entries =((List<String>)Collections.list(keys))
				.parallelStream().map(k->k+"="+traducir(baseBoundle.getString(k)));
		try {
		FileWriter writer = new FileWriter(ret);
		entries.forEach(e->{
			try {
				writer.write(e+"\n");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}	
		});
		writer.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
		//traducir lo que viene despues del =
		//insertar la linea en ret
		return ret;
	}

	public static void main(String[] args) {
		Locale loc = new Locale("FR");
		ResourceBundle baseBoundle1 = Messages.getBoundle();
		GoogleTranslatorHelper t = new GoogleTranslatorHelper(baseBoundle1,loc);
		t.call();
	}

}



/**
  from deep_translator import GoogleTranslator

# Funci�n para traducir las l�neas que no han sido traducidas
def auto_translate_line(line):
    if "=" in line:
        key, value = line.split("=", 1)
        translated_value = translations.get(value.strip())
        if not translated_value:
            translated_value = GoogleTranslator(source='es', target='fr').translate(value.strip())
        return f"{key}={translated_value}\n"
    return line

# Re-traducir todo el contenido incluyendo las traducciones autom�ticas
translated_content_auto = ''.join([auto_translate_line(line) for line in content.splitlines()])

# Guardar el contenido completamente traducido en un nuevo archivo
translated_file_path_auto = '/mnt/data/messages_fr_auto.properties'
with open(translated_file_path_auto, 'w', encoding='ISO-8859-1') as file:
    file.write(translated_content_auto)

translated_file_path_auto

 */
