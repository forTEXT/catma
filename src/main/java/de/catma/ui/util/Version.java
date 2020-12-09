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


    public final static Version CATMA_6_1_3 = new Version(6, 1, 3);


    public final static Version LATEST = CATMA_6_1_3;


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
