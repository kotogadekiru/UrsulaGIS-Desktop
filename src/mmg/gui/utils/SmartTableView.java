package mmg.gui.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Calendar;
import java.util.function.Function;
import java.util.function.Supplier;

import dao.cosecha.CosechaItem;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;


public class SmartTableView<T> extends TableView<T> {
	private Supplier<T> onDoubleClick=null;
	
	public SmartTableView(ObservableList<T> data,ObservableList<T> filtered){
		super(data);
		
		  // 3. Wrap the FilteredList in a SortedList. 
        SortedList<T> sortedData = new SortedList<>(filtered);

        // 4. Bind the SortedList comparator to the TableView comparator.
        sortedData.comparatorProperty().bind(this.comparatorProperty());
        // 5. Add sorted (and filtered) data to the table.
        this.setItems(sortedData);
        
		if(data.size()>0){
			populateColumns(data.get(0).getClass());
		}else{
			System.out.println("no creo las columnas porque no hay datos");
		}
		
		this.setOnMouseClicked( event->{
				if ( MouseButton.PRIMARY.equals(event.getButton()) && event.getClickCount() == 2) {
		        	  if(onDoubleClick!=null){
			            	data.add(onDoubleClick.get());
			            }		            
		        } else if(MouseButton.SECONDARY.equals(event.getButton()) && event.getClickCount() == 2){
		        	T rowData = this.getSelectionModel().getSelectedItem();
		        	if(rowData!=null){
		        	data.remove(rowData);
		        	
		        	//DAH.remove(rowData);
		        	}
		        }
		    
		});
		
		data.addListener(new ListChangeListener<T>(){

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends T> c) {
			if(	getColumns().size()==0){
				
				
				populateColumns(c.getList().get(0).getClass());
			}
				
			}
			
		});

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
					//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
					String propName = name.replace("Property", "");
					TableColumn<T,String> column = new TableColumn<T,String>(propName);
					column.setEditable(true);
					column.setCellFactory(TextFieldTableCell.forTableColumn());
					column.setCellValueFactory(new PropertyValueFactory<>(propName));

					try {
						
						
						Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
						if(setMethod!=null){
							column.setOnEditCommit(cellEditingEvent -> { 
								int row = cellEditingEvent.getTablePosition().getRow();
								T p = cellEditingEvent.getTableView().getItems().get(row);
								try {
									setMethod.invoke(p,cellEditingEvent.getNewValue());
								//	DAH.save(p);
									refresh();
								} catch (Exception e) {		
									e.printStackTrace();
								}
								
							});
						}
					
					} catch (NoSuchMethodException e1) {
						//XXX el metodo es solo de tipo get
					} catch (SecurityException e1) {
						e1.printStackTrace();
					}


					this.getColumns().add(0,column);
				} else 	if(Double.class.isAssignableFrom(fieldType)){
					//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
					String propName = name.replace("Property", "");
					DoubleTableColumn<T> dColumn = new DoubleTableColumn<T>(propName,
							(p)->{
								try {
									return ((Double) method.invoke(p, (Object[])null));
								} catch (Exception e) {
									
									e.printStackTrace();
								}
								return null;
							},
							(p,d)->{
								try {
									Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
									setMethod.invoke(p,d);
								//	DAH.save(p);
									refresh();
								} catch (Exception e) {
									
									e.printStackTrace();
								}
							}
							);


					this.getColumns().add(0,dColumn);
					//this.getColumns().add(dColumn);

				}else 	if(Integer.class.isAssignableFrom(fieldType)){
					//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
					String propName = name.replace("Property", "");
					IntegerTableColumn<T> dColumn = new IntegerTableColumn<T>(propName,
							(p)->{
								try {
									return ((Integer) method.invoke(p, (Object[])null));
								} catch (Exception e) {
									
									e.printStackTrace();
								}
								return null;
							},
							(p,d)->{
								try {
									Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
									setMethod.invoke(p,d);
								//	DAH.save(p);
									refresh();
								} catch (Exception e) {
									
									e.printStackTrace();
								}
							}
							);

					this.getColumns().add(0,dColumn);
					//this.getColumns().add(dColumn);

				} else 	if(Calendar.class.isAssignableFrom(fieldType)){
					//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
					String propName = name.replace("Property", "");
					DateColumn<T> dColumn = new DateColumn<T>(propName,
							(p)->{
								try {
									return ((Calendar) method.invoke(p, null));
								} catch (Exception e) {
									
									e.printStackTrace();
								}
								return null;
							},
							(p,d)->{
								try {
									Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
									setMethod.invoke(p,d);
								//	DAH.save(p);
									refresh();
								} catch (Exception e) {
									
									e.printStackTrace();
								}
							}
							);

					this.getColumns().add(0,dColumn);
					//this.getColumns().add(dColumn);

				}
				/*
				else 	if(CosechaItem.class.isAssignableFrom(fieldType)){
					//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
					String propName = name.replace("Property", "");
					ChoiceTableColumn<T, CosechaItem> dColumn = new ChoiceTableColumn<T,CosechaItem>(propName,DAH.getAllCultivos(),
							(p)->{try {
								return ((CosechaItem) method.invoke(p, null));
							} catch (Exception e) {
								
								e.printStackTrace();
							}
							return null;},
							(p,d)->{
								try {
									Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
									setMethod.invoke(p,d);
								//	DAH.save(p);
									refresh();
								} catch (Exception e) {
									
									e.printStackTrace();
								}
							}
							);
					this.getColumns().add(dColumn);

				}else 	if(Empresa.class.isAssignableFrom(fieldType)){
					//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
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

				}else 	if(Establecimiento.class.isAssignableFrom(fieldType)){
					//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
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

				}else 	if(Lote.class.isAssignableFrom(fieldType)){
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

				}else 	if(Campania.class.isAssignableFrom(fieldType)){
					//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
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
	*/

			}//fin del if method name starts with get
			
		}
	
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
