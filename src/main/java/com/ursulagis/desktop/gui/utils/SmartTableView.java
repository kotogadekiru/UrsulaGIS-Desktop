package com.ursulagis.desktop.gui.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.persistence.Entity;
import javax.persistence.Transient;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableFilter.Builder;
import org.geotools.api.feature.simple.SimpleFeature;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.Agroquimico;
import com.ursulagis.desktop.dao.config.Campania;
import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.config.Empresa;
import com.ursulagis.desktop.dao.config.Establecimiento;
import com.ursulagis.desktop.dao.config.Fertilizante;
import com.ursulagis.desktop.dao.config.Lote;
import com.ursulagis.desktop.dao.config.Semilla;
import com.ursulagis.desktop.dao.ordenCompra.Producto;
import com.ursulagis.desktop.dao.utils.JPAStringProperty;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ResizeFeaturesBase;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import com.ursulagis.desktop.utils.DAH;
import com.ursulagis.desktop.utils.ExcelHelper;



public class SmartTableView<T> extends TableView<T> {
	private Supplier<T> onDoubleClick=null;
	private Consumer<T> onShowClick=null;

	private Consumer<List<T>> eliminarAction = list->DAH.removeAll((List<Object>) list);
	private Map<MenuItem,Consumer<T>> consumerMap=new HashMap<>();
	private List<String> rejectedColumns=new ArrayList<>();
	private List<String> orderColumns=new ArrayList<>();
	private  Map<String,String>  namesColumnsMap=null;
	private boolean permiteEliminar=true;
	
	//	public HBox filters = new HBox();
	//	private FilteredList<T> filteredData=null;
	//i18n FilterTable Strings
	//"filterpanel.search.field"
	//"filterpanel.apply.button"
	//"filterpanel.none.button"
	//"filterpanel.all.button"
	//"filterpanel.resetall.button"
	public SmartTableView(ObservableList<T> data,
			List<String> rejectedColumns,List<String> order){
		super(data);
		this.rejectedColumns.addAll(rejectedColumns);
		this.orderColumns.addAll(order);
		construct(data);

	}

	public SmartTableView(ObservableList<T> data,
			List<String> rejectedColumns,List<String> order,List<String> names){
		super(data);
		this.rejectedColumns.addAll(rejectedColumns);
		this.orderColumns.addAll(order);
		this.namesColumnsMap=new HashMap<>();
		for(int i=0;i<order.size();i++) {			
			namesColumnsMap.put(order.get(i), names.get(i));
		}
		construct(data);
	}
	
	public SmartTableView(ObservableList<T> data){//,ObservableList<T> observable){
		super(data);
		construct(data);
	}

	public void toExcel() {
		Platform.runLater(()->{
			//implementar exportar a excell 
			ExcelHelper xHelper = new ExcelHelper();
			Map<String, Object[]> data=new TreeMap<String,Object[]>();//HashMap no mantiene el orden
			List<Object> itemData = new ArrayList<Object>();


			Integer row = new Integer(1);//excel empieza a contar desde 1 duh!
			//headers
			for(TableColumn<?, ?> col:this.getColumns()) {		
				Object cellData = col.getText();
				itemData.add(cellData);
			}
			data.put("0",itemData.toArray());
			//row++;
			itemData.clear();

			for(T item :  this.getItems()) {			

				for(TableColumn col:this.getColumns()) {		
					Object cellData = col.getCellData(item);
					itemData.add(cellData);
				}
				data.put(row.toString(),itemData.toArray());
				row++;
				itemData.clear();
			}

			xHelper.exportData(null, data);
		});
	}

	private void construct(ObservableList<T> data) {
		this.rejectedColumns.add("Class");
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

		//	Map<String,Consumer<T>> consumerMap = new HashMap<String,Consumer<T>>();

		ContextMenu contextMenu = new ContextMenu();
		MenuItem mostrarItem = new MenuItem(Messages.getString("SmartTableView.Cargar"));//"Cargar"
		MenuItem eliminarItem = new MenuItem(Messages.getString("SmartTableView.Eliminar"));//Eliminar
		MenuItem activarItem = new MenuItem(Messages.getString("SmartTableView.Activar"));//Activar
		

		//Map<MenuItem,Consumer<T>> mIMap = new HashMap<MenuItem,Consumer<T>>();
		this.needsLayoutProperty()
        .addListener((obs, o, n) -> TableUtils.setDataTableMinColumnWidth(this));
		
		this.setContextMenu(contextMenu);

		this.setOnMouseClicked( event->{
			contextMenu.getItems().clear();
			List<T> rowData = this.getSelectionModel().getSelectedItems();

			if(rowData != null && rowData.size()>0 ){
				if(onShowClick!=null) contextMenu.getItems().add(mostrarItem);
				if(permiteEliminar)   contextMenu.getItems().add(eliminarItem);
				
				if ( MouseButton.PRIMARY.equals(event.getButton()) && event.getClickCount() == 2) {
					if(onDoubleClick!=null){
						data.add(onDoubleClick.get());
					}		            
				} 
				else if(MouseButton.SECONDARY.equals(event.getButton()) && event.getClickCount() == 1){

					consumerMap.keySet().stream().forEach(mi->{
						contextMenu.getItems().add(mi);
						mi.setOnAction((ev)->{
							Platform.runLater(() ->	{rowData.forEach(consumerMap.get(mi));
													refresh();
							});
						});
					});


					mostrarItem.setOnAction((ev)->{
						Platform.runLater(()->	rowData.forEach(onShowClick));
						//onShowClick.accept(rowData);
					});

					eliminarItem.setOnAction((aev)->{						
						Alert alert = new Alert(AlertType.CONFIRMATION);
						Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();

						//stage.getIcons().addAll(JFXMain.stage.getIcons());
						stage.getIcons().addAll(JFXMain.stage.getIcons());
						alert.setTitle(Messages.getString("SmartTableView.BorrarRegistro"));//"Borrar registro"
						alert.setHeaderText(Messages.getString("SmartTableView.BorrarRegistroWarning"));//"Esta accion borrara permanentemente el registro. Desea Continuar?");
						Optional<ButtonType> res = alert.showAndWait();
						if(res.get().equals(ButtonType.OK) && rowData!=null){
							try{								
								this.eliminarAction.accept((List<T>) rowData);							
								data.removeAll(rowData);
								if(data.size()==0){
									data.add(onDoubleClick.get());
								}
								refresh();
							}catch(Exception e){
								Alert eliminarFailAlert = new Alert(AlertType.ERROR);
								((Stage) eliminarFailAlert.getDialogPane().getScene().getWindow()).
								getIcons().addAll(JFXMain.stage.getIcons());
								eliminarFailAlert.setTitle(Messages.getString("SmartTableView.BorrarRegistro"));//"Borrar registro");
								eliminarFailAlert.setHeaderText(Messages.getString("SmartTableView.BorrarRegistroError"));//"No se pudo borrar el registro");
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
	
	static class TableUtils{
	
    static void doSetWidth(TableColumnBase column, double width) {
    	double nWidth =	Math.min(
								column.getMaxWidth(), 
								Math.max(width, column.getMinWidth())
								);
    	//column.setMinWidth(500);//funciona
    	//column.setPrefWidth(500);//funciona
    	column.setPrefWidth(nWidth);
    	//column.widthProperty().subtract(column.getWidth()).add(nWidth);    		
    }
    
	private static double resizeColumns(List<? extends TableColumnBase<?,?>> columns, double delta) {
        // distribute space between all visible children who can be resized.
        // To do this we need to work out if we're shrinking or growing the
        // children, and then which children can be resized based on their
        // min/pref/max/fixed properties. The results of this are in the
        // resizingChildren observableArrayList above.
        final int columnCount = columns.size();

        // work out how much of the delta we should give to each child. It should
        // be an equal amount (at present), although perhaps we'll allow for
        // functions to calculate this at a later date.
        double colDelta = delta / columnCount;

        // we maintain a count of the amount of delta remaining to ensure that
        // the column resize operation accurately reflects the location of the
        // mouse pointer. Every time this value is not 0, the UI is a teeny bit
        // more inaccurate whilst the user continues to resize.
        double remainingDelta = delta;

        // We maintain a count of the current column that we're on in case we
        // need to redistribute the remainingDelta among remaining sibling.
        int col = 0;

        // This is a bit hacky - often times the leftOverDelta is zero, but
        // remainingDelta doesn't quite get down to 0. In these instances we
        // short-circuit and just return 0.0.
        boolean isClean = true;
        for (TableColumnBase<?,?> childCol : columns) {
            col++;

            // resize each child column
            double leftOverDelta = resize(childCol, colDelta);

            // calculate the remaining delta if the was anything left over in
            // the last resize operation
            remainingDelta = remainingDelta - colDelta + leftOverDelta;

            //      println("\tResized {childCol.text} with {colDelta}, but {leftOverDelta} was left over. RemainingDelta is now {remainingDelta}");

            if (leftOverDelta != 0) {
                isClean = false;
                // and recalculate the distribution of the remaining delta for
                // the remaining siblings.
                colDelta = remainingDelta / (columnCount - col);
            }
        }

        // see isClean above for why this is done
        return isClean ? 0.0 : remainingDelta;
    }
	
	 // function used to actually perform the resizing of the given column,
    // whilst ensuring it stays within the min and max bounds set on the column.
    // Returns the remaining delta if it could not all be applied.
    static double resize(TableColumnBase column, double delta) {
        if (delta == 0) return 0.0F;
        if (! column.isResizable()) return delta;

        final boolean isShrinking = delta < 0;
        final List<TableColumnBase<?,?>> resizingChildren = getResizableChildren(column, isShrinking);

        if (resizingChildren.size() > 0) {
            return resizeColumns(resizingChildren, delta);
        } else {
            double newWidth = column.getWidth() + delta;

            if (newWidth > column.getMaxWidth()) {
                doSetWidth(column,column.getMaxWidth());
                return newWidth - column.getMaxWidth();
            } else if (newWidth < column.getMinWidth()) {
                doSetWidth(column,column.getMinWidth());
                return newWidth - column.getMinWidth();
            } else {
                doSetWidth(column,newWidth);
                return 0.0F;
            }
        }
    }

    // Returns all children columns of the given column that are able to be
    // resized. This is based on whether they are visible, resizable, and have
    // not space before they hit the min / max values.
    private static List<TableColumnBase<?,?>> getResizableChildren(TableColumnBase<?,?> column, boolean isShrinking) {
        if (column == null || column.getColumns().isEmpty()) {
            return Collections.emptyList();
        }

        List<TableColumnBase<?,?>> tablecolumns = new ArrayList<TableColumnBase<?,?>>();
        for (TableColumnBase c : column.getColumns()) {
            if (! c.isVisible()) continue;
            if (! c.isResizable()) continue;

            if (isShrinking && c.getWidth() > c.getMinWidth()) {
                tablecolumns.add(c);
            } else if (!isShrinking && c.getWidth() < c.getMaxWidth()) {
                tablecolumns.add(c);
            }
        }
        return tablecolumns;
    }

	static boolean constrainedResize(
									ResizeFeaturesBase prop,
									boolean isFirstRun,
									double tableWidth,
									List<? extends TableColumnBase<?,?>> visibleLeafColumns) {
		TableColumnBase<?,?> column = prop.getColumn();
		double delta = prop.getDelta();

		/*
		 * There are two phases to the constrained resize policy:
		 *   1) Ensuring internal consistency (i.e. table width == sum of all visible
		 *      columns width). This is often called when the table is resized.
		 *   2) Resizing the given column by __up to__ the given delta.
		 *
		 * It is possible that phase 1 occur and there be no need for phase 2 to
		 * occur.
		 */

		boolean isShrinking;
		double target;
		double totalLowerBound = 0;
		double totalUpperBound = 0;

		if (tableWidth == 0) return false;

		/*
		 * PHASE 1: Check to ensure we have internal consistency. Based on the
		 *          Swing JTable implementation.
		 */
		// determine the width of all visible columns, and their preferred width
		double colWidth = 0;
		for (TableColumnBase<?,?> col : visibleLeafColumns) {
			colWidth += col.getWidth();
		}

		if (Math.abs(colWidth - tableWidth) > 1) {
			isShrinking = colWidth > tableWidth;
			target = tableWidth;

			if (isFirstRun) {
				// if we are here we have an inconsistency - these two values should be
				// equal when this resizing policy is being used.
				for (TableColumnBase<?,?> col : visibleLeafColumns) {
					totalLowerBound += col.getMinWidth();
					totalUpperBound += col.getMaxWidth();
				}

				// We run into trouble if the numbers are set to infinity later on
				totalUpperBound = totalUpperBound == Double.POSITIVE_INFINITY ?
						Double.MAX_VALUE :
							(totalUpperBound == Double.NEGATIVE_INFINITY ? Double.MIN_VALUE : totalUpperBound);

				for (TableColumnBase col : visibleLeafColumns) {
					double lowerBound = col.getMinWidth();
					double upperBound = col.getMaxWidth();

					// Check for zero. This happens when the distribution of the delta
					// finishes early due to a series of "fixed" entries at the end.
					// In this case, lowerBound == upperBound, for all subsequent terms.
					double newSize;
					if (Math.abs(totalLowerBound - totalUpperBound) < .0000001) {
						newSize = lowerBound;
					} else {
						double f = (target - totalLowerBound) / (totalUpperBound - totalLowerBound);
						newSize = Math.round(lowerBound + f * (upperBound - lowerBound));
					}

					double remainder = resize(col, newSize - col.getWidth());

					target -= newSize + remainder;
					totalLowerBound -= lowerBound;
					totalUpperBound -= upperBound;
				}

				isFirstRun = false;
			} else {
				double actualDelta = tableWidth - colWidth;
				List<? extends TableColumnBase<?,?>> cols = visibleLeafColumns;
				resizeColumns(cols, actualDelta);
			}
		}

		// At this point we can be happy in the knowledge that we have internal
		// consistency, i.e. table width == sum of the width of all visible
		// leaf columns.

		/*
		 * Column may be null if we just changed the resize policy, and we
		 * just wanted to enforce internal consistency, as mentioned above.
		 */
		if (column == null) {
			return false;
		}

		/*
		 * PHASE 2: Handling actual column resizing (by the user). Based on my own
		 *          implementation (based on the UX spec).
		 */

		isShrinking = delta < 0;

		// need to find the last leaf column of the given column - it is this
		// column that we actually resize from. If this column is a leaf, then we
		// use it.
		TableColumnBase<?,?> leafColumn = column;
		while (leafColumn.getColumns().size() > 0) {
			leafColumn = leafColumn.getColumns().get(leafColumn.getColumns().size() - 1);
		}

		int colPos = visibleLeafColumns.indexOf(leafColumn);
		int endColPos = visibleLeafColumns.size() - 1;

		// we now can split the observableArrayList into two subobservableArrayLists, representing all
		// columns that should grow, and all columns that should shrink
		//    var growingCols = if (isShrinking)
		//        then table.visibleLeafColumns[colPos+1..endColPos]
		//        else table.visibleLeafColumns[0..colPos];
		//    var shrinkingCols = if (isShrinking)
		//        then table.visibleLeafColumns[0..colPos]
		//        else table.visibleLeafColumns[colPos+1..endColPos];


		double remainingDelta = delta;
		while (endColPos > colPos && remainingDelta != 0) {
			TableColumnBase<?,?> resizingCol = visibleLeafColumns.get(endColPos);
			endColPos--;

			// if the column width is fixed, break out and try the next column
			if (! resizingCol.isResizable()) continue;

			// for convenience we discern between the shrinking and growing columns
			TableColumnBase<?,?> shrinkingCol = isShrinking ? leafColumn : resizingCol;
			TableColumnBase<?,?> growingCol = !isShrinking ? leafColumn : resizingCol;

			//        (shrinkingCol.width == shrinkingCol.minWidth) or (growingCol.width == growingCol.maxWidth)

			if (growingCol.getWidth() > growingCol.getPrefWidth()) {
				// growingCol is willing to be generous in this case - it goes
				// off to find a potentially better candidate to grow
				List<? extends TableColumnBase> seq = visibleLeafColumns.subList(colPos + 1, endColPos + 1);
				for (int i = seq.size() - 1; i >= 0; i--) {
					TableColumnBase<?,?> c = seq.get(i);
					if (c.getWidth() < c.getPrefWidth()) {
						growingCol = c;
						break;
					}
				}
			}
			//
			//        if (shrinkingCol.width < shrinkingCol.prefWidth) {
			//            for (c in reverse table.visibleLeafColumns[colPos+1..endColPos]) {
			//                if (c.width > c.prefWidth) {
			//                    shrinkingCol = c;
			//                    break;
			//                }
			//            }
			//        }



			double sdiff = Math.min(Math.abs(remainingDelta), shrinkingCol.getWidth() - shrinkingCol.getMinWidth());

			//System.out.println("\tshrinking " + shrinkingCol.getText() + " and growing " + growingCol.getText());
			//System.out.println("\t\tMath.min(Math.abs("+remainingDelta+"), "+shrinkingCol.getWidth()+" - "+shrinkingCol.getMinWidth()+") = " + sdiff);

			double delta1 = resize(shrinkingCol, -sdiff);
			double delta2 = resize(growingCol, sdiff);
			remainingDelta += isShrinking ? sdiff : -sdiff;
		}
		return remainingDelta == 0;
	}

		public static void setDataTableMinColumnWidth(TableView<?> dataTable){
			for (Node columnHeader : dataTable.lookupAll(".column-header"))	{
				String columnString = columnHeader.getId();
				if (columnString != null)
				{
					for (Node columnHeaderLabel : columnHeader.lookupAll(".label"))
					{
						Optional<?> tableColumn = dataTable.getColumns()
								.stream()
								.filter(x -> x.getId()
										.equals(columnString))
								.findFirst();
						if (columnHeaderLabel instanceof Label && tableColumn.isPresent())
						{
							Label label = (Label) columnHeaderLabel;
							/* calc text width based on font */
							Text theText = new Text(label.getText());
							theText.setFont(label.getFont());
							double width = theText.getBoundsInLocal().getWidth();
							/*
							 * add 10px because of paddings/margins for the button
							 */
							//((TableColumn)tableColumn.get()).setMinWidth(width + 10);
							doSetWidth((TableColumn)tableColumn.get(), width+30);						
						}
					}
				}
			}
		}
	}//end of TableUtils
	
	/**
	 * 
	 * @param method
	 * @return the name of the Method without the get set or is
	 */
	private String getMethodName(Method method) {
		String name = method.getName();

		if(name.startsWith("get")) {
			name = name.substring("get".length(), name.length());//.replace("is", "");
		}
		if(name.startsWith("set")) {
			name = name.substring("set".length(), name.length());//.replace("is", "");
		}
		if(name.startsWith("is")) {
			name = name.substring("is".length(), name.length());//.replace("is", "");
		}

		return name;

	}

	private void populateColumns(Class<?> clazz) {
		Method[] methods = clazz.getMethods();//ok esto me trae todos los metodos heredados
		List<Method> methodList = Arrays.asList(methods);

		methodList.sort((a,b)->{

			String nameA = getMethodName(a);//.getName();
			String nameB =  getMethodName(b);//b.getName();
			if(orderColumns.contains(nameA)&&orderColumns.contains(nameB)) {
				return Integer.compare(orderColumns.indexOf(nameA),orderColumns.indexOf(nameB));
			} else 	if(orderColumns.contains(nameA)&&!orderColumns.contains(nameB)) {
				return -1;
			}  else if(!orderColumns.contains(nameA)&&orderColumns.contains(nameB)) {
				return 1;
			} 
			return nameA.compareToIgnoreCase(nameB);
		});
		

		for (Method method :  methodList) {
			int mods = method.getModifiers();
			boolean transiente = method.isAnnotationPresent(Transient.class);
			if(Modifier.isStatic(mods) || Modifier.isAbstract(mods)||transiente){
				continue;
			}
			String name = method.getName();
			if(name.startsWith("get")||name.startsWith("is")){//solo tomo los get
				Class<?> fieldType = method.getReturnType();
				name = getMethodName(method);
				String setMethodName = "set"+name;
			
				if(this.rejectedColumns.contains(name)) {continue;}
				if(this.namesColumnsMap!=null) {
					if(namesColumnsMap.containsKey(name)) {
						name = namesColumnsMap.get(name);
					}else {
						System.out.println("el map no contiene "+name);
						 {continue;}
					}
				}
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
				} else if(Semilla.class.isAssignableFrom(fieldType)){
					getSemillaColumn(clazz, method, name, fieldType, setMethodName);
				} else if(Ndvi.class.isAssignableFrom(fieldType)){
					getNdviColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Agroquimico.class.isAssignableFrom(fieldType)){
					getAgroquimicoColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Fertilizante.class.isAssignableFrom(fieldType)){
					getFertilizanteColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Poligono.class.isAssignableFrom(fieldType)){
					getPoligonoColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Producto.class.isAssignableFrom(fieldType)){
					getProductoColumn(clazz, method, name, fieldType, setMethodName);
				}
				//				else {//no quiero que muestre los metodos class ni id
				//					getStringColumn(clazz,method, name, fieldType, setMethodName);
				//				}


			}//fin del if method name starts with get

		}//fin del method list
		this.getColumns().stream().forEach(c->{
			javafx.scene.layout.StackPane graphic = (StackPane) c.getGraphic();
			double maxWidth =0;
			if(graphic==null)return;
			for(Node child:graphic.getChildren()) {
				if(child instanceof Label) {
					javafx.scene.control.Label l = (Label) child;
					l.setWrapText(false);
					maxWidth=Math.max(maxWidth, l.getWidth());
				}
				//System.out.println(child.getClass().getName());//javafx.scene.Parent$2
			}
			//c.setPrefWidth(maxWidth+20);
		});
		//this.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

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
		dColumn.setId(propName);
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
						// Platform.runLater(() -> {
						 	DAH.save(p);
						// });					
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		dColumn.setId(propName);
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
		dColumn.setId(propName);
		this.getColumns().add(dColumn);
	}
	
	private void getSemillaColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Semilla> dColumn = new ChoiceTableColumn<T,Semilla>(propName,DAH.getAllSemillas(),
				(p)->{try {
					return ((Semilla) method.invoke(p, (Object[])null));
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
		dColumn.setId(propName);
		this.getColumns().add(dColumn);
	}

	private void getNdviColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,String setMethodName) {

		String propName = name.replace("Property", "");
		DoubleTableColumn<T> dColumn = new DoubleTableColumn<T>(propName,
				(p)->{	try {
					return ((Ndvi) method.invoke(p, (Object[])null)).getMeanNDVI();
				} catch (Exception e) {	e.printStackTrace();}
				return null;
				},(p,d)->{ 
					//try {
					//Method setMethod = clazz.getMethod(setMethodName, fieldType);
					//setMethod.invoke(p,d);
					//DAH.save(p);
					//refresh();
					//} catch (Exception e) {	e.printStackTrace();}
				});
		dColumn.setEditable(false);
		dColumn.setId(propName);
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
		dColumn.setId(propName);
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
		dColumn.setId(propName);
		this.getColumns().add(dColumn);
	}


	private void getAgroquimicoColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Agroquimico> dColumn = new ChoiceTableColumn<T,Agroquimico>(propName,DAH.getAgroquimicosActivos(),
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
						//DAH.save(p);
						refresh();
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		dColumn.setId(propName);
		this.getColumns().add(dColumn);
	}
	private void getAgroquimicoColumnOld(Class<?> clazz, Method method, String name, Class<?> fieldType,
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
						//System.out.println("setting "+p+" en "+d);
						//DAH.save(p);
						refresh();
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		dColumn.setId(propName);
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
		dColumn.setId(propName);
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
		dColumn.setId(propName);
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
		dColumn.setId(propName);
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
		dColumn.setId(propName);
		this.getColumns().add(dColumn);
	}


	private void getStringColumn(Class<?> clazz,Method getMethod, String name, Class<?> fieldType, String setMethodName) {
		//System.out.println("Obteniendo stringColumn para "+name);
		//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
		String propName = name.replace("Property", "");
		TableColumn<T,String> column = new TableColumn<T,String>(propName);
		column.setId(propName);
	
		column.setEditable(true);
		column.setCellFactory(TextFieldTableCell.forTableColumn());
		column.setCellValueFactory(//new PropertyValueFactory<>(propName)
				cellData ->{
					String stringValue = null;
					try{
						stringValue =(String)  getMethod.invoke(cellData.getValue(), (Object[])null);
						return new SimpleStringProperty(stringValue);	
					}catch(Exception e){
						//System.out.println("La creacion de SimpleStringProperty en getStringColumn "+name +" con valor: "+stringValue);

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


/**
 * String table column para objetos de tipo Producto
 * @param clazz
 * @param getMethod
 * @param name
 * @param fieldType
 * @param setMethodName
 */
	private void getProductoColumn(Class<?> clazz,Method getMethod, String name, Class<?> fieldType, String setMethodName) {
		//System.out.println("Obteniendo stringColumn para "+name);
		//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
		String propName = name.replace("Property", "");
		TableColumn<T,String> column = new TableColumn<T,String>(propName);
		column.setEditable(false);
		column.setId(propName);
		column.setCellFactory(TextFieldTableCell.forTableColumn());
		column.setCellValueFactory(
				cellData ->{
					String stringValue = null;
					try{
						stringValue =((Producto)  getMethod.invoke(cellData.getValue(), (Object[])null)).getNombre();
						return new SimpleStringProperty(stringValue);	
					}catch(Exception e){
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
						((Producto)  getMethod.invoke(p, (Object[])null)).setNombre(cellEditingEvent.getNewValue());
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
		column.setId(propName);
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

//	private void getDoubleColumn(Class<?> clazz, Method method, String name, Class<?> fieldType, String setMethodName) {
//		String propName = name.replace("Property", "");
//		DoubleTableColumn<T> dColumn = new DoubleTableColumn<T>(propName,
//				(p)->{	try {
//					return ((Double) method.invoke(p, (Object[])null));
//				} catch (Exception e) {	e.printStackTrace();}
//				return null;
//				},(p,d)->{ try {
//					Method setMethod = clazz.getMethod(setMethodName, fieldType);
//					setMethod.invoke(p,d);
//					DAH.save(p);
//					refresh();
//				} catch (Exception e) {	e.printStackTrace();}
//				});
//		dColumn.setId(propName);
//		this.getColumns().add(dColumn);
//	}

	private void getNumberColumn(Class<?> clazz, Method method, String name, Class<?> fieldType, String setMethodName) {
		String propName = name.replace("Property", "");
		DoubleTableColumn<T> dColumn = new DoubleTableColumn<T>(propName,
				(p)->{	try {
					//Class gui.utils.SmartTableView can not access a member of class dao.suelo.Suelo$1 with modifiers "public"
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
					if (clazz.getAnnotation(Entity.class) != null) {								
						DAH.save(p);
					}					
					refresh();
				} catch (Exception e) {	e.printStackTrace();}
				});
		dColumn.setId(propName);
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
		dColumn.setId(propName);
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
		dColumn.setId(propName);
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
		dColumn.setId(propName);
		this.getColumns().add(dColumn);
	}

	/**
	 * @param onDoubleClick the onDoubleClick to set
	 */
	public void setOnShowClick(Consumer<T> onShowClick) {
		this.onShowClick = onShowClick;
	}

	public void addSecondaryClickConsumer(String localizedName, Consumer<T> consumer) {
		this.consumerMap.put(new MenuItem(localizedName), consumer);
	}

	public void setPermiteEliminar(boolean b) {
		this.permiteEliminar=b;
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


	/**
	 * @param eliminarAction Consumer que se ocupa de eliminar el objeto deseado. 
	 */
	public void setEliminarAction(Consumer<List<T>> eliminarAction) {
		this.eliminarAction = eliminarAction;
	}

	
	public void refresh() { 
		//Wierd JavaFX bug 
		ObservableList<T> data = this.getItems();
		this.setItems(null); 
		this.layout(); 
		this.setItems(data); 
	}

	public static void showLaborTable(Labor<?> labor) {
		Platform.runLater(()->{

			ArrayList<LaborItem> liLista = new ArrayList<LaborItem>();
			System.out.println("Comenzando a cargar la los datos de la tabla"); //$NON-NLS-1$
			Iterator<?> it = labor.outCollection.iterator();
			while(it.hasNext()){
				LaborItem lI = labor.constructFeatureContainerStandar((SimpleFeature)it.next(), false);
				liLista.add(lI);
			}

			final ObservableList<LaborItem> dataLotes =	FXCollections.observableArrayList(liLista);

			SmartTableView<LaborItem> table = new SmartTableView<LaborItem>(dataLotes);
			table.setEditable(false);
			//Button toExcel = new Button("To Excel");
			Button exportButton = new Button(Messages.getString("CosechaHistoChart.16")); //"Exportar"
			exportButton.setOnAction(a->{
				table.toExcel();
			});
			BorderPane bottom = new BorderPane();
			bottom.setRight(exportButton);
			VBox.setVgrow(table, Priority.ALWAYS);
			VBox vBox = new VBox(table,bottom);
			Scene scene = new Scene(vBox, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(labor.getNombre());
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});

	}


}
