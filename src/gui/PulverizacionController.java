package gui;

import dao.pulverizacion.PulverizacionLabor;
import dao.recorrida.Recorrida;
import tasks.CompartirPulverizacionLaborTask;
import tasks.CompartirRecorridaTask;
import utils.DAH;

public class PulverizacionController {
	private static final String DD_MM_YYYY = "dd/MM/yyyy";
	JFXMain main=null;

	public PulverizacionController(JFXMain _main) {
		this.main=_main;		
	}
	/**
	 *  updload recorrida to server and show url to access
	 * @param recorrida
	 */
	public void doCompartirPulverizacion(PulverizacionLabor value) {
	
	//	private void doCompartirRecorrida(Recorrida recorrida) {		
//			if(value.getUrl()!=null && value.getUrl().length()>0) {			
//				new ConfigGUI(this).showQR(recorrida.getUrl());
//				//XXX editar la recorrida remota con la informacion actualizada de la local?
//				//XXX recupero la recorrida remota?
//				return;
//			}
		CompartirPulverizacionLaborTask task = new CompartirPulverizacionLaborTask(value);			
			task.installProgressBar(main.progressBox);
			task.setOnSucceeded(handler -> {
				String ret = (String)handler.getSource().getValue();
//				value.setUrl(ret);
//				DAH.save(value);
				if(ret!=null) {
					new ConfigGUI(this.main).showQR(ret);
				}
				//XXX agregar boton de actualizar desde la nube?
				task.uninstallProgressBar();			
			});
			System.out.println("ejecutando Compartir Recorrida"); //$NON-NLS-1$
			JFXMain.executorPool.submit(task);		
	}
}
