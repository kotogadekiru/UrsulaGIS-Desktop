package gui.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;


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
import utils.DAH;
import gui.JFXMain;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;



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
		impl.org.controlsfx.i18n.Localization.setLocale(Locale.forLanguageTag("es-ES"));
	
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
				//			else if(MouseButton.SECONDARY.equals(event.getButton()) && event.getClickCount() == 2){
				//				T rowData = this.getSelectionModel().getSelectedItem();
				//			
				//				Alert alert = new Alert(AlertType.CONFIRMATION);
				//				Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
				//				
				//				stage.getIcons().add(new Image(JFXMain.ICON));
				//				alert.setTitle("Borrar registro");
				//				alert.setHeaderText("Esta accion borrara permanentemente el registro. Desea Continuar?");
				//				Optional<ButtonType> res = alert.showAndWait();
				//				if(res.get().equals(ButtonType.OK) && rowData!=null){
				//					data.remove(rowData);
				//
				//					DAH.remove(rowData);
				//				}
				//			} 
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
							data.remove(rowData);
							DAH.remove(rowData);
						}
					});			
				}
			}
		});

		data.addListener((javafx.collections.ListChangeListener.Change<? extends T> c)->{if(	getColumns().size()==0){
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
		builder.lazy(true).apply();

	}


	private void populateColumns(Class<?> clazz) {
		Method[] methods = clazz.getDeclaredMethods();
		Class<?> superclass =clazz.getSuperclass();
		Method[] superMethods = superclass.getDeclaredMethods();

		Method[] result = Arrays.copyOf(methods, methods.length + superMethods.length);
		System.arraycopy(superMethods, 0, result, methods.length, superMethods.length);


		for (Method method :  result) {
			int mods = method.getModifiers();
			if(Modifier.isStatic(mods) || Modifier.isAbstract(mods)){
				continue;
			}
			String name = method.getName();
			if(name.startsWith("get")){
				Class<?> fieldType = method.getReturnType();
				String setMethodName = name.replace("get", "set");
				name = name.replace("get", "");
				if(String.class.isAssignableFrom(fieldType)){
					getStringColumn(clazz,method, name, fieldType, setMethodName);
				} else 	if(StringProperty.class.isAssignableFrom(fieldType)  ){
					getJPAStringPropertyColumn(clazz, method, name, fieldType, setMethodName);				
				} else 	if(double.class.isAssignableFrom(fieldType) ||Double.class.isAssignableFrom(fieldType) ){
					getDoubleColumn(clazz, method, name, fieldType, setMethodName);				
				} else if(boolean.class.isAssignableFrom(fieldType) ||Boolean.class.isAssignableFrom(fieldType) ){
					getBooleanColumn(clazz, method, name, fieldType, setMethodName);				
				}else if(Calendar.class.isAssignableFrom(fieldType)){
					getCalendarColumn(clazz, method, name, fieldType, setMethodName);
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
				} else if(Agroquimico.class.isAssignableFrom(fieldType)){
					getAgroquimicoColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Fertilizante.class.isAssignableFrom(fieldType)){
					getFertilizanteColumn(clazz, method, name, fieldType, setMethodName);
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
					return ((Campania) method.invoke(p, null));
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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
					return ((Lote) method.invoke(p, null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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
					return ((Cultivo) method.invoke(p, null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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
					return ((Fertilizante) method.invoke(p, null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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
					return ((Agroquimico) method.invoke(p, null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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
					return ((Establecimiento) method.invoke(p, null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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
					return ((Empresa) method.invoke(p, null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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
					return ((Calendar) method.invoke(p, null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;
				},(p,d)->{try {
					Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
					setMethod.invoke(p,d);
					DAH.save(p);
					refresh();
				} catch (Exception e) {e.printStackTrace();}
				});

		this.getColumns().add(dColumn);
	}


	private void getStringColumn(Class<?> clazz,Method method, String name, Class<?> fieldType, String setMethodName) {
		//System.out.println("Obteniendo stringColumn para "+name);
		//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
		String propName = name.replace("Property", "");
		TableColumn<T,String> column = new TableColumn<T,String>(propName);
		column.setEditable(true);
		column.setCellFactory(TextFieldTableCell.forTableColumn());
		column.setCellValueFactory(//new PropertyValueFactory<>(propName)
				cellData ->{
					String doubleValue="";
					try {
						Object o = method.invoke(cellData.getValue(), (Object[])null);
						
						doubleValue = o!=null?o.toString():"";
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					try{
					return new SimpleStringProperty(doubleValue);	
					}catch(Exception e){
						System.out.println("Falló el Decimal Format en String Table Column "+name +" para "+doubleValue);
						
						return new SimpleStringProperty(doubleValue);
					}
				});
		try {
			Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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

//		String getMethodName = setMethodName.replace("set", "get");
//		System.out.println("creando el filtro para "+getMethodName);
//		try {
//			Method getMethod = clazz.getDeclaredMethod(getMethodName);
//			System.out.println("getMethod es "+getMethod);
//			if(getMethod!=null){
//
//				TextField tf= new TextField();
//				tf.textProperty().addListener((observable, oldValue, newValue) -> {
//					filteredData.setPredicate(object -> {
//						// If filter text is empty, display all persons.
//						if (newValue == null || newValue.isEmpty()) {
//							return true;
//						}
//
//						// Compare first name and last name of every person with filter text.
//						String lowerCaseFilter = newValue.toLowerCase();
//
//						String objectValue ="";
//						try {
//							objectValue=(String) getMethod.invoke(object);
//
//						} catch (Exception e) {	e.printStackTrace();}
//
//						if (objectValue.toLowerCase().contains(lowerCaseFilter)) {
//							return true; // Filter matches first name.
//						} 
//						return false; // Does not match.
//					});
//				});
//
//				VBox fb = new VBox();
//				fb.getChildren().addAll(new Label(name),tf);
//				filters.getChildren().add(fb);
//				System.out.println("agregando el filtro para "+getMethodName);
//			}
//
//		} catch (NoSuchMethodException e1) {
//			e1.printStackTrace();
//			//XXX el metodo es solo de tipo get
//		} catch (SecurityException e1) {
//			e1.printStackTrace();
//		}

		this.getColumns().add(column);

	}

	
	private void getJPAStringPropertyColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,
			String setMethodName) {
		String propName = name.replace("Property", "");
		TableColumn<T,String> column = new TableColumn<T,String>(propName);
		column.setEditable(true);
		column.setCellFactory(TextFieldTableCell.forTableColumn());
		column.setCellValueFactory(
				cellData ->{
					try {
						StringProperty o = (StringProperty) method.invoke(cellData.getValue(), (Object[])null);
						if(o==null){
							o=new JPAStringProperty();
							try {
								Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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
						
						o.addListener((obj,old,n)->{
							DAH.save(cellData.getValue());
						});
						return o;	
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
					Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
					setMethod.invoke(p,d);
					DAH.save(p);
					refresh();
				} catch (Exception e) {	e.printStackTrace();}
				});

		this.getColumns().add(dColumn);
	}

	private void getBooleanColumn(Class<?> clazz, Method method, String name, Class<?> fieldType, String setMethodName) {
		String propName = name.replace("Property", "");
		
		BooleanTableColumn<T> dColumn = new BooleanTableColumn<T>(propName,
				(p)->{	try {
					return ((Boolean) method.invoke(p, (Object[])null));
				} catch (Exception e) {	e.printStackTrace();}
				return null;
				},(p,d)->{ try {
					Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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
