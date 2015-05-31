package es.ucm.fdi.edd.core.graphviz;

public class GraphvizJava {

	public static void main(String[] arg) {
		new GraphvizJava(arg[0], arg[1]);
	}

	public GraphvizJava(String direccionDot, String direccionPng) {
		dibujar(direccionDot, direccionPng);
	}

	/**
	 * Realiza la construccion del comando en la linea de comandos esto es: 
	 * 		dot -Tpng -o archivo.png archivo.dot
	 */
	public void dibujar(String direccionDot, String direccionPng) {
		try {
			ProcessBuilder pbuilder;
			pbuilder = new ProcessBuilder("dot", "-Tpng", "-o", direccionPng, direccionDot);
			pbuilder.redirectErrorStream(true);
			pbuilder.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}