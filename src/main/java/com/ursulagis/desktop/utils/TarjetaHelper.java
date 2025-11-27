package com.ursulagis.desktop.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.geotools.data.store.ContentFeatureCollection;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.MultipartContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;

import com.ursulagis.desktop.dao.config.Configuracion;

public class TarjetaHelper {
	private static final String UUID_TARJETA = "UuidTarjeta";
	private static final String NONEFOUND = "nonefound";
	public static final String BASE_URL = "https://www.ursulagis.com";
	//public static final String BASE_URL = "http://localhost:5000";	
	//public static final String BASE_URL = "https://sheltered-mesa-69562-dev-514e4d674053.herokuapp.com";
	
	public static String REGISTRAR_TARJETA_URL=BASE_URL+"/api/file_server/registrar_tarjeta/";	
	public static String REGISTRAR_ARCHIVO_URL=BASE_URL+"/api/file_server/upload_file/";
	
	public static void initTarjeta() {	//ok funciona	
		//TODO registrar una tarjeta si no tiene una actualmente
		//1 llamar a REGISTRAR_TARJETA_URL con post param "token"=userNumber
		//2 obtener uuid de la tarjeta
		//3 guardar el uuid en config		
		String uuidTarjeta = getUuidTarjeta();
		if(uuidTarjeta.equals(NONEFOUND)) {
			GenericUrl url = new GenericUrl(REGISTRAR_TARJETA_URL);	
			url.set("token", Configuracion.getInstance().getPropertyOrDefault("USER", NONEFOUND));
			System.out.println("llamand la url "+url.build());
			//llamand la url http://www.ursulagis.com/api/file_server/registrar_tarjeta/?:token=749,586
			byte[] bytes=null;
			try {
				bytes = "register tarjeta".getBytes("UTF8");
			} catch (UnsupportedEncodingException e) {				
				e.printStackTrace();
			}
			final HttpContent content = new ByteArrayContent("application/json", bytes );
			HttpResponse res = makeJsonPostRequest(url, content);
			try {
				//GenericJson resContent = res.parseAs(GenericJson.class);
				String tarjetaUuid = res.parseAsString();
				res.disconnect();
				Configuracion config = Configuracion.getInstance();
				config.setProperty(UUID_TARJETA, tarjetaUuid);
				System.out.println("cree la tarjeta "+tarjetaUuid);
				config.save();
			} catch (IOException e) {			
				e.printStackTrace();
			}
			
		}		
	}
	
	public static void uploadFile(File f,String destUrl) {
		//hacer un put a REGISTRAR_ARCHIVO_URL 
		GenericUrl url = new GenericUrl(REGISTRAR_ARCHIVO_URL);	
		//agregar al param token=userNumber, uuid=uuidTarjeta, url="urlDestino"
		url.put("token", Configuracion.getInstance().getPropertyOrDefault("USER", NONEFOUND));
		url.put("uuid", getUuidTarjeta());
		url.put("url", destUrl);
		
		//byte[] byteArray = FileHelper.fileToByteArray(f);		
		
		//final HttpContent content = new ByteArrayContent("application/octet-stream", byteArray );
		try {
		FileInputStream fin = new FileInputStream(f);
		MultipartContent.Part part = new MultipartContent.Part()
	            .setContent(new InputStreamContent("application/octet-stream", fin))
	            .setHeaders(new HttpHeaders().set(
	                    "Content-Disposition",
	                    String.format("form-data; name=\"file\"; filename=\"%s\"", f.getName()) // TODO: escape fileName?
	            ));
	    MultipartContent content = new MultipartContent()
	            .setMediaType(new HttpMediaType("multipart/form-data")
	            .setParameter("boundary", UUID.randomUUID().toString()))
	            .addPart(part);
	    
		HttpResponse response = makeBinaryPostRequest(url,content,f.getName());
		if(response != null) {
			System.out.println("file uploaded to "+f.getName()+" "+response.parseAsString());
		}else {
			System.err.println("no se pudo insertar la tarjeta el response fue null");
		}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String getUuidTarjeta() {		
		return Configuracion.getInstance().getPropertyOrDefault(UUID_TARJETA, NONEFOUND);
	}
	
	/**
	 * metodo que ejecuta un request json
	 * @param url
	 * @return HttResponse
	 */
	private static HttpResponse makeBinaryPostRequest(GenericUrl url,HttpContent req_content,String fileName){
		HttpResponse response = null;
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

		//JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						//request.setParser(new JsonObjectParser(JSON_FACTORY));
						request.setReadTimeout(0);
						request.setConnectTimeout(0);
						//en el server va response.type("application/octet-stream");
						HttpHeaders headers = request.getHeaders();//USER=693,468
						headers.set("USER", Configuracion.getInstance().getPropertyOrDefault("USER", NONEFOUND));
						String fileLength="0";
						try {
							fileLength = Long.toString(req_content.getLength());
						} catch (IOException e) {							
							e.printStackTrace();
						}
						headers.set("Content-Disposition", "attachment; filename=\""+fileName+"\"; size="+fileLength);

					}
				});//java.net.SocketException: Address family not supported by protocol family: connect

		try {
			HttpRequest request = requestFactory.buildPostRequest(url, req_content);//(url);
			//request.getHeaders().set("USER", getUser());
			response= request.execute();
		} catch (Exception e) {			
			e.printStackTrace();
			return null;// si no se pudo hacer el request devuelvo null. puede ser por falta de conexion u otra cosa
		}	
		return response;
	}
	
	/**
	 * metodo que ejecuta un request json
	 * @param url
	 * @return HttResponse
	 */
	private static HttpResponse makeJsonPostRequest(GenericUrl url,HttpContent req_content){
		HttpResponse response = null;
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

		JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
						request.setReadTimeout(0);
						request.setConnectTimeout(0);
						HttpHeaders headers = request.getHeaders();//USER=693,468
						headers.set("USER", Configuracion.getInstance().getPropertyOrDefault("USER", NONEFOUND));
						//headers.set("token", Configuracion.getInstance().getPropertyOrDefault("USER", NONEFOUND));
						//url.set(":token", Configuracion.getInstance().getPropertyOrDefault("USER", NONEFOUND));

					}
				});//java.net.SocketException: Address family not supported by protocol family: connect

		try {
			HttpRequest request = requestFactory.buildPostRequest(url, req_content);//(url);
			//request.getHeaders().set("USER", getUser());
			response= request.execute();
		} catch (Exception e) {			
			e.printStackTrace();
			return null;// si no se pudo hacer el request devuelvo null. puede ser por falta de conexion u otra cosa
		}	
		return response;
	}
}
