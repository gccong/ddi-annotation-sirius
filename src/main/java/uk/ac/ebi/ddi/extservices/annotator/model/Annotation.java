package uk.ac.ebi.ddi.extservices.annotator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by yperez on 29/05/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Annotation {

    @JsonProperty("from")
    int fromPosition;

    @JsonProperty("hierarchySize")
    int hierarchySize;

    @JsonProperty("matchType")
    String matchType;

    @JsonProperty("text")
    String text;

    @JsonProperty("to")
    int toPosition;

    @JsonProperty("annotatedClass")
    AnnotatedClass annotatedClass;

    public int getFromPosition() {
        return fromPosition;
    }

    public void setFromPosition(int fromPosition) {
        this.fromPosition = fromPosition;
    }

    public int getHierarchySize() {
        return hierarchySize;
    }

    public void setHierarchySize(int hierarchySize) {
        this.hierarchySize = hierarchySize;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getToPosition() {
        return toPosition;
    }

    public void setToPosition(int toPosition) {
        this.toPosition = toPosition;
    }

    public AnnotatedClass getAnnotatedClass() {
        return annotatedClass;
    }
    public void setAnnotatedClass(AnnotatedClass annotatedClass) {
        this.annotatedClass = annotatedClass;
    }

    @Override
    public String toString() {
        return "Annotation{" +
                "fromPosition=" + fromPosition +
                ", hierarchySize=" + hierarchySize +
                ", matchType='" + matchType + '\'' +
                ", text='" + text + '\'' +
                ", toPosition=" + toPosition +
                ", annotatedClass=" + annotatedClass +
                '}';
    }

}
