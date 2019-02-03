package gui.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.persistence.Transient;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableFilter.Builder;

import dao.Poligono;
import dao.config.Agroquimico;
import dao.config.Campania;
import dao.config.Cultivo;
import dao.config.Empresa;
import dao.config.Establecimiento;
import dao.config.Fertilizante;
import dao.config.Lote;
import dao.utils.JPAStringProperty;
import gui.JFXMain;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import utils.DAH;



public class SmartTableView<T> extends TableView<T> {
	private Supplier<T> onDoubleClick=null;
	private Consumer<T> onShowClick=null;
//	public HBox filters = new HBox();
//	private FilteredList<T> filteredData=null;
	//i18n FilterTable Strings
	//"filterpanel.search.field"
	//"filterpanel.apply.button"
	//"filterpanel.none.button"
	//"filterpanel.all.button"
	//"filterpanel.resetall.button"
	
	public SmartTableView(ObservableList<T> data){//,ObservableList<T> observable){
		super(data);
		impl.org.controlsfx.i18n.Localization.setLocale(Locale.forLanguageTag("es-ES"));//XXX en java 10 falla; pasar al controlsfx-9.0.0.jar
	
		//filteredData = new FilteredList<>(observable, p -> true);
		// 3. Wrap the FilteredList in a SortedList. 
//		SortedList<T> sortedData = new SortedList<>(data);
//
//		// 4. Bind the SortedList comparator to the TableView comparator.
//		sortedData.comparatorProperty().bind(this.comparatorProperty());
//		// 5. Add sorted (and filtered) data to the table.
//		this.setItems(sortedData);

		if(data.size()>0){
			populateColumns(data.get(0).getClass());
		}else{
			//populateColumns(onDoubleClick.get().getClass());
			System.out.println("no creo las columnas porque no hay datos");
		}

		ContextMenu contextMenu = new ContextMenu();
		MenuItem mostrarItem = new MenuItem("Cargar");
		MenuItem eliminarItem = new MenuItem("Eliminar");

		this.setContextMenu(contextMenu);

		this.setOnMouseClicked( event->{
			contextMenu.getItems().clear();
			T rowData = this.getSelectionModel().getSelectedItem();
			if(rowData != null ){
				if(onShowClick!=null) contextMenu.getItems().add(mostrarItem);
				contextMenu.getItems().add(eliminarItem);

				if ( MouseButton.PRIMARY.equals(event.getButton()) && event.getClickCount() == 2) {
					if(onDoubleClick!=null){
						data.add(onDoubleClick.get());
					}		            
				} 
				else if(MouseButton.SECONDARY.equals(event.getButton()) && event.getClickCount() == 1){

					mostrarItem.setOnAction((ev)->{
						onShowClick.accept(rowData);
					});
					eliminarItem.setOnAction((aev)->{						
						Alert alert = new Alert(AlertType.CONFIRMATION);
						Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();

						stage.getIcons().add(new Image(JFXMain.ICON));
						alert.setTitle("Borrar registro");
						alert.setHeaderText("Esta accion borrara permanentemente el registro. Desea Continuar?");
						Optional<ButtonType> res = alert.showAndWait();
						if(res.get().equals(ButtonType.OK) && rowData!=null){
							try{
								DAH.remove(rowData);
								data.remove(rowData);
								if(data.size()==0){
									data.add(onDoubleClick.get());
								}
								refresh();
							}catch(Exception e){
								Alert eliminarFailAlert = new Alert(AlertType.ERROR);
								((Stage) eliminarFailAlert.getDialogPane().getScene().getWindow()).
										getIcons().add(new Image(JFXMain.ICON));
								eliminarFailAlert.setTitle("Borrar registro");
								eliminarFailAlert.setHeaderText("No se pudo borrar el registro");
								eliminarFailAlert.setContentText(e.getMessage());
								eliminarFailAlert.show();
							}							
						}
					});			
				}
			}
		});

		data.addListener((javafx.collections.ListChangeListener.Change<? extends T> c)->{
			if(	getColumns().size()==0){			
			populateColumns(c.getList().get(0).getClass());
		}});

		//		new ListChangeListener<T>(){
		//
		//			@Override
		//			public void onChanged(javafx.collections.ListChangeListener.Change<? extends T> c) {
		//			if(	getColumns().size()==0){
		//				populateColumns(c.getList().get(0).getClass());
		//			}
		//				
		//			}
		//			
		//		});
		
		Builder<T> builder = TableFilter.forTableView(this);
		TableFilter<T> tableFilter = builder.lazy(true).apply();
		tableFilter.setSearchStrategy((input,target) -> {
		    try {
		        return target.toLowerCase().startsWith(input.toLowerCase());
		    } catch (Exception e) {
		    	e.printStackTrace();
		        return false;
		    }
		});
		

	}


	private void populateColumns(Class<?> clazz) {
		Method[] methods = clazz.getMethods();//ok esto me trae todos los metodos heredados
		List<Method> methodList = Arrays.asList(methods);
		methodList.sort((a,b)->{
			String nameA = a.getName();
			String nameB = b.getName();
			return nameA.compareToIgnoreCase(nameB);
		});
	//	System.out.print("creando tabla para "+ clazz+" con los metodos\n"+methodList);
	//	Class<?> superclass =clazz.getSuperclass();
	//	Method[] superMethods = superclass.getDeclaredMethods();

	//	Method[] result = Arrays.copyOf(methods, methods.length + superMethods.length);
	//	System.arraycopy(superMethods, 0, result, methods.length, superMethods.length);


		for (Method method :  methodList) {
			int mods = method.getModifiers();
			boolean transiente = method.isAnnotationPresent(Transient.class);
			if(Modifier.isStatic(mods) || Modifier.isAbstract(mods)||transiente){
				continue;
			}
			String name = method.getName();
			if(name.startsWith("get")||name.startsWith("is")){
				Class<?> fieldType = method.getReturnType();
				String setMethodName = null;
				if(name.startsWith("is")){
					setMethodName = name.replace("is", "set");
				} else {
					setMethodName = name.replace("get", "set");
				}
				
				name = name.replace("get", "");
				if(String.class.isAssignableFrom(fieldType)){
					getStringColumn(clazz,method, name, fieldType, setMethodName);
				} else 	if(StringProperty.class.isAssignableFrom(fieldType)  ){
					getJPAStringPropertyColumn(clazz, method, name, fieldType, setMethodName);				
				} else 	if(double.class.isAssignableFrom(fieldType) ||Number.class.isAssignableFrom(fieldType) ){
					getNumberColumn(clazz, method, name, fieldType, setMethodName);				
				} else 	if(DoubleProperty.class.isAssignableFrom(fieldType) ){
					getDoublePropertyColumn(clazz, method, name, fieldType, setMethodName);				
				}else if(boolean.class.isAssignableFrom(fieldType) ||Boolean.class.isAssignableFrom(fieldType) ){
					getBooleanColumn(clazz, method, name, fieldType, setMethodName);				
				}else if(Calendar.class.isAssignableFrom(fieldType)){
					getCalendarColumn(clazz, method, name, fieldType, setMethodName);
				}else if(LocalDate.class.isAssignableFrom(fieldType)){
					getLocalDateColumn(clazz, method, name, fieldType, setMethodName);
				} else if(Empresa.class.isAssignableFrom(fieldType)){					
					getEmpresaColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Establecimiento.class.isAssignableFrom(fieldType)){
					getEstablecimientoColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Lote.class.isAssignableFrom(fieldType)){
					getLoteColumn(clazz, method, name, fieldType, setMethodName);
				} else if(Campania.class.isAssignableFrom(fieldType)){
					getCampaniaColumn(clazz, method, name, fieldType, setMethodName);
				} else if(Cultivo.class.isAssignableFrom(fieldType)){
					getCultivoColumn(clazz, method, name, fieldType, setMethodName);
				} else if(Cultivo.class.isAssignableFrom(fieldType)){
					getCultivoColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Agroquimico.class.isAssignableFrom(fieldType)){
					getAgroquimicoColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Fertilizante.class.isAssignableFrom(fieldType)){
					getFertilizanteColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Poligono.class.isAssignableFrom(fieldType)){
					getPoligonoColumn(clazz, method, name, fieldType, setMethodName);
				}
//				else {//no quiero que muestre los metodos class ni id
//					getStringColumn(clazz,method, name, fieldType, setMethodName);
//				}


			}//fin del if method name starts with get

		}

	}







	private void getCampaniaColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,
			String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Campania> dColumn = new ChoiceTableColumn<T,Campania>(propName,DAH.getAllCampanias(),
				(p)->{try {
					return ((Campania) method.invoke(p, (Object[])null));
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getMethod(setMethodName, fieldType);
						setMethod.invoke(p,d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);
	}


	private void getLoteColumn(Class<?> clazz, Method method, String name, Class<?> fieldType, String setMethodName) {
		//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Lote> dColumn = new ChoiceTableColumn<T,Lote>(propName,DAH.getAllLotes(),
				(p)->{try {
					return ((Lote) method.invoke(p, (Object[])null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getMethod(setMethodName, fieldType);
						setMethod.invoke(p,d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);
	}

	private void getCultivoColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Cultivo> dColumn = new ChoiceTableColumn<T,Cultivo>(propName,DAH.getAllCultivos(),
				(p)->{try {
					return ((Cultivo) method.invoke(p, (Object[])null));
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getMethod(setMethodName, fieldType);
						setMethod.invoke(p,d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);
	}

	private void getFertilizanteColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Fertilizante> dColumn = new ChoiceTableColumn<T,Fertilizante>(propName,DAH.getAllFertilizantes(),
				(p)->{try {
					return ((Fertilizante) method.invoke(p, (Object[])null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getMethod(setMethodName, fieldType);
						setMethod.invoke(p,d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);

	}
	
	
	private void getPoligonoColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Poligono> dColumn = new ChoiceTableColumn<T,Poligono>(propName,DAH.getAllPoligonos(),
				(p)->{try {
					return ((Poligono) method.invoke(p, (Object[])null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getMethod(setMethodName, fieldType);
						setMethod.invoke(p,d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);

	}

	private void getAgroquimicoColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,
			String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Agroquimico> dColumn = new ChoiceTableColumn<T,Agroquimico>(propName,DAH.getAllAgroquimicos(),
				(p)->{try {
					return ((Agroquimico) method.invoke(p, (Object[])null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getMethod(setMethodName, fieldType);
						setMethod.invoke(p,d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);

	}

	private void getEstablecimientoColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,
			String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Establecimiento> dColumn = new ChoiceTableColumn<T,Establecimiento>(propName,DAH.getAllEstablecimientos(),
				(p)->{try {
					return ((Establecimiento) method.invoke(p, (Object[])null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getMethod(setMethodName, fieldType);
						setMethod.invoke(p,d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);
	}


	private void getEmpresaColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,
			String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Empresa> dColumn = new ChoiceTableColumn<T,Empresa>(propName,DAH.getAllEmpresas(),
				(p)->{try {
					return ((Empresa) method.invoke(p, (Object[])null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getMethod(setMethodName, fieldType);
						setMethod.invoke(p,d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);
	}


	private void getCalendarColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,
			String setMethodName) {
		String propName = name.replace("Property", "");
		DateTableColumn<T> dColumn = new DateTableColumn<T>(propName,
				(p)->{try {
					return ((Calendar) method.invoke(p, (Object[])null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;
				},(p,d)->{try {
					Method setMethod = clazz.getMethod(setMethodName, fieldType);
					setMethod.invoke(p,d);
					DAH.save(p);
					refresh();
				} catch (Exception e) {e.printStackTrace();}
				});

		this.getColumns().add(dColumn);
	}

	private void getLocalDateColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,
			String setMethodName) {
		String propName = name.replace("Property", "");
		LocalDateTableColumn<T> dColumn = new LocalDateTableColumn<T>(propName,
				(p)->{try {
					return ((LocalDate) method.invoke(p, (Object[])null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;
				},(p,d)->{try {
					Method setMethod = clazz.getMethod(setMethodName, fieldType);
					setMethod.invoke(p,d);
					DAH.save(p);
					refresh();
				} catch (Exception e) {e.printStackTrace();}
				});

		this.getColumns().add(dColumn);
	}
	

	private void getStringColumn(Class<?> clazz,Method getMethod, String name, Class<?> fieldType, String setMethodName) {
		//System.out.println("Obteniendo stringColumn para "+name);
		//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
		String propName = name.replace("Property", "");
		TableColumn<T,String> column = new TableColumn<T,String>(propName);
		column.setEditable(true);
		column.setCellFactory(TextFieldTableCell.forTableColumn());
		column.setCellValueFactory(//new PropertyValueFactory<>(propName)
				cellData ->{
					String stringValue = null;
					try{
					 stringValue =(String)  getMethod.invoke(cellData.getValue(), (Object[])null);
					
					
					return new SimpleStringProperty(stringValue);	
					}catch(Exception e){
						System.out.println("La creacion de SimpleStringProperty en getStringColumn "+name +" con valor: "+stringValue);
						
						return new SimpleStringProperty("sin datos");
					}
				});
		try {
			Method setMethod = clazz.getMethod(setMethodName, fieldType);
			if(setMethod!=null){
				column.setOnEditCommit(cellEditingEvent -> { 
					int row = cellEditingEvent.getTablePosition().getRow();
					T p = cellEditingEvent.getTableView().getItems().get(row);
					try {
						setMethod.invoke(p,cellEditingEvent.getNewValue());
						DAH.save(p);
						refresh();
					} catch (Exception e) {	e.printStackTrace();}
				});
			}

		} catch (NoSuchMethodException e1) {
			//XXX el metodo es solo de tipo get
		} catch (SecurityException e1) {
			e1.printStackTrace();
		}

		this.getColumns().add(column);

	}

	
	private void getJPAStringPropertyColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,
			String setMethodName) {
		String propName = name.replace("Property", "");
		//System.out.print("construyendo un JPASTringPropertyColumn para "+name);
		TableColumn<T,String> column = new TableColumn<T,String>(propName);
		column.setEditable(true);
		column.setCellFactory(TextFieldTableCell.forTableColumn());
		column.setCellValueFactory(
				cellData ->{
					try {
						Object o =  method.invoke(cellData.getValue(), (Object[]) null);
						if(o==null){
							o=new JPAStringProperty();
							try {
								Method setMethod = clazz.getMethod(setMethodName, fieldType);
								if(setMethod!=null){							
									T p = cellData.getValue();
									try {
										setMethod.invoke(p,o);
									} catch (Exception e) {	e.printStackTrace();}
								}
							} catch (NoSuchMethodException | SecurityException e1) {
								e1.printStackTrace();
							}
						}					
						if(StringProperty.class.isAssignableFrom(o.getClass())) {
							StringProperty ssp = (StringProperty)o;
							ssp.addListener((obj,old,n)->{
								DAH.save(cellData.getValue());
							});
							return ssp;
						}
					
						return null;	
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
						e1.printStackTrace();
						return new SimpleStringProperty();
					}
				});
		this.getColumns().add(column);
	}

	private void getDoubleColumn(Class<?> clazz, Method method, String name, Class<?> fieldType, String setMethodName) {
		String propName = name.replace("Property", "");
		DoubleTableColumn<T> dColumn = new DoubleTableColumn<T>(propName,
				(p)->{	try {
					return ((Double) method.invoke(p, (Object[])null));
				} catch (Exception e) {	e.printStackTrace();}
				return null;
				},(p,d)->{ try {
					Method setMethod = clazz.getMethod(setMethodName, fieldType);
					setMethod.invoke(p,d);
					DAH.save(p);
					refresh();
				} catch (Exception e) {	e.printStackTrace();}
				});

		this.getColumns().add(dColumn);
	}
	
	private void getNumberColumn(Class<?> clazz, Method method, String name, Class<?> fieldType, String setMethodName) {
		String propName = name.replace("Property", "");
		DoubleTableColumn<T> dColumn = new DoubleTableColumn<T>(propName,
				(p)->{	try {
					Number n = ((Number) method.invoke(p, (Object[])null));
					if(n!=null) {
						return n.doubleValue();
					} else {
						return 0.0;
					}
				} catch (Exception e) {	e.printStackTrace();}
				return null;
				},(p,d)->{ try {
					Method setMethod = clazz.getMethod(setMethodName, fieldType);
					setMethod.invoke(p,d);
					DAH.save(p);
					refresh();
				} catch (Exception e) {	e.printStackTrace();}
				});

		this.getColumns().add(dColumn);
	}

	private void getDoublePropertyColumn(Class<?> clazz, Method method, String name, Class<?> fieldType, String setMethodName) {
		String propName = name.replace("Property", "");
		DoubleTableColumn<T> dColumn = new DoubleTableColumn<T>(propName,
				(p)->{	try {
					DoubleProperty n = ((DoubleProperty) method.invoke(p, (Object[])null));
					if(n!=null) {
						return n.get();
					} else {
						return 0.0;
					}
				} catch (Exception e) {	e.printStackTrace();}
				return null;
				},(p,d)->{ try {
					
					DoubleProperty n = ((DoubleProperty) method.invoke(p, (Object[])null));
					if(n!=null) {
						n.set(d);
					} 
				//	Method setMethod = clazz.getMethod(setMethodName, fieldType);
				//	setMethod.invoke(p,d);
					DAH.save(p);
					refresh();
				} catch (Exception e) {	e.printStackTrace();}
				});

		this.getColumns().add(dColumn);
	}
	
	//No se usa. definir los getters y setters es mas facil creo.
	private void getEstablecimientoPropertyColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,	String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Establecimiento> dColumn = new ChoiceTableColumn<T,Establecimiento>(propName,DAH.getAllEstablecimientos(),
				(p)->{try {
					Property<Establecimiento>prop = ((Property<Establecimiento>) method.invoke(p, (Object[])null));
					return prop.getValue();
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Property<Establecimiento>prop = ((Property<Establecimiento>) method.invoke(p, (Object[])null));
						prop.setValue(d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);
	}
	
	private void getBooleanColumn(Class<?> clazz, Method method, String name, Class<?> fieldType, String setMethodName) {
		String propName = name.replace("Property", "");
		
		
		try {
			clazz.getMethod(setMethodName, fieldType);	//check method exists
		} catch (Exception e) {	
			//e.printStackTrace();
			return;
		}
		
		
		BooleanTableColumn<T> dColumn = new BooleanTableColumn<T>(propName,
				(p)->{	try {
					return ((Boolean) method.invoke(p, (Object[])null));
				} catch (Exception e) {	e.printStackTrace();}
				return null;
				},(p,d)->{ try {
					Method setMethod = clazz.getMethod(setMethodName, fieldType);
					setMethod.invoke(p,d);
					DAH.save(p);
					refresh();
				} catch (Exception e) {	e.printStackTrace();}
				});

		this.getColumns().add(dColumn);
	}
	
	/**
	 * @param onDoubleClick the onDoubleClick to set
	 */
	public void setOnShowClick(Consumer<T> onShowClick) {
		this.onShowClick = onShowClick;
	}


	/**
	 * @return the onDoubleClick
	 */
	public Supplier<T> getOnDoubleClick() {
		return onDoubleClick;
	}


	/**
	 * @param onDoubleClick the onDoubleClick to set
	 */
	public void setOnDoubleClick(Supplier<T> onDoubleClick) {
		this.onDoubleClick = onDoubleClick;
	}

	public void refresh() { 
		//Wierd JavaFX bug 
		ObservableList<T> data = this.getItems();
		this.setItems(null); 
		this.layout(); 
		this.setItems(data); 
	}


}
