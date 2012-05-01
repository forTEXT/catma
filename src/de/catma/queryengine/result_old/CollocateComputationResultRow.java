package de.catma.queryengine.result;

import java.util.List;

import de.catma.core.document.Range;

/**
 * A row of a {@link org.catma.queryengine.result.CollocateComputationResult}.
 *
 *
 * @author Malte Meister
 *
 */
public class CollocateComputationResultRow {
    private String text;
    private List<Range> rangeList;
    private int typeFrequency;
    private double zScore;

    /**
     * Constructor
     *
     * @param text the type
     * @param rangeList the ranges of the collocating tokens
     * @param typeFrequency the frequency of the type
     * @param zScore the z-score for the collocation
     */
    public CollocateComputationResultRow(String text, List<Range> rangeList, int typeFrequency, double zScore) {
        this.text = text;
        this.rangeList = rangeList;
        this.typeFrequency = typeFrequency;
        this.zScore = zScore;
    }

    /**
     * @return the type
     */
    public String getText() {
        return text;
    }

    /**
     * @return the number of collocating tokens
     */
    public int getTokenCount() {
        return getRangeList().size();
    }

    /**
     * @return the list of ranges of the collocating tokens
     */
    public List<Range> getRangeList() {
        return rangeList;
    }

    /**
     * @return the frequencey of the type
     */
    public int getTypeFrequency() {
        return typeFrequency;
    }

    /**
     * @return the z-score
     */
    public double getZscore() {
        return zScore;
    }
}
