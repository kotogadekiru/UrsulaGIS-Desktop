package com.ursulagis.desktop.tasks;


import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

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
//import com.google.maps.GeoApiContext;
//import com.google.maps.GeocodingApi;
//import com.google.maps.errors.ApiException;
//import com.google.maps.model.GeocodingResult;

import gov.nasa.worldwind.geom.Position;

public class GoogleGeocodingHelper {
	private static String key ="AIzaSyBYcc2x7_tskwlltuImLmTx8W_j079l8Q8";
	private static String API_KEY="&key="+key;
	private static String GEOCODE_API_GOOGLE_URL ="https://maps.googleapis.com/maps/api/geocode/json";//?address=";
	//https://maps.googleapis.com/maps/api/geocode/json?address=1600+Amphitheatre+Parkway,+Mountain+View,+CA&key=YOUR_API_KEY
//	private static GeoApiContext context = new GeoApiContext().setApiKey(key);
//	public static Position obtenerPosition(String query){
//		GeocodingResult[] results = null;
//		try {
//			results = GeocodingApi.geocode(context,
//			    query).await();
//		} catch (ApiException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		if(results.length>0){
//		System.out.println(results[0].formattedAddress);
//		GeocodingResult res = results[0];
//		return Position.fromDegrees(res.geometry.location.lat, res.geometry.location.lng);
//		} else {
//			return null;
//		}
//	}
	
	public static Position obtenerPositionDirect(String query){
		GenericUrl url = new GenericUrl(GEOCODE_API_GOOGLE_URL);// "http://www.lanacion.com.ar");
		
		//url.appendRawPath(query);
		//url.appendRawPath(API_KEY);
		url.put("address", query);
		url.put("key", key);
		System.out.println("buscando la ubicacion de "+query+" con el url \n"+url);
		//https://maps.googleapis.com/maps/api/geocode/json?address=1600+Amphitheatre+Parkway,+Mountain+View,+CA&key=YOUR_API_KEY
		//https://maps.googleapis.com/maps/api/geocode/json?address=Pehuajo&key=AIzaSyBYcc2x7_tskwlltuImLmTx8W_j079l8Q8
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
			
//		    "lat" : 37.4224764,
//            "lng" : -122.0842499
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
	
	/*
{
   "results" : [
      {
         "address_components" : [
            {
               "long_name" : "Pehuaj�",
               "short_name" : "Pehuaj�",
               "types" : [ "locality", "political" ]
            },
            {
               "long_name" : "Pehuaj� Partido",
               "short_name" : "Pehuaj� Partido",
               "types" : [ "administrative_area_level_2", "political" ]
            },
            {
               "long_name" : "Buenos Aires Province",
               "short_name" : "Buenos Aires Province",
               "types" : [ "administrative_area_level_1", "political" ]
            },
            {
               "long_name" : "Argentina",
               "short_name" : "AR",
               "types" : [ "country", "political" ]
            }
         ],
         "formatted_address" : "Pehuaj�, Buenos Aires Province, Argentina",
         "geometry" : {
            "bounds" : {
               "northeast" : {
                  "lat" : -35.7909625,
                  "lng" : -61.8469892
               },
               "southwest" : {
                  "lat" : -35.8613171,
                  "lng" : -61.9405142
               }
            },
            "location" : {
               "lat" : -35.8107166,
               "lng" : -61.8987832
            },
            "location_type" : "APPROXIMATE",
            "viewport" : {
               "northeast" : {
                  "lat" : -35.7909625,
                  "lng" : -61.8469892
               },
               "southwest" : {
                  "lat" : -35.8613171,
                  "lng" : -61.9405142
               }
            }
         },
         "place_id" : "ChIJ86BrWCz4wJURA89cs7G_REg",
         "types" : [ "locality", "political" ]
      }
   ],
   "status" : "OK"
}
	 */

}
