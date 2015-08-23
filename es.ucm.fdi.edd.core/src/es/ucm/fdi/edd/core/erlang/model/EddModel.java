package es.ucm.fdi.edd.core.erlang.model;

import java.util.LinkedList;

public class EddModel {

	private DebugTree debugTree;
	private ZoomDebugTree zoomDebugTree;
	private EddState state;
	private Integer currentQuestionIndex;
	private Integer currentZoomQuestionIndex;
	private String currentZoomQuestion;
	private Integer buggyNodeIndex;
	private String buggyErrorCall;

	private LinkedList<String> answerList;

	public EddModel() {
		// buggyNodeIndex = -1;
	}

	public DebugTree getDebugTree() {
		return debugTree;
	}

	public void setDebugTree(DebugTree debugTree) {
		this.debugTree = debugTree;
	}

	public ZoomDebugTree getZoomDebugTree() {
		return zoomDebugTree;
	}

	public void setZoomDebugTree(ZoomDebugTree zoomDebugTree) {
		this.zoomDebugTree = zoomDebugTree;
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

	public Integer getCurrentZoomQuestionIndex() {
		return currentZoomQuestionIndex;
	}

	public void setCurrentZoomQuestionIndex(Integer currentZoomQuestionIndex) {
		this.currentZoomQuestionIndex = currentZoomQuestionIndex;
	}

	public String getCurrentZoomQuestion() {
		return currentZoomQuestion;
	}

	public void setCurrentZoomQuestion(String currentZoomQuestion) {
		this.currentZoomQuestion = currentZoomQuestion;
	}

	public Integer getBuggyNodeIndex() {
		return buggyNodeIndex;
	}

	public void setBuggyNodeIndex(Integer buggyNodeIndex) {
		this.buggyNodeIndex = buggyNodeIndex;
	}

	public String getBuggyErrorCall() {
		return buggyErrorCall;
	}

	public void setBuggyErrorCall(String buggyErrorCall) {
		this.buggyErrorCall = buggyErrorCall;
	}

	public LinkedList<String> getAnswerList() {
		return answerList;
	}

	public void setAnswerList(LinkedList<String> answerList) {
		this.answerList = answerList;
	}
}