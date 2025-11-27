package com.ursulagis.desktop.api;

import com.google.gson.JsonElement;

import lombok.Data;

@Data
public class StandardResponse {
    private StatusResponse status;
    private String message;
    private JsonElement data;
     
    public StandardResponse() {
    	
    }
    
    public StandardResponse(StatusResponse status) {
    	this.status=status;
        // ...
    }
    
    public StandardResponse(StatusResponse status, String message) {
    	this.status=status;
    	this.message=message;
        // ...
    }
    public StandardResponse(StatusResponse status, JsonElement data) {
    	this.status=status;
    	this.data=data;
        // ...
    }
     
    // getters and setters
	public enum StatusResponse {
	    SUCCESS ("Success"),
	    ERROR ("Error");
	    private String status=null; 
	    
		private StatusResponse(String status) {
			this.status=status;
		}      
	    // constructors, getters
	}
	
}
