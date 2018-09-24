package de.catma.v10ui.util;

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


    public final static Version CATMA_5_0_0 = new Version(5,0,0);

    public final static Version CATMA_6_0_0 = new Version(6,0,0);


    public final static Version LATEST = CATMA_6_0_0;


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
