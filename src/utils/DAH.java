package utils;

import static org.eclipse.persistence.config.PersistenceUnitProperties.CREATE_OR_EXTEND;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DDL_GENERATION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DDL_GENERATION_MODE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.ECLIPSELINK_PERSISTENCE_UNITS;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.TARGET_DATABASE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.TARGET_SERVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.TRANSACTION_TYPE;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.eclipse.persistence.config.TargetServer;

import api.OrdenPulverizacion;
import api.OrdenSiembra;
import dao.Labor;
import dao.Ndvi;
import dao.Poligono;
import dao.config.Agroquimico;
import dao.config.Asignacion;
import dao.config.Campania;
import dao.config.Configuracion;
import dao.config.Cultivo;
import dao.config.Empresa;
import dao.config.Establecimiento;
import dao.config.Fertilizante;
import dao.config.Lote;
import dao.config.Semilla;
import dao.cosecha.CosechaLabor;
import dao.ordenCompra.OrdenCompra;
import dao.ordenCompra.Producto;
import dao.ordenCompra.ProductoLabor;
import dao.recorrida.Recorrida;
import gui.JFXMain;
import sun.security.krb5.Config;

public class DAH {
	private static final String APPDATA = "APPDATA";
	private static final String OBJECTDB_DB_URSULAGIS_ODB = "$ursulaGIS.odb";
	private static final String H2_URSULAGIS_DB = "ursulaGIS.h2";//mv.db
	private static final String AUTO_SERVE=";AUTO_SERVER=TRUE";
	public static final String PROJECT_URL_KEY="DAH.PROJECT_URL_KEY";
	//private static final String OBJECTDB_DB_MONITORES_H2 = "$ursulaGIS.odb";
	//	private static final String SQLLITE_PU = "UrsulaGIS";
	/**
	 * @deprecated use em() instead
	 */
	@Deprecated
	private static EntityManager emODB = null;
	private static EntityManager emLite = null;
	static EntityTransaction transaction=null;


	public static EntityManager em(){
		return emLite();
	}

	public static void beginTransaction() {
		DAH.transaction = em().getTransaction();
		DAH.transaction.begin();
	}

	public static void commitTransaction() {
		DAH.transaction.commit();
		DAH.transaction=null;
	}

	public static void rollbackTransaction() {
		DAH.transaction.rollback();
		DAH.transaction=null;
	}

	public static EntityManager emODB(){
		if(emODB == null){
			String currentUsersHomeDir =System.getenv(APPDATA);
			//	System.out.println("obtuve la direccion de appData : "+currentUsersHomeDir);
			//obtuve la direccion de appData : C:\Users\quero\AppData\Roaming
			String ursulaGISFolder = currentUsersHomeDir + File.separator + Configuracion.URSULA_GIS_APPDATA_FOLDER;
			String  db_url = ursulaGISFolder + File.separator + OBJECTDB_DB_URSULAGIS_ODB;		
			System.out.println("abriendo la base de datos de: "+db_url);
			EntityManagerFactory emf =
					Persistence.createEntityManagerFactory(db_url);		     

			emODB = emf.createEntityManager();
		}
		return emODB;
	}

	/**
	 * obtener un acceso a la db de sqllite
	 * @return
	 */
	public static EntityManager emLite(){
		if(emLite == null){
			/*
   <property name="javax.persistence.jdbc.driver" value="org.sqlite.JDBC" />
  <property name="javax.persistence.jdbc.url" value="jdbc:sqlite:ursulaGIS.db" />
  <property name="eclipselink.ddl-generation" value="drop-and-create-tables" />
  <property name="eclipselink.ddl-generation.output-mode" value="database" />
			 */
			
			String db_url = JFXMain.config.getPropertyOrDefault(PROJECT_URL_KEY, "NOT_SET");
			if("NOT_SET".equals(db_url)) {
				String currentUsersHomeDir =System.getenv(APPDATA);
				String ursulaGISFolder = currentUsersHomeDir + File.separator + Configuracion.URSULA_GIS_APPDATA_FOLDER;
				//en ursulaGISFolder estan todos los tif temporales. borrarlos antes de cerrar el programa
				db_url = ursulaGISFolder + File.separator + H2_URSULAGIS_DB;		
			}
			db_url+=AUTO_SERVE;
			System.out.println("loading project "+db_url);
			File sqliteDBFile=new File(db_url);
			if(!sqliteDBFile.exists()){
				System.out.println("need to migrate from ObjectDB");
			}

			Map<String,String> properties = new HashMap<>();

			properties.put(ECLIPSELINK_PERSISTENCE_UNITS,"UrsulaGIS");//  persistence-unit name="UrsulaGIS" transaction-type="RESOURCE_LOCAL">, value)
			properties.put(TARGET_DATABASE,"auto");//  eclipselink.target-database
			// Ensure RESOURCE_LOCAL transactions is used.
			properties.put(TRANSACTION_TYPE,    PersistenceUnitTransactionType.RESOURCE_LOCAL.name());

			// Configure the internal connection pool
			properties.put(JDBC_DRIVER, "org.h2.Driver");
			//properties.put(JDBC_URL, "jdbc:h2:~/test");

			properties.put(JDBC_URL, "jdbc:h2:"+db_url);
			properties.put(DDL_GENERATION, CREATE_OR_EXTEND);
			properties.put(DDL_GENERATION_MODE, "database");
			//  properties.put(JDBC_USER, "scott");
			// properties.put(JDBC_PASSWORD, "tiger");

			// Configure logging. FINE ensures all SQL is shown
			// properties.put(LOGGING_LEVEL, "FINE");
			//  properties.put(LOGGING_TIMESTAMP, "false");
			// properties.put(LOGGING_THREAD, "false");
			// properties.put(LOGGING_SESSION, "false");

			// Ensure that no server-platform is configured
			properties.put(TARGET_SERVER, TargetServer.None);
			EntityManagerFactory factory =  Persistence.createEntityManagerFactory("UrsulaGIS", properties);
			//step 2
			emLite = factory.createEntityManager();

			//			String hUUID = JFXMain.config.getPropertyOrDefault("DAH.HAS_UUIDS", "False");
			//			if(!"True".equals(hUUID)) {
			//				//	runUpdates();
			//				JFXMain.config.setProperty("DAH.HAS_UUIDS", "True");
			//				JFXMain.config.save();
			//			}

		}
		return emLite;
	}

	public static void runUpdates() {
		String table ="ORDENCOMPRA";
		String col = "UUID";

		DAH.transaction = emLite.getTransaction();
		DAH.transaction.begin();
		Query q = emLite.createNativeQuery("SELECT DISTINCT TABLE_NAME FROM information_schema.columns as c where c.TABLE_SCHEMA='PUBLIC' ");//and c.COLUMN_NAME='"+col+"'");// where Name = "+table); 
		List<String> results = q.getResultList();
		results.stream().forEach((tableName)->{
			System.out.println("cheking "+tableName);
			Query q2 = emLite.createNativeQuery("SELECT TABLE_NAME FROM information_schema.columns as c where c.TABLE_NAME='"+tableName+"' AND c.COLUMN_NAME='"+col+"'");
			if(!(q2.getResultList().size()>0)) {
				System.out.println("no hay uuids, creando");
				//ALTER TABLE TABLE_NAME ADD COLUMN IF NOT EXISTS COLUMN_NAME VARCHAR(50);
				Query q3 = emLite.createNativeQuery("ALTER TABLE "+table+" ADD COLUMN IF NOT EXISTS "+col+" VARCHAR(36)");
				q3.executeUpdate();
				Query q4 = emLite.createNativeQuery("UPDATE "+table+" SET "+col+"=RANDOM_UUID() ");
				q4.executeUpdate();			 
			}
		}
				);


		//				"          AND Object_ID = Object_ID(N'schemaName."+table+"')");
		//		"ALTER TABLE TABLE_NAME ADD COLUMN IF NOT EXISTS COLUMN_NAME VARCHAR(50);"

		//		\r\n" + 
		//		"BEGIN\r\n" + 
		//				" alter table "+table+" INSERT UUID varchar(255)\\r\\n"+
		//		//"UPDATE "+table+" set UUID=random_uuid();" + 
		//		"END");//
		//		UPDATE table_name
		//		SET column1 = value1, column2 = value2, ...
		//		WHERE condition;

		//q.executeUpdate();
		DAH.commitTransaction();
	}
	//	public static EntityManager em(){
	//		if(em == null){
	//			String currentUsersHomeDir =System.getenv(APPDATA);
	//			//	System.out.println("obtuve la direccion de appData : "+currentUsersHomeDir);
	//				//obtuve la direccion de appData : C:\Users\quero\AppData\Roaming
	//			String ursulaGISFolder = currentUsersHomeDir + File.separator + Configuracion.URSULA_GIS_APPDATA_FOLDER;
	//			String  db_url = ursulaGISFolder + File.separator + OBJECTDB_DB_MONITORES_ODB;		
	//			System.out.println("abriendo la base de datos de: "+db_url);
	//			EntityManagerFactory emf =
	//					Persistence.createEntityManagerFactory(db_url);		     
	//		emf.
	//			em = emf.createEntityManager();
	//		}
	//		return em;
	//	}


	public static void save(Object entidad) {	
		if (entidad.getClass().getAnnotation(Entity.class) == null) {	
			System.out.println("no se guardan las clases que no son entidades "+entidad);
			return;
		}
		
		EntityManager em = em();
		if(DAH.transaction == null){
			//	DAH.transaction = em.getTransaction();
			try{
				em.getTransaction().begin();		
				if(em.contains(entidad)) {
					em.merge(entidad);
					System.out.println("merging entidad "+entidad);
				}else {
					System.out.println("persistiendo entidad "+entidad);
					em.persist(entidad);			
				}
				em.getTransaction().commit();
			}catch(javax.persistence.RollbackException rbe){
				em.getTransaction().begin();		
				em.merge(entidad);
				em.getTransaction().commit();			
			}finally {

			}
		} else{
			if(em.contains(entidad)) {
				em.merge(entidad);
				System.out.println("merging entidad "+entidad);
			}else {
				System.out.println("persistiendo entidad "+entidad);
				em.persist(entidad);			
			}
		}
	}

	public static void removeAll(List<Object> entidades) {
		EntityManager em = em();
		if(DAH.transaction == null){
			//	DAH.transaction = em.getTransaction();
			em.getTransaction().begin();		
			entidades.forEach(each->em.remove(each));
			//em.remove(entidad);			
			em.getTransaction().commit();
		} else{
			entidades.forEach(each->em.remove(each));

			//em.remove(entidad);	
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

	/**
	 * 
	 * @param laborName
	 * @return el producto existente en la base de datos o crea uno nuevo con ese nombre y lo devuelve
	 */
	public static ProductoLabor getProductoLabor(String laborName) {	
		Number count = (Number)em().createNamedQuery(ProductoLabor.COUNT_ALL).getSingleResult();
		//System.out.print("hay "+count+" productos en la base de datos");
		if(count.intValue() ==0) {
			List<ProductoLabor> results = getAllProductosLabores(DAH.em());//getAll crea los cultivos default
		}

		TypedQuery<ProductoLabor> query = em().createNamedQuery(
				ProductoLabor.FIND_NAME, ProductoLabor.class);
		query.setParameter("name", laborName);

		ProductoLabor result = null;
		if(query.getResultList().size()>0){
			result = query.getResultList().get(0);
		}  
		//		else {
		//			result = new Cultivo(cultivoName);
		//			DAH.save(result);
		//		}
		return result;
	}

	public static List<ProductoLabor> getAllProductosLabores(EntityManager em) {
		TypedQuery<ProductoLabor> query = em.createNamedQuery(
				ProductoLabor.FIND_ALL, ProductoLabor.class);
		List<ProductoLabor> results = query.getResultList();

		if(results.size()==0){
			ProductoLabor.laboresDefault.values().forEach((d)->{DAH.save(d);
			results.add(d);
			});			
		}
		return results;
	}

	public static List<Establecimiento> getAllEstablecimientos(EntityManager em) {
		TypedQuery<Establecimiento> query = em.createNamedQuery(
				Establecimiento.FIND_ALL, Establecimiento.class);
		List<Establecimiento> results = query.getResultList();
		return results;
	}

	public static Establecimiento getEstablecimiento(String establecimientoName) throws Exception {
		//	EntityManager em = em();
		//em().getReference(arg0, arg1) //usarlo cuando solo se quiere guardar un objeto que apunta a este.
		TypedQuery<Establecimiento> query = em().createNamedQuery(
				Establecimiento.FIND_NAME, Establecimiento.class);
		query.setParameter("name", establecimientoName);

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

	public static List<? extends Labor<?>> getAllLabores() {
		return getAllLabores(em());
	}
	public static List<? extends Labor<?>> getAllLabores(EntityManager em) {
		@SuppressWarnings("rawtypes")
		TypedQuery<CosechaLabor> query = em.createNamedQuery(
				CosechaLabor.FIND_ALL, CosechaLabor.class);
		@SuppressWarnings("unchecked")
		List<? extends Labor<?>> results = (List<? extends Labor<?>>) query.getResultList();
		return results;
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
		Number count = (Number)em().createNamedQuery(Cultivo.COUNT_ALL).getSingleResult();
		//System.out.println("hay "+count+" cultivos en la base de datos");

		if(count.intValue() == 0) {
			List<Cultivo> results = getAllCultivos();//getAll crea los cultivos default
		}

		TypedQuery<Cultivo> query = em().createNamedQuery(Cultivo.FIND_NAME, Cultivo.class);
		query.setParameter("name", cultivoName);

		Cultivo result = null;
		if(query.getResultList().size()>0){
			result = query.getResultList().get(0);//getFirstResult();
			//System.out.println("buscando "+cultivoName+" encontre "+result);
		}  
		return result;
	}

	public static List<Cultivo> getAllCultivos(EntityManager em) {
		TypedQuery<Cultivo> query = em.createNamedQuery(Cultivo.FIND_ALL, Cultivo.class);
		List<Cultivo> results = query.getResultList();
		if(results.size()==0){
			Cultivo.getCultivosDefault().values().forEach((d)->{				
				DAH.save(d);
				results.add(d);
			});
		} else {
			results.forEach(cult->{
				//Cultivo.cultivos.s
				if(cult.getEstival()==null)cult.setEstival(true);
				if(cult.getAporteMO()==null)cult.setAporteMO(1000*(-1+cult.getAbsN()/cult.getExtN()));//kg por tn;  estimacion en base a la extraccion de n vs absorcion de n
			});
		}
		return results;
	}

	public static List<Poligono> getAllPoligonos() {
		TypedQuery<Poligono> query = em().createNamedQuery(Poligono.FIND_ALL, Poligono.class);
		List<Poligono> results = query.getResultList();
		return results;
	}

	public static List<Ndvi> getAllNdvi() {
		TypedQuery<Ndvi> query =
				em().createNamedQuery(Ndvi.FIND_ALL, Ndvi.class);
		List<Ndvi> results = query.getResultList();
		return results;
	}

	public static List<Ndvi> getNdvi(Poligono contorno, LocalDate assetDate) {
		TypedQuery<Ndvi> query =
				em().createNamedQuery(Ndvi.FIND_BY_CONTORNO_DATE, Ndvi.class);
		query.setParameter("contorno", contorno);
		query.setParameter("date", assetDate);
		List<Ndvi> results = query.getResultList();
		//  closeEm();
		return results;
	}

	public static List<Ndvi> getNdvi(Poligono contorno) {
		TypedQuery<Ndvi> query =
				em().createNamedQuery(Ndvi.FIND_BY_CONTORNO, Ndvi.class);
		query.setParameter("contorno", contorno);
		List<Ndvi> results = query.getResultList();
		return results;
	}

	public static List<Recorrida> getAllRecorridas() {
		TypedQuery<Recorrida> query =
				em().createNamedQuery(Recorrida.FIND_ALL, Recorrida.class);
		List<Recorrida> results = query.getResultList();
		return results;
	}

	public static List<Poligono> getPoligonosActivos() {
		TypedQuery<Poligono> query =
				em().createNamedQuery(Poligono.FIND_ACTIVOS, Poligono.class);
		List<Poligono> results = query.getResultList();
		return results;
	}



	//se llama al cerrar la aplicacion
	public static void closeEm() {
		if(emODB!=null){
			emODB.close();
			emODB=null;
		}	
		if(emLite!=null){
			emLite.close();
			emLite=null;
		}
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
			Fertilizante.getFertilizantesDefault().values().forEach((d)->{
				DAH.save(d);
				results.add(d);
			});
		}
		return results;
	}

	public static List<Agroquimico> getAllAgroquimicos() {
		TypedQuery<Agroquimico> query = em().createNamedQuery(
				Agroquimico.FIND_ALL, Agroquimico.class);
		List<Agroquimico> results = query.getResultList();
		if(results.size()==0){
			results = new ArrayList<Agroquimico>();
			System.out.println("guardando los agroquimicos default");
			DAH.beginTransaction();
			for(Agroquimico d:Agroquimico.getAgroquimicosDefault().values()) {
				DAH.save(d);	
				results.add(d);
			}
			DAH.commitTransaction();
		}
		
//		List<Agroquimico> results = new ArrayList<Agroquimico>();
//		results.addAll(Agroquimico.getAgroquimicosDefault().values());

		return  results;
	}


	public static List<Semilla> getAllSemillas() {
		TypedQuery<Semilla> query = em().createNamedQuery(
				Semilla.FIND_ALL, Semilla.class);
		List<Semilla> results = query.getResultList();
		if(results.size()==0){
			Semilla.getSemillasDefault().values().forEach((d)->{
				System.out.println("guardando semilla default "+d);					
				DAH.save(d);
				results.add(d);
			});
		}

		return results;
	}

	public static List<Ndvi> getNdviActivos() {
		//System.out.println("buscando ndvi activos");
		TypedQuery<Ndvi> query =
				em().createNamedQuery(Ndvi.FIND_ACTIVOS, Ndvi.class);
		List<Ndvi> results = query.getResultList();
		//  closeEm();
		return results;
	}

	public static List<OrdenCompra> getAllOrdenesCompra() {
		TypedQuery<OrdenCompra> query = em().createNamedQuery(
				OrdenCompra.FIND_ALL, OrdenCompra.class);
		List<OrdenCompra> results = (List<OrdenCompra> ) query.getResultList();
		return results;
	}
	
	public static List<OrdenPulverizacion> getAllOrdenesPulverizacion() {
		TypedQuery<OrdenPulverizacion> query = em().createNamedQuery(
				OrdenPulverizacion.FIND_ALL, OrdenPulverizacion.class);
		List<OrdenPulverizacion> results = (List<OrdenPulverizacion> ) query.getResultList();
		return results;
	}

	public static List<OrdenSiembra> getAllOrdenesSiembra() {
		TypedQuery<OrdenSiembra> query = em().createNamedQuery(
				OrdenSiembra.FIND_ALL, OrdenSiembra.class);
		List<OrdenSiembra> results = (List<OrdenSiembra> ) query.getResultList();
		return results;
	}
	
	public static List<Asignacion> getAllAsignaciones() {
		TypedQuery<Asignacion> query = em().createNamedQuery(
				Asignacion.FIND_ALL, Asignacion.class);
		List<Asignacion> results = (List<Asignacion> ) query.getResultList();
		return results;
	}

	public static Producto findProducto(String productoNombre) {
		TypedQuery<Producto> query = em().createNamedQuery(
				Producto.FIND_NAME, Producto.class);
		query.setParameter("name", productoNombre);
		Producto results = query.getSingleResult();
		return results;		
	}

	public static Semilla getSemilla(String nombre) {
		TypedQuery<Semilla> query = em().createNamedQuery(
				Semilla.FIND_NAME, Semilla.class);
		query.setParameter("name", nombre);
		Semilla result = null;
		if(query.getResultList().size()>0){
			result = query.getResultList().get(0);//getFirstResult();
			//System.out.println("buscando "+nombre+" encontre "+result);
		}  
		return result;	
	}

	public static Fertilizante getFertilizante(String nombre) {
		TypedQuery<Fertilizante> query = em().createNamedQuery(
				Fertilizante.FIND_NAME, Fertilizante.class);
		query.setParameter("name", nombre);
		Fertilizante result = null;
		if(query.getResultList().size()>0){
			result = query.getResultList().get(0);//getFirstResult();
			//System.out.println("buscando "+nombre+" encontre "+result);
		}  
		return result;	
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
