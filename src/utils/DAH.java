package utils;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.util.Callback;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import dao.Poligono;
import dao.config.*;

public class DAH {
	private static final String APPDATA = "APPDATA";
	private static final String OBJECTDB_DB_MONITORES_ODB = "$ursulaGIS.odb";
	private static EntityManager em = null;
	static EntityTransaction transaction=null;

	public static EntityManager em(){
		if(em == null){
			String currentUsersHomeDir =System.getenv(APPDATA);
			//	System.out.println("obtuve la direccion de appData : "+currentUsersHomeDir);
				//obtuve la direccion de appData : C:\Users\quero\AppData\Roaming
			String ursulaGISFolder = currentUsersHomeDir + File.separator + Configuracion.URSULA_GIS_APPDATA_FOLDER;
			String  db_url = ursulaGISFolder + File.separator + OBJECTDB_DB_MONITORES_ODB;		
			System.out.println("abriendo la base de datos de: "+db_url);
			EntityManagerFactory emf =
					Persistence.createEntityManagerFactory(db_url);		     
			
			em = emf.createEntityManager();
		}
		return em;
	}



	public static void save(Object entidad) {
	
		EntityManager em = em();
		if(DAH.transaction == null){
			//	DAH.transaction = em.getTransaction();
			try{
			em.getTransaction().begin();		
			em.persist(entidad);			
			em.getTransaction().commit();
			}catch(javax.persistence.RollbackException rbe){
				em.getTransaction().begin();		
				em.merge(entidad);
				em.getTransaction().commit();
				
			}
		} else{
			em.persist(entidad);	
		}

	}
	
	public static void remove(Object entidad) {
		EntityManager em = em();
		if(DAH.transaction == null){
			//	DAH.transaction = em.getTransaction();
			em.getTransaction().begin();		
			em.remove(entidad);			
			em.getTransaction().commit();
		} else{
			em.remove(entidad);	
		}

	}

	public static List<Establecimiento> getAllEstablecimientos(EntityManager em) {
		TypedQuery<Establecimiento> query = em.createNamedQuery(
				Establecimiento.FIND_ALL, Establecimiento.class);
		List<Establecimiento> results = query.getResultList();
		return results;
	}
	
	public static Establecimiento getEstablecimiento(String establecimientoName) throws Exception {
	//	EntityManager em = em();
		TypedQuery<Establecimiento> query = em().createNamedQuery(
				Establecimiento.FIND_NAME, Establecimiento.class);
		query.setParameter(0, establecimientoName);
		
		Establecimiento result = null;
		if(query.getResultList().size()>0){
			result = query.getSingleResult();
		} else {
			result = new Establecimiento(establecimientoName);
			DAH.save(result);
		}
		return result;
	}

	public static List<Establecimiento> getAllEstablecimientos() {
		return getAllEstablecimientos(em());
	}

	/**
	 * 
	 * @param periodoName
	 * @return el periodo existente en la base de datos o crea uno nuevo con ese nombre y lo devuelve
	 * @throws Exception 
	 */
	public static Campania getCampania(String periodoName) throws Exception {	
	//	EntityManager em = em();
		TypedQuery<Campania> query = em().createNamedQuery(
				Campania.FIND_NAME, Campania.class);
		query.setParameter(0, periodoName);
//		TypedQuery<Campania> query =
//				em.createQuery("SELECT p FROM Campania p where p.nombre like '"+periodoName+"'", Campania.class);
		Campania result = null;
		if(query.getResultList().size()>0){
			result = query.getSingleResult();
		} else {
			result = new Campania(periodoName);
			DAH.save(result);
		}
		return result;
	}



	/**
	 * 
	 * @param cultivoName
	 * @return el producto existente en la base de datos o crea uno nuevo con ese nombre y lo devuelve
	 */
	public static Cultivo getCultivo(String cultivoName) {	
//		EntityManager em = em();
//		TypedQuery<Producto> query =
//				em.createQuery("SELECT p FROM Producto p where p.nombre like '"+cultivoName+"'", Producto.class);

		TypedQuery<Cultivo> query = em().createNamedQuery(
				Cultivo.FIND_NAME, Cultivo.class);
		query.setParameter(0, cultivoName);
		
		Cultivo result = null;
		if(query.getResultList().size()>0){
			result = query.getSingleResult();
		} else {
			result = new Cultivo(cultivoName);
			DAH.save(result);
		}
		return result;
	}

	public static List<Cultivo> getAllCultivos(EntityManager em) {
		  TypedQuery<Cultivo> query =
				  em.createNamedQuery(Cultivo.FIND_ALL, Cultivo.class);
			  List<Cultivo> results = query.getResultList();
			  if(results.size()==0){
					Cultivo.cultivos.values().forEach((d)->DAH.save(d));
					results = query.getResultList();
					//results.addAll(Cultivo.cultivos.values());
				}
			  return results;
	}
	
	public static List<Poligono> getAllPoligonos() {
		  TypedQuery<Poligono> query =
				  em().createNamedQuery(Poligono.FIND_ALL, Poligono.class);
			  List<Poligono> results = query.getResultList();
			//  closeEm();
		return results;
	}



	private static void closeEm() {
		em.close();
		  em=null;
	}
	
	public static List<Cultivo> getAllCultivos() {
		return getAllCultivos(em());
	}

	public static List<Empresa> getAllEmpresas() {
		TypedQuery<Empresa> query = em().createNamedQuery(
				Empresa.FIND_ALL, Empresa.class);
		List<Empresa> results = query.getResultList();
		
		return results;
	}



	public static List<Lote> getAllLotes() {
		TypedQuery<Lote> query = em().createNamedQuery(
				Lote.FIND_ALL, Lote.class);
		List<Lote> results = query.getResultList();
		
		return results;
	}



	public static List<Campania> getAllCampanias() {
		TypedQuery<Campania> query = em().createNamedQuery(
				Campania.FIND_ALL, Campania.class);
		List<Campania> results = query.getResultList();
		
		return results;
	}



	public static List<Fertilizante> getAllFertilizantes() {
		TypedQuery<Fertilizante> query = em().createNamedQuery(
				Fertilizante.FIND_ALL, Fertilizante.class);
		List<Fertilizante> results = query.getResultList();
		if(results.size()==0){
			Fertilizante.fertilizantes.values().forEach((d)->DAH.save(d));
			results.addAll(Fertilizante.fertilizantes.values());
		}
		
		return results;
	}



	public static List<Agroquimico> getAllAgroquimicos() {
		TypedQuery<Agroquimico> query = em().createNamedQuery(
				Agroquimico.FIND_ALL, Agroquimico.class);
		List<Agroquimico> results = query.getResultList();
		if(results.size()==0){
			Agroquimico.agroquimicos.values().forEach((d)->{
				DAH.save(d);	
			});
			results = query.getResultList();
			//results.addAll(Fertilizante.fertilizantes.values());
		}
		
		return results;
	}


	public static List<Semilla> getAllSemillas() {
		TypedQuery<Semilla> query = em().createNamedQuery(
				Semilla.FIND_ALL, Semilla.class);
		List<Semilla> results = query.getResultList();
		if(results.size()==0){
			Semilla.semillas.values().forEach((d)->{
				System.out.println("guardando semilla default "+d);
		//	DAH.save(d.getCultivo());	
			DAH.save(d);
			});
			results = query.getResultList();
			//results.addAll(Fertilizante.fertilizantes.values());
		}
		
		return results;
	}




	//	public static void main(String[] args) throws Exception {
	//	       // Open a database connection
	//     // (create a new database if it doesn't exist yet):
	//     EntityManagerFactory emf =
	//         Persistence.createEntityManagerFactory("$objectdb/db/monitores.odb");
	//     
	//   
	//     EntityManager em = emf.createEntityManager();
	//
	//     // Store 1000 Point objects in the database:
	//     em.getTransaction().begin();
	//     
	//     for (int i = 0; i < 10; i++) {
	//         Producto p = new Producto("Producto "+i,100.0- i*10, i*10-100.0);
	//         em.persist(p);
	//     }
	// 
	//     
	//     Establecimiento establecimiento = new Establecimiento("La Tablada");
	//     em.persist(establecimiento);
	//     
	//     em.getTransaction().commit();
	//     
	//     //creo un monitor
	//     em.getTransaction().begin();
	//     Periodo periodo = new Periodo("01-07-2014");
	//     em.persist(periodo);
	//     
	//     Monitor mon = new Monitor(establecimiento, periodo);
	//     em.persist(mon);
	//
	//     List<Producto> productos = DAH.getAllProducts(em);
	//     
	//     for(Producto producto : productos){
	//     	Suplementacion suple = new Suplementacion(mon, producto);//, 20.5, null, null, null, null);
	//     	suple.setCantidadEstaca(20.5);
	//     	suple.setCantidadRecria(39700.0);
	//     	suple.setCantidadVo(0.0);
	//     	suple.setCantidadVs(50.3);
	//     	suple.setPrecio(3.25);   
	//     	
	//     	 em.persist(suple);
	//     }
	//     
	//     
	//     em.getTransaction().commit();
	//
	//     // Find the number of Point objects in the database:
	//     Query q1 = em.createQuery("SELECT COUNT(p) FROM Suplementacion p");
	//     //System.out.println("Cantidad de Suplementaciones: " + q1.getSingleResult());
	//
	//     // Find the average X value:
	////     Query q2 = em.createQuery("SELECT AVG(p.x) FROM Establecimiento p");
	////     System.out.println("Average X: " + q2.getSingleResult());
	//
	//     // Retrieve all the Point objects from the database:
	//     TypedQuery<Establecimiento> query =
	//         em.createQuery("SELECT p FROM Establecimiento p", Establecimiento.class);
	//     
	//     List<Establecimiento> results = query.getResultList();
	//     for (Establecimiento p : results) {
	//         System.out.println(p);
	//     }
	//
	//     // Close the database connection:
	//     em.close();
	//     emf.close();
	//
	//	}

	





	

}
