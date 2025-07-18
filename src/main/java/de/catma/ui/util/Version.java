package de.catma.ui.util;

/**
 *
 * This class provides programmable semantic version information for catma
 *
 * @author db
 */
public final class Version {

    private int major;
    private int minor;
    private int patch;


    public final static Version CATMA_7_2_2 = new Version(7, 2, 2);


    public final static Version LATEST = CATMA_7_2_2;


    /**
     * no custom instances of version allowed
     */
    private Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }


    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
