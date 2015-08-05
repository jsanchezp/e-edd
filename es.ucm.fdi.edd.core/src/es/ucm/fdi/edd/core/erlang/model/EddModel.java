package es.ucm.fdi.edd.core.erlang.model;

public class EddModel {

	private DebugTree debugTree;
	private EddState state;
	private Integer currentQuestionIndex;
	private Integer buggyNodeIndex;
	
	public EddModel() {
		buggyNodeIndex = -1;
	}

	public DebugTree getDebugTree() {
		return debugTree;
	}

	public void setDebugTree(DebugTree debugTree) {
		this.debugTree = debugTree;
	}

	public EddState getState() {
		return state;
	}

	public void setState(EddState state) {
		this.state = state;
	}

	public Integer getCurrentQuestionIndex() {
		return currentQuestionIndex;
	}

	public void setCurrentQuestionIndex(Integer currentQuestionIndex) {
		this.currentQuestionIndex = currentQuestionIndex;
	}

	public Integer getBuggyNodeIndex() {
		return buggyNodeIndex;
	}

	public void setBuggyNodeIndex(Integer buggyNodeIndex) {
		this.buggyNodeIndex = buggyNodeIndex;
	}
}