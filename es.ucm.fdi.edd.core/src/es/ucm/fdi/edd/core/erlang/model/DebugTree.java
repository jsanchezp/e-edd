package es.ucm.fdi.edd.core.erlang.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangLong;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangString;
import com.ericsson.otp.erlang.OtpErlangTuple;

public class DebugTree {
	
	private OtpErlangTuple dbgTreeTuple;
	
	private Map<Integer, EddVertex> vertexesMap;
	private List<EddEdge> edgesMap;
	
	public DebugTree(OtpErlangTuple tuple) {
		this.dbgTreeTuple = tuple;
		
		vertexesMap = new HashMap<Integer, EddVertex>();
		edgesMap = new LinkedList<EddEdge>();
		
		initialize();
	}
	
	private void initialize() {
		processVertexes();
		processEdges();
	}

	private void processVertexes() {
		OtpErlangTuple vertextesTuple = (OtpErlangTuple) dbgTreeTuple.elementAt(0);
		if (vertextesTuple.arity() == 2) {
			OtpErlangAtom vertexesAtom = (OtpErlangAtom) vertextesTuple.elementAt(0);
//			System.out.println(vertexesAtom);
			OtpErlangList vertexesList = (OtpErlangList) vertextesTuple.elementAt(1);
			System.out.println(Arrays.toString(vertexesList.elements()));
			for (OtpErlangObject child : vertexesList) {
				if (child instanceof OtpErlangTuple) {
					OtpErlangTuple vertexTuple = (OtpErlangTuple) child;
					processVertexTuple(vertexTuple);
				}
			}
		}
		else {
			System.out.println("The 'vertexes' tuple is malformmed...");
		}
	}
	
	private void processVertexTuple(OtpErlangTuple vertexTuple) {
		if (vertexTuple.arity() == 3) {
			
			Long node = null; 
			String question = null;
			EddInfo info = null;
			
			OtpErlangTuple t1 = (OtpErlangTuple) vertexTuple.elementAt(0);
			if (t1.arity() == 2) {
				OtpErlangAtom idAtom = (OtpErlangAtom) t1.elementAt(0);
//				System.out.println(idAtom);
				OtpErlangLong nodeIndex = (OtpErlangLong) t1.elementAt(1);
				node = nodeIndex.longValue();
			}
			else {
				System.out.println("The 'edge' tuple is malformmed...");
			}
			
			OtpErlangTuple t2 = (OtpErlangTuple) vertexTuple.elementAt(1);
			if (t2.arity() == 2) {
				OtpErlangAtom questionAtom = (OtpErlangAtom) t2.elementAt(0);
//				System.out.println(questionAtom);
				OtpErlangString questionMsg = (OtpErlangString) t2.elementAt(1);
				question = questionMsg.stringValue();
			}
			else {
				System.out.println("The 'edge' tuple is malformmed...");
			}
			
			OtpErlangTuple t3 = (OtpErlangTuple) vertexTuple.elementAt(2);
			if (t2.arity() == 2) {
				OtpErlangAtom infoAtom = (OtpErlangAtom) t3.elementAt(0);
//				System.out.println(infoAtom);
				OtpErlangTuple infoTuple = (OtpErlangTuple) t3.elementAt(1);
				if (infoTuple.arity() == 4) {
					OtpErlangString questionUnformatted = (OtpErlangString) infoTuple.elementAt(0);
					OtpErlangLong clause = (OtpErlangLong) infoTuple.elementAt(1);
					OtpErlangAtom fileAtom = (OtpErlangAtom) infoTuple.elementAt(2);
					OtpErlangLong line = (OtpErlangLong) infoTuple.elementAt(3);
					
					info = new EddInfo(questionUnformatted.stringValue(), clause.longValue(), fileAtom.atomValue(), line.longValue());
				}
				else {
					System.out.println("The 'info' tuple is malformmed...");
				}
			}
			else {
				System.out.println("The 'edge' tuple is malformmed...");
			}
			
			EddVertex vertex = new EddVertex(node, question, info);
			vertexesMap.put(node.intValue(), vertex);
		}
		else {
			System.out.println("The 'vertex' tuple is malformmed...");
		}
	}

	private void processEdges() {
		OtpErlangTuple edgesTuple = (OtpErlangTuple) dbgTreeTuple.elementAt(1);
		if (edgesTuple.arity() == 2) {
			OtpErlangAtom edgesAtom = (OtpErlangAtom) edgesTuple.elementAt(0);
			System.out.println(edgesAtom);
			OtpErlangList edgesList = (OtpErlangList) edgesTuple.elementAt(1);
			System.out.println(Arrays.toString(edgesList.elements()));
			for (OtpErlangObject child : edgesList) {
				if (child instanceof OtpErlangTuple) {
					OtpErlangTuple edgeTuple = (OtpErlangTuple) child;
					if (edgeTuple.arity() == 2) {
						OtpErlangLong from = (OtpErlangLong) edgeTuple.elementAt(0);
						OtpErlangLong to = (OtpErlangLong) edgeTuple.elementAt(1);
						EddEdge edge = new EddEdge(from.longValue(), to.longValue());
						edgesMap.add(edge);
					}
					else {
						System.out.println("The 'edge' tuple is malformmed...");
					}
				}
			}
		}
		else {
			System.out.println("The 'edges' tuple is malformmed...");
		}
	}

	public Map<Integer, EddVertex> getVertexesMap() {
		return vertexesMap;
	}

	public void setVertexesMap(Map<Integer, EddVertex> vertexesMap) {
		this.vertexesMap = vertexesMap;
	}

	public List<EddEdge> getEdgesMap() {
		return edgesMap;
	}

	public void setEdgesMap(List<EddEdge> edgesMap) {
		this.edgesMap = edgesMap;
	}
}