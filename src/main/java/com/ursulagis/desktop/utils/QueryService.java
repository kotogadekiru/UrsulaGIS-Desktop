package com.ursulagis.desktop.utils;

import java.util.List;

import javax.persistence.TypedQuery;




import javafx.concurrent.Service;
import javafx.concurrent.Task;


public class QueryService<T> extends Service<List<T>> {
	TypedQuery<T> q =null;
	int first =0;
	 public final void setQuery(TypedQuery<T> query){
		 q=query;//tiene que ser un nuevo query porque sino cuando configuro el first se modifica en todas
	 }
	 
	 public final void setFirst(int f){
		first=f;
	 }
	 
	@Override
	protected Task<List<T>> createTask() {
		 return new Task<List<T>>() {
                protected List<T> call()    {                
                	List<T> result = null;
                   q.setFirstResult(first);
                   result = q.getResultList();      
           		
                        return result;
                }
            };
	}

}
